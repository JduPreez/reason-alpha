CREATE TABLE IF NOT EXISTS "reason-alpha".time_frame (
  id          int PRIMARY KEY,
  name        varchar
);
--;;
CREATE INDEX IF NOT EXISTS idx_time_frame_name ON "REASON-ALPHA".time_frame (name);