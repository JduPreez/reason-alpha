CREATE TABLE IF NOT EXISTS "reason-alpha".trade_pattern (
  id          uuid PRIMARY KEY,
  creation_id uuid,
  name        varchar,
  description varchar,
  parent_id   uuid,
  user_id     uuid);
--;;
CREATE INDEX IF NOT EXISTS idx_trade_pattern_name ON "REASON-ALPHA".trade_pattern (name);
--;;
CREATE INDEX IF NOT EXISTS idx_trade_pattern_parent_id ON "REASON-ALPHA".trade_pattern (parent_id);
