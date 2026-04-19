package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.DiscountCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DiscountCodeRepository extends JpaRepository<DiscountCode, Long> {

    Optional<DiscountCode> findByCode(String code);

    List<DiscountCode> findByAffiliateIdAndIsActiveTrue(Long affiliateId);

    Page<DiscountCode> findByIsActiveTrue(Pageable pageable);

    /**
     * Validate mã: còn hiệu lực, chưa hết lượt, giá trị đơn đủ điều kiện.
     * Dùng để kiểm tra trước khi áp dụng.
     */
    @Query("""
        SELECT dc FROM DiscountCode dc
        WHERE dc.code = :code
          AND dc.isActive = true
          AND (dc.validFrom IS NULL OR dc.validFrom <= :today)
          AND (dc.validUntil IS NULL OR dc.validUntil >= :today)
          AND (dc.maxUses IS NULL OR dc.usedCount < dc.maxUses)
          AND (dc.minOrderValue IS NULL OR dc.minOrderValue <= :orderTotal)
    """)
    Optional<DiscountCode> findValidCode(String code, LocalDate today, BigDecimal orderTotal);
}
