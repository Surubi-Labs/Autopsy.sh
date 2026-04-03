-- Stripe subscription tracking
ALTER TABLE organizations ADD COLUMN stripe_subscription_id TEXT;
ALTER TABLE organizations ADD COLUMN stripe_price_id TEXT;
ALTER TABLE organizations ADD COLUMN plan_updated_at TIMESTAMPTZ;
