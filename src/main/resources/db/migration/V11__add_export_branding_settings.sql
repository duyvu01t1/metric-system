-- Branding settings for Analytics Excel/PDF export
INSERT INTO settings (setting_key, setting_value, description, category, data_type, is_editable)
VALUES
    ('branding.logo_path', '', 'Absolute path to shop logo for report export (PNG/JPG)', 'ANALYTICS', 'STRING', true),
    ('branding.report_footer_text', 'Báo cáo nội bộ - Metric System', 'Footer text shown in exported analytics reports', 'ANALYTICS', 'STRING', true)
ON CONFLICT (setting_key) DO NOTHING;
