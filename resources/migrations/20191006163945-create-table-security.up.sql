CREATE TABLE IF NOT EXISTS "reason-alpha".Security (
  id bigint PRIMARY KEY,
  name varchar);
--;;
CREATE INDEX IF NOT EXISTS idx_security_name ON "REASON-ALPHA".Security (name);