package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * QCItem Entity — Từng tiêu chí kiểm tra trong phiếu QC
 *
 * Tiêu chí chuẩn (item_code) được hệ thống khởi tạo tự động khi tạo QCCheck.
 * Người dùng chỉ cần đánh giá: PASS | FAIL | NA cho từng item.
 *
 * Danh sách tiêu chí mặc định (8 nhóm):
 *   CHI_THUA         — Chỉ thừa / chỉ bung
 *   PHAN_KE          — Phấn kẻ còn dính trên vải
 *   VAI_CHAT_LUONG   — Chất lượng / màu sắc vải đúng yêu cầu
 *   DO_MAY_CHUAN     — Đường may thẳng / chính xác
 *   SO_DO_KHOP       — Số đo khớp với yêu cầu của khách
 *   PHU_LIEU_DAY_DU  — Phụ liệu đầy đủ (nút, khóa kéo, lót)
 *   VE_SINH          — Vệ sinh / không bám bẩn
 *   UI_PHANG         — Ủi phẳng / bóng đẹp
 */
@Entity
@Table(name = "qc_items", indexes = {
    @Index(name = "idx_qc_items_qc_check_id", columnList = "qc_check_id"),
    @Index(name = "idx_qc_items_order_id",    columnList = "order_id"),
    @Index(name = "idx_qc_items_result",      columnList = "result")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QCItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FK -> qc_checks.id */
    @Column(name = "qc_check_id", nullable = false)
    private Long qcCheckId;

    /** FK -> tailoring_orders.id (dư thừa nhưng giúp query nhanh) */
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    /**
     * Mã tiêu chí chuẩn hóa:
     * CHI_THUA | PHAN_KE | VAI_CHAT_LUONG | DO_MAY_CHUAN |
     * SO_DO_KHOP | PHU_LIEU_DAY_DU | VE_SINH | UI_PHANG | CUSTOM
     */
    @Column(name = "item_code", nullable = false, length = 50)
    private String itemCode;

    /** Tên tiêu chí hiển thị (tiếng Việt) */
    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    /**
     * Danh mục tiêu chí:
     * THREAD | CHALK_MARK | FABRIC | STITCHING | MEASUREMENT | ACCESSORIES | FINISHING | OTHER
     */
    @Column(name = "category", nullable = false, length = 50)
    private String category = "OTHER";

    /** Mô tả chi tiết / hướng dẫn kiểm tra */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Kết quả đánh giá: PASS | FAIL | NA (không áp dụng)
     * Default NA — chưa kiểm tra
     */
    @Column(name = "result", length = 10)
    private String result = "NA";

    /** Mô tả lỗi cụ thể (bắt buộc nếu result = FAIL) */
    @Column(name = "fail_note", columnDefinition = "TEXT")
    private String failNote;

    /** URL hình ảnh lỗi */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /** FK -> users.id — người kiểm tra item này */
    @Column(name = "checked_by")
    private Long checkedBy;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    /** Thứ tự hiển thị */
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
