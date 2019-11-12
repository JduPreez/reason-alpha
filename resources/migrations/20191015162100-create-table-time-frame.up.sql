CREATE TABLE IF NOT EXISTS "REASON-ALPHA".time_frame (
  id          uuid PRIMARY KEY,
  name        varchar
);
--;;
CREATE INDEX IF NOT EXISTS idx_time_frame_name ON "REASON-ALPHA".time_frame (name);