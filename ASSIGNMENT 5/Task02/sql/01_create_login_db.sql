-- Local test database created ONLY for CSA09 Assignment 5
CREATE DATABASE IF NOT EXISTS csa09_assignment5;
USE csa09_assignment5;
DROP TABLE IF EXISTS users;
CREATE TABLE users (
  id INT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(40) NOT NULL UNIQUE,
  password VARCHAR(40) NOT NULL,   -- plain text ONLY to demonstrate the flaw; real systems store salted hashes
  role VARCHAR(20) NOT NULL
);
INSERT INTO users(username, password, role) VALUES
 ('admin',   'Adm1n@2026',  'ADMIN'),
 ('warden',  'W@rden#77',   'WARDEN'),
 ('naveen',  'sim@ts2026',  'STUDENT');
