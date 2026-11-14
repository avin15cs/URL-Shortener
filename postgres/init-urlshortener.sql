SELECT 'CREATE ROLE urlshortener LOGIN PASSWORD ''local-urlshortener-password'''
WHERE NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'urlshortener')\gexec

ALTER ROLE urlshortener WITH LOGIN PASSWORD 'local-urlshortener-password';

SELECT 'CREATE DATABASE urlshortener OWNER urlshortener'
WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'urlshortener')\gexec

ALTER DATABASE urlshortener OWNER TO urlshortener;

\connect urlshortener

GRANT USAGE, CREATE ON SCHEMA public TO urlshortener;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO urlshortener;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO urlshortener;

SELECT format('ALTER TABLE %I.%I OWNER TO urlshortener', schemaname, tablename)
FROM pg_catalog.pg_tables
WHERE schemaname = 'public'\gexec

SELECT format('ALTER SEQUENCE %I.%I OWNER TO urlshortener', sequence_schema, sequence_name)
FROM information_schema.sequences
WHERE sequence_schema = 'public'\gexec
