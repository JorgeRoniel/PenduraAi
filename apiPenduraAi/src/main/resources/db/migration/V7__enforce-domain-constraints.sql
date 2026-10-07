ALTER TABLE user_tb
    ALTER COLUMN nome TYPE VARCHAR(50),
    ALTER COLUMN nome SET NOT NULL,
    ALTER COLUMN email TYPE VARCHAR(254),
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE user_tb
    ADD CONSTRAINT ck_user_email_normalized
    CHECK (email = LOWER(TRIM(email)));

ALTER TABLE dividas_tb
    ALTER COLUMN cliente SET NOT NULL,
    ALTER COLUMN valor SET NOT NULL,
    ALTER COLUMN user_id SET NOT NULL,
    ALTER COLUMN created_at SET NOT NULL,
    ALTER COLUMN updated_at SET NOT NULL;

ALTER TABLE dividas_tb
    ADD CONSTRAINT ck_dividas_valor_positivo
    CHECK ( valor >= 0.01 );