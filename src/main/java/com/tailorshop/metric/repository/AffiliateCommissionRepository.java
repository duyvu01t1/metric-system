package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.AffiliateCommission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface AffiliateCommissionRepository extends JpaRepository<AffiliateCommission, Long> {

    List<AffiliateCommission> findByAffiliateId(Long affiliateId);

    Optional<AffiliateCommission> findByOrderId(Long orderId);

    List<AffiliateCommission> findByAffiliateIdAndIsPaidFalse(Long affiliateId);

    @Query("SELECT COALESCE(SUM(ac.commissionAmount), 0) FROM AffiliateCommission ac WHERE ac.affiliateId = :affiliateId AND ac.isPaid = false")
    BigDecimal sumUnpaidByAffiliateId(Long affiliateId);
}
