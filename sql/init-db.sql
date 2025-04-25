CREATE SCHEMA IF NOT EXISTS core;

CREATE TABLE IF NOT EXISTS core."user"(
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    email text NOT NULL,
    username text NOT NULL,
    cognito_id text NOT NULL,
    name text,
    surname text,
    profile_picture text,
    country text,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_pk PRIMARY KEY (uuid),
    CONSTRAINT uk_email UNIQUE (email),
    CONSTRAINT uk_username UNIQUE (username),
    CONSTRAINT uk_cognito_id UNIQUE (cognito_id)
);