
CREATE TABLE IF NOT EXISTS users
(
    id UUID PRIMARY KEY DEFAULT gen_random_uuid (),
    username VARCHAR NOT NULL UNIQUE  -- Index is auto-generated
);
