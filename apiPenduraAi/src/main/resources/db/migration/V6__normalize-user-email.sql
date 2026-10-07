UPDATE user_tb
SET email = LOWER(TRIM(email));

ALTER TABLE user_tb
DROP CONSTRAINT IF EXISTS uk_user_email;

CREATE UNIQUE INDEX uk_user_email_normalized
ON user_tb (LOWER(TRIM(email)));