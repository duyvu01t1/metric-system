package com.tailorshop.metric.service;

import com.tailorshop.metric.dto.ChatbotFaqDTO;
import com.tailorshop.metric.entity.ChatbotFaq;
import com.tailorshop.metric.exception.ResourceNotFoundException;
import com.tailorshop.metric.repository.ChatbotFaqRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ChatbotFaq Service — Quản lý câu hỏi thường gặp & gợi ý trả lời
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class ChatbotFaqService {

    private final ChatbotFaqRepository faqRepository;

    @Transactional(readOnly = true)
    public List<ChatbotFaqDTO> getAllActiveFaqs() {
        return faqRepository.findByIsActiveTrueOrderBySortOrderAsc()
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatbotFaqDTO> getAllFaqs() {
        return faqRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ChatbotFaqDTO> getFaqsByCategory(String category) {
        return faqRepository.findByCategoryAndIsActiveTrueOrderBySortOrderAsc(category)
            .stream().map(this::toDTO).collect(Collectors.toList());
    }

    /**
     * Tìm câu trả lời gợi ý cho một keyword
     * Đồng thời tăng hit_count để biết câu hỏi nào phổ biến nhất
     */
    @Transactional
    public List<ChatbotFaqDTO> suggest(String keyword) {
        List<ChatbotFaq> results = faqRepository.findByKeyword(keyword);
        results.forEach(faq -> {
            faq.setHitCount(faq.getHitCount() + 1);
            faqRepository.save(faq);
        });
        return results.stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public ChatbotFaqDTO createFaq(ChatbotFaqDTO dto) {
        ChatbotFaq faq = new ChatbotFaq();
        faq.setQuestion(dto.getQuestion());
        faq.setAnswer(dto.getAnswer());
        faq.setCategory(dto.getCategory());
        faq.setKeywords(dto.getKeywords());
        faq.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        faq.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        ChatbotFaq saved = faqRepository.save(faq);
        log.info("FAQ created: id={}", saved.getId());
        return toDTO(saved);
    }

    @Transactional
    public ChatbotFaqDTO updateFaq(Long id, ChatbotFaqDTO dto) {
        ChatbotFaq faq = faqRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FAQ not found: " + id));
        faq.setQuestion(dto.getQuestion());
        faq.setAnswer(dto.getAnswer());
        faq.setCategory(dto.getCategory());
        faq.setKeywords(dto.getKeywords());
        faq.setIsActive(dto.getIsActive());
        faq.setSortOrder(dto.getSortOrder());
        return toDTO(faqRepository.save(faq));
    }

    @Transactional
    public void deleteFaq(Long id) {
        ChatbotFaq faq = faqRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("FAQ not found: " + id));
        faq.setIsActive(false);
        faqRepository.save(faq);
    }

    private ChatbotFaqDTO toDTO(ChatbotFaq f) {
        return ChatbotFaqDTO.builder()
            .id(f.getId())
            .question(f.getQuestion())
            .answer(f.getAnswer())
            .category(f.getCategory())
            .categoryLabel(resolveCategoryLabel(f.getCategory()))
            .keywords(f.getKeywords())
            .hitCount(f.getHitCount())
            .isActive(f.getIsActive())
            .sortOrder(f.getSortOrder())
            .createdAt(f.getCreatedAt())
            .updatedAt(f.getUpdatedAt())
            .build();
    }

    private String resolveCategoryLabel(String category) {
        if (category == null) return "Khác";
        return switch (category) {
            case "GIA_CA"    -> "Giá cả";
            case "QUY_TRINH" -> "Quy trình";
            case "SAN_PHAM"  -> "Sản phẩm";
            case "GIAO_HANG" -> "Giao hàng";
            default          -> "Khác";
        };
    }
}
