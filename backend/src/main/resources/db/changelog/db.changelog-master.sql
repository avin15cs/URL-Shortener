--liquibase formatted sql

--changeset url-shortener:001-create-urls-baseline dbms:postgresql
CREATE TABLE IF NOT EXISTS urls (
    id BIGSERIAL PRIMARY KEY,
    original_url VARCHAR(2048) NOT NULL,
    short_code VARCHAR(50) NOT NULL UNIQUE,
    custom_alias VARCHAR(50),
    title VARCHAR(255),
    click_count BIGINT DEFAULT 0 NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITHOUT TIME ZONE,
    is_active BOOLEAN DEFAULT TRUE NOT NULL,
    owner_uid UUID,
    owner_client_id VARCHAR(100)
);

CREATE INDEX IF NOT EXISTS idx_urls_short_code ON urls(short_code);
CREATE INDEX IF NOT EXISTS idx_urls_custom_alias ON urls(custom_alias);
CREATE INDEX IF NOT EXISTS idx_urls_owner_uid ON urls(owner_uid);
CREATE INDEX IF NOT EXISTS idx_urls_owner_client_id ON urls(owner_client_id);

--changeset url-shortener:002-add-url-ownership dbms:postgresql
ALTER TABLE urls ADD COLUMN IF NOT EXISTS owner_uid UUID;
ALTER TABLE urls ADD COLUMN IF NOT EXISTS owner_client_id VARCHAR(100);
CREATE INDEX IF NOT EXISTS idx_urls_owner_uid ON urls(owner_uid);
CREATE INDEX IF NOT EXISTS idx_urls_owner_client_id ON urls(owner_client_id);
