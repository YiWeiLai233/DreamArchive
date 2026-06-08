-- Run this once on existing databases before using the admin console.
ALTER TABLE user
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Then run admin_user_status_patch.sql to add account status and soft-delete tags.

-- Pick your real admin account.
-- UPDATE user SET role = 'ADMIN' WHERE email = 'your-email@example.com';
