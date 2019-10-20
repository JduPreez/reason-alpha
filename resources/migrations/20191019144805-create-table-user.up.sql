CREATE TABLE IF NOT EXISTS "REASON-ALPHA".user (
  id          bigint PRIMARY KEY,
  user_name   varchar,
  email       varchar);
--;;
CREATE INDEX IF NOT EXISTS idx_user_user_name ON "REASON-ALPHA".user (user_name);