-- Create Settings table
CREATE TABLE IF NOT EXISTS settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(255) NOT NULL UNIQUE,
    setting_value TEXT,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    data_type VARCHAR(50) DEFAULT 'STRING',
    is_editable BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create index on setting_key for fast lookups
CREATE INDEX IF NOT EXISTS idx_settings_key ON settings(setting_key);
CREATE INDEX IF NOT EXISTS idx_settings_category ON settings(category);

-- Insert default settings
INSERT INTO settings (setting_key, setting_value, description, category, data_type, is_editable)
VALUES
    -- GENERAL Settings
    ('app_name', 'Metric Tailoring System', 'Application name', 'GENERAL', 'STRING', true),
    ('app_version', '1.0.0', 'Application version', 'GENERAL', 'STRING', false),
    ('timezone', 'UTC', 'Default timezone for the application', 'GENERAL', 'STRING', true),
    ('date_format', 'yyyy-MM-dd', 'Date format for display', 'GENERAL', 'STRING', true),
    
    -- BUSINESS Settings
    ('shop_name', 'Premium Tailoring Shop', 'Official shop name', 'BUSINESS', 'STRING', true),
    ('shop_phone', '+1-800-TAILOR', 'Shop contact phone', 'BUSINESS', 'STRING', true),
    ('shop_email', 'info@tailorshop.com', 'Shop email address', 'BUSINESS', 'STRING', true),
    ('shop_address', '123 Fashion Street', 'Shop physical address', 'BUSINESS', 'STRING', true),
    ('shop_city', 'New York', 'Shop city', 'BUSINESS', 'STRING', true),
    ('shop_country', 'USA', 'Shop country', 'BUSINESS', 'STRING', true),
    ('tax_rate', '8.5', 'Sales tax rate percentage', 'BUSINESS', 'DECIMAL', true),
    ('currency_symbol', '$', 'Currency symbol used for pricing', 'BUSINESS', 'STRING', true),
    ('default_payment_method', 'CASH', 'Default payment method', 'BUSINESS', 'STRING', true),
    
    -- EMAIL Settings
    ('smtp_host', 'smtp.gmail.com', 'SMTP server host', 'EMAIL', 'STRING', true),
    ('smtp_port', '587', 'SMTP server port', 'EMAIL', 'INT', true),
    ('smtp_username', 'your-email@gmail.com', 'SMTP authentication username', 'EMAIL', 'STRING', true),
    ('smtp_password', '', 'SMTP authentication password', 'EMAIL', 'STRING', true),
    ('email_from', 'noreply@tailorshop.com', 'Default sender email address', 'EMAIL', 'STRING', true),
    ('enable_email_notifications', 'true', 'Enable email notifications', 'EMAIL', 'BOOLEAN', true),
    ('order_confirmation_email', 'true', 'Send order confirmation emails', 'EMAIL', 'BOOLEAN', true),
    ('payment_receipt_email', 'true', 'Send payment receipt emails', 'EMAIL', 'BOOLEAN', true),
    
    -- PAYMENT Settings
    ('stripe_publishable_key', '', 'Stripe publishable key for payments', 'PAYMENT', 'STRING', true),
    ('stripe_secret_key', '', 'Stripe secret key for payments', 'PAYMENT', 'STRING', true),
    ('paypal_client_id', '', 'PayPal client ID', 'PAYMENT', 'STRING', true),
    ('accepted_payment_methods', 'CASH,CARD,CHECK,BANK_TRANSFER', 'Accepted payment methods', 'PAYMENT', 'STRING', true),
    ('minimum_payment_amount', '5.00', 'Minimum payment amount allowed', 'PAYMENT', 'DECIMAL', true),
    ('payment_processing_fee', '2.9', 'Payment processing fee percentage', 'PAYMENT', 'DECIMAL', true),
    
    -- SYSTEM Settings
    ('max_upload_size_mb', '50', 'Maximum file upload size in MB', 'SYSTEM', 'INT', true),
    ('session_timeout_minutes', '30', 'Session timeout in minutes', 'SYSTEM', 'INT', true),
    ('enable_api_logging', 'true', 'Enable API request logging', 'SYSTEM', 'BOOLEAN', false),
    ('database_backup_enabled', 'true', 'Enable automatic database backups', 'SYSTEM', 'BOOLEAN', true),
    ('maintenance_mode', 'false', 'Enable maintenance mode', 'SYSTEM', 'BOOLEAN', true),
    ('system_debug_mode', 'false', 'Enable debug mode', 'SYSTEM', 'BOOLEAN', false)
ON CONFLICT (setting_key) DO NOTHING;

-- Create trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_settings_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS settings_updated_at_trigger ON settings;
CREATE TRIGGER settings_updated_at_trigger
BEFORE UPDATE ON settings
FOR EACH ROW
EXECUTE FUNCTION update_settings_timestamp();
