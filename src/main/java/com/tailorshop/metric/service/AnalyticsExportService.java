package com.tailorshop.metric.service;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.tailorshop.metric.repository.SettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.ClientAnchor;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Drawing;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Picture;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * Real export generation for Analytics dashboard.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsExportService {

    private final AnalyticsService analyticsService;
    private final SettingsRepository settingsRepository;

    public byte[] exportExcel() {
        return exportExcel(null, null);
    }

    public byte[] exportExcel(java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        Map<String, Object> dashboard = analyticsService.getFullAnalyticsDashboard(fromDate, toDate);
        String periodLabel = extractPeriodLabel(dashboard);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            var headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle titleStyle = workbook.createCellStyle();
            titleStyle.setAlignment(HorizontalAlignment.CENTER);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            var titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleStyle.setFont(titleFont);

            CellStyle infoStyle = workbook.createCellStyle();
            infoStyle.setAlignment(HorizontalAlignment.LEFT);
            infoStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            createOverviewSheet(workbook, headerStyle, titleStyle, infoStyle, (Map<String, Object>) dashboard.get("overview"), periodLabel);
            createChannelSheet(workbook, headerStyle, titleStyle, infoStyle, (List<Map<String, Object>>) ((Map<String, Object>) dashboard.get("marketingEffectiveness")).get("channels"), periodLabel);
            createStaffSheet(workbook, headerStyle, titleStyle, infoStyle, (List<Map<String, Object>>) dashboard.get("staffPerformance"), periodLabel);
            createLeadSheet(workbook, headerStyle, titleStyle, infoStyle, (Map<String, Object>) dashboard.get("leadConversion"), periodLabel);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to export analytics Excel", e);
            throw new RuntimeException("Không thể xuất file Excel analytics", e);
        }
    }

    public byte[] exportPdf() {
        return exportPdf(null, null);
    }

    public byte[] exportPdf(java.time.LocalDate fromDate, java.time.LocalDate toDate) {
        Map<String, Object> dashboard = analyticsService.getFullAnalyticsDashboard(fromDate, toDate);
        String periodLabel = extractPeriodLabel(dashboard);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 30, 28);
            BaseFont baseFont = loadUnicodeBaseFont();
            String shopName = getSettingValue("shop_name", "Metric Tailoring System");
            String footerText = getSettingValue("branding.report_footer_text", "Báo cáo nội bộ - Metric System");

            PdfWriter writer = PdfWriter.getInstance(document, out);
            writer.setPageEvent(new BrandedPdfPageEvent(baseFont, shopName, footerText));
            document.open();

            Font brandFont = new Font(baseFont, 16, Font.BOLD, new Color(25, 118, 210));
            Font titleFont = new Font(baseFont, 18, Font.BOLD, new Color(25, 118, 210));
            Font subTitleFont = new Font(baseFont, 10, Font.NORMAL, Color.DARK_GRAY);
            Font sectionFont = new Font(baseFont, 13, Font.BOLD, new Color(56, 142, 60));
            Font textFont = new Font(baseFont, 10, Font.NORMAL, Color.BLACK);
            Font tableHeaderFont = new Font(baseFont, 10, Font.BOLD, Color.WHITE);

            tryAddLogoToPdf(document);

            Paragraph brand = new Paragraph(shopName.toUpperCase(), brandFont);
            brand.setAlignment(Element.ALIGN_CENTER);
            brand.setSpacingAfter(4f);
            document.add(brand);

            Paragraph title = new Paragraph("BÁO CÁO PHÂN TÍCH & ĐÁNH GIÁ", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(6f);
            document.add(title);

            Paragraph subTitle = new Paragraph(
                    buildContactLine() + "\nKỳ báo cáo: " + periodLabel +
                            "\nXuất lúc: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")),
                    subTitleFont
            );
            subTitle.setAlignment(Element.ALIGN_CENTER);
            subTitle.setSpacingAfter(12f);
            document.add(subTitle);

            Map<String, Object> overview = (Map<String, Object>) dashboard.get("overview");
            document.add(sectionParagraph("1. Tổng quan điều hành", sectionFont));
            PdfPTable overviewTable = new PdfPTable(new float[]{3f, 2f});
            overviewTable.setWidthPercentage(100);
            addHeader(overviewTable, "Chỉ số", tableHeaderFont);
            addHeader(overviewTable, "Giá trị", tableHeaderFont);
            addCell(overviewTable, "Tổng doanh thu", textFont, Element.ALIGN_LEFT);
            addCell(overviewTable, fmtMoney(overview.get("totalRevenue")), textFont, Element.ALIGN_RIGHT);
            addCell(overviewTable, "Tổng đơn hàng", textFont, Element.ALIGN_LEFT);
            addCell(overviewTable, String.valueOf(overview.get("totalOrders")), textFont, Element.ALIGN_RIGHT);
            addCell(overviewTable, "Tổng lead", textFont, Element.ALIGN_LEFT);
            addCell(overviewTable, String.valueOf(overview.get("totalLeads")), textFont, Element.ALIGN_RIGHT);
            addCell(overviewTable, "Lead trong tháng", textFont, Element.ALIGN_LEFT);
            addCell(overviewTable, String.valueOf(overview.get("monthlyLeads")), textFont, Element.ALIGN_RIGHT);
            addCell(overviewTable, "Tỷ lệ chuyển đổi", textFont, Element.ALIGN_LEFT);
            addCell(overviewTable, pct(overview.get("leadConversionRate")), textFont, Element.ALIGN_RIGHT);
            document.add(overviewTable);
            document.add(spacer());

            document.add(sectionParagraph("2. Doanh thu theo kênh marketing", sectionFont));
            PdfPTable channelTable = new PdfPTable(new float[]{3f, 1.2f, 1.2f, 2f, 1.6f, 1.6f});
            channelTable.setWidthPercentage(100);
            addHeader(channelTable, "Kênh", tableHeaderFont);
            addHeader(channelTable, "Lead", tableHeaderFont);
            addHeader(channelTable, "Đơn", tableHeaderFont);
            addHeader(channelTable, "Doanh thu", tableHeaderFont);
            addHeader(channelTable, "DT/Lead", tableHeaderFont);
            addHeader(channelTable, "Tỷ lệ chốt", tableHeaderFont);
            for (Map<String, Object> row : (List<Map<String, Object>>) ((Map<String, Object>) dashboard.get("marketingEffectiveness")).get("channels")) {
                addCell(channelTable, String.valueOf(row.get("channelName")), textFont, Element.ALIGN_LEFT);
                addCell(channelTable, String.valueOf(row.get("leadCount")), textFont, Element.ALIGN_RIGHT);
                addCell(channelTable, String.valueOf(row.get("orderCount")), textFont, Element.ALIGN_RIGHT);
                addCell(channelTable, fmtMoney(row.get("revenue")), textFont, Element.ALIGN_RIGHT);
                addCell(channelTable, fmtMoney(row.get("revenuePerLead")), textFont, Element.ALIGN_RIGHT);
                addCell(channelTable, pct(row.get("orderToLeadRate")), textFont, Element.ALIGN_RIGHT);
            }
            document.add(channelTable);
            document.add(spacer());

            document.add(sectionParagraph("3. Hiệu suất nhân sự", sectionFont));
            PdfPTable staffTable = new PdfPTable(new float[]{3f, 1.5f, 1.5f, 1.2f, 1.3f, 2f});
            staffTable.setWidthPercentage(100);
            addHeader(staffTable, "Nhân viên", tableHeaderFont);
            addHeader(staffTable, "Vai trò", tableHeaderFont);
            addHeader(staffTable, "Performance", tableHeaderFont);
            addHeader(staffTable, "Lead", tableHeaderFont);
            addHeader(staffTable, "CR", tableHeaderFont);
            addHeader(staffTable, "Doanh thu", tableHeaderFont);
            for (Map<String, Object> row : (List<Map<String, Object>>) dashboard.get("staffPerformance")) {
                addCell(staffTable, String.valueOf(row.get("staffName")), textFont, Element.ALIGN_LEFT);
                addCell(staffTable, String.valueOf(row.get("staffRole")), textFont, Element.ALIGN_CENTER);
                addCell(staffTable, String.valueOf(row.get("performanceScore")), textFont, Element.ALIGN_RIGHT);
                addCell(staffTable, String.valueOf(row.get("totalLeads")), textFont, Element.ALIGN_RIGHT);
                addCell(staffTable, pct(row.get("conversionRate")), textFont, Element.ALIGN_RIGHT);
                addCell(staffTable, fmtMoney(row.get("totalRevenue")), textFont, Element.ALIGN_RIGHT);
            }
            document.add(staffTable);
            document.add(spacer());

            Map<String, Object> leadConversion = (Map<String, Object>) dashboard.get("leadConversion");
            document.add(sectionParagraph("4. Chuyển đổi lead", sectionFont));
            PdfPTable leadTable = new PdfPTable(new float[]{3f, 2f});
            leadTable.setWidthPercentage(60);
            addHeader(leadTable, "Hạng mục", tableHeaderFont);
            addHeader(leadTable, "Giá trị", tableHeaderFont);
            addCell(leadTable, "Lead chuyển đổi", textFont, Element.ALIGN_LEFT);
            addCell(leadTable, String.valueOf(leadConversion.get("convertedLeads")), textFont, Element.ALIGN_RIGHT);
            addCell(leadTable, "Lead bị mất", textFont, Element.ALIGN_LEFT);
            addCell(leadTable, String.valueOf(leadConversion.get("lostLeads")), textFont, Element.ALIGN_RIGHT);
            addCell(leadTable, "Lead đang hoạt động", textFont, Element.ALIGN_LEFT);
            addCell(leadTable, String.valueOf(leadConversion.get("activeLeads")), textFont, Element.ALIGN_RIGHT);
            addCell(leadTable, "Tỷ lệ chuyển đổi", textFont, Element.ALIGN_LEFT);
            addCell(leadTable, pct(leadConversion.get("conversionRate")), textFont, Element.ALIGN_RIGHT);
            document.add(leadTable);

            document.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to export analytics PDF", e);
            throw new RuntimeException("Không thể xuất file PDF analytics", e);
        }
    }

    private void createOverviewSheet(XSSFWorkbook workbook, CellStyle headerStyle, CellStyle titleStyle,
                                     CellStyle infoStyle, Map<String, Object> overview, String periodLabel) {
        XSSFSheet sheet = workbook.createSheet("Overview");
        applySheetBranding(workbook, sheet, "Tổng quan Analytics", titleStyle, infoStyle, 2, periodLabel);

        Row header = sheet.createRow(4);
        header.createCell(0).setCellValue("Metric");
        header.createCell(1).setCellValue("Value");
        header.getCell(0).setCellStyle(headerStyle);
        header.getCell(1).setCellStyle(headerStyle);

        String[][] rows = {
                {"Tổng doanh thu", fmtMoney(overview.get("totalRevenue"))},
                {"Tổng đơn hàng", String.valueOf(overview.get("totalOrders"))},
                {"Tổng lead", String.valueOf(overview.get("totalLeads"))},
                {"Lead tháng này", String.valueOf(overview.get("monthlyLeads"))},
                {"Tỷ lệ chuyển đổi", pct(overview.get("leadConversionRate"))}
        };
        for (int i = 0; i < rows.length; i++) {
            Row row = sheet.createRow(i + 5);
            row.createCell(0).setCellValue(rows[i][0]);
            row.createCell(1).setCellValue(rows[i][1]);
        }
        sheet.createFreezePane(0, 5);
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        tryAddLogo(workbook, sheet);
    }

    private void createChannelSheet(XSSFWorkbook workbook, CellStyle headerStyle, CellStyle titleStyle,
                                    CellStyle infoStyle, List<Map<String, Object>> rows, String periodLabel) {
        XSSFSheet sheet = workbook.createSheet("RevenueByChannel");
        applySheetBranding(workbook, sheet, "Doanh thu theo kênh", titleStyle, infoStyle, 5, periodLabel);

        Row header = sheet.createRow(4);
        String[] headers = {"Kênh", "Lead", "Đơn", "Doanh thu", "Doanh thu/Lead", "Tỷ lệ chốt"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> rowData = rows.get(i);
            Row row = sheet.createRow(i + 5);
            row.createCell(0).setCellValue(String.valueOf(rowData.get("channelName")));
            row.createCell(1).setCellValue(String.valueOf(rowData.get("leadCount")));
            row.createCell(2).setCellValue(String.valueOf(rowData.get("orderCount")));
            row.createCell(3).setCellValue(fmtMoney(rowData.get("revenue")));
            row.createCell(4).setCellValue(fmtMoney(rowData.get("revenuePerLead")));
            row.createCell(5).setCellValue(pct(rowData.get("orderToLeadRate")));
        }
        sheet.createFreezePane(0, 5);
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
        tryAddLogo(workbook, sheet);
    }

    private void createStaffSheet(XSSFWorkbook workbook, CellStyle headerStyle, CellStyle titleStyle,
                                  CellStyle infoStyle, List<Map<String, Object>> rows, String periodLabel) {
        XSSFSheet sheet = workbook.createSheet("StaffPerformance");
        applySheetBranding(workbook, sheet, "Hiệu suất nhân sự", titleStyle, infoStyle, 6, periodLabel);

        Row header = sheet.createRow(4);
        String[] headers = {"Nhân viên", "Role", "Performance", "Lead", "Converted", "CR", "Doanh thu"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        for (int i = 0; i < rows.size(); i++) {
            Map<String, Object> rowData = rows.get(i);
            Row row = sheet.createRow(i + 5);
            row.createCell(0).setCellValue(String.valueOf(rowData.get("staffName")));
            row.createCell(1).setCellValue(String.valueOf(rowData.get("staffRole")));
            row.createCell(2).setCellValue(String.valueOf(rowData.get("performanceScore")));
            row.createCell(3).setCellValue(String.valueOf(rowData.get("totalLeads")));
            row.createCell(4).setCellValue(String.valueOf(rowData.get("totalConverted")));
            row.createCell(5).setCellValue(pct(rowData.get("conversionRate")));
            row.createCell(6).setCellValue(fmtMoney(rowData.get("totalRevenue")));
        }
        sheet.createFreezePane(0, 5);
        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
        tryAddLogo(workbook, sheet);
    }

    private void createLeadSheet(XSSFWorkbook workbook, CellStyle headerStyle, CellStyle titleStyle,
                                 CellStyle infoStyle, Map<String, Object> leadConversion, String periodLabel) {
        XSSFSheet sheet = workbook.createSheet("LeadConversion");
        applySheetBranding(workbook, sheet, "Phân tích chuyển đổi lead", titleStyle, infoStyle, 2, periodLabel);

        Row header = sheet.createRow(4);
        header.createCell(0).setCellValue("Metric");
        header.createCell(1).setCellValue("Value");
        header.getCell(0).setCellStyle(headerStyle);
        header.getCell(1).setCellStyle(headerStyle);

        String[][] rows = {
                {"Tổng lead", String.valueOf(leadConversion.get("totalLeads"))},
                {"Lead converted", String.valueOf(leadConversion.get("convertedLeads"))},
                {"Lead lost", String.valueOf(leadConversion.get("lostLeads"))},
                {"Lead active", String.valueOf(leadConversion.get("activeLeads"))},
                {"Tỷ lệ chuyển đổi", pct(leadConversion.get("conversionRate"))}
        };
        for (int i = 0; i < rows.length; i++) {
            Row row = sheet.createRow(i + 5);
            row.createCell(0).setCellValue(rows[i][0]);
            row.createCell(1).setCellValue(rows[i][1]);
        }
        sheet.createFreezePane(0, 5);
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
        tryAddLogo(workbook, sheet);
    }

    private void applySheetBranding(XSSFWorkbook workbook, XSSFSheet sheet, String reportTitle,
                                   CellStyle titleStyle, CellStyle infoStyle, int mergeEndColumn, String periodLabel) {
        String shopName = getSettingValue("shop_name", "Metric Tailoring System");
        String contactLine = buildContactLine();

        Row titleRow = sheet.createRow(0);
        titleRow.createCell(0).setCellValue(shopName + " - " + reportTitle);
        titleRow.getCell(0).setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, mergeEndColumn));

        Row contactRow = sheet.createRow(1);
        contactRow.createCell(0).setCellValue(contactLine);
        contactRow.getCell(0).setCellStyle(infoStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, mergeEndColumn));

        Row generatedRow = sheet.createRow(2);
        generatedRow.createCell(0).setCellValue("Kỳ báo cáo: " + periodLabel + " | Xuất lúc: " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
        generatedRow.getCell(0).setCellStyle(infoStyle);
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, mergeEndColumn));

        sheet.getHeader().setCenter(shopName + " - Analytics Report");
        sheet.getFooter().setLeft(getSettingValue("branding.report_footer_text", "Báo cáo nội bộ - Metric System"));
        sheet.getFooter().setRight("Trang &P / &N");
    }

    private void tryAddLogo(XSSFWorkbook workbook, XSSFSheet sheet) {
        String logoPath = getSettingValue("branding.logo_path", "");
        if (logoPath.isBlank() || !Files.exists(Path.of(logoPath))) {
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(Path.of(logoPath));
            int pictureType = detectPictureType(logoPath);
            int pictureIdx = workbook.addPicture(bytes, pictureType);
            CreationHelper helper = workbook.getCreationHelper();
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = helper.createClientAnchor();
            anchor.setCol1(7);
            anchor.setRow1(0);
            anchor.setCol2(8);
            anchor.setRow2(3);
            Picture picture = drawing.createPicture(anchor, pictureIdx);
            picture.resize(0.8);
        } catch (Exception ex) {
            log.warn("Cannot add logo to Excel export", ex);
        }
    }

    private int detectPictureType(String logoPath) {
        String lower = logoPath.toLowerCase();
        if (lower.endsWith(".png")) {
            return Workbook.PICTURE_TYPE_PNG;
        }
        return Workbook.PICTURE_TYPE_JPEG;
    }

    private BaseFont loadUnicodeBaseFont() {
        String[] fontCandidates = {
                "C:/Windows/Fonts/arial.ttf",
                "C:/Windows/Fonts/tahoma.ttf",
                "C:/Windows/Fonts/verdana.ttf"
        };

        for (String fontPath : fontCandidates) {
            if (Files.exists(Path.of(fontPath))) {
                try {
                    return BaseFont.createFont(fontPath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
                } catch (Exception ex) {
                    log.warn("Cannot load font {} for PDF export", fontPath, ex);
                }
            }
        }

        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception ex) {
            throw new RuntimeException("Không thể nạp font PDF", ex);
        }
    }

    private String getSettingValue(String key, String defaultValue) {
        return settingsRepository.findBySettingKey(key)
                .map(s -> s.getSettingValue())
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue);
    }

    private String extractPeriodLabel(Map<String, Object> dashboard) {
        Object appliedFilter = dashboard.get("appliedFilter");
        if (appliedFilter instanceof Map<?, ?> filterMap && filterMap.get("label") != null) {
            return String.valueOf(filterMap.get("label"));
        }

        Object overview = dashboard.get("overview");
        if (overview instanceof Map<?, ?> overviewMap && overviewMap.get("rangeLabel") != null) {
            return String.valueOf(overviewMap.get("rangeLabel"));
        }
        return "Toàn bộ dữ liệu";
    }

    private String buildContactLine() {
        return String.join(" | ",
                getSettingValue("shop_name", "Metric Tailoring System"),
                getSettingValue("shop_phone", "N/A"),
                getSettingValue("shop_email", "N/A"),
                getSettingValue("shop_address", ""));
    }

    private void tryAddLogoToPdf(Document document) {
        String logoPath = getSettingValue("branding.logo_path", "");
        if (logoPath.isBlank() || !Files.exists(Path.of(logoPath))) {
            return;
        }

        try {
            Image logo = Image.getInstance(logoPath);
            logo.scaleToFit(64, 64);
            logo.setAlignment(Image.ALIGN_CENTER);
            document.add(logo);
        } catch (Exception ex) {
            log.warn("Cannot add logo to PDF export", ex);
        }
    }

    private Paragraph sectionParagraph(String text, Font font) {
        Paragraph paragraph = new Paragraph(text, font);
        paragraph.setSpacingBefore(4f);
        paragraph.setSpacingAfter(6f);
        return paragraph;
    }

    private Paragraph spacer() {
        Paragraph paragraph = new Paragraph(" ");
        paragraph.setSpacingAfter(6f);
        return paragraph;
    }

    private void addHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(25, 118, 210));
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private static class BrandedPdfPageEvent extends PdfPageEventHelper {
        private final BaseFont baseFont;
        private final String shopName;
        private final String footerText;

        private BrandedPdfPageEvent(BaseFont baseFont, String shopName, String footerText) {
            this.baseFont = baseFont;
            this.shopName = shopName;
            this.footerText = footerText;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte canvas = writer.getDirectContent();
            Font footerFont = new Font(baseFont, 8, Font.NORMAL, Color.DARK_GRAY);
            ColumnText.showTextAligned(canvas, Element.ALIGN_LEFT,
                    new Phrase(shopName + " | " + footerText, footerFont),
                    document.left(), document.bottom() - 8, 0);
            ColumnText.showTextAligned(canvas, Element.ALIGN_RIGHT,
                    new Phrase("Trang " + writer.getPageNumber(), footerFont),
                    document.right(), document.bottom() - 8, 0);
        }
    }

    private String fmtMoney(Object value) {
        if (value == null) {
            return "0 ₫";
        }
        BigDecimal number = value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(String.valueOf(value));
        return number.setScale(0, java.math.RoundingMode.HALF_UP).toPlainString() + " ₫";
    }

    private String pct(Object value) {
        if (value == null) {
            return "0%";
        }
        BigDecimal number = new BigDecimal(String.valueOf(value));
        return number.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString() + "%";
    }
}
