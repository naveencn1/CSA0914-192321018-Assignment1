CREATE DATABASE IF NOT EXISTS csa09_assignment5;
USE csa09_assignment5;
DROP TABLE IF EXISTS bus_pass;
CREATE TABLE bus_pass (
  pass_id       VARCHAR(12) NOT NULL,
  register_no   VARCHAR(12) NOT NULL,
  name          VARCHAR(60) NOT NULL,
  route_no      VARCHAR(4)  NOT NULL,
  boarding_stop VARCHAR(40) NOT NULL,
  valid_until   DATE        NOT NULL,
  amount_paid   INT         NOT NULL,
  PRIMARY KEY (pass_id),
  CONSTRAINT chk_amount CHECK (amount_paid >= 0),
  INDEX idx_route (route_no)
) ENGINE=InnoDB;
