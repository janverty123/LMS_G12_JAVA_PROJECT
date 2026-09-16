-- Preflight: take a database backup and confirm public.users exists.
-- Additive migration; preserves all existing accounts and profile data.
BEGIN;
SET LOCAL lock_timeout = '5s';
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS profile_picture text;
COMMIT;
