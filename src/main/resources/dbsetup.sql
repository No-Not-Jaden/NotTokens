CREATE TABLE IF NOT EXISTS tokenplayer
(
    uuid CHAR(36) NOT NULL,
    tokens BIGINT DEFAULT 0 NOT NULL,
    PRIMARY KEY (uuid)
);

