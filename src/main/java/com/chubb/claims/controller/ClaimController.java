package com.chubb.claims.controller;

import static com.chubb.claims.claim.api.ClaimDtos.*;

import com.chubb.claims.claim.application.ClaimService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/claims")
@SecurityRequirement(name = "basicAuth")
public class ClaimController {

    private final ClaimService claims;

    public ClaimController(ClaimService claims) {
        this.claims = claims;
    }

    @PostMapping
    @PreAuthorize("hasRole('CLAIMANT')")
    public ResponseEntity<ClaimantClaimResponse> submit(
            @Valid @RequestBody SubmitClaimRequest request, Principal principal) {
        ClaimantClaimResponse response = claims.submit(request, principal.getName());
        return ResponseEntity.created(URI.create("/api/v1/claims/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('CLAIMANT')")
    public List<ClaimantClaimResponse> list(Principal principal) {
        return claims.listClaimantClaims(principal.getName());
    }

    @GetMapping("/{claimId}")
    @PreAuthorize("hasRole('CLAIMANT')")
    public ClaimantClaimResponse get(
            @PathVariable UUID claimId, Principal principal) {
        return claims.getClaimantClaim(claimId, principal.getName());
    }

    @PostMapping("/{claimId}/information-responses")
    @PreAuthorize("hasRole('CLAIMANT')")
    public ClaimantClaimResponse provideInformation(
            @PathVariable UUID claimId,
            @Valid @RequestBody InformationResponse request,
            Principal principal) {
        return claims.provideInformation(claimId, principal.getName(), request.information());
    }
}
