package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.LeadAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LeadAssignmentRepository extends JpaRepository<LeadAssignment, Long> {

    Optional<LeadAssignment> findByLeadIdAndIsCurrentTrue(Long leadId);

    List<LeadAssignment> findByLeadIdOrderByCreatedAtDesc(Long leadId);

    List<LeadAssignment> findByApprovalStatusAndIsCurrentTrue(String approvalStatus);

    List<LeadAssignment> findByStaffIdAndIsCurrentTrue(Long staffId);

    /** Hủy tất cả assignment hiện tại của lead trước khi tạo mới */
    @Modifying
    @Query("UPDATE LeadAssignment la SET la.isCurrent = false WHERE la.leadId = :leadId AND la.isCurrent = true")
    void deactivateByLeadId(@Param("leadId") Long leadId);
}
