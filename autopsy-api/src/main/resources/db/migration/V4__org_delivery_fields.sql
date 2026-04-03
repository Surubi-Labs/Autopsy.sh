-- Delivery configuration per organization
ALTER TABLE organizations ADD COLUMN email_address TEXT;
ALTER TABLE organizations ADD COLUMN slack_webhook_url TEXT;
