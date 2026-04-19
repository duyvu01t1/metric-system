package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.Quotation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationRepository extends JpaRepository<Quotation, Long> {

    Optional<Quotation> findByQuotationCode(String quotationCode);

    List<Quotation> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    Page<Quotation> findByStatus(String status, Pageable pageable);

    Page<Quotation> findByCustomerIdAndStatus(Long customerId, String status, Pageable pageable);

    @Query("SELECT COUNT(q) FROM Quotation q WHERE YEAR(q.createdAt) = :year AND MONTH(q.createdAt) = :month")
    long countByYearAndMonth(int year, int month);

    boolean existsByQuotationCode(String quotationCode);
}
