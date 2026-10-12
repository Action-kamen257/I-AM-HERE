import { randomUUID } from 'crypto';
import { 
  SosSession, 
  StartSosRequest, 
  LocationPoint, 
  ContactAlertStatus, 
  DeliveryState, 
  ContactResponseRequest,
  SosStatus 
} from './types';

class EmergencyStore {
  private sessionsById = new Map<string, SosSession>();
  private sessionsByToken = new Map<string, SosSession>();
  private usersByPhone = new Map<string, { phone: string; name: string; bloodGroup?: string; medicalNotes?: string; registeredAt: number }>();

  public registerUser(profile: { phone: string; name: string; bloodGroup?: string; medicalNotes?: string }) {
    const cleanPhone = profile.phone.replace(/\s+/g, '');
    const user = {
      ...profile,
      phone: cleanPhone,
      phoneNumber: cleanPhone,
      registeredAt: Date.now()
    };
    this.usersByPhone.set(cleanPhone, user);
    return user;
  }

  public getUserByPhone(phone: string) {
    const cleanPhone = phone.replace(/\s+/g, '');
    return this.usersByPhone.get(cleanPhone);
  }

  public getActiveSessionByPhone(phone: string): SosSession | undefined {
    const cleanPhone = phone.replace(/\s+/g, '');
    for (const session of this.sessionsByToken.values()) {
      if (session.userPhone.replace(/\s+/g, '') === cleanPhone && (session.status === 'ACTIVE' || session.status === 'DURESS_CANCELLED')) {
        return session;
      }
    }
    return undefined;
  }

  public createSession(req: StartSosRequest): SosSession {
    const id = randomUUID();
    // Secure URL-safe token for live tracking link
    const token = randomUUID().replace(/-/g, '').slice(0, 16);

    const contacts: ContactAlertStatus[] = (req.contacts || []).map((c, index) => ({
      contactId: c.contactId || `contact_${index + 1}_${Date.now()}`,
      name: c.name,
      phone: c.phone,
      relationship: c.relationship || 'Emergency Contact',
      deliveryState: 'QUEUED',
      seen: false,
      responding: false,
    }));

    const initialLocations: LocationPoint[] = req.initialLocation ? [req.initialLocation] : [];

    const session: SosSession = {
      id,
      token,
      userId: req.userId || 'user_local',
      userName: req.userName || 'Anonymous User',
      userPhone: req.userPhone || 'Unknown',
      medicalNotes: req.medicalNotes,
      bloodGroup: req.bloodGroup,
      status: 'ACTIVE',
      startedAt: Date.now(),
      lastLocation: req.initialLocation,
      locations: initialLocations,
      contacts,
      batteryLevel: req.initialLocation?.batteryLevel,
      isTestMode: !!req.isTestMode,
      cancelPinHash: req.cancelPin,
      duressPinHash: req.duressPin,
    };

    this.sessionsById.set(id, session);
    this.sessionsByToken.set(token, session);
    return session;
  }

  public getSessionById(id: string): SosSession | undefined {
    return this.sessionsById.get(id);
  }

  public getSessionByToken(token: string): SosSession | undefined {
    const session = this.sessionsByToken.get(token);
    if (!session) return undefined;

    // Security & Privacy Hardening: Token TTL Expiration
    const now = Date.now();
    const TWO_HOURS_MS = 2 * 60 * 60 * 1000;
    const TWENTY_FOUR_HOURS_MS = 24 * 60 * 60 * 1000;

    // 1. Expire token 2 hours after resolution to prevent historical location exposure
    if (session.status === 'RESOLVED' && session.endedAt && (now - session.endedAt > TWO_HOURS_MS)) {
      return undefined;
    }

    // 2. Absolute TTL: 24 hours maximum for emergency session tracking
    if (now - session.startedAt > TWENTY_FOUR_HOURS_MS) {
      return undefined;
    }

    return session;
  }

