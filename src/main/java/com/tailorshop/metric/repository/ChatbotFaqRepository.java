package com.tailorshop.metric.repository;

import com.tailorshop.metric.entity.ChatbotFaq;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ChatbotFaq Repository
 */
@Repository
public interface ChatbotFaqRepository extends JpaRepository<ChatbotFaq, Long> {

    List<ChatbotFaq> findByIsActiveTrueOrderBySortOrderAsc();

    List<ChatbotFaq> findByCategoryAndIsActiveTrueOrderBySortOrderAsc(String category);

    /**
     * Tìm câu trả lời phù hợp dựa trên từ khóa trong câu hỏi
     */
    @Query("SELECT f FROM ChatbotFaq f WHERE f.isActive = true AND " +
           "(LOWER(f.question) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(f.keywords) LIKE LOWER(CONCAT('%', :keyword, '%')))" +
           " ORDER BY f.sortOrder ASC")
    List<ChatbotFaq> findByKeyword(@Param("keyword") String keyword);
}
