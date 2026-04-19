package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.Affiliate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AffiliateRepository extends JpaRepository<Affiliate, Long> {

    Optional<Affiliate> findByAffiliateCode(String affiliateCode);

    Page<Affiliate> findByIsActiveTrue(Pageable pageable);

    boolean existsByAffiliateCode(String affiliateCode);
}
