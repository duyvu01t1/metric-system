package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.OrderItemDTO;
import com.tailorshop.metric.dto.TailoringOrderDTO;
import com.tailorshop.metric.entity.Customer;
import com.tailorshop.metric.entity.DiscountCode;
import com.tailorshop.metric.entity.OrderItem;
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
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TailoringOrderService {

    private final TailoringOrderRepository tailoringOrderRepository;
    private final CustomerRepository       customerRepository;
    private final StaffRepository          staffRepository;
    private final ChannelRepository        channelRepository;
    private final DiscountCodeRepository   discountCodeRepository;

    // ── CRUD ─────────────────────────────────────────────────────────────────

    @Transactional
    public TailoringOrderDTO createOrder(TailoringOrderDTO dto) {
        Customer customer = customerRepository.findById(dto.getCustomerId())
            .orElseThrow(() -> new ResourceNotFoundException("Customer not found"));

        TailoringOrder order = new TailoringOrder();
        order.setOrderNumber(generateOrderNumber());
        order.setOrderCode(generateOrderCode());
        order.setCustomer(customer);
        order.setOrderDate(dto.getOrderDate() != null ? dto.getOrderDate() : LocalDate.now());
        order.setTryOnDate(dto.getTryOnDate());
        order.setPromisedDate(dto.getPromisedDate());
        order.setOrderType(dto.getOrderType() != null ? dto.getOrderType() : "CUSTOM");
        order.setDescription(dto.getDescription());
        order.setStatus(dto.getStatus() != null ? dto.getStatus() : "PENDING");
        order.setPaymentStatus(dto.getPaymentStatus() != null ? dto.getPaymentStatus() : "UNPAID");
        order.setNotes(dto.getNotes());
        order.setIsArchived(false);
        order.setFabricMaterial(dto.getFabricMaterial());
        order.setFabricColor(dto.getFabricColor());
        order.setAccessories(dto.getAccessories());
        order.setSourceChannelId(dto.getSourceChannelId());
        order.setDepositAmount(dto.getDepositAmount() != null ? dto.getDepositAmount() : BigDecimal.ZERO);
        order.setDepositStatus(dto.getDepositStatus() != null ? dto.getDepositStatus() : "NONE");
        order.setPrimaryStaffId(dto.getPrimaryStaffId());
        order.setSecondaryStaffId(dto.getSecondaryStaffId());

        List<OrderItem> items = buildItems(dto.getItems(), order);
        order.getItems().addAll(items);
        recalculateTotals(order);

        TailoringOrder saved = tailoringOrderRepository.save(order);
        log.info("Order created: {} (SBL: {})", saved.getOrderCode(), saved.getOrderNumber());
        return convertToDTO(saved);
    }

    @Transactional
    public TailoringOrderDTO updateOrder(Long id, TailoringOrderDTO dto) {
        TailoringOrder order = tailoringOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (dto.getOrderDate() != null)     order.setOrderDate(dto.getOrderDate());
        order.setTryOnDate(dto.getTryOnDate());
        order.setPromisedDate(dto.getPromisedDate());
        if (dto.getOrderType() != null)     order.setOrderType(dto.getOrderType());
        order.setDescription(dto.getDescription());
        if (dto.getStatus() != null)        order.setStatus(dto.getStatus());
        if (dto.getPaymentStatus() != null) order.setPaymentStatus(dto.getPaymentStatus());
        order.setNotes(dto.getNotes());
        order.setFabricMaterial(dto.getFabricMaterial());
        order.setFabricColor(dto.getFabricColor());
        order.setAccessories(dto.getAccessories());
        order.setSourceChannelId(dto.getSourceChannelId());
        if (dto.getDepositAmount() != null) order.setDepositAmount(dto.getDepositAmount());
        if (dto.getDepositStatus() != null) order.setDepositStatus(dto.getDepositStatus());
        if (dto.getPrimaryStaffId() != null)   order.setPrimaryStaffId(dto.getPrimaryStaffId());
        if (dto.getSecondaryStaffId() != null) order.setSecondaryStaffId(dto.getSecondaryStaffId());

        if (dto.getItems() != null) {
            order.getItems().clear();
            order.getItems().addAll(buildItems(dto.getItems(), order));
            recalculateTotals(order);
        }

        TailoringOrder updated = tailoringOrderRepository.save(order);
        log.info("Order updated: {}", order.getOrderCode());
        return convertToDTO(updated);
    }

    @Transactional(readOnly = true)
    public TailoringOrderDTO getOrderById(Long id) {
        TailoringOrder order = tailoringOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return convertToDTO(order);
    }

    @Transactional(readOnly = true)
    public Page<TailoringOrderDTO> getAllOrders(Pageable pageable) {
        return toPage(tailoringOrderRepository.findByIsArchivedFalse(pageable), pageable);
    }

    /**
     * Search theo SỐ BIÊN LẠI / HỌ TÊN / SỐ ĐIỆN THOẠI.
     * Nếu cả 3 đều rỗng → trả về tất cả đơn chưa archive.
     */
    @Transactional(readOnly = true)
    public Page<TailoringOrderDTO> searchOrders(String orderNumber, String customerName,
                                                 String phone, Pageable pageable) {
        String qNum  = StringUtils.hasText(orderNumber)  ? orderNumber.trim()  : null;
        String qName = StringUtils.hasText(customerName) ? customerName.trim() : null;
        String qPhone= StringUtils.hasText(phone)        ? phone.trim()        : null;
        return toPage(tailoringOrderRepository.search(qNum, qName, qPhone, pageable), pageable);
    }

    @Transactional(readOnly = true)
    public Page<TailoringOrderDTO> getOrdersByCustomerId(Long customerId, Pageable pageable) {
        return toPage(tailoringOrderRepository.findByCustomerId(customerId, pageable), pageable);
    }

    @Transactional(readOnly = true)
    public Page<TailoringOrderDTO> getOrdersByStatus(String status, Pageable pageable) {
        return toPage(tailoringOrderRepository.findByStatus(status, pageable), pageable);
    }

    @Transactional
    public void deleteOrder(Long id) {
        TailoringOrder order = tailoringOrderRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        order.setIsArchived(true);
        tailoringOrderRepository.save(order);
        log.info("Order archived: {}", order.getOrderCode());
    }

    public void assertDepositConfirmed(Long orderId) {
        TailoringOrder order = tailoringOrderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng id=" + orderId));
        if (!"CONFIRMED".equals(order.getDepositStatus())) {
            throw new BusinessException("DEPOSIT_NOT_CONFIRMED",
                "Đơn hàng chưa được xác nhận đặt cọc. Không thể tiến hành sản xuất.");
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Page<TailoringOrderDTO> toPage(Page<TailoringOrder> page, Pageable pageable) {
        List<TailoringOrderDTO> dtos = page.getContent().stream()
            .map(this::convertToDTO).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, page.getTotalElements());
    }

    private List<OrderItem> buildItems(List<OrderItemDTO> dtos, TailoringOrder order) {
        if (dtos == null || dtos.isEmpty()) return new ArrayList<>();
        List<OrderItem> result = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            OrderItemDTO d = dtos.get(i);
            if (d.getProductName() == null || d.getUnitPrice() == null) continue;
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProductName(d.getProductName());
            item.setProductCode(d.getProductCode());
            item.setFabricMeters(d.getFabricMeters());
            int qty = d.getQuantity() != null ? d.getQuantity() : 1;
            item.setQuantity(qty);
            item.setUnitPrice(d.getUnitPrice());
            item.setTotalPrice(d.getUnitPrice().multiply(BigDecimal.valueOf(qty)));
            item.setSortOrder(d.getSortOrder() != null ? d.getSortOrder() : i);
            result.add(item);
        }
        return result;
    }

    private void recalculateTotals(TailoringOrder order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            order.setQuantity(0);
            order.setUnitPrice(BigDecimal.ZERO);
            order.setTotalPrice(BigDecimal.ZERO);
            return;
        }
        BigDecimal total = order.getItems().stream()
            .map(OrderItem::getTotalPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        int qty = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        order.setTotalPrice(total);
        order.setQuantity(qty);
        order.setUnitPrice(total);
    }

    private Long generateOrderNumber() {
        return tailoringOrderRepository.findMaxOrderNumber() + 1L;
    }

    private String generateOrderCode() {
        return "ORD-" + System.currentTimeMillis() + "-"
            + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private TailoringOrderDTO convertToDTO(TailoringOrder order) {
        Customer c = order.getCustomer();
        String channelName = order.getSourceChannelId() != null
            ? channelRepository.findById(order.getSourceChannelId())
                .map(ch -> ch.getDisplayName()).orElse(null) : null;
        String discountCodeStr = order.getDiscountCodeId() != null
            ? discountCodeRepository.findById(order.getDiscountCodeId())
                .map(DiscountCode::getCode).orElse(null) : null;
        String primaryStaffName = order.getPrimaryStaffId() != null
            ? staffRepository.findById(order.getPrimaryStaffId())
                .map(s -> s.getFullName()).orElse(null) : null;
        String secondaryStaffName = order.getSecondaryStaffId() != null
            ? staffRepository.findById(order.getSecondaryStaffId())
                .map(s -> s.getFullName()).orElse(null) : null;

        BigDecimal total   = order.getTotalPrice()   != null ? order.getTotalPrice()   : BigDecimal.ZERO;
        BigDecimal deposit = order.getDepositAmount() != null ? order.getDepositAmount(): BigDecimal.ZERO;

        List<OrderItemDTO> itemDTOs = order.getItems() == null ? new ArrayList<>()
            : order.getItems().stream().map(this::convertItemToDTO).collect(Collectors.toList());

        return TailoringOrderDTO.builder()
            .id(order.getId())
            .orderNumber(order.getOrderNumber())
            .orderCode(order.getOrderCode())
            .customerId(c.getId())
            .customerName(c.getFirstName() + " " + c.getLastName())
            .customerPhone(c.getPhone())
            .customerCountry(c.getCountry())
            .orderDate(order.getOrderDate())
            .tryOnDate(order.getTryOnDate())
            .promisedDate(order.getPromisedDate())
            .completedDate(order.getCompletedDate())
            .orderType(order.getOrderType())
            .description(order.getDescription())
            .quantity(order.getQuantity())
            .unitPrice(order.getUnitPrice())
            .totalPrice(total)
            .remainingAmount(total.subtract(deposit))
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
            .discountCode(discountCodeStr)
            .discountAmount(order.getDiscountAmount())
            .depositAmount(deposit)
            .depositStatus(order.getDepositStatus())
            .depositDate(order.getDepositDate())
            .depositConfirmedBy(order.getDepositConfirmedBy())
            .depositConfirmedAt(order.getDepositConfirmedAt())
            .primaryStaffId(order.getPrimaryStaffId())
            .primaryStaffName(primaryStaffName)
            .secondaryStaffId(order.getSecondaryStaffId())
            .secondaryStaffName(secondaryStaffName)
            .items(itemDTOs)
            .createdAt(order.getCreatedAt())
            .updatedAt(order.getUpdatedAt())
            .build();
    }

    private OrderItemDTO convertItemToDTO(OrderItem item) {
        return OrderItemDTO.builder()
            .id(item.getId())
            .productName(item.getProductName())
            .productCode(item.getProductCode())
            .fabricMeters(item.getFabricMeters())
            .quantity(item.getQuantity())
            .unitPrice(item.getUnitPrice())
            .totalPrice(item.getTotalPrice())
            .sortOrder(item.getSortOrder())
            .build();
    }
}
