CREATE DATABASE IF NOT EXISTS trailo;

CREATE SCHEMA IF NOT EXISTS core;

CREATE TABLE IF NOT EXISTS core."user"(
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    email text NOT NULL,
    username text NOT NULL,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_pk PRIMARY KEY (uuid),
    CONSTRAINT uk_email UNIQUE (email),
    CONSTRAINT uk_username UNIQUE (username)
);