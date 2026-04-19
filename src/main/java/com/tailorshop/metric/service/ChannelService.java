package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.ChannelDTO;
import com.tailorshop.metric.entity.Channel;
import com.tailorshop.metric.exception.BusinessException;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.ChannelRepository;
import com.tailorshop.metric.repository.LeadRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Channel Service — Quản lý kênh tiếp nhận (Omnichannel)
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final LeadRepository    leadRepository;

    /**
     * Lấy tất cả kênh đang hoạt động (kèm thống kê lead)
     */
    @Transactional(readOnly = true)
    public List<ChannelDTO> getActiveChannels() {
        return channelRepository.findByIsActiveTrueOrderBySortOrderAsc()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Lấy tất cả kênh (bao gồm inactive)
     */
    @Transactional(readOnly = true)
    public List<ChannelDTO> getAllChannels() {
        return channelRepository.findAll()
            .stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    /**
     * Lấy kênh theo ID
     */
    @Transactional(readOnly = true)
    public ChannelDTO getChannelById(Long id) {
        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Channel not found with id: " + id));
        return toDTO(channel);
    }

    /**
     * Tạo kênh mới
     */
    @Transactional
    public ChannelDTO createChannel(ChannelDTO dto) {
        if (channelRepository.existsByChannelCode(dto.getChannelCode())) {
            throw new BusinessException("CHANNEL_CODE_EXISTS", "Channel code already exists: " + dto.getChannelCode());
        }
        Channel channel = toEntity(dto);
        Channel saved = channelRepository.save(channel);
        log.info("Channel created: {}", saved.getChannelCode());
        return toDTO(saved);
    }

    /**
     * Cập nhật kênh
     */
    @Transactional
    public ChannelDTO updateChannel(Long id, ChannelDTO dto) {
        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Channel not found with id: " + id));
        channel.setDisplayName(dto.getDisplayName());
        channel.setIconClass(dto.getIconClass());
        channel.setWebhookUrl(dto.getWebhookUrl());
        channel.setDescription(dto.getDescription());
        channel.setIsActive(dto.getIsActive());
        channel.setSortOrder(dto.getSortOrder());
        Channel updated = channelRepository.save(channel);
        log.info("Channel updated: {}", updated.getChannelCode());
        return toDTO(updated);
    }

    /**
     * Bật/tắt kênh
     */
    @Transactional
    public void toggleChannel(Long id, boolean active) {
        Channel channel = channelRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Channel not found with id: " + id));
        channel.setIsActive(active);
        channelRepository.save(channel);
        log.info("Channel {} status set to: {}", channel.getChannelCode(), active);
    }

    // ─── Mapping ─────────────────────────────────────────────────────────────

    public ChannelDTO toDTO(Channel c) {
        return ChannelDTO.builder()
            .id(c.getId())
            .channelCode(c.getChannelCode())
            .displayName(c.getDisplayName())
            .iconClass(c.getIconClass())
            .webhookUrl(c.getWebhookUrl())
            .description(c.getDescription())
            .isActive(c.getIsActive())
            .sortOrder(c.getSortOrder())
            .createdAt(c.getCreatedAt())
            .updatedAt(c.getUpdatedAt())
            .build();
    }

    private Channel toEntity(ChannelDTO dto) {
        Channel c = new Channel();
        c.setChannelCode(dto.getChannelCode().toUpperCase().trim());
        c.setDisplayName(dto.getDisplayName());
        c.setIconClass(dto.getIconClass());
        c.setWebhookUrl(dto.getWebhookUrl());
        c.setDescription(dto.getDescription());
        c.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        c.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        return c;
    }
}
