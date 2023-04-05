CREATE TABLE IF NOT EXISTS subscription (
 id uuid PRIMARY KEY,
 version INTEGER,
 endpoint VARCHAR(255),
 p256dh VARCHAR(255),
 auth VARCHAR(50)
);
CREATE TABLE IF NOT EXISTS notification (
 id uuid PRIMARY KEY,
 version INTEGER,
 subscription_id uuid REFERENCES subscription(id),
 enabled BOOLEAN,
 price NUMERIC(10,2),
 up BOOLEAN,
 extra_msg VARCHAR(255)
);