  public addLocation(token: string, loc: LocationPoint): SosSession | undefined {
    const session = this.sessionsByToken.get(token);
    if (!session || (session.status !== 'ACTIVE' && session.status !== 'DURESS_CANCELLED')) return undefined;

    session.locations.push(loc);
    session.lastLocation = loc;
    if (loc.batteryLevel !== undefined) {
      session.batteryLevel = loc.batteryLevel;
    }
    return session;
  }

  public addLocationsBatch(token: string, batch: LocationPoint[]): SosSession | undefined {
    const session = this.sessionsByToken.get(token);
    if (!session || (session.status !== 'ACTIVE' && session.status !== 'DURESS_CANCELLED')) return undefined;

    // Filter out points with accuracy radius worse than 60m unless it is the only fix
    const filtered = batch.filter(p => p.accuracy <= 60 || batch.length === 1);
    const sorted = [...filtered].sort((a, b) => a.timestamp - b.timestamp);

    for (const loc of sorted) {
      session.locations.push(loc);
    }

    if (sorted.length > 0) {
      const latest = sorted[sorted.length - 1];
      session.lastLocation = latest;
      if (latest.batteryLevel !== undefined) {
        session.batteryLevel = latest.batteryLevel;
      }
    }

    return session;
  }

  public updateContactStatus(
    token: string, 
    contactId: string, 
    deliveryState: DeliveryState, 
    evidence?: string
  ): { session?: SosSession; contact?: ContactAlertStatus } {
    const session = this.sessionsByToken.get(token);
    if (!session) return {};

    const contact = session.contacts.find(c => c.contactId === contactId || c.phone === contactId);
    if (!contact) return { session };

    contact.deliveryState = deliveryState;
    if (evidence) {
      contact.deliveryEvidence = evidence;
    }

    if (deliveryState === 'SENT' && !contact.sentAt) {
      contact.sentAt = Date.now();
    } else if (deliveryState === 'DELIVERED') {
      contact.deliveredAt = Date.now();
    }

    return { session, contact };
  }

  public markContactSeen(token: string, contactId?: string): { session?: SosSession; contact?: ContactAlertStatus } {
    const session = this.sessionsByToken.get(token);
    if (!session) return {};

    let targetContact: ContactAlertStatus | undefined;
    if (contactId) {
      targetContact = session.contacts.find(c => c.contactId === contactId || c.phone === contactId);
      if (targetContact) {
        targetContact.seen = true;
        targetContact.seenAt = targetContact.seenAt || Date.now();
      }
    }

    return { session, contact: targetContact };
  }

  public recordContactResponse(
    token: string, 
    resp: ContactResponseRequest
  ): { session?: SosSession; contact?: ContactAlertStatus } {
    const session = this.sessionsByToken.get(token);
    if (!session) return {};

    let contact = session.contacts.find(
      c => (resp.contactId && (c.contactId === resp.contactId || c.phone === resp.contactId)) ||
           (resp.contactName && c.name.toLowerCase() === resp.contactName.toLowerCase())
    );

    if (!contact && session.contacts.length > 0) {
      contact = session.contacts[0];
    }

    if (contact) {
      contact.seen = true;
      contact.seenAt = contact.seenAt || Date.now();
      contact.responding = true;
      contact.respondingAt = Date.now();
      contact.responseNote = resp.note || resp.action;
    }

    return { session, contact };
  }

  public stopSession(token: string, pin?: string): { session?: SosSession; status: 'SUCCESS' | 'DURESS' | 'INVALID_PIN' | 'NOT_FOUND' } {
    const session = this.sessionsByToken.get(token);
    if (!session) return { status: 'NOT_FOUND' };

    // Check Duress PIN
    if (session.duressPinHash && pin === session.duressPinHash) {
      session.status = 'DURESS_CANCELLED';
      session.endedAt = Date.now();
      return { session, status: 'DURESS' };
    }

    // Check normal PIN
    if (session.cancelPinHash && pin !== session.cancelPinHash) {
      return { session, status: 'INVALID_PIN' };
    }

    session.status = 'RESOLVED';
    session.endedAt = Date.now();
    return { session, status: 'SUCCESS' };
  }

  public clearAll() {
    this.sessionsById.clear();
    this.sessionsByToken.clear();
  }
}

export const emergencyStore = new EmergencyStore();
