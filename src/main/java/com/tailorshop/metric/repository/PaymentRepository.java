package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Payment Repository
 */
@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByTailoringOrderId(Long tailoringOrderId);

}
