CREATE TABLE IF NOT EXISTS "reason-alpha".account (
  id            int PRIMARY KEY,
  name          varchar,
  number        varchar,
  currency_id   int,
  owner_user_id bigint
);
--;;
CREATE INDEX IF NOT EXISTS idx_account_name ON "REASON-ALPHA".account (name);