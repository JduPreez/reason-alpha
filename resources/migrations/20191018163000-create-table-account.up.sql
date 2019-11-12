CREATE TABLE IF NOT EXISTS "reason-alpha".account (
  id            uuid PRIMARY KEY,
  name          varchar,
  number        varchar,
  currency_id   int,
  owner_user_id uuid
);
--;;
CREATE INDEX IF NOT EXISTS idx_account_name ON "REASON-ALPHA".account (name);