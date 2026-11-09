DELETE FROM users;

INSERT INTO users (
    id,
    email,
    username,
    password,
    dob,
    first_name,
    last_name,
    role
) VALUES
(
    101,
    'admin@test.com',
    'admin',
    '$2a$10$qruNBUYShWcQC4S4vVuFNObFD7NvjY5ULk5sTvTY6F8pY2CmcHSoK',
    '1995-01-01',
    'Admin',
    'User',
    'ADMIN'
),
(
    102,
    'user@test.com',
    'user1',
    '$2a$10$qruNBUYShWcQC4S4vVuFNObFD7NvjY5ULk5sTvTY6F8pY2CmcHSoK',
    '2000-05-20',
    'Normal',
    'User',
    'USER'
);