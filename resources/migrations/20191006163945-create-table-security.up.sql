CREATE TABLE IF NOT EXISTS "reason-alpha".security (
  id            bigint PRIMARY KEY,
  name          varchar,
  owner_user_id bigint);
--;;
CREATE INDEX IF NOT EXISTS idx_security_name ON "REASON-ALPHA".security (name);