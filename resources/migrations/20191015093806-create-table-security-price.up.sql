CREATE TABLE IF NOT EXISTS "REASON-ALPHA".security_price (
  id          uuid PRIMARY KEY,
  security_id uuid,
  date        timestamp,
  openp       double,
  high        double,
  low         double,
  close       double,
  adj_close   double,
  volume      bigint);
--;;
CREATE INDEX IF NOT EXISTS idx_security_price_security_id ON "REASON-ALPHA".security_price (security_id);