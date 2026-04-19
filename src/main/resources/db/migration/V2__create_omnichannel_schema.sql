-- =====================================================
-- PHÂN HỆ 1: QUẢN LÝ KÊNH TIẾP NHẬN (OMNICHANNEL)
-- V2__create_omnichannel_schema.sql
-- =====================================================

-- =====================================================
-- 1. CHANNELS TABLE (Kênh tiếp nhận)
-- =====================================================
CREATE TABLE IF NOT EXISTS channels (
    id           BIGSERIAL PRIMARY KEY,
    channel_code VARCHAR(50)  NOT NULL UNIQUE,  -- MESSENGER, ZALO, WHATSAPP, EMAIL, PHONE, WALK_IN
    display_name VARCHAR(100) NOT NULL,
    icon_class   VARCHAR(100),                   -- Font Awesome class, e.g. fab fa-facebook-messenger
    webhook_url  VARCHAR(500),                   -- Webhook endpoint nếu tích hợp
    description  VARCHAR(500),
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order   INT          NOT NULL DEFAULT 0,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_channels_code     ON channels (channel_code);
CREATE INDEX IF NOT EXISTS idx_channels_active   ON channels (is_active);

-- Seed default channels
INSERT INTO channels (channel_code, display_name, icon_class, description, sort_order)
VALUES
    ('MESSENGER',  'Facebook Messenger', 'fab fa-facebook-messenger', 'Tin nhắn qua Facebook Messenger', 1),
    ('ZALO',       'Zalo',               'fas fa-comment-dots',       'Tin nhắn qua Zalo',               2),
    ('WHATSAPP',   'WhatsApp',           'fab fa-whatsapp',           'Tin nhắn qua WhatsApp',           3),
    ('EMAIL',      'Email',              'fas fa-envelope',           'Liên hệ qua Email',               4),
    ('PHONE',      'Điện thoại',         'fas fa-phone',              'Gọi điện trực tiếp',              5),
    ('WALK_IN',    'Khách đến trực tiếp','fas fa-store',              'Khách hàng đến tại cửa hàng',     6),
    ('WEBSITE',    'Website',            'fas fa-globe',              'Form liên hệ trên Website',       7),
    ('REFERRAL',   'Giới thiệu',         'fas fa-user-friends',       'Được giới thiệu từ khách cũ',    8)
ON CONFLICT (channel_code) DO NOTHING;

-- =====================================================
-- 2. LEADS TABLE (Bản ghi tiếp nhận khách hàng tiềm năng)
-- =====================================================
CREATE TABLE IF NOT EXISTS leads (
    id                   BIGSERIAL    PRIMARY KEY,
    lead_code            VARCHAR(50)  NOT NULL UNIQUE,   -- L-YYYYMMDD-XXXX
    channel_id           BIGINT       NOT NULL,          -- FK -> channels
    full_name            VARCHAR(200) NOT NULL,
    phone                VARCHAR(20),
    email                VARCHAR(255),
    source_message       TEXT,                           -- Nội dung tin nhắn/hội thoại gốc
    -- Nhu cầu / phân loại
    need_type            VARCHAR(50),                    -- SUIT, SHIRT, PANTS, DRESS, WEDDING, OTHER
    need_description     TEXT,                           -- Mô tả chi tiết nhu cầu
    estimated_budget     DECIMAL(15, 2),                 -- Ngân sách ước tính
    -- Trạng thái
    status               VARCHAR(30)  NOT NULL DEFAULT 'NEW',
        -- NEW | CONTACTED | QUALIFIED | NEGOTIATING | CONVERTED | LOST
    lost_reason          TEXT,                           -- Lý do bỏ lỡ (nếu status = LOST)
    -- Phân công nhân viên
    assigned_staff_id    BIGINT,                         -- FK -> users
    -- Chuyển đổi thành khách hàng
    converted_customer_id BIGINT,                        -- FK -> customers (sau khi chốt)
    converted_order_id   BIGINT,                         -- FK -> tailoring_orders
    converted_at         TIMESTAMP,
    -- Follow-up
    followup_at          TIMESTAMP,                      -- Thời gian hẹn liên lạc lại
    last_contacted_at    TIMESTAMP,
    contact_count        INT          NOT NULL DEFAULT 0,
    -- Metadata
    notes                TEXT,
    tags                 VARCHAR(500),                   -- comma-separated tags
    is_returning_customer BOOLEAN     NOT NULL DEFAULT FALSE, -- TRUE = khách cũ
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by           BIGINT,

    CONSTRAINT fk_leads_channel         FOREIGN KEY (channel_id)            REFERENCES channels(id),
    CONSTRAINT fk_leads_assigned_staff  FOREIGN KEY (assigned_staff_id)     REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT fk_leads_customer        FOREIGN KEY (converted_customer_id) REFERENCES customers(id) ON DELETE SET NULL,
    CONSTRAINT fk_leads_order           FOREIGN KEY (converted_order_id)    REFERENCES tailoring_orders(id) ON DELETE SET NULL,
    CONSTRAINT fk_leads_created_by      FOREIGN KEY (created_by)            REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_leads_code           ON leads (lead_code);
CREATE INDEX IF NOT EXISTS idx_leads_channel        ON leads (channel_id);
CREATE INDEX IF NOT EXISTS idx_leads_status         ON leads (status);
CREATE INDEX IF NOT EXISTS idx_leads_assigned_staff ON leads (assigned_staff_id);
CREATE INDEX IF NOT EXISTS idx_leads_phone          ON leads (phone);
CREATE INDEX IF NOT EXISTS idx_leads_email          ON leads (email);
CREATE INDEX IF NOT EXISTS idx_leads_created_at     ON leads (created_at);
CREATE INDEX IF NOT EXISTS idx_leads_followup_at    ON leads (followup_at);

-- =====================================================
-- 3. LEAD INTERACTION LOG (Lịch sử tương tác với lead)
-- =====================================================
CREATE TABLE IF NOT EXISTS lead_interactions (
    id              BIGSERIAL    PRIMARY KEY,
    lead_id         BIGINT       NOT NULL,
    interaction_type VARCHAR(30) NOT NULL,  -- NOTE | CALL | MESSAGE | MEETING | EMAIL
    content         TEXT         NOT NULL,
    outcome         VARCHAR(100),           -- Kết quả: INTERESTED, NOT_INTERESTED, FOLLOW_UP, etc.
    interacted_by   BIGINT,                 -- FK -> users (nhân viên thực hiện)
    interacted_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_lead_interactions_lead        FOREIGN KEY (lead_id)       REFERENCES leads(id) ON DELETE CASCADE,
    CONSTRAINT fk_lead_interactions_staff       FOREIGN KEY (interacted_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_lead_interactions_lead ON lead_interactions (lead_id);
CREATE INDEX IF NOT EXISTS idx_lead_interactions_at   ON lead_interactions (interacted_at);

-- =====================================================
-- 4. CHATBOT FAQ (Câu hỏi thường gặp)
-- =====================================================
CREATE TABLE IF NOT EXISTS chatbot_faqs (
    id              BIGSERIAL    PRIMARY KEY,
    question        TEXT         NOT NULL,
    answer          TEXT         NOT NULL,
    category        VARCHAR(100),           -- GIA_CA, QUY_TRINH, SAN_PHAM, GIAO_HANG, KHAC
    keywords        VARCHAR(500),           -- comma-separated keywords để matching
    hit_count       INT          NOT NULL DEFAULT 0,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    sort_order      INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by      BIGINT,

    CONSTRAINT fk_faqs_created_by FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_chatbot_faqs_active   ON chatbot_faqs (is_active);
CREATE INDEX IF NOT EXISTS idx_chatbot_faqs_category ON chatbot_faqs (category);

-- Seed basic FAQs
INSERT INTO chatbot_faqs (question, answer, category, keywords, sort_order)
VALUES
    ('Giá may một bộ vest bao nhiêu?',
     'Giá may vest dao động từ 2.000.000đ - 10.000.000đ tùy chất liệu vải và độ phức tạp. Vui lòng đến cửa hàng để được tư vấn cụ thể.',
     'GIA_CA', 'giá,vest,bộ vest,chi phí', 1),
    ('Thời gian may mất bao lâu?',
     'Thông thường từ 7-14 ngày làm việc. Đơn hàng cần gấp có thể được ưu tiên, vui lòng liên hệ trực tiếp.',
     'QUY_TRINH', 'thời gian,bao lâu,nhanh,gấp', 2),
    ('Có cần đặt cọc không?',
     'Có, chúng tôi yêu cầu đặt cọc 30-50% giá trị đơn hàng trước khi tiến hành sản xuất.',
     'QUY_TRINH', 'đặt cọc,deposit,trả trước', 3),
    ('Shop có may áo cưới không?',
     'Có, chúng tôi nhận may vest cưới, áo dài cưới và các trang phục dự tiệc theo yêu cầu.',
     'SAN_PHAM', 'cưới,áo cưới,đám cưới,wedding', 4),
    ('Làm thế nào để đặt hàng?',
     'Bạn có thể đến trực tiếp cửa hàng để được đo và tư vấn, hoặc nhắn tin cho chúng tôi để đặt lịch hẹn.',
     'QUY_TRINH', 'đặt hàng,đặt lịch,hẹn,order', 5)
ON CONFLICT DO NOTHING;
