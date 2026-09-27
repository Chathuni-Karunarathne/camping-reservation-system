CREATE TABLE IF NOT EXISTS users (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 full_name TEXT NOT NULL CHECK(length(trim(full_name)) BETWEEN 1 AND 100),
 email TEXT NOT NULL COLLATE NOCASE UNIQUE,
 username TEXT NOT NULL COLLATE NOCASE UNIQUE,
 password_hash TEXT NOT NULL,
 role TEXT NOT NULL CHECK(role IN ('ADMIN','USER'))
);
CREATE TABLE IF NOT EXISTS campsites (
 id TEXT PRIMARY KEY NOT NULL COLLATE NOCASE,
 name TEXT NOT NULL,
 province TEXT NOT NULL,
 description TEXT NOT NULL DEFAULT '',
 rate_cents INTEGER NOT NULL CHECK(rate_cents BETWEEN 1 AND 100000000),
 active INTEGER NOT NULL DEFAULT 1 CHECK(active IN (0,1))
);
CREATE TABLE IF NOT EXISTS reservations (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 campsite_id TEXT NOT NULL REFERENCES campsites(id) ON DELETE RESTRICT,
 user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
 arrival TEXT NOT NULL CHECK(length(arrival)=10 AND date(arrival)=arrival),
 departure TEXT NOT NULL CHECK(length(departure)=10 AND date(departure)=departure AND departure > arrival),
 people INTEGER NOT NULL CHECK(people BETWEEN 1 AND 100),
 rate_cents INTEGER NOT NULL CHECK(rate_cents BETWEEN 1 AND 100000000),
 status TEXT NOT NULL DEFAULT 'CONFIRMED' CHECK(status IN ('CONFIRMED','CANCELLED')),
 created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS reservations_dates ON reservations(campsite_id, status, arrival, departure);
CREATE INDEX IF NOT EXISTS reservations_user ON reservations(user_id, arrival);
CREATE TABLE IF NOT EXISTS feedback (
 id INTEGER PRIMARY KEY AUTOINCREMENT,
 user_id INTEGER NOT NULL REFERENCES users(id),
 message TEXT NOT NULL CHECK(length(trim(message)) BETWEEN 1 AND 2000),
 created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS feedback_user ON feedback(user_id);
-- @statement
CREATE TRIGGER IF NOT EXISTS reservations_no_overlap_insert BEFORE INSERT ON reservations
WHEN NEW.status='CONFIRMED' AND EXISTS (
 SELECT 1 FROM reservations WHERE campsite_id=NEW.campsite_id AND status='CONFIRMED'
 AND arrival < NEW.departure AND departure > NEW.arrival
)
BEGIN SELECT RAISE(ABORT, 'reservation_overlap'); END;
-- @statement
CREATE TRIGGER IF NOT EXISTS reservations_no_overlap_update BEFORE UPDATE ON reservations
WHEN NEW.status='CONFIRMED' AND EXISTS (
 SELECT 1 FROM reservations WHERE campsite_id=NEW.campsite_id AND id<>NEW.id AND status='CONFIRMED'
 AND arrival < NEW.departure AND departure > NEW.arrival
)
BEGIN SELECT RAISE(ABORT, 'reservation_overlap'); END;
