package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * QCCheck Entity — Phiếu kiểm tra chất lượng sản phẩm (Phân hệ 5)
 *
 * Luồng:
 *  1. Sau khi công đoạn FITTING (hoặc DELIVERY) hoàn thành → tạo QCCheck [PENDING]
 *  2. Nhân viên bắt đầu kiểm → status = IN_PROGRESS
 *  3. Hệ thống tự tính overall_result dựa trên toàn bộ QCItem:
 *       - Tất cả items PASS/NA → overall_result = PASS → status = PASSED
 *       - Ít nhất 1 item FAIL → overall_result = FAIL → status = FAILED
 *  4. Supervisor có thể override overall_result (approved_by / approved_at)
 *  5. Nếu FAILED → sửa hàng → tạo QCCheck mới (check_round + 1)
 *  6. Nếu PASSED → mở khóa tạo Delivery
 *
 * Ràng buộc (5.3):
 *  - Delivery chỉ được tạo khi QCCheck.status = PASSED
 *  - QCService.assertQCPassed(orderId) phải được gọi trước khi tạo delivery
 */
@Entity
@Table(name = "qc_checks", indexes = {
    @Index(name = "idx_qc_checks_order_id", columnList = "order_id"),
    @Index(name = "idx_qc_checks_status",   columnList = "status"),
    @Index(name = "idx_qc_checks_result",   columnList = "overall_result")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QCCheck {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK -> tailoring_orders.id */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /** Mã phiếu QC: QC-20260411-001 */
    @Column(name = "qc_number", nullable = false, unique = true, length = 60)
    private String qcNumber;

    /**
     * Trạng thái phiếu: PENDING | IN_PROGRESS | PASSED | FAILED
     */
    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING";

    /**
     * Kết quả tổng: PASS | FAIL | null (chưa kết luận)
     * Tự động tính từ QCItems; supervisor có thể override thủ công.
     */
    @Column(name = "overall_result", length = 10)
    private String overallResult;

    /** FK -> users.id — nhân viên thực hiện kiểm tra */
    @Column(name = "checked_by")
    private Long checkedBy;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    /** FK -> users.id — supervisor phê duyệt */
    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    /**
     * Lần kiểm tra thứ mấy (1 = đầu tiên, 2+ = re-check sau khi sửa lỗi)
     */
    @Column(name = "check_round", nullable = false)
    private Integer checkRound = 1;

    /** Ghi chú tổng hợp phiếu QC */
    @Column(name = "overall_notes", columnDefinition = "TEXT")
    private String overallNotes;

    /** Ghi chú nội bộ (không hiển thị cho khách) */
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
