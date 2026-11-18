-- Campus Event Registration - schema (MySQL 8+)
CREATE DATABASE IF NOT EXISTS campus_events CHARACTER SET utf8mb4;
USE campus_events;

DROP TABLE IF EXISTS registrations;
DROP TABLE IF EXISTS events;

CREATE TABLE events (
    event_id    INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    event_date  DATE         NOT NULL,
    venue       VARCHAR(100) NOT NULL
);

CREATE TABLE registrations (
    reg_id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_id    VARCHAR(12)  NOT NULL,
    student_name  VARCHAR(60)  NOT NULL,
    email         VARCHAR(100) NOT NULL,
    event_id      INT          NOT NULL,
    registered_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_reg_event FOREIGN KEY (event_id) REFERENCES events(event_id),
    -- Rule: a student cannot register twice for the same event. Enforced by the database as the final safety net.
    CONSTRAINT uq_student_event UNIQUE (student_id, event_id)
);

-- Speeds up "filter by event" and ORDER BY reg_id paging within an event.
CREATE INDEX idx_reg_event ON registrations (event_id, reg_id);

INSERT INTO events (name, event_date, venue) VALUES
 ('Hackathon 2026',        '2026-12-05', 'Main Auditorium'),
 ('Tech Talk: AI Safety',  '2026-12-10', 'Seminar Hall 2'),
 ('Cultural Fest',         '2026-12-18', 'Open Air Theatre'),
 ('Sports Day',            '2026-12-20', 'Campus Ground'),
 ('Career Fair',           '2027-01-12', 'Convention Centre');

-- Sample rows for the demo (normal data)
INSERT INTO registrations (student_id, student_name, email, event_id) VALUES
 ('S100001', 'Asha Raman',  'asha@campus.edu',  1),
 ('S100002', 'Bala Kumar',  'bala@campus.edu',  1),
 ('S100003', 'Chitra Devi', 'chitra@campus.edu', 2);
