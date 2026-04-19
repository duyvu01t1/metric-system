package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.TailoringOrderDTO;
import com.tailorshop.metric.entity.Customer;
import com.tailorshop.metric.entity.DiscountCode;
import com.tailorshop.metric.entity.TailoringOrder;
import com.tailorshop.metric.exception.BusinessException;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.ChannelRepository;
import com.tailorshop.metric.repository.CustomerRepository;
import com.tailorshop.metric.repository.DiscountCodeRepository;
import com.tailorshop.metric.repository.StaffRepository;
import com.tailorshop.metric.repository.TailoringOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tailoring Order Service
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TailoringOrderService {

    private final TailoringOrderRepository  tailoringOrderRepository;
    private final CustomerRepository          customerRepository;
    private final StaffRepository             staffRepository;
    private final ChannelRepository           channelRepository;
    private final DiscountCodeRepository      discountCodeRepository;

    /**
     * Create a new tailoring order
     */
    @Transactional
    public TailoringOrderDTO createOrder(TailoringOrderDTO dto) {
        // Check if customer exists
        Customer customer = customerRepository.findById(dto.getCustomerId())
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        // Generate unique order code
        String orderCode = generateOrderCode();
        
        // Create order
        TailoringOrder order = new TailoringOrder();
        order.setOrderCode(orderCode);
        order.setCustomer(customer);
        order.setOrderDate(dto.getOrderDate() != null ? dto.getOrderDate() : LocalDate.now());
        order.setPromisedDate(dto.getPromisedDate());
        order.setOrderType(dto.getOrderType());
        order.setDescription(dto.getDescription());
        order.setQuantity(dto.getQuantity() != null ? dto.getQuantity() : 1);
        order.setUnitPrice(dto.getUnitPrice());
        order.setTotalPrice(dto.getTotalPrice());
        order.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDING");
        order.setPaymentStatus(dto.getPaymentStatus() != null ? dto.getPaymentStatus() : "UNPAID");
        order.setNotes(dto.getNotes());
        order.setIsArchived(false);

        // Phase 3 fields
        order.setFabricMaterial(dto.getFabricMaterial());
        order.setFabricColor(dto.getFabricColor());
        order.setAccessories(dto.getAccessories());
        order.setSourceChannelId(dto.getSourceChannelId());
        order.setDepositStatus(dto.getDepositStatus() != null ? dto.getDepositStatus() : "NONE");

        TailoringOrder savedOrder = tailoringOrderRepository.save(order);
        log.info("Order created: {}", orderCode);

        return convertToDTO(savedOrder);
    }

    /**
     * Update existing order
     */
    @Transactional
    public TailoringOrderDTO updateOrder(Long id, TailoringOrderDTO dto) {
        TailoringOrder order = tailoringOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        order.setPromisedDate(dto.getPromisedDate());
        order.setOrderType(dto.getOrderType());
        order.setDescription(dto.getDescription());
        order.setQuantity(dto.getQuantity());
        order.setUnitPrice(dto.getUnitPrice());
        order.setTotalPrice(dto.getTotalPrice());
        order.setStatus(dto.getStatus());
        order.setPaymentStatus(dto.getPaymentStatus());
        order.setNotes(dto.getNotes());

        TailoringOrder updatedOrder = tailoringOrderRepository.save(order);
        log.info("Order updated: {}", order.getOrderCode());

        return convertToDTO(updatedOrder);
    }

    /**
     * Get order by ID
     */
    public TailoringOrderDTO getOrderById(Long id) {
        TailoringOrder order = tailoringOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return convertToDTO(order);
    }

    /**
     * Get all orders with pagination
     */
    public Page<TailoringOrderDTO> getAllOrders(Pageable pageable) {
        Page<TailoringOrder> orders = tailoringOrderRepository.findAll(pageable);
        List<TailoringOrderDTO> dtos = orders.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, orders.getTotalElements());
    }

    /**
     * Get orders by customer ID
     */
    public Page<TailoringOrderDTO> getOrdersByCustomerId(Long customerId, Pageable pageable) {
        Page<TailoringOrder> orders = tailoringOrderRepository.findByCustomerId(customerId, pageable);
        List<TailoringOrderDTO> dtos = orders.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, orders.getTotalElements());
    }

    /**
     * Get orders by status
     */
    public Page<TailoringOrderDTO> getOrdersByStatus(String status, Pageable pageable) {
        Page<TailoringOrder> orders = tailoringOrderRepository.findByStatus(status, pageable);
        List<TailoringOrderDTO> dtos = orders.getContent().stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, orders.getTotalElements());
    }

    /**
     * Delete order
     */
    @Transactional
    public void deleteOrder(Long id) {
        TailoringOrder order = tailoringOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        order.setIsArchived(true);
        tailoringOrderRepository.save(order);
        log.info("Order archived: {}", order.getOrderCode());
    }

    /**
     * Kiểm tra xem đơn hàng đã xác nhận đặt cọc chưa.
     * Gọi từ Phân hệ Sản xuất (Phase 4) trước khi tạo production stage đầu tiên.
     *
     * @throws BusinessException nếu chưa xác nhận đặt cọc
     */
    public void assertDepositConfirmed(Long orderId) {
        TailoringOrder order = tailoringOrderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng id=" + orderId));
        if (!"CONFIRMED".equals(order.getDepositStatus())) {
            throw new BusinessException("DEPOSIT_NOT_CONFIRMED",
                "Đơn hàng chưa được xác nhận đặt cọc. Không thể tiến hành sản xuất.");
        }
    }

    /**
     * Generate unique order code
     */
    private String generateOrderCode() {
        return "ORD-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * Convert TailoringOrder to DTO
     */
    private TailoringOrderDTO convertToDTO(TailoringOrder order) {
        // Populate derived names
        String channelName = order.getSourceChannelId() != null
            ? channelRepository.findById(order.getSourceChannelId()).map(ch -> ch.getDisplayName()).orElse(null) : null;
        String discountCode = order.getDiscountCodeId() != null
            ? discountCodeRepository.findById(order.getDiscountCodeId()).map(DiscountCode::getCode).orElse(null) : null;
        String primaryStaffName = order.getPrimaryStaffId() != null
            ? staffRepository.findById(order.getPrimaryStaffId()).map(s -> s.getFullName()).orElse(null) : null;
        String secondaryStaffName = order.getSecondaryStaffId() != null
            ? staffRepository.findById(order.getSecondaryStaffId()).map(s -> s.getFullName()).orElse(null) : null;

        return TailoringOrderDTO.builder()
            .id(order.getId())
            .orderCode(order.getOrderCode())
            .customerId(order.getCustomer().getId())
            .customerName(order.getCustomer().getFirstName() + " " + order.getCustomer().getLastName())
            .orderDate(order.getOrderDate())
            .promisedDate(order.getPromisedDate())
            .completedDate(order.getCompletedDate())
            .orderType(order.getOrderType())
            .description(order.getDescription())
            .quantity(order.getQuantity())
            .unitPrice(order.getUnitPrice())
            .totalPrice(order.getTotalPrice())
            .status(order.getStatus())
            .paymentStatus(order.getPaymentStatus())
            .notes(order.getNotes())
            .isArchived(order.getIsArchived())
            .fabricMaterial(order.getFabricMaterial())
            .fabricColor(order.getFabricColor())
            .accessories(order.getAccessories())
            .sourceChannelId(order.getSourceChannelId())
            .sourceChannelName(channelName)
            .quotationId(order.getQuotationId())
            .discountCodeId(order.getDiscountCodeId())
            .discountCode(discountCode)
            .discountAmount(order.getDiscountAmount())
            .depositAmount(order.getDepositAmount())
            .depositStatus(order.getDepositStatus())
            .depositDate(order.getDepositDate())
            .depositConfirmedBy(order.getDepositConfirmedBy())
            .depositConfirmedAt(order.getDepositConfirmedAt())
            .primaryStaffId(order.getPrimaryStaffId())
            .primaryStaffName(primaryStaffName)
            .secondaryStaffId(order.getSecondaryStaffId())
            .secondaryStaffName(secondaryStaffName)
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }
}
