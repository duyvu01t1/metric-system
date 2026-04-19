package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.CustomerInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerInteractionRepository extends JpaRepository<CustomerInteraction, Long> {

    List<CustomerInteraction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    List<CustomerInteraction> findByStaffIdOrderByCreatedAtDesc(Long staffId);

    int countByCustomerId(Long customerId);
}
