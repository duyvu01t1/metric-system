-- Fix schema mismatch for AffiliateCommission entity
-- JPA expects updated_at to exist on affiliate_commissions

ALTER TABLE affiliate_commissions
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;
