# Booking API

The REST API uses JSON and the base path `/api/v1`. Dates use `YYYY-MM-DD`. Times are ISO local date-times in the Asia/Shanghai time zone. Each fictional room is reserved as a unit.

| Method and path | Input | Success |
| --- | --- | --- |
| `GET /rooms` | None | 200 with room IDs, names, and capacities |
| `GET /rooms/{roomId}/availability` | `date=YYYY-MM-DD` | 200 with the room, the requested date, and free time ranges |
| `POST /bookings` | JSON with `roomId`, `studentId`, `start`, and `end` | 201 with the booking ID and status |
| `GET /bookings` | `studentId` | 200 with the student's active and cancelled bookings |
| `DELETE /bookings/{id}` | `studentId` matching the booking | 200 with the cancelled booking. Repeating the request returns the same cancelled status. |

Example request

```json
{"roomId":"R101","studentId":"DEMO-1","start":"2030-02-01T09:00","end":"2030-02-01T10:00"}
```

Bookings start in the future, stay within one day from 08:00 to 22:00, and start and end on whole minutes. Time ranges are half-open. An interval ending at 10:00 leaves a new interval starting at 10:00 available. The server returns a 409 error if another active booking overlaps.

Room and student IDs are case-insensitive and contain one to 30 letters, digits, or hyphens. The first character is a letter or digit. For today's date, availability starts at the next whole minute or at opening time, whichever comes later.

Error responses contain a stable `code` and an English `message`. Invalid parameters or JSON return 400 (`INVALID_REQUEST`), unknown rooms and bookings return 404, and conflicting reservations return 409 (`BOOKING_CONFLICT`). Cancelling with a mismatched student ID returns 404.
