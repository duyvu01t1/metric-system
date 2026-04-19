package com.tailorshop.metric.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Channel Entity — Kênh tiếp nhận khách hàng (Omnichannel)
 * Đại diện cho các kênh: Messenger, Zalo, WhatsApp, Email, Phone, Walk-in, Website, Referral
 */
@Entity
@Table(name = "channels", indexes = {
    @Index(name = "idx_channels_code",   columnList = "channel_code"),
    @Index(name = "idx_channels_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Channel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã định danh kênh: MESSENGER, ZALO, WHATSAPP, EMAIL, PHONE, WALK_IN, WEBSITE, REFERRAL
     */
    @Column(name = "channel_code", unique = true, nullable = false, length = 50)
    private String channelCode;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * Font Awesome icon class, e.g. "fab fa-facebook-messenger"
     */
    @Column(name = "icon_class", length = 100)
    private String iconClass;

    /**
     * Webhook URL cho tích hợp tự động (Zalo OA, Messenger webhook, v.v.)
     */
    @Column(name = "webhook_url", length = 500)
    private String webhookUrl;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
