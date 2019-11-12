CREATE TABLE IF NOT EXISTS "reason-alpha".security (
  id            uuid PRIMARY KEY,
  name          varchar,
  owner_user_id uuid);
--;;
CREATE INDEX IF NOT EXISTS idx_security_name ON "REASON-ALPHA".security (name);