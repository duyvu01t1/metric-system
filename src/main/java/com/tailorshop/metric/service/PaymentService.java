package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.PaymentDTO;
import com.tailorshop.metric.entity.Payment;
import com.tailorshop.metric.entity.TailoringOrder;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.PaymentRepository;
import com.tailorshop.metric.repository.TailoringOrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Payment Service
 * Handles business logic for payment management
 */
@Service
@Slf4j
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private TailoringOrderRepository tailoringOrderRepository;

    /**
     * Create a new payment
     * @param paymentDTO Payment data
     * @return Created payment
     */
    @Transactional
    public PaymentDTO createPayment(PaymentDTO paymentDTO) {
        log.info("Creating new payment for order: {}", paymentDTO.getTailoringOrderId());
        
        TailoringOrder order = tailoringOrderRepository.findById(paymentDTO.getTailoringOrderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + paymentDTO.getTailoringOrderId()));
        
        Payment payment = convertToEntity(paymentDTO);
        payment.setTailoringOrder(order);
        payment.setCreatedAt(LocalDateTime.now());
        
        Payment saved = paymentRepository.save(payment);
        log.info("Payment created with ID: {}", saved.getId());
        
        return convertToDTO(saved);
    }

    /**
     * Get payment by ID
     * @param id Payment ID
     * @return Payment data
     */
    @Transactional(readOnly = true)
    public PaymentDTO getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        return convertToDTO(payment);
    }

    /**
     * Get all payments with pagination
     * @param pageable Pagination info
     * @return Page of payments
     */
    @Transactional(readOnly = true)
    public Page<PaymentDTO> getAllPayments(Pageable pageable) {
        Page<Payment> payments = paymentRepository.findAll(pageable);
        return new PageImpl<>(
            payments.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()),
            pageable,
            payments.getTotalElements()
        );
    }

    /**
     * Get payments by order ID
     * @param orderId Order ID
     * @return List of payments
     */
    @Transactional(readOnly = true)
    public List<PaymentDTO> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByTailoringOrderId(orderId)
            .stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    /**
     * Update payment
     * @param id Payment ID
     * @param paymentDTO Updated payment data
     * @return Updated payment
     */
    @Transactional
    public PaymentDTO updatePayment(Long id, PaymentDTO paymentDTO) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentMethod(paymentDTO.getPaymentMethod());
        payment.setTransactionReference(paymentDTO.getTransactionReference());
        payment.setPaymentDate(paymentDTO.getPaymentDate());
        payment.setNotes(paymentDTO.getNotes());
        
        Payment updated = paymentRepository.save(payment);
        log.info("Payment updated with ID: {}", id);
        
        return convertToDTO(updated);
    }

    /**
     * Delete payment
     * @param id Payment ID
     */
    @Transactional
    public void deletePayment(Long id) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + id));
        
        paymentRepository.delete(payment);
        log.info("Payment deleted with ID: {}", id);
    }

    /**
     * Convert Entity to DTO
     */
    private PaymentDTO convertToDTO(Payment payment) {
        return PaymentDTO.builder()
            .id(payment.getId())
            .tailoringOrderId(payment.getTailoringOrder().getId())
            .orderCode(payment.getTailoringOrder().getOrderCode())
            .customerName(payment.getTailoringOrder().getCustomer().getFirstName() + " " + 
                         payment.getTailoringOrder().getCustomer().getLastName())
            .amount(payment.getAmount())
            .paymentMethod(payment.getPaymentMethod())
            .transactionReference(payment.getTransactionReference())
            .paymentDate(payment.getPaymentDate())
            .notes(payment.getNotes())
            .createdAt(payment.getCreatedAt())
            .createdBy(payment.getCreatedBy())
            .build();
    }

    /**
     * Convert DTO to Entity
     */
    private Payment convertToEntity(PaymentDTO paymentDTO) {
        Payment payment = new Payment();
        payment.setAmount(paymentDTO.getAmount());
        payment.setPaymentMethod(paymentDTO.getPaymentMethod());
        payment.setTransactionReference(paymentDTO.getTransactionReference());
        payment.setPaymentDate(paymentDTO.getPaymentDate());
        payment.setNotes(paymentDTO.getNotes());
        return payment;
    }
}
