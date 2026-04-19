-- =====================================================
-- TAILORING MANAGEMENT SYSTEM - DATABASE SCHEMA
-- PostgreSQL Script
-- =====================================================

-- Drop existing schema if needed
-- DROP SCHEMA IF EXISTS metric_system CASCADE;
-- CREATE SCHEMA metric_system;

-- Set default schema
-- SET search_path TO metric_system, public;

-- =====================================================
-- 1. USER AND AUTHENTICATION TABLES
-- =====================================================

-- User Roles Table
CREATE TABLE IF NOT EXISTS user_roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Users Table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(20),
    avatar_url VARCHAR(500),
    oauth_provider VARCHAR(50), -- 'GOOGLE', 'AZURE', 'LOCAL'
    oauth_id VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    is_locked BOOLEAN DEFAULT FALSE,
    last_login TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT
);

-- User Roles Mapping Table
CREATE TABLE IF NOT EXISTS user_role_mappings (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES user_roles(id) ON DELETE CASCADE,
    UNIQUE(user_id, role_id)
);

-- =====================================================
-- 2. CUSTOMER INFORMATION TABLES
-- =====================================================

-- Customers Table
CREATE TABLE IF NOT EXISTS customers (
    id BIGSERIAL PRIMARY KEY,
    customer_code VARCHAR(100) NOT NULL UNIQUE,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255),
    phone VARCHAR(20) NOT NULL,
    address VARCHAR(500),
    city VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100),
    identification_number VARCHAR(50),
    identification_type VARCHAR(50), -- ID_CARD, PASSPORT, etc.
    date_of_birth DATE,
    gender VARCHAR(20), -- MALE, FEMALE, OTHER
    notes TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- =====================================================
-- 3. TAILORING MEASUREMENT TABLES
-- =====================================================

