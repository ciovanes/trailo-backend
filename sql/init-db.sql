CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS social;

CREATE TABLE IF NOT EXISTS core."user"(
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    email text NOT NULL,
    username text NOT NULL,
    cognito_id text NOT NULL,
    name text,
    surname text,
    profile_picture text,
    country text,
    last_login_at timestamptz,
    last_modified_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT user_pk PRIMARY KEY (uuid),
    CONSTRAINT uk_email UNIQUE (email),
    CONSTRAINT uk_username UNIQUE (username),
    CONSTRAINT uk_cognito_id UNIQUE (cognito_id)
);

CREATE TYPE social.friendship_status AS ENUM(
    'PENDING',
    'ACCEPTED',
    'REJECTED'
);

CREATE TABLE IF NOT EXISTS social.friendship(
    uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES core."user"(uuid) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES core."user"(uuid) ON DELETE CASCADE,
    status social.friendship_status NOT NULL,
    last_modified_at timestamptz,
    created_at timestamptz DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_friendship_user_friend UNIQUE(user_id, friend_id)
);

CREATE INDEX idx_friendship_user_id ON social.friendship(user_id);
CREATE INDEX idx_friendship_friend_id ON social.friendship(friend_id);
CREATE INDEX idx_friendship_status ON social.friendship(status);