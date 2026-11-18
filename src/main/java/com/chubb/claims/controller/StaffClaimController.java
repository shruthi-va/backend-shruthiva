package com.chubb.claims.controller;

import static com.chubb.claims.claim.api.ClaimDtos.*;

import com.chubb.claims.claim.application.ClaimService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/staff")
@SecurityRequirement(name = "basicAuth")
public class StaffClaimController {

    private final ClaimService claims;

    public StaffClaimController(ClaimService claims) {
        this.claims = claims;
    }

    @GetMapping("/work-queue")
    @PreAuthorize("hasRole('CLAIMS_STAFF')")
    public List<InternalClaimResponse> workQueue(Principal principal) {
        return claims.getStaffWorkQueue(principal.getName());
    }

    @GetMapping("/claims/{claimId}")
    @PreAuthorize("hasRole('CLAIMS_STAFF')")
    public InternalClaimResponse getClaim(@PathVariable UUID claimId, Principal principal) {
        return claims.getStaffClaim(claimId, principal.getName());
    }

    @PostMapping("/claims/{claimId}/pickup")
    @PreAuthorize("hasRole('CLAIMS_STAFF')")
    public InternalClaimResponse pickUp(@PathVariable UUID claimId, Principal principal) {
        return claims.pickUp(claimId, principal.getName());
    }

    @PostMapping("/claims/{claimId}/information-requests")
    @PreAuthorize("hasRole('CLAIMS_STAFF')")
    public InternalClaimResponse requestInformation(
            @PathVariable UUID claimId,
            @Valid @RequestBody InformationRequest request,
            Principal principal) {
        return claims.requestInformation(claimId, principal.getName(), request.message());
    }

    @PostMapping("/claims/{claimId}/assessment")
    @PreAuthorize("hasRole('CLAIMS_STAFF')")
    public InternalClaimResponse assess(
            @PathVariable UUID claimId,
            @Valid @RequestBody AssessmentRequest request,
            Principal principal) {
        return claims.assess(
                claimId, principal.getName(), request.recommendation(), request.notes());
    }

    @PostMapping("/claims/{claimId}/settlement-completion")
    @PreAuthorize("hasRole('CLAIMS_STAFF')")
    public InternalClaimResponse completeSettlement(
            @PathVariable UUID claimId, Principal principal) {
        return claims.completeSettlement(claimId, principal.getName());
    }

    @GetMapping("/team-workload")
    @PreAuthorize("hasAnyRole('CLAIMS_STAFF', 'MANAGER')")
    public List<StaffWorkload> teamWorkload() {
        return claims.teamWorkload();
    }
}
