package com.chubb.claims.claim.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClaimRepository extends JpaRepository<ClaimRecord, UUID> {

    List<ClaimRecord> findAllByClaimantUsernameOrderByCreatedAtDesc(String claimantUsername);

    List<ClaimRecord> findAllByStatusIn(Collection<ClaimStatus> statuses);

    List<ClaimRecord> findAllByStatusNotIn(Collection<ClaimStatus> statuses);

    @Query("""
            select c from ClaimRecord c
            where c.status in :statuses
              and (c.assignedStaffUsername is null or c.assignedStaffUsername = :staffUsername)
            order by c.createdAt asc
            """)
    List<ClaimRecord> findWorkQueue(
            @Param("staffUsername") String staffUsername,
            @Param("statuses") Collection<ClaimStatus> statuses);

    Optional<ClaimRecord> findByIdAndClaimantUsername(UUID id, String claimantUsername);

    List<ClaimRecord> findAllByAssignedStaffUsernameIsNotNull();
}
