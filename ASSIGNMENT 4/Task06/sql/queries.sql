-- Queries used by the JDBC layer (shown here for the report)
-- 1. Duplicate check
SELECT 1 FROM registrations WHERE student_id = ? AND event_id = ? LIMIT 1;
-- 2. Register
INSERT INTO registrations (student_id, student_name, email, event_id, registered_at) VALUES (?, ?, ?, ?, ?);
-- 3. Filter by event + pagination (keyset-friendly ORDER BY on the primary key)
SELECT r.reg_id, r.student_id, r.student_name, r.email, r.event_id, e.name, r.registered_at
FROM registrations r JOIN events e ON e.event_id = r.event_id
WHERE (? IS NULL OR r.event_id = ?)
ORDER BY r.reg_id LIMIT ? OFFSET ?;
-- 4. Count for the page indicator
SELECT COUNT(*) FROM registrations WHERE (? IS NULL OR event_id = ?);
