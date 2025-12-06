-- liquibase formatted sql

-- changeset ilyatr:1-create-users-table
CREATE TABLE users(
    id VARCHAR(36) NOT NULL unique ,
    name varchar(20) not null ,
    email varchar(120) unique not null unique ,
    password varchar(255) not null ,
    created_at timestamp NOT NULL default CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY(id)
);
-- changeset ilyatr:2-create-events-table
CREATE TABLE events(
    id VARCHAR(36) NOT NULL unique ,
    title varchar(120) NOT NULL,
    date timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
    category varchar(20) NOT NULL,
    price decimal(10) NOT NULL,
    CONSTRAINT pk_events PRIMARY KEY(id)
);
CREATE INDEX idx_events_date ON events(date);
CREATE INDEX idx_events_category ON events(category);
CREATE INDEX idx_events_category_date ON events(category, date);

-- changeset ilyatr:3-create-subscriptions-table
CREATE TABLE subscriptions(
    user_id  VARCHAR(36) NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    created_at timestamp NOT NULL default CURRENT_TIMESTAMP,
    CONSTRAINT fk_sub_users FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_sub_events FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE,
    CONSTRAINT pk_events PRIMARY KEY(user_id, event_id)
);


-- changeset ilyatr:4-create-coments-table
CREATE TABLE comments(
    id VARCHAR(36) NOT NULL unique ,
    user_id VARCHAR(36) NOT NULL,
    event_id VARCHAR(36) NOT NULL,
    text text NOT NULL,
    created_at timestamp NOT NULL default CURRENT_TIMESTAMP,
    updated_at timestamp NOT NULL default CURRENT_TIMESTAMP,
    constraint pk_comments primary key(id),
    constraint fk_com_users FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    constraint fk_com_events FOREIGN KEY(event_id) REFERENCES events(id) ON DELETE CASCADE
);

CREATE INDEX idx_comments_event_id ON comments(event_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_event_id_created_at ON comments(event_id, created_at DESC);

