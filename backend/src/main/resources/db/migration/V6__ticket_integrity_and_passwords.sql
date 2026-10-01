ALTER TABLE tickets ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE SEQUENCE ticket_number_seq START WITH 1000000;

-- Existing databases retain their migration history. Upgrade legacy plaintext
-- passwords in place; new passwords are written through the BCrypt encoder.
UPDATE app_users
SET password_hash = '{bcrypt}' || crypt(substring(password_hash from 7), gen_salt('bf', 12))
WHERE password_hash LIKE '{noop}%';