-- Tailoring Orders Table
CREATE TABLE IF NOT EXISTS tailoring_orders (
    id BIGSERIAL PRIMARY KEY,
    order_code VARCHAR(100) NOT NULL UNIQUE,
    customer_id BIGINT NOT NULL,
    order_date DATE NOT NULL,
    promised_date DATE,
    completed_date DATE,
    order_type VARCHAR(50) NOT NULL, -- SUIT, SHIRT, PANTS, DRESS, CUSTOM
    description TEXT,
    quantity INT DEFAULT 1,
    unit_price DECIMAL(15, 2),
    total_price DECIMAL(15, 2),
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    payment_status VARCHAR(50) DEFAULT 'UNPAID', -- UNPAID, PARTIAL, PAID
    notes TEXT,
    is_archived BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_by BIGINT,
    FOREIGN KEY (customer_id) REFERENCES customers(id),
    FOREIGN KEY (created_by) REFERENCES users(id),
    FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- Measurement Templates Table (Standard measurements for different order types)
CREATE TABLE IF NOT EXISTS measurement_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    order_type VARCHAR(50) NOT NULL,
    description TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Measurement Fields Table (Fields for each template)
CREATE TABLE IF NOT EXISTS measurement_fields (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    unit VARCHAR(20), -- CM, INCH, etc.
    field_order INT,
    is_required BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (template_id) REFERENCES measurement_templates(id) ON DELETE CASCADE
);

-- Measurements Table (Actual measurements for each order)
CREATE TABLE IF NOT EXISTS measurements (
    id BIGSERIAL PRIMARY KEY,
    tailoring_order_id BIGINT NOT NULL,
    measurement_template_id BIGINT,
    field_id BIGINT,
    field_name VARCHAR(100),
    value DECIMAL(10, 2),
    unit VARCHAR(20),
    notes TEXT,
    measured_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    FOREIGN KEY (tailoring_order_id) REFERENCES tailoring_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (measurement_template_id) REFERENCES measurement_templates(id),
    FOREIGN KEY (field_id) REFERENCES measurement_fields(id),
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- =====================================================
-- 4. PAYMENT AND TRANSACTION TABLES
-- =====================================================

-- Payments Table
CREATE TABLE IF NOT EXISTS payments (
    id BIGSERIAL PRIMARY KEY,
    tailoring_order_id BIGINT NOT NULL,
    amount DECIMAL(15, 2) NOT NULL,
    payment_method VARCHAR(50), -- CASH, CARD, BANK_TRANSFER, etc.
    transaction_reference VARCHAR(255),
    payment_date TIMESTAMP NOT NULL,
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    FOREIGN KEY (tailoring_order_id) REFERENCES tailoring_orders(id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(id)
);

-- =====================================================
-- 5. AUDIT AND HISTORY TABLES
-- =====================================================

-- Audit Log Table
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(100) NOT NULL, -- CUSTOMER, TAILORING_ORDER, MEASUREMENT
    entity_id BIGINT NOT NULL,
    action VARCHAR(50), -- CREATE, UPDATE, DELETE
    old_values JSONB,
    new_values JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

-- API Access Log Table (For monitoring API usage)
CREATE TABLE IF NOT EXISTS api_access_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    endpoint VARCHAR(255),
    method VARCHAR(10), -- GET, POST, PUT, DELETE
    status_code INT,
    request_duration_ms INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- =====================================================
-- 6. INDEXES FOR PERFORMANCE
-- =====================================================

-- User Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_oauth_provider_id ON users(oauth_provider, oauth_id);
CREATE INDEX idx_users_is_active ON users(is_active);

-- Customer Indexes
CREATE INDEX idx_customers_customer_code ON customers(customer_code);
CREATE INDEX idx_customers_email ON customers(email);
CREATE INDEX idx_customers_phone ON customers(phone);
CREATE INDEX idx_customers_is_active ON customers(is_active);

-- Order Indexes
CREATE INDEX idx_orders_order_code ON tailoring_orders(order_code);
CREATE INDEX idx_orders_customer_id ON tailoring_orders(customer_id);
CREATE INDEX idx_orders_status ON tailoring_orders(status);
CREATE INDEX idx_orders_order_date ON tailoring_orders(order_date);
CREATE INDEX idx_orders_promised_date ON tailoring_orders(promised_date);

-- Measurement Indexes
CREATE INDEX idx_measurements_order_id ON measurements(tailoring_order_id);
CREATE INDEX idx_measurements_template_id ON measurements(measurement_template_id);

-- Audit Indexes
CREATE INDEX idx_audit_entity_type_id ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at);

-- API Log Indexes
CREATE INDEX idx_api_logs_user_id ON api_access_logs(user_id);
CREATE INDEX idx_api_logs_created_at ON api_access_logs(created_at);

-- =====================================================
-- 7. INITIAL DATA - ROLES
-- =====================================================

INSERT INTO user_roles (name, description) VALUES
    ('ADMIN', 'Administrator - Full system access'),
    ('USER', 'Regular user - Basic operations'),
    ('MANAGER', 'Manager - Elevated permissions')
ON CONFLICT DO NOTHING;

-- =====================================================
-- 8. INITIAL DATA - MEASUREMENT TEMPLATES
-- =====================================================

INSERT INTO measurement_templates (name, order_type, description, is_active) VALUES
    ('Standard Suit Measurements', 'SUIT', 'Standard measurements for tailoring a suit', TRUE),
    ('Standard Shirt Measurements', 'SHIRT', 'Standard measurements for tailoring a shirt', TRUE),
    ('Standard Pants Measurements', 'PANTS', 'Standard measurements for tailoring pants', TRUE),
    ('Standard Dress Measurements', 'DRESS', 'Standard measurements for tailoring a dress', TRUE)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 9. INITIAL DATA - MEASUREMENT FIELDS (for Suit)
-- =====================================================

-- Get template ID for SUIT (typically 1, but using subquery for safety)
INSERT INTO measurement_fields (template_id, field_name, display_name, unit, field_order, is_required) VALUES
    (1, 'JACKET_LENGTH', 'Jacket Length', 'CM', 1, TRUE),
    (1, 'JACKET_CHEST', 'Jacket Chest', 'CM', 2, TRUE),
    (1, 'JACKET_WAIST', 'Jacket Waist', 'CM', 3, TRUE),
    (1, 'JACKET_SLEEVE', 'Jacket Sleeve Length', 'CM', 4, TRUE),
    (1, 'JACKET_SHOULDER', 'Jacket Shoulder Width', 'CM', 5, TRUE),
    (1, 'PANT_LENGTH', 'Pants Length', 'CM', 6, TRUE),
    (1, 'PANT_WAIST', 'Pants Waist', 'CM', 7, TRUE),
    (1, 'PANT_INSEAM', 'Pants Inseam', 'CM', 8, TRUE),
    (1, 'PANT_RISE', 'Pants Rise', 'CM', 9, FALSE)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 10. MEASUREMENT FIELDS (for SHIRT)
-- =====================================================

INSERT INTO measurement_fields (template_id, field_name, display_name, unit, field_order, is_required) VALUES
    (2, 'SHIRT_LENGTH', 'Shirt Length', 'CM', 1, TRUE),
    (2, 'SHIRT_CHEST', 'Shirt Chest', 'CM', 2, TRUE),
    (2, 'SHIRT_WAIST', 'Shirt Waist', 'CM', 3, TRUE),
    (2, 'SHIRT_SLEEVE', 'Shirt Sleeve Length', 'CM', 4, TRUE),
    (2, 'SHIRT_SHOULDER', 'Shirt Shoulder Width', 'CM', 5, TRUE),
    (2, 'SHIRT_NECK', 'Neck Size', 'CM', 6, TRUE)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 11. MEASUREMENT FIELDS (for PANTS)
-- =====================================================

INSERT INTO measurement_fields (template_id, field_name, display_name, unit, field_order, is_required) VALUES
    (3, 'PANTS_LENGTH', 'Pants Length', 'CM', 1, TRUE),
    (3, 'PANTS_WAIST', 'Pants Waist', 'CM', 2, TRUE),
    (3, 'PANTS_INSEAM', 'Pants Inseam', 'CM', 3, TRUE),
    (3, 'PANTS_RISE', 'Pants Rise', 'CM', 4, FALSE),
    (3, 'PANTS_THIGH', 'Thigh Width', 'CM', 5, FALSE),
    (3, 'PANTS_CALF', 'Calf Width', 'CM', 6, FALSE)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 12. MEASUREMENT FIELDS (for DRESS)
-- =====================================================

INSERT INTO measurement_fields (template_id, field_name, display_name, unit, field_order, is_required) VALUES
    (4, 'DRESS_LENGTH', 'Dress Length', 'CM', 1, TRUE),
    (4, 'DRESS_BUST', 'Bust', 'CM', 2, TRUE),
    (4, 'DRESS_WAIST', 'Waist', 'CM', 3, TRUE),
    (4, 'DRESS_HIP', 'Hip', 'CM', 4, TRUE),
    (4, 'DRESS_SLEEVE', 'Sleeve Length', 'CM', 5, FALSE),
    (4, 'DRESS_SHOULDER', 'Shoulder Width', 'CM', 6, FALSE)
ON CONFLICT DO NOTHING;

-- =====================================================
-- 13. SEQUENCES
-- =====================================================

-- Customer Code sequence (format: CUST-001, CUST-002, etc.)
CREATE SEQUENCE IF NOT EXISTS customer_code_seq START WITH 1001;

-- Order Code sequence (format: ORD-001, ORD-002, etc.)
CREATE SEQUENCE IF NOT EXISTS order_code_seq START WITH 1001;
