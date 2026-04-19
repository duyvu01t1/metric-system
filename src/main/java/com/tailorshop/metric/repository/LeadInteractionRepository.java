package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.LeadInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * LeadInteraction Repository
 */
@Repository
public interface LeadInteractionRepository extends JpaRepository<LeadInteraction, Long> {

    List<LeadInteraction> findByLeadIdOrderByInteractedAtDesc(Long leadId);

    long countByLeadId(Long leadId);
}
