--liquibase formatted sql
--changeset url-shortener:002-url-ownership

ALTER TABLE urls ADD COLUMN IF NOT EXISTS owner_uid uuid;
ALTER TABLE urls ADD COLUMN IF NOT EXISTS owner_client_id varchar(100);

CREATE INDEX IF NOT EXISTS idx_urls_owner_uid ON urls (owner_uid);
CREATE INDEX IF NOT EXISTS idx_urls_owner_client_id ON urls (owner_client_id);
