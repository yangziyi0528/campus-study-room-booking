CREATE TABLE IF NOT EXISTS rooms (
    id VARCHAR(30) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    capacity INTEGER NOT NULL CHECK (capacity > 0)
);

CREATE TABLE IF NOT EXISTS bookings (
    id UUID PRIMARY KEY,
    room_id VARCHAR(30) NOT NULL REFERENCES rooms(id),
    student_id VARCHAR(30) NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    status VARCHAR(12) NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLED')),
    CHECK (start_at < end_at AND CAST(start_at AS DATE) = CAST(end_at AS DATE))
);

CREATE INDEX IF NOT EXISTS booking_room_day ON bookings(room_id, status, start_at);
CREATE INDEX IF NOT EXISTS booking_student ON bookings(student_id, start_at);
