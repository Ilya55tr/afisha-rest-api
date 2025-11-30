-- liquibase formatted sql

-- changeset ilyatr:1-create-users-table
CREATE TABLE users(
    id SERIAL,
    name varchar(20) not null ,
    email varchar(120) unique not null unique ,
    password varchar(255) not null ,
    created_at timestamp default CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY(id)
);
-- changeset ilyatr:2-create-events-table
CREATE TABLE events(
                       id SERIAL,
                       title varchar(120) NOT NULL,
                       date timestamp NOT NULL,
                       category varchar(20) NOT NULL,
                       price decimal(10) NOT NULL
);
CREATE INDEX idx_events_date ON events(date);
CREATE INDEX idx_events_category ON events(category);
CREATE INDEX idx_events_category_date ON events(category, date);

-- changeset ilyatr:3-create-subscriptions-table
CREATE TABLE subscriptions(
    user_id BIGINT UNSIGNED NOT NULL ,
    event_id BIGINT UNSIGNED NOT NULL,
    created_at timestamp default CURRENT_TIMESTAMP,
    CONSTRAINT fk_users FOREIGN KEY(user_id) REFERENCES users(id),
    CONSTRAINT fk_events FOREIGN KEY(event_id) REFERENCES events(id),
    CONSTRAINT pk_events PRIMARY KEY(user_id, event_id)
);


-- changeset ilyatr:4-create-coments-table
CREATE TABLE comments(
    id SERIAL,
    user_id BIGINT UNSIGNED NOT NULL,
    event_id BIGINT UNSIGNED NOT NULL,
    text text NOT NULL,
    created_at timestamp default CURRENT_TIMESTAMP,
    updated_at timestamp default CURRENT_TIMESTAMP
);

CREATE INDEX idx_comments_event_id ON comments(event_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_event_id_created_at ON comments(event_id, created_at DESC);

