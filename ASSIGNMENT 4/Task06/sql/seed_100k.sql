-- Generates 100,000 test registrations for the "load all vs paginated" comparison (MySQL 8+).
USE campus_events;
SET SESSION cte_max_recursion_depth = 100000;

INSERT INTO registrations (student_id, student_name, email, event_id)
WITH RECURSIVE seq (n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 100000
)
SELECT CONCAT('T', LPAD(n, 6, '0')),
       CONCAT('Test Student ', n),
       CONCAT('test', n, '@campus.edu'),
       1 + (n MOD 5)
FROM seq;

SELECT COUNT(*) AS total_rows FROM registrations;
