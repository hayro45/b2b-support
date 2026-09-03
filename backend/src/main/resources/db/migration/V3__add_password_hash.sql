ALTER TABLE app_users
ADD COLUMN IF NOT EXISTS password_hash VARCHAR(255);

UPDATE app_users
SET password_hash = '{noop}demo12345'
WHERE password_hash IS NULL;

ALTER TABLE app_users
ALTER COLUMN password_hash SET NOT NULL;
