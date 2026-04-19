package com.tailorshop.metric.service;

import com.tailorshop.metric.entity.Channel;
import com.tailorshop.metric.entity.Lead;
import com.tailorshop.metric.entity.Staff;
import com.tailorshop.metric.entity.TailoringOrder;
import com.tailorshop.metric.repository.ChannelRepository;
import com.tailorshop.metric.repository.CustomerRepository;
import com.tailorshop.metric.repository.LeadRepository;
import com.tailorshop.metric.repository.StaffRepository;
import com.tailorshop.metric.repository.TailoringOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AnalyticsService — Phân hệ 8: Báo cáo & Đánh giá
 *
 * 8.1 Doanh thu theo kênh marketing
 * 8.2 Doanh thu theo nhân viên
 * 8.3 Đánh giá nhân sự (performanceScore, năng suất)
 * 8.4 Tỷ lệ chuyển đổi lead → đơn hàng
 * 8.5 Báo cáo hiệu quả marketing
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AnalyticsService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final TailoringOrderRepository orderRepository;
    private final LeadRepository leadRepository;
    private final StaffRepository staffRepository;
    private final ChannelRepository channelRepository;
    private final CustomerRepository customerRepository;

    public Map<String, Object> getOverview() {
        return getOverview(null, null);
    }

    public Map<String, Object> getOverview(LocalDate fromDate, LocalDate toDate) {
        LocalDate normalizedFrom = normalizeFromDate(fromDate, toDate);
        LocalDate normalizedTo = normalizeToDate(fromDate, toDate);

        List<TailoringOrder> filteredOrders = getFilteredOrders(normalizedFrom, normalizedTo);
        List<Lead> filteredLeads = getFilteredLeads(normalizedFrom, normalizedTo);

        long totalCustomers = customerRepository.count();
        long totalLeads = filteredLeads.size();
        long totalOrders = filteredOrders.size();
        long completedOrders = filteredOrders.stream()
                .filter(order -> "COMPLETED".equalsIgnoreCase(order.getStatus()))
                .count();

        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        LocalDateTime endOfMonth = YearMonth.now().atEndOfMonth().atTime(23, 59, 59);
        long monthlyLeads = hasDateFilter(normalizedFrom, normalizedTo)
                ? totalLeads
                : leadRepository.countByCreatedAtBetween(startOfMonth, endOfMonth);

        BigDecimal totalRevenue = filteredOrders.stream()
                .map(order -> safeDecimal(order.getTotalPrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long convertedLeads = filteredLeads.stream()
                .filter(lead -> "CONVERTED".equalsIgnoreCase(lead.getStatus()))
                .count();

        double leadConversionRate = totalLeads > 0
                ? (double) convertedLeads / (double) totalLeads * 100.0
                : 0.0;

        double completionRate = totalOrders > 0
                ? (double) completedOrders / (double) totalOrders * 100.0
                : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("fromDate", normalizedFrom != null ? normalizedFrom.toString() : null);
        result.put("toDate", normalizedTo != null ? normalizedTo.toString() : null);
        result.put("rangeLabel", buildRangeLabel(normalizedFrom, normalizedTo));
        result.put("hasFilter", hasDateFilter(normalizedFrom, normalizedTo));
        result.put("totalCustomers", totalCustomers);
        result.put("totalLeads", totalLeads);
        result.put("monthlyLeads", monthlyLeads);
        result.put("periodLeadCount", totalLeads);
        result.put("totalOrders", totalOrders);
        result.put("periodOrderCount", totalOrders);
        result.put("completedOrders", completedOrders);
        result.put("convertedLeads", convertedLeads);
        result.put("totalRevenue", totalRevenue);
        result.put("periodRevenue", totalRevenue);
        result.put("leadConversionRate", round(leadConversionRate));
        result.put("completionRate", round(completionRate));
        result.put("avgRevenuePerOrder", totalOrders > 0
                ? totalRevenue.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        return result;
    }

    public List<Map<String, Object>> getRevenueByChannel() {
        return getRevenueByChannel(null, null);
    }

    public List<Map<String, Object>> getRevenueByChannel(LocalDate fromDate, LocalDate toDate) {
        Map<Long, Channel> channelMap = channelRepository.findAll().stream()
                .collect(Collectors.toMap(Channel::getId, c -> c, (a, b) -> a, LinkedHashMap::new));

        Map<Long, Long> orderCountMap = new LinkedHashMap<>();
        Map<Long, BigDecimal> revenueMap = new LinkedHashMap<>();

        for (TailoringOrder order : getFilteredOrders(fromDate, toDate)) {
            Long key = order.getSourceChannelId() != null ? order.getSourceChannelId() : 0L;
            orderCountMap.merge(key, 1L, Long::sum);
            revenueMap.merge(key, safeDecimal(order.getTotalPrice()), BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : orderCountMap.entrySet()) {
            Long channelId = entry.getKey();
            Channel channel = channelMap.get(channelId);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("channelId", channelId == 0L ? null : channelId);
            item.put("channelCode", channel != null ? channel.getChannelCode() : "UNKNOWN");
            item.put("channelName", channel != null ? channel.getDisplayName() : "Chưa gắn nguồn");
            item.put("orderCount", entry.getValue());
            item.put("revenue", revenueMap.getOrDefault(channelId, BigDecimal.ZERO));
            result.add(item);
        }

        result.sort(Comparator.comparing((Map<String, Object> item) -> decimalOf(item.get("revenue"))).reversed());
        return result;
    }

    public List<Map<String, Object>> getRevenueByStaff() {
        return getRevenueByStaff(null, null);
    }

    public List<Map<String, Object>> getRevenueByStaff(LocalDate fromDate, LocalDate toDate) {
        Map<Long, Staff> staffMap = staffRepository.findAll().stream()
                .collect(Collectors.toMap(Staff::getId, s -> s, (a, b) -> a, LinkedHashMap::new));

        Map<Long, Long> orderCountMap = new LinkedHashMap<>();
        Map<Long, BigDecimal> revenueMap = new LinkedHashMap<>();

        for (TailoringOrder order : getFilteredOrders(fromDate, toDate)) {
            Long key = order.getPrimaryStaffId() != null ? order.getPrimaryStaffId() : 0L;
            orderCountMap.merge(key, 1L, Long::sum);
            revenueMap.merge(key, safeDecimal(order.getTotalPrice()), BigDecimal::add);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Long> entry : orderCountMap.entrySet()) {
            Long staffId = entry.getKey();
            Staff staff = staffMap.get(staffId);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("staffId", staffId == 0L ? null : staffId);
            item.put("staffName", staff != null ? staff.getFullName() : "Chưa gán nhân viên");
            item.put("staffRole", staff != null ? staff.getStaffRole() : null);
            item.put("performanceScore", staff != null ? staff.getPerformanceScore() : BigDecimal.ZERO);
            item.put("conversionRate", staff != null ? staff.getConversionRate() : BigDecimal.ZERO);
            item.put("orderCount", entry.getValue());
            item.put("revenue", revenueMap.getOrDefault(staffId, BigDecimal.ZERO));
            result.add(item);
        }

        result.sort(Comparator.comparing((Map<String, Object> item) -> decimalOf(item.get("revenue"))).reversed());
        return result;
    }

    public List<Map<String, Object>> getStaffPerformance() {
        return getStaffPerformance(null, null);
    }

    public List<Map<String, Object>> getStaffPerformance(LocalDate fromDate, LocalDate toDate) {
        boolean filtered = hasDateFilter(fromDate, toDate);
        List<Lead> filteredLeads = getFilteredLeads(fromDate, toDate);
        List<TailoringOrder> filteredOrders = getFilteredOrders(fromDate, toDate);

        Map<Long, Long> periodLeads = filteredLeads.stream()
                .filter(lead -> lead.getAssignedStaffId() != null)
                .collect(Collectors.groupingBy(Lead::getAssignedStaffId, LinkedHashMap::new, Collectors.counting()));

        Map<Long, Long> periodConverted = filteredLeads.stream()
                .filter(lead -> lead.getAssignedStaffId() != null && "CONVERTED".equalsIgnoreCase(lead.getStatus()))
                .collect(Collectors.groupingBy(Lead::getAssignedStaffId, LinkedHashMap::new, Collectors.counting()));

        Map<Long, Long> periodOrders = filteredOrders.stream()
                .filter(order -> order.getPrimaryStaffId() != null)
                .collect(Collectors.groupingBy(TailoringOrder::getPrimaryStaffId, LinkedHashMap::new, Collectors.counting()));

        Map<Long, BigDecimal> periodRevenue = new LinkedHashMap<>();
        for (TailoringOrder order : filteredOrders) {
            if (order.getPrimaryStaffId() != null) {
                periodRevenue.merge(order.getPrimaryStaffId(), safeDecimal(order.getTotalPrice()), BigDecimal::add);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Staff staff : staffRepository.findAll()) {
            long leads = periodLeads.getOrDefault(staff.getId(), 0L);
            long converted = periodConverted.getOrDefault(staff.getId(), 0L);
            long orders = periodOrders.getOrDefault(staff.getId(), 0L);
            BigDecimal revenue = periodRevenue.getOrDefault(staff.getId(), BigDecimal.ZERO);
            BigDecimal targetAchievement = calculateTargetAchievement(filtered ? revenue : staff.getTotalRevenue(), staff.getMonthlyTarget());
            double scopedConversionRate = leads > 0 ? round((double) converted / (double) leads * 100.0) : 0.0;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("staffId", staff.getId());
            item.put("staffCode", staff.getStaffCode());
            item.put("staffName", staff.getFullName());
            item.put("staffRole", staff.getStaffRole());
            item.put("performanceScore", safeDecimal(staff.getPerformanceScore()));
            item.put("totalLeads", filtered ? leads : safeLong(staff.getTotalLeads()));
            item.put("totalConverted", filtered ? converted : safeLong(staff.getTotalConverted()));
            item.put("conversionRate", filtered ? BigDecimal.valueOf(scopedConversionRate) : safeDecimal(staff.getConversionRate()));
            item.put("totalRevenue", filtered ? revenue : safeDecimal(staff.getTotalRevenue()));
            item.put("monthlyTarget", safeDecimal(staff.getMonthlyTarget()));
            item.put("targetAchievement", targetAchievement);
            item.put("periodOrderCount", orders);
            item.put("avgOrderValue", orders > 0
                    ? revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            item.put("healthLevel", deriveHealthLevel(safeDecimal(staff.getPerformanceScore()), targetAchievement));
            result.add(item);
        }

        result.sort(Comparator
                .comparing((Map<String, Object> item) -> decimalOf(item.get("performanceScore"))).reversed()
                .thenComparing(item -> decimalOf(item.get("totalRevenue")), Comparator.reverseOrder()));

        for (int i = 0; i < result.size(); i++) {
            result.get(i).put("rank", i + 1);
        }
        return result;
    }

    public Map<String, Object> getLeadConversionAnalytics() {
        return getLeadConversionAnalytics(null, null);
    }

    public Map<String, Object> getLeadConversionAnalytics(LocalDate fromDate, LocalDate toDate) {
        List<Lead> filteredLeads = getFilteredLeads(fromDate, toDate);
        long totalLeads = filteredLeads.size();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Lead lead : filteredLeads) {
            String status = lead.getStatus() != null ? lead.getStatus() : "UNKNOWN";
            byStatus.merge(status, 1L, Long::sum);
        }

        long converted = byStatus.getOrDefault("CONVERTED", 0L);
        long lost = byStatus.getOrDefault("LOST", 0L);
        long active = totalLeads - converted - lost;
        double avgContactCount = totalLeads > 0
                ? round(filteredLeads.stream().mapToInt(lead -> lead.getContactCount() != null ? lead.getContactCount() : 0).average().orElse(0.0))
                : 0.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalLeads", totalLeads);
        result.put("convertedLeads", converted);
        result.put("lostLeads", lost);
        result.put("activeLeads", active);
        result.put("avgContactCount", avgContactCount);
        result.put("conversionRate", totalLeads > 0 ? round((double) converted / totalLeads * 100.0) : 0.0);
        result.put("leadStatusBreakdown", byStatus);
        return result;
    }

    public Map<String, Object> getMarketingEffectiveness() {
        return getMarketingEffectiveness(null, null);
    }

    public Map<String, Object> getMarketingEffectiveness(LocalDate fromDate, LocalDate toDate) {
        Map<Long, Channel> channelMap = channelRepository.findAll().stream()
                .collect(Collectors.toMap(Channel::getId, channel -> channel, (a, b) -> a, LinkedHashMap::new));

        Map<String, Long> leadByChannel = new LinkedHashMap<>();
        for (Lead lead : getFilteredLeads(fromDate, toDate)) {
            String channelCode = lead.getChannel() != null && lead.getChannel().getChannelCode() != null
                    ? lead.getChannel().getChannelCode()
                    : "UNKNOWN";
            leadByChannel.merge(channelCode, 1L, Long::sum);
        }

        Map<String, Map<String, Object>> mergedMap = new LinkedHashMap<>();
        for (Map<String, Object> row : getRevenueByChannel(fromDate, toDate)) {
            mergedMap.put(String.valueOf(row.get("channelCode")), new LinkedHashMap<>(row));
        }

        for (Map.Entry<String, Long> entry : leadByChannel.entrySet()) {
            if (!mergedMap.containsKey(entry.getKey())) {
                Channel channel = channelMap.values().stream()
                        .filter(item -> entry.getKey().equals(item.getChannelCode()))
                        .findFirst()
                        .orElse(null);

                Map<String, Object> emptyChannel = new LinkedHashMap<>();
                emptyChannel.put("channelId", channel != null ? channel.getId() : null);
                emptyChannel.put("channelCode", entry.getKey());
                emptyChannel.put("channelName", channel != null ? channel.getDisplayName() : entry.getKey());
                emptyChannel.put("orderCount", 0L);
                emptyChannel.put("revenue", BigDecimal.ZERO);
                mergedMap.put(entry.getKey(), emptyChannel);
            }
        }

        long totalLeadCount = leadByChannel.values().stream().mapToLong(Long::longValue).sum();
        List<Map<String, Object>> merged = new ArrayList<>();
        for (Map<String, Object> row : mergedMap.values()) {
            String channelCode = String.valueOf(row.get("channelCode"));
            long leadCount = leadByChannel.getOrDefault(channelCode, 0L);
            BigDecimal revenue = decimalOf(row.get("revenue"));
            long orderCount = safeLong(row.get("orderCount"));

            Map<String, Object> item = new LinkedHashMap<>(row);
            item.put("leadCount", leadCount);
            item.put("revenuePerLead", leadCount > 0
                    ? revenue.divide(BigDecimal.valueOf(leadCount), 2, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO);
            item.put("orderToLeadRate", leadCount > 0 ? round((double) orderCount / leadCount * 100.0) : 0.0);
            item.put("leadShare", totalLeadCount > 0 ? round((double) leadCount / totalLeadCount * 100.0) : 0.0);
            merged.add(item);
        }

        merged.sort(Comparator.comparing((Map<String, Object> item) -> decimalOf(item.get("revenue"))).reversed());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channels", merged);
        result.put("totalChannels", merged.size());
        result.put("topChannel", merged.isEmpty() ? null : merged.get(0));
        return result;
    }

    public Map<String, Object> getMonthlyTrend(LocalDate fromDate, LocalDate toDate) {
        LocalDate normalizedFrom = normalizeFromDate(fromDate, toDate);
        LocalDate normalizedTo = normalizeToDate(fromDate, toDate);

        LocalDate trendFrom = normalizedFrom != null ? normalizedFrom : YearMonth.now().minusMonths(5).atDay(1);
        LocalDate trendTo = normalizedTo != null ? normalizedTo : YearMonth.now().atEndOfMonth();

        Map<YearMonth, BigDecimal> revenueMap = new LinkedHashMap<>();
        Map<YearMonth, Long> orderMap = new LinkedHashMap<>();
        Map<YearMonth, Long> leadMap = new LinkedHashMap<>();

        YearMonth cursor = YearMonth.from(trendFrom);
        YearMonth endCursor = YearMonth.from(trendTo);
        while (!cursor.isAfter(endCursor)) {
            revenueMap.put(cursor, BigDecimal.ZERO);
            orderMap.put(cursor, 0L);
            leadMap.put(cursor, 0L);
            cursor = cursor.plusMonths(1);
        }

        for (TailoringOrder order : getFilteredOrders(trendFrom, trendTo)) {
            if (order.getOrderDate() == null) {
                continue;
            }
            YearMonth key = YearMonth.from(order.getOrderDate());
            revenueMap.merge(key, safeDecimal(order.getTotalPrice()), BigDecimal::add);
            orderMap.merge(key, 1L, Long::sum);
        }

        for (Lead lead : getFilteredLeads(trendFrom, trendTo)) {
            if (lead.getCreatedAt() == null) {
                continue;
            }
            YearMonth key = YearMonth.from(lead.getCreatedAt().toLocalDate());
            leadMap.merge(key, 1L, Long::sum);
        }

        List<String> labels = new ArrayList<>();
        List<BigDecimal> revenue = new ArrayList<>();
        List<Long> orders = new ArrayList<>();
        List<Long> leads = new ArrayList<>();
        DateTimeFormatter monthFormat = DateTimeFormatter.ofPattern("MM/yyyy");

        for (YearMonth yearMonth : revenueMap.keySet()) {
            labels.add(yearMonth.atDay(1).format(monthFormat));
            revenue.add(revenueMap.getOrDefault(yearMonth, BigDecimal.ZERO));
            orders.add(orderMap.getOrDefault(yearMonth, 0L));
            leads.add(leadMap.getOrDefault(yearMonth, 0L));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("labels", labels);
        result.put("revenue", revenue);
        result.put("orders", orders);
        result.put("leads", leads);
        return result;
    }

    public Map<String, Object> getFullAnalyticsDashboard() {
        return getFullAnalyticsDashboard(null, null);
    }

    public Map<String, Object> getFullAnalyticsDashboard(LocalDate fromDate, LocalDate toDate) {
        LocalDate normalizedFrom = normalizeFromDate(fromDate, toDate);
        LocalDate normalizedTo = normalizeToDate(fromDate, toDate);

        Map<String, Object> overview = getOverview(normalizedFrom, normalizedTo);
        List<Map<String, Object>> revenueByChannel = getRevenueByChannel(normalizedFrom, normalizedTo);
        List<Map<String, Object>> revenueByStaff = getRevenueByStaff(normalizedFrom, normalizedTo);
        List<Map<String, Object>> staffPerformance = getStaffPerformance(normalizedFrom, normalizedTo);
        Map<String, Object> leadConversion = getLeadConversionAnalytics(normalizedFrom, normalizedTo);
        Map<String, Object> marketingEffectiveness = getMarketingEffectiveness(normalizedFrom, normalizedTo);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("overview", overview);
        result.put("revenueByChannel", revenueByChannel);
        result.put("revenueByStaff", revenueByStaff);
        result.put("staffPerformance", staffPerformance);
        result.put("leadConversion", leadConversion);
        result.put("marketingEffectiveness", marketingEffectiveness);
        result.put("monthlyTrend", getMonthlyTrend(normalizedFrom, normalizedTo));
        result.put("orderStatusBreakdown", buildOrderStatusBreakdown(getFilteredOrders(normalizedFrom, normalizedTo)));
        result.put("topInsights", buildTopInsights(overview, marketingEffectiveness, staffPerformance));

        Map<String, Object> appliedFilter = new LinkedHashMap<>();
        appliedFilter.put("from", normalizedFrom != null ? normalizedFrom.toString() : null);
        appliedFilter.put("to", normalizedTo != null ? normalizedTo.toString() : null);
        appliedFilter.put("label", buildRangeLabel(normalizedFrom, normalizedTo));
        result.put("appliedFilter", appliedFilter);

        return result;
    }

    private Map<String, Long> buildOrderStatusBreakdown(List<TailoringOrder> orders) {
        Map<String, Long> orderStatusMap = new LinkedHashMap<>();
        for (TailoringOrder order : orders) {
            String status = order.getStatus() != null ? order.getStatus() : "UNKNOWN";
            orderStatusMap.merge(status, 1L, Long::sum);
        }
        return orderStatusMap;
    }

    private Map<String, Object> buildTopInsights(Map<String, Object> overview,
                                                 Map<String, Object> marketingEffectiveness,
                                                 List<Map<String, Object>> staffPerformance) {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> topChannel = marketingEffectiveness != null
                ? (Map<String, Object>) marketingEffectiveness.get("topChannel")
                : null;
        Map<String, Object> topStaff = staffPerformance != null && !staffPerformance.isEmpty()
                ? staffPerformance.get(0)
                : null;

        result.put("bestChannelName", topChannel != null ? topChannel.get("channelName") : "Chưa có dữ liệu");
        result.put("bestChannelRevenue", topChannel != null ? topChannel.get("revenue") : BigDecimal.ZERO);
        result.put("bestChannelConversion", topChannel != null ? topChannel.get("orderToLeadRate") : 0.0);
        result.put("bestStaffName", topStaff != null ? topStaff.get("staffName") : "Chưa có dữ liệu");
        result.put("bestStaffScore", topStaff != null ? topStaff.get("performanceScore") : BigDecimal.ZERO);
        result.put("avgOrderValue", overview.getOrDefault("avgRevenuePerOrder", BigDecimal.ZERO));
        result.put("completionRate", overview.getOrDefault("completionRate", 0.0));
        return result;
    }

    private List<TailoringOrder> getFilteredOrders(LocalDate fromDate, LocalDate toDate) {
        return orderRepository.findAll().stream()
                .filter(order -> isInRange(order.getOrderDate(), fromDate, toDate))
                .collect(Collectors.toList());
    }

    private List<Lead> getFilteredLeads(LocalDate fromDate, LocalDate toDate) {
        return leadRepository.findAll().stream()
                .filter(lead -> isInRange(lead.getCreatedAt() != null ? lead.getCreatedAt().toLocalDate() : null, fromDate, toDate))
                .collect(Collectors.toList());
    }

    private boolean isInRange(LocalDate value, LocalDate fromDate, LocalDate toDate) {
        if (value == null) {
            return !hasDateFilter(fromDate, toDate);
        }
        if (fromDate != null && value.isBefore(fromDate)) {
            return false;
        }
        return toDate == null || !value.isAfter(toDate);
    }

    private boolean hasDateFilter(LocalDate fromDate, LocalDate toDate) {
        return fromDate != null || toDate != null;
    }

    private LocalDate normalizeFromDate(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return toDate;
        }
        return fromDate;
    }

    private LocalDate normalizeToDate(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            return fromDate;
        }
        return toDate;
    }

    private String buildRangeLabel(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null && toDate == null) {
            return "Toàn bộ dữ liệu";
        }
        if (fromDate != null && toDate != null && fromDate.equals(toDate)) {
            return "Ngày " + fromDate.format(DATE_FORMATTER);
        }
        if (fromDate != null && toDate != null) {
            return fromDate.format(DATE_FORMATTER) + " → " + toDate.format(DATE_FORMATTER);
        }
        if (fromDate != null) {
            return "Từ " + fromDate.format(DATE_FORMATTER) + " đến nay";
        }
        return "Đến " + toDate.format(DATE_FORMATTER);
    }

    private String deriveHealthLevel(BigDecimal performanceScore, BigDecimal targetAchievement) {
        if (performanceScore.compareTo(BigDecimal.valueOf(80)) >= 0 && targetAchievement.compareTo(BigDecimal.valueOf(100)) >= 0) {
            return "EXCELLENT";
        }
        if (performanceScore.compareTo(BigDecimal.valueOf(60)) >= 0 || targetAchievement.compareTo(BigDecimal.valueOf(70)) >= 0) {
            return "GOOD";
        }
        return "NEEDS_ATTENTION";
    }

    private BigDecimal calculateTargetAchievement(BigDecimal revenue, BigDecimal target) {
        if (target == null || target.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal safeRevenue = revenue != null ? revenue : BigDecimal.ZERO;
        return safeRevenue.multiply(BigDecimal.valueOf(100)).divide(target, 2, RoundingMode.HALF_UP);
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private BigDecimal decimalOf(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private long safeLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private double round(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
