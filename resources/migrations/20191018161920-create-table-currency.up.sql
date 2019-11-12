CREATE TABLE IF NOT EXISTS "reason-alpha".currency (
  id          uuid PRIMARY KEY,
  name        varchar,
  code        varchar
);
--;;
CREATE INDEX IF NOT EXISTS idx_currency_name ON "REASON-ALPHA".currency (name);
--;;
CREATE INDEX IF NOT EXISTS idx_currency_code ON "REASON-ALPHA".currency (code);