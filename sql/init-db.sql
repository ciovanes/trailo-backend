-- schemas
CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS social;
CREATE SCHEMA IF NOT EXISTS geo;

-- enums
CREATE TYPE social.friendship_status AS ENUM('PENDING', 'ACCEPTED', 'REJECTED');
CREATE TYPE social.membership_status AS ENUM('PENDING', 'ACCEPTED', 'REJECTED');
CREATE TYPE social.group_role AS ENUM('LEADER', 'CO_LEADER', 'ELDER', 'MEMBER');
CREATE TYPE geo.difficulty AS ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED');
CREATE TYPE geo.terrain_type AS ENUM('UNSPECIFIED', 'ROCKY', 'MUDDY', 'SANDY', 'FOREST', 'MOUNTAIN', 'DESERT',
    'RIVER', 'SNOW', 'GRAVEL', 'CLAY', 'VOLCANIC');
CREATE TYPE geo.meetup_status AS ENUM('WAITING', 'CURRENT', 'CANCELLED', 'FINISHED');

-- core
CREATE TABLE IF NOT EXISTS core."user"(
    uuid UUID NOT NULL,
    email TEXT NOT NULL,
    username TEXT NOT NULL,
    cognito_id TEXT NOT NULL,
    name TEXT,
    surname TEXT,
    profile_picture TEXT,
    country TEXT,

    last_login_at TIMESTAMPTZ,
    last_modified_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT user_pk PRIMARY KEY (uuid),
    CONSTRAINT uk_email UNIQUE (email),
    CONSTRAINT uk_username UNIQUE (username),
    CONSTRAINT uk_cognito_id UNIQUE (cognito_id)
    );

CREATE TABLE IF NOT EXISTS core.group(
    uuid UUID NOT NULL,
    name TEXT NOT NULL,
    description TEXT,
    private BOOLEAN NOT NULL DEFAULT false,
    image_url TEXT,

    last_modified_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT group_pk PRIMARY KEY (uuid),
    CONSTRAINT uk_name UNIQUE (name)
    );

CREATE INDEX idx_group_name ON core.group(name);

-- geo
CREATE TABLE IF NOT EXISTS geo.meetup(
    uuid UUID NOT NULL,
    host_id UUID NULL REFERENCES core."user"(uuid),
    group_id UUID NOT NULL REFERENCES core.group(uuid) ON DELETE CASCADE,
    title TEXT NOT NULL,
    description TEXT,
    meetup_picture TEXT,
    max_participants SMALLINT CHECK (max_participants > 0),
    difficulty geo.difficulty NOT NULL,
    distance_km NUMERIC(8, 2) NOT NULL CHECK (distance_km >= 0),
    estimated_duration_in_min BIGINT NOT NULL,
    meeting_time TIMESTAMPTZ NOT NULL,
    meeting_point geometry(POINT, 4326) NOT NULL,
    status geo.meetup_status NOT NULL DEFAULT 'WAITING',
    creation_date TIMESTAMPTZ NOT NULL,

    CONSTRAINT meetup_pk PRIMARY KEY (uuid)
    );

CREATE TABLE IF NOT EXISTS geo.meetup_terrain_type(
    uuid UUID NOT NULL,
    meetup_id UUID NOT NULL REFERENCES geo.meetup(uuid) ON DELETE CASCADE,
    terrain_type geo.terrain_type NOT NULL DEFAULT 'UNSPECIFIED',

    UNIQUE(meetup_id, terrain_type)
    );

-- social
CREATE TABLE IF NOT EXISTS social.friendship(
    uuid UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES core."user"(uuid) ON DELETE CASCADE,
    friend_id UUID NOT NULL REFERENCES core."user"(uuid) ON DELETE CASCADE,
    status social.friendship_status NOT NULL,

    last_modified_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT uk_friendship_user_friend UNIQUE(user_id, friend_id)
    );

CREATE INDEX idx_friendship_user_id ON social.friendship(user_id);
CREATE INDEX idx_friendship_friend_id ON social.friendship(friend_id);
CREATE INDEX idx_friendship_status ON social.friendship(status);

CREATE TABLE IF NOT EXISTS social.user_group(
    uuid UUID NOT NULL,
    group_id UUID NOT NULL REFERENCES core.group(uuid) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES core."user"(uuid) ON DELETE CASCADE,
    status social.membership_status NOT NULL,
    role social.group_role NOT NULL DEFAULT 'MEMBER',
    invited_by UUID NOT NULL,
    is_favorite BOOLEAN DEFAULT false,

    last_modified_at TIMESTAMPTZ NOT NULL,
    join_date TIMESTAMPTZ NOT NULL,

    CONSTRAINT user_group_pk PRIMARY KEY (uuid),
    CONSTRAINT uk_group_user UNIQUE (group_id, user_id)
    );

CREATE INDEX idx_user_group_group_id ON social.user_group(group_id);
CREATE INDEX idx_user_group_user_id ON social.user_group(user_id);
CREATE INDEX idx_user_group_status ON social.user_group(status);

CREATE TABLE IF NOT EXISTS social.user_meetup(
    uuid UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES core."user"(uuid) ON DELETE CASCADE,
    meetup_id UUID NOT NULL REFERENCES geo.meetup(uuid) ON DELETE CASCADE,

    join_date TIMESTAMPTZ NOT NULL,

    CONSTRAINT user_meetup_pk PRIMARY KEY (uuid),
    CONSTRAINT uk_user_meetup UNIQUE (user_id, meetup_id)
    );