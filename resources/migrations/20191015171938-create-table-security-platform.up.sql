CREATE TABLE IF NOT EXISTS "reason-alpha".security_platform (
  security_id bigint,
  platform_id int,
  symbol      char,
  PRIMARY KEY (security_id, platform_id));
--;;
CREATE INDEX IF NOT EXISTS idx_security_platform_symbol ON "REASON-ALPHA".security_platform (symbol);