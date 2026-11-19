CREATE DATABASE IF NOT EXISTS csa09_assignment5;
USE csa09_assignment5;
DROP TABLE IF EXISTS bus_pass_migrated;
CREATE TABLE bus_pass_migrated (
  pass_id       VARCHAR(12) NOT NULL,
  register_no   VARCHAR(12) NOT NULL,
  name          VARCHAR(60) NOT NULL,
  route_no      VARCHAR(4)  NOT NULL,
  boarding_stop VARCHAR(40) NOT NULL,
  valid_until   DATE        NOT NULL,
  amount_paid   INT         NOT NULL,
  PRIMARY KEY (pass_id),
  CONSTRAINT chk_amount_m CHECK (amount_paid >= 0),
  INDEX idx_route_m (route_no)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS decision_pass;
CREATE TABLE decision_pass (
  pass_id       VARCHAR(12) NOT NULL,
  campus        TINYINT     NOT NULL,
  register_no   VARCHAR(12) NOT NULL,
  name          VARCHAR(60) NOT NULL,
  route_no      VARCHAR(4)  NOT NULL,
  boarding_stop VARCHAR(40) NOT NULL,
  valid_until   DATE        NOT NULL,
  amount_paid   INT         NOT NULL,
  PRIMARY KEY (pass_id),
  INDEX idx_campus_route (campus, route_no)
) ENGINE=InnoDB;

DROP TABLE IF EXISTS issue_counter;
CREATE TABLE issue_counter (
  issue_no    INT AUTO_INCREMENT PRIMARY KEY,
  counter_id  INT NOT NULL,
  register_no VARCHAR(12) NOT NULL
) ENGINE=InnoDB;
