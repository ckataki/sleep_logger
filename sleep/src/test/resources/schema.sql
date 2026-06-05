CREATE TABLE IF NOT EXISTS users
(
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    username VARCHAR NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS sleep_log
(
    id UUID DEFAULT RANDOM_UUID() PRIMARY KEY,
    user_id UUID REFERENCES users (id) ON DELETE CASCADE,
    sleep_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    duration INT NOT NULL,
    quality VARCHAR NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS user_id_sleep_date_unique_idx ON sleep_log (user_id, sleep_date);
