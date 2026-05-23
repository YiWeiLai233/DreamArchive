-- Run this once before using admin account management.
-- "deleted" is a soft-delete tag: rows stay in the database.
ALTER TABLE user
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' AFTER role,
    ADD COLUMN deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER status;
