# Phase 02 - Backend Foundation

> I AM HERE emergency safety application.

## Status

Completed

## Objective

Establish the core backend foundation for the I AM HERE women's safety platform, providing real-time SOS session management, high-accuracy GPS ingestion, an interactive web-based live tracking viewer, and an evidence-based contact state machine.

## Implemented Architecture

### 1. SOS Session Management (`/api/sos`)
- **`POST /api/sos/start`**: Initializes emergency sessions with user metadata, medical notes, blood group, initial GPS fix, cancel PIN, secret duress PIN, and trusted contacts list. Generates a secure, URL-safe 16-character tracking token.
- **`GET /api/sos/:token`**: Returns active session telemetry, breadcrumbs, battery level, and contact delivery states. If accessed with `?cid=<contactId>`, automatically registers the contact as `SEEN`.
- **`POST /api/sos/:token/location`**: Ingests high-frequency GPS telemetry (lat, lng, accuracy, speed, heading, altitude, battery percentage). Maintains historical breadcrumb trail.
- **`POST /api/sos/:token/contact-status`**: Updates contact delivery state (`SENT`, `DELIVERED`, `DELIVERY_UNAVAILABLE`, `FAILED`) based on hardware/carrier broadcast evidence.
- **`POST /api/sos/:token/respond`**: Records contact intervention (`ON_MY_WAY`, `CALLED_POLICE`, `SAFE_CONFIRMED`) and syncs status back to the victim's phone.
- **`POST /api/sos/:token/stop`**: Resolves session with PIN verification. Detects secret Duress PIN and transitions status to `DURESS_CANCELLED` to silently alert circle.

### 2. Public Secure Live-Location Web Viewer (`/live/:token`)
- Mobile-optimized, dark-mode emergency interface with Leaflet.js and OpenStreetMap.
- Real-time pulsating victim location pin, accuracy radius circle, and breadcrumb path.
- Live telemetry cards: Battery %, accuracy radius in meters, speed.
- Interactive response buttons for trusted contacts ("I'm On My Way", "Dial 112", "Add Helper Note").
- Live Contact Response Network card displaying exact status of every trusted contact.

### 3. Evidence-Based Contact State Engine
Strict adherence to project rule: *Never claim a state without platform evidence.*
- `QUEUED`: Alert generated, awaiting dispatch.
- `SENT`: Confirmed dispatched from phone via SMS or push network.
- `DELIVERED`: Confirmed carrier delivery report received.
- `DELIVERY_UNAVAILABLE`: SMS sent, but carrier delivery receipt not provided.
- `SEEN`: Contact opened tracking link or acknowledged push.
- `RESPONDING`: Contact pressed response button on web viewer.

## Acceptance Criteria

- [x] Emergency Session API operational with token generation.
- [x] GPS location telemetry stream ingested and breadcrumbs preserved.
- [x] Public live-location web dashboard accessible without app installation.
- [x] Contact state machine tracks Queued, Sent, Delivered, Seen, and Responding.
- [x] Duress PIN and regular PIN validation tested.
- [x] Automated test suite passing with 100% success rate.

## Testing

Automated Jest integration tests executed via `npm test` in `backend/`:
- `√ starts an SOS session and creates tracking token`
- `√ marks contact SEEN when opening live tracking link with cid`
- `√ updates contact delivery state based on carrier evidence`
- `√ records contact responding action from live map`
- `√ ingests live location stream and updates breadcrumb history`
- `√ handles regular cancel PIN and secret Duress PIN`

## Completion Rule

All acceptance criteria verified against live server tests. No fake delivery states or synthetic government APIs used.

