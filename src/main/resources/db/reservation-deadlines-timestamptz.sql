-- Run with psql and pass -v legacy_zone=<previous JVM timezone>.
-- Both deadlines must be converted with the same timezone.
\set ON_ERROR_STOP on
BEGIN;
SELECT set_config('ticketbox.legacy_deadline_zone', :'legacy_zone', true);
DO $migration$
DECLARE
    legacy_zone text := current_setting('ticketbox.legacy_deadline_zone');
    deadline record;
    column_type text;
BEGIN
    IF NOT EXISTS (SELECT FROM pg_timezone_names WHERE name = legacy_zone) THEN
        RAISE EXCEPTION 'Invalid legacy deadline timezone: %', legacy_zone;
    END IF;

    FOR deadline IN SELECT * FROM (VALUES ('bookings', 'expires_at'), ('show_seats', 'held_until'))
            AS columns_to_migrate(table_name, column_name)
    LOOP
        SELECT a.atttypid::regtype::text INTO column_type
        FROM pg_attribute a
        WHERE a.attrelid = to_regclass(deadline.table_name)
          AND a.attname = deadline.column_name AND NOT a.attisdropped;

        IF column_type = 'timestamp without time zone' THEN
            EXECUTE format(
                'ALTER TABLE %I ALTER COLUMN %I TYPE timestamptz USING %I AT TIME ZONE %L',
                deadline.table_name, deadline.column_name, deadline.column_name, legacy_zone
            );
        ELSIF column_type IS DISTINCT FROM 'timestamp with time zone' THEN
            RAISE EXCEPTION 'Unexpected deadline column type for %.%: %',
                deadline.table_name, deadline.column_name, column_type;
        END IF;
    END LOOP;
END
$migration$;
COMMIT;
