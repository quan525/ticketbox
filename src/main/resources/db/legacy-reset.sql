-- Destructive development reset. Do not run against a shared or production database.
DELETE FROM booking_seats;
DELETE FROM payments;
DELETE FROM bookings;
DELETE FROM show_seats;
DELETE FROM shows;
DELETE FROM movies;
DELETE FROM seats;
DELETE FROM rooms;
DELETE FROM users;
