-- Idempotent demo seed for PostgreSQL. Safe to run more than once.
-- Enable with SQL_INIT_MODE=always, or apply with:
--   docker compose --profile seed up seed
-- Demo logins (password for both): password123
--   admin1  ADMIN
--   user    USER

INSERT INTO users (email, username, password, dob, first_name, last_name, role)
SELECT 'admin@test.com', 'admin1', '$2b$10$Fz1pkyranPFwzsa5gpkXb.F.VJqVD7OB4NSQGLzToUSYsJawiLyN6', DATE '1995-01-01', 'Admin', 'User', 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'admin1');

INSERT INTO users (email, username, password, dob, first_name, last_name, role)
SELECT 'user@test.com', 'user', '$2b$10$Fz1pkyranPFwzsa5gpkXb.F.VJqVD7OB4NSQGLzToUSYsJawiLyN6', DATE '2000-05-20', 'Normal', 'User', 'USER'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username = 'user');

INSERT INTO rooms (name, seat_rows, capacity)
SELECT 'Screen 01', 6, 60
WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE name = 'Screen 01');

INSERT INTO rooms (name, seat_rows, capacity)
SELECT 'Room A', 10, 100
WHERE NOT EXISTS (SELECT 1 FROM rooms WHERE name = 'Room A');

INSERT INTO seats (room_id, row_label, seat_number, seat_type)
SELECT r.id, chr(65 + row_idx), seat_num, CASE WHEN row_idx >= 3 THEN 'VIP' ELSE 'STANDARD' END
FROM rooms r
CROSS JOIN generate_series(0, 5) AS row_idx
CROSS JOIN generate_series(1, 10) AS seat_num
WHERE r.name = 'Screen 01'
  AND NOT EXISTS (
      SELECT 1 FROM seats s
      WHERE s.room_id = r.id
        AND s.row_label = chr(65 + row_idx)
        AND s.seat_number = seat_num
  );

INSERT INTO movies (title, author, description, duration_min, language)
SELECT title, author, description, duration_min, language
FROM (VALUES
    ('The Last Orbit', 'Alex Morgan', 'One final mission. A universe of possibilities. An astronaut follows a mysterious signal beyond the edge of the known world.', 110, 'EN'),
    ('Midnight in Kyoto', 'Yuki Tanaka', 'Two strangers cross paths on the last train through Kyoto, discovering a city and a connection neither expected.', 113, 'JA'),
    ('Wild Current', 'Sofia Reed', 'A marine biologist returns to her coastal hometown to protect the ocean that shaped her childhood.', 116, 'EN'),
    ('A Little More Time', 'Daniel Park', 'An unexpected friendship reminds two neighbors that it is never too late to begin again.', 119, 'EN'),
    ('Concrete Dreams', 'Maya Ellis', 'A young architect finds beauty, belonging, and a new perspective in a city that never stands still.', 122, 'EN'),
    ('Beyond the Pines', 'Oliver West', 'Two siblings retrace a childhood journey through the mountains and discover what it means to come home.', 125, 'EN'),
    ('The Quiet Season', 'Lena Bloom', 'A quiet village prepares for a summer that will change everything.', 128, 'EN'),
    ('After the Applause', 'Noah Chen', 'Behind the curtain, a musician learns to find her own voice.', 131, 'EN')
) AS catalog(title, author, description, duration_min, language)
WHERE NOT EXISTS (SELECT 1 FROM movies m WHERE m.title = catalog.title);

INSERT INTO shows (movie_id, room_id, start_time, end_time)
SELECT m.id,
       r.id,
       date_trunc('day', NOW()) + (1 + movie_ord + day_off * 6) * INTERVAL '1 day' + TIME '18:30',
       date_trunc('day', NOW()) + (1 + movie_ord + day_off * 6) * INTERVAL '1 day' + TIME '18:30' + (m.duration_min || ' minutes')::interval
FROM movies m
JOIN rooms r ON r.name = 'Screen 01'
JOIN (VALUES
    ('The Last Orbit', 0),
    ('Midnight in Kyoto', 1),
    ('Wild Current', 2),
    ('A Little More Time', 3),
    ('Concrete Dreams', 4),
    ('Beyond the Pines', 5)
) AS billed(title, movie_ord) ON billed.title = m.title
CROSS JOIN generate_series(0, 1) AS day_off
WHERE NOT EXISTS (
    SELECT 1 FROM shows s
    WHERE s.movie_id = m.id
      AND s.room_id = r.id
      AND s.start_time = date_trunc('day', NOW()) + (1 + billed.movie_ord + day_off * 6) * INTERVAL '1 day' + TIME '18:30'
);

INSERT INTO show_seats (show_id, seat_id, status, price, version)
SELECT s.id,
       st.id,
       'AVAILABLE',
       CASE WHEN st.seat_type = 'VIP' THEN 150000 ELSE 100000 END,
       0
FROM shows s
JOIN seats st ON st.room_id = s.room_id
WHERE NOT EXISTS (
    SELECT 1 FROM show_seats ss
    WHERE ss.show_id = s.id AND ss.seat_id = st.id
);
