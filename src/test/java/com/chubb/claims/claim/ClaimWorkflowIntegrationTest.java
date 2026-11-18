package com.chubb.claims.claim;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chubb.claims.claim.domain.ClaimRecord;
import com.chubb.claims.claim.domain.ClaimRepository;
import com.chubb.claims.notification.StatusOutboxRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = {
        "claims.demo-users.claimant-password=claimant-test-password",
        "claims.demo-users.staff-password=staff-test-password",
        "claims.demo-users.manager-password=manager-test-password",
        "claims.notifications.kafka.enabled=false"
})
@AutoConfigureMockMvc
class ClaimWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClaimRepository claims;

    @Autowired
    private StatusOutboxRepository outbox;

    @BeforeEach
    void cleanDatabase() {
        outbox.deleteAll();
        claims.deleteAll();
    }

    @Test
    void claimCanMoveThroughInformationRequestAndSettlement() throws Exception {
        MvcResult submitted = mockMvc.perform(post("/api/v1/claims")
                        .with(httpBasic("claimant", "claimant-test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentDescription": "A damaged fence",
                                  "incidentDate": "%s",
                                  "claimedAmount": 250.00,
                                  "currency": "USD",
                                  "notificationChannel": "EMAIL",
                                  "notificationAddress": "claimant@example.com"
                                }
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SUBMITTED")))
                .andReturn();

        JsonNode submittedBody = objectMapper.readTree(submitted.getResponse().getContentAsString());
        UUID claimId = UUID.fromString(submittedBody.get("id").asText());
        org.junit.jupiter.api.Assertions.assertEquals(0, outbox.countByPublishedAtIsNull());

        String claimPath = "/api/v1/staff/claims/" + claimId;
        mockMvc.perform(post(claimPath + "/pickup")
                        .with(httpBasic("staff", "staff-test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_REVIEW")));

        mockMvc.perform(post(claimPath + "/information-requests")
                        .with(httpBasic("staff", "staff-test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"Please provide a repair estimate."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("AWAITING_INFORMATION")));

        mockMvc.perform(post("/api/v1/claims/" + claimId + "/information-responses")
                        .with(httpBasic("claimant", "claimant-test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"information":"The estimate is 250 USD."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_REVIEW")));

        mockMvc.perform(post(claimPath + "/assessment")
                        .with(httpBasic("staff", "staff-test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"recommendation":"SETTLE","notes":"Estimate reviewed."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_MANAGER_DECISION")));

        mockMvc.perform(post("/api/v1/manager/claims/" + claimId + "/decision")
                        .with(httpBasic("manager", "manager-test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"decision":"SETTLE","reason":"Approved","settlementAmount":250.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SETTLEMENT_IN_PROGRESS")));

        mockMvc.perform(post(claimPath + "/settlement-completion")
                        .with(httpBasic("staff", "staff-test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SETTLED")));

        org.junit.jupiter.api.Assertions.assertEquals(6, outbox.countByPublishedAtIsNull());
        mockMvc.perform(get("/api/v1/claims/" + claimId)
                        .with(httpBasic("claimant", "claimant-test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SETTLED")))
                .andExpect(jsonPath("$.assessmentNotes").doesNotExist());
    }

    @Test
    void claimantCannotReadAnotherClaimOrUseStaffEndpoints() throws Exception {
        ClaimRecord anotherClaim = claims.save(new ClaimRecord(
                "another-claimant",
                "Private incident",
                LocalDate.now(),
                new BigDecimal("125.00"),
                "USD",
                com.chubb.claims.claim.domain.NotificationChannel.EMAIL,
                "other@example.com"));

        mockMvc.perform(get("/api/v1/claims/" + anotherClaim.getId())
                        .with(httpBasic("claimant", "claimant-test-password")))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/staff/work-queue")
                        .with(httpBasic("claimant", "claimant-test-password")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/staff/claims/" + anotherClaim.getId())
                        .with(httpBasic("staff", "staff-test-password")))
                .andExpect(status().isForbidden());
    }
}
