package com.tailorshop.metric.controller;

import com.tailorshop.metric.dto.ApiResponse;
import com.tailorshop.metric.dto.ChannelDTO;
import com.tailorshop.metric.dto.ChatbotFaqDTO;
import com.tailorshop.metric.dto.LeadDTO;
import com.tailorshop.metric.dto.LeadInteractionDTO;
import com.tailorshop.metric.service.ChannelService;
import com.tailorshop.metric.service.ChatbotFaqService;
import com.tailorshop.metric.service.LeadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Lead Controller — REST API cho Phân hệ Omnichannel
 *
 * Base path: /leads
 * Sub-resources: /channels, /faqs
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class LeadController {

    private final LeadService       leadService;
    private final ChannelService    channelService;
    private final ChatbotFaqService faqService;

    // =========================================================================
    // CHANNELS
    // =========================================================================

    /**
     * GET /channels — Lấy tất cả kênh đang hoạt động
     */
    @GetMapping("/channels")
    public ResponseEntity<ApiResponse<List<ChannelDTO>>> getActiveChannels() {
        return ResponseEntity.ok(ApiResponse.success("Channels retrieved",
            channelService.getActiveChannels()));
    }

    /**
     * GET /channels/all — Lấy tất cả kênh (bao gồm inactive)
     */
    @GetMapping("/channels/all")
    public ResponseEntity<ApiResponse<List<ChannelDTO>>> getAllChannels() {
        return ResponseEntity.ok(ApiResponse.success("All channels retrieved",
            channelService.getAllChannels()));
    }

    /**
     * GET /channels/{id}
     */
    @GetMapping("/channels/{id}")
    public ResponseEntity<ApiResponse<ChannelDTO>> getChannel(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Channel retrieved",
            channelService.getChannelById(id)));
    }

    /**
     * POST /channels — Tạo kênh mới
     */
    @PostMapping("/channels")
    public ResponseEntity<ApiResponse<ChannelDTO>> createChannel(@RequestBody ChannelDTO dto) {
        log.info("Creating channel: {}", dto.getChannelCode());
        ChannelDTO created = channelService.createChannel(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Channel created successfully", created));
    }

    /**
     * PUT /channels/{id} — Cập nhật kênh
     */
    @PutMapping("/channels/{id}")
    public ResponseEntity<ApiResponse<ChannelDTO>> updateChannel(
            @PathVariable Long id, @RequestBody ChannelDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Channel updated",
            channelService.updateChannel(id, dto)));
    }

    /**
     * PATCH /channels/{id}/toggle?active=true|false
     */
    @PatchMapping("/channels/{id}/toggle")
    public ResponseEntity<ApiResponse<Void>> toggleChannel(
            @PathVariable Long id,
            @RequestParam boolean active) {
        channelService.toggleChannel(id, active);
        return ResponseEntity.ok(ApiResponse.success("Channel status updated", null));
    }

    // =========================================================================
    // LEADS
    // =========================================================================

    /**
     * POST /leads — Tạo lead mới
     */
    @PostMapping("/leads")
    public ResponseEntity<ApiResponse<LeadDTO>> createLead(@RequestBody LeadDTO dto) {
        log.info("Creating lead from channel: {}", dto.getChannelId());
        LeadDTO created = leadService.createLead(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Lead created successfully", created));
    }

    /**
     * GET /leads — Lấy tất cả leads (có phân trang, lọc)
     */
    @GetMapping("/leads")
    public ResponseEntity<ApiResponse<Page<LeadDTO>>> getLeads(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long channelId,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<LeadDTO> result;

        if (q != null && !q.isBlank()) {
            result = leadService.searchLeads(q, pageable);
        } else if (status != null && channelId != null) {
            result = leadService.getLeadsByChannel(channelId, pageable);
        } else if (status != null) {
            result = leadService.getLeadsByStatus(status, pageable);
        } else if (channelId != null) {
            result = leadService.getLeadsByChannel(channelId, pageable);
        } else {
            result = leadService.getAllLeads(pageable);
        }

        return ResponseEntity.ok(ApiResponse.success("Leads retrieved", result));
    }

    /**
     * GET /leads/stats — Thống kê tổng quan leads
     */
    @GetMapping("/leads/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getLeadStats() {
        return ResponseEntity.ok(ApiResponse.success("Lead stats retrieved",
            leadService.getLeadStats()));
    }

    /**
     * GET /leads/followup-today — Danh sách cần follow-up hôm nay
     */
    @GetMapping("/leads/followup-today")
    public ResponseEntity<ApiResponse<List<LeadDTO>>> getFollowUpToday() {
        return ResponseEntity.ok(ApiResponse.success("Follow-up list retrieved",
            leadService.getFollowUpToday()));
    }

    /**
     * GET /leads/{id}
     */
    @GetMapping("/leads/{id}")
    public ResponseEntity<ApiResponse<LeadDTO>> getLead(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Lead retrieved",
            leadService.getLeadById(id)));
    }

    /**
     * PUT /leads/{id} — Cập nhật thông tin lead
     */
    @PutMapping("/leads/{id}")
    public ResponseEntity<ApiResponse<LeadDTO>> updateLead(
            @PathVariable Long id, @RequestBody LeadDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("Lead updated",
            leadService.updateLead(id, dto)));
    }

    /**
     * PATCH /leads/{id}/status — Cập nhật trạng thái lead
     * Body: { "status": "CONTACTED", "lostReason": "..." }
     */
    @PatchMapping("/leads/{id}/status")
    public ResponseEntity<ApiResponse<LeadDTO>> updateLeadStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status     = body.get("status");
        String lostReason = body.get("lostReason");
        return ResponseEntity.ok(ApiResponse.success("Lead status updated",
            leadService.updateLeadStatus(id, status, lostReason)));
    }

    /**
     * POST /leads/{id}/convert — Chốt đơn: chuyển lead thành khách hàng
     * Body: { "customerId": 1, "orderId": 2 }
     */
    @PostMapping("/leads/{id}/convert")
    public ResponseEntity<ApiResponse<LeadDTO>> convertLead(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        Long customerId = body.get("customerId");
        Long orderId    = body.get("orderId");
        return ResponseEntity.ok(ApiResponse.success("Lead converted successfully",
            leadService.convertLead(id, customerId, orderId)));
    }

    // ─── INTERACTIONS ─────────────────────────────────────────────────────────

    /**
     * POST /leads/{id}/interactions — Thêm tương tác mới
     */
    @PostMapping("/leads/{id}/interactions")
    public ResponseEntity<ApiResponse<LeadInteractionDTO>> addInteraction(
            @PathVariable Long id, @RequestBody LeadInteractionDTO dto) {
        dto.setLeadId(id);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Interaction added",
                leadService.addInteraction(id, dto)));
    }

    /**
     * GET /leads/{id}/interactions — Lịch sử tương tác của lead
     */
    @GetMapping("/leads/{id}/interactions")
    public ResponseEntity<ApiResponse<List<LeadInteractionDTO>>> getInteractions(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Interactions retrieved",
            leadService.getInteractions(id)));
    }

    // =========================================================================
    // CHATBOT FAQ
    // =========================================================================

    /**
     * GET /faqs — Lấy tất cả câu hỏi thường gặp
     */
    @GetMapping("/faqs")
    public ResponseEntity<ApiResponse<List<ChatbotFaqDTO>>> getFaqs(
            @RequestParam(required = false) String category) {
        List<ChatbotFaqDTO> result = category != null
            ? faqService.getFaqsByCategory(category)
            : faqService.getAllActiveFaqs();
        return ResponseEntity.ok(ApiResponse.success("FAQs retrieved", result));
    }

    /**
     * GET /faqs/suggest?q=giá vest — Gợi ý trả lời theo keyword
     */
    @GetMapping("/faqs/suggest")
    public ResponseEntity<ApiResponse<List<ChatbotFaqDTO>>> suggestFaq(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success("FAQ suggestions retrieved",
            faqService.suggest(q)));
    }

    /**
     * POST /faqs — Tạo FAQ mới
     */
    @PostMapping("/faqs")
    public ResponseEntity<ApiResponse<ChatbotFaqDTO>> createFaq(@RequestBody ChatbotFaqDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("FAQ created", faqService.createFaq(dto)));
    }

    /**
     * PUT /faqs/{id} — Cập nhật FAQ
     */
    @PutMapping("/faqs/{id}")
    public ResponseEntity<ApiResponse<ChatbotFaqDTO>> updateFaq(
            @PathVariable Long id, @RequestBody ChatbotFaqDTO dto) {
        return ResponseEntity.ok(ApiResponse.success("FAQ updated", faqService.updateFaq(id, dto)));
    }

    /**
     * DELETE /faqs/{id} — Deactivate FAQ
     */
    @DeleteMapping("/faqs/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteFaq(@PathVariable Long id) {
        faqService.deleteFaq(id);
        return ResponseEntity.ok(ApiResponse.success("FAQ deleted", null));
    }
}
