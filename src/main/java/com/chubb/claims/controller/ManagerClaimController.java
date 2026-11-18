package com.chubb.claims.controller;

import static com.chubb.claims.claim.api.ClaimDtos.*;

import com.chubb.claims.claim.application.ClaimService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager")
@PreAuthorize("hasRole('MANAGER')")
@SecurityRequirement(name = "basicAuth")
public class ManagerClaimController {

    private final ClaimService claims;

    public ManagerClaimController(ClaimService claims) {
        this.claims = claims;
    }

    @GetMapping("/dashboard")
    public ManagerDashboard dashboard() {
        return claims.managerDashboard();
    }

    @GetMapping("/claims/{claimId}")
    public InternalClaimResponse getClaim(@PathVariable UUID claimId) {
        return claims.getInternalClaim(claimId);
    }

    @PostMapping("/claims/{claimId}/decision")
    public InternalClaimResponse decide(
            @PathVariable UUID claimId,
            @Valid @RequestBody ManagerDecisionRequest request,
            Principal principal) {
        return claims.decide(
                claimId,
                principal.getName(),
                request.decision(),
                request.reason(),
                request.settlementAmount());
    }
}
