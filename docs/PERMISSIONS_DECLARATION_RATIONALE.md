# Google Play Permissions Declaration Rationale

## Application: I AM HERE (com.iamhere.emergency)

This document provides the formal Google Play Developer Policy declaration and architectural justification for sensitive and high-risk permissions requested by **I AM HERE**.

---

### 1. Sensitive Permission: `android.permission.SEND_SMS`
- **Policy Category**: Personal Safety / Emergency Assistance (Default SMS / Emergency Handler exception).
- **Core User-Facing Need**:
  In critical emergencies (abduction, stalking, remote highway travel, domestic violence), cellular mobile data networks are frequently degraded, throttled, or completely absent. Native multi-part SMS is the only resilient communication protocol guaranteed to penetrate weak 2G/EDGE cellular signals.
- **Usage Trigger**:
  1. Sent exclusively upon intentional user SOS trigger (long-press, Quick Settings tile, Power Button toggle, or verified impact).
  2. Dispatches real-time distress coordinates and secure live-tracking tokens directly to pre-configured emergency contacts.
  3. Dispatches silent duress alert if an attacker forces cancellation.
- **Privacy Safeguard**: The app never inspects personal SMS inboxes or contacts outside the user's explicitly selected emergency circle.

---

### 2. Sensitive Permission: `android.permission.CALL_PHONE`
- **Policy Category**: Direct Emergency Calling.
- **Core User-Facing Need**:
  In moments of extreme duress, victims cannot manually unlock their handset, open the dialer, type "112", and hit call. The app provides a 1-tap direct dispatch to the National Emergency Response Support System (ERSS 112).
- **Usage Trigger**:
  Tapping the dedicated "Call 112 Police" emergency button triggers direct dialing while preparing an automated Text-to-Speech location readout for the operator.

---

### 3. Sensitive Permission: `ACCESS_FINE_LOCATION` & `FOREGROUND_SERVICE_LOCATION`
- **Policy Category**: Emergency Tracking & Life-Safety Guidance.
- **Core User-Facing Need**:
  Emergency responders and family members need sub-10-meter precision GPS breadcrumbs to locate the victim in motion (e.g. in a moving vehicle or running along an unlit street).
- **Zero Passive Tracking Guarantee**:
  - The foreground location service runs **ONLY** during active SOS or during an active "Walk With Me" journey.
  - GPS hardware is completely powered off and unsubscribed when the emergency is concluded.
  - A permanent high-priority ongoing notification alerts the user whenever location tracking is engaged.

---

### 4. Background Execution: `WAKE_LOCK` & Battery Optimization Exemption
- **Core Need**:
  Prevents OEM OS task killers and Android Doze mode from sleeping CPU threads while broadcasting emergency location packets and awaiting carrier PDU receipts during an active distress event.
