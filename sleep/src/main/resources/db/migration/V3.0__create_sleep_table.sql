
CREATE TABLE IF NOT EXISTS sleep_log
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    sleep_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration INTERVAL HOUR TO MINUTE NOT NULL,
    quality VARCHAR NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS user_id_sleep_date_unique_idx ON sleep_log (user_id, sleep_date);
