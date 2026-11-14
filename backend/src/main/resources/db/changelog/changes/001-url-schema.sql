--liquibase formatted sql
--changeset url-shortener:001-url-schema
--preconditions onFail:MARK_RAN
--precondition-sql-check expectedResult:0 SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'urls'

CREATE TABLE urls (
    id bigserial PRIMARY KEY,
    original_url varchar(2048) NOT NULL,
    short_code varchar(50) NOT NULL,
    custom_alias varchar(50),
    title varchar(255),
    click_count bigint DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamp without time zone,
    is_active boolean DEFAULT true NOT NULL,
    CONSTRAINT uk_urls_short_code UNIQUE (short_code)
);

CREATE INDEX idx_urls_custom_alias ON urls (custom_alias);
