import express, { Request, Response } from 'express';
import cors from 'cors';
import path from 'path';
import { emergencyStore } from './store';
import { pushService } from './push';
import { smsGateway } from './smsGateway';
import { 
  StartSosRequest, 
  LocationUpdateRequest, 
  ContactStatusUpdateRequest, 
  ContactResponseRequest, 
  StopSosRequest 
} from './types';

import fs from 'fs';

export const app = express();

app.use(cors());
app.use(express.json());

// Serve static assets for the web live viewer
app.use(express.static(path.join(__dirname, '../public')));

// Health check
app.get('/health', (req: Request, res: Response) => {
  res.json({ status: 'ok', time: Date.now() });
});

/**
 * App Auto-Update API: Version check endpoint
 * Mobile app queries this to determine if a newer build is available.
 */
app.get('/api/app/version', (req: Request, res: Response) => {
  const apkPath = path.join(__dirname, '../../android/app/build/outputs/apk/debug/app-debug.apk');
  let fileSizeBytes = 0;
  let lastModified = new Date().toISOString();
  if (fs.existsSync(apkPath)) {
    const stat = fs.statSync(apkPath);
    fileSizeBytes = stat.size;
    lastModified = stat.mtime.toISOString();
  }

  return res.json({
    success: true,
    versionCode: 3,
    versionName: '1.2.0',
    minSupportedVersionCode: 1,
    apkUrl: '/api/app/latest.apk',
    fileSizeBytes,
    releaseNotes: '• Polished Typography & Dock Layout\n• One-Click In-App Build Streaming\n• Victim Silencing & Active Buzzer Alerting',
    publishedAt: lastModified,
  });
});

/**
 * App Auto-Update API: Stream the latest APK binary
 */
app.get('/api/app/latest.apk', (req: Request, res: Response) => {
  const primaryApk = path.join(__dirname, '../../android/app/build/outputs/apk/debug/app-debug.apk');
  const fallbackApk = path.join(__dirname, '../public/downloads/app-latest.apk');

  const targetPath = fs.existsSync(primaryApk) ? primaryApk : (fs.existsSync(fallbackApk) ? fallbackApk : null);

  if (!targetPath) {
    return res.status(404).json({
      error: 'Latest APK binary build not found. Run gradle assembleDebug first.',
    });
  }

  res.setHeader('Content-Type', 'application/vnd.android.package-archive');
  res.setHeader('Content-Disposition', 'attachment; filename="iamhere-latest.apk"');
  return res.sendFile(targetPath);
});

/**
  * Phone Number Login & Victim Profile Registration
  * The emergency system runs specifically for this victim phone number.
  */
app.post('/api/auth/phone-login', (req: Request, res: Response) => {
  const rawPhone = req.body.phone || req.body.phoneNumber;
  const { name, bloodGroup, medicalNotes } = req.body;

  if (!rawPhone || typeof rawPhone !== 'string' || rawPhone.trim().replace(/\D/g, '').length < 10) {
    return res.status(400).json({ error: 'Valid 10+ digit phone number is required for emergency victim registration' });
  }

  const profile = emergencyStore.registerUser({
    phone: rawPhone.trim(),
    name: name && typeof name === 'string' ? name.trim() : 'Emergency User',
    bloodGroup: bloodGroup || undefined,
    medicalNotes: medicalNotes || undefined,
  });

  return res.status(200).json({
    success: true,
    message: 'Victim emergency identity registered successfully',
    victim: profile,
    user: profile,
  });
});

/**
 * Get registered victim profile by phone number
 */
app.get('/api/user/profile/:phone', (req: Request, res: Response) => {
  const phone = String(req.params.phone);
  const profile = emergencyStore.getUserByPhone(phone);
  if (!profile) {
    return res.status(404).json({ error: 'Victim profile not found' });
  }
  return res.json({ success: true, victim: profile, user: profile });
});

/**
 * Query active SOS session specifically for a victim phone number
 */
app.get('/api/victim/:phone/active-sos', (req: Request, res: Response) => {
  const phone = String(req.params.phone);
  const session = emergencyStore.getActiveSessionByPhone(phone);
  return res.json({
    success: true,
    hasActiveSos: !!session,
    session: session || null,
  });
});

/**
 * Start a new SOS emergency session
 */
app.post('/api/sos/start', (req: Request, res: Response) => {
  const rawContacts = req.body.contacts || req.body.trustedContacts;
  if (!rawContacts || !Array.isArray(rawContacts)) {
    return res.status(400).json({ error: 'Contacts list is required' });
  }

  const startReq: StartSosRequest = {
    ...req.body,
    contacts: rawContacts.map((c: any, index: number) => ({
      contactId: c.contactId || c.id || `contact_${index + 1}`,
      name: c.name || 'Emergency Contact',
      phone: c.phone || c.phoneNumber || '',
      relationship: c.relationship || 'Emergency Contact',
    }))
  };

  const session = emergencyStore.createSession(startReq);
  const host = req.get('host') || 'localhost:4000';
  const protocol = req.protocol;
  const liveUrl = `${protocol}://${host}/live/${session.token}`;

  return res.status(201).json({
    success: true,
    session,
    token: session.token,
    liveUrl,
  });
});

/**
 * Get active SOS session by token (for web tracking portal & status check)
 * Passing ?cid=contact_id marks that specific contact as SEEN
 */
app.get('/api/sos/:token', (req: Request, res: Response) => {
  const token = String(req.params.token);
  const cid = req.query.cid;

  const session = emergencyStore.getSessionByToken(token);
  if (!session) {
    return res.status(404).json({ error: 'Emergency session not found or expired' });
  }

  // If a contact ID was provided via URL query, mark them as seen
  if (cid && typeof cid === 'string') {
    emergencyStore.markContactSeen(token, cid);
  }

  return res.json({
    success: true,
    session,
  });
});

/**
 * Ingest high-accuracy location telemetry from device
 */
app.post('/api/sos/:token/location', (req: Request, res: Response) => {
  const token = String(req.params.token);
  const locReq: LocationUpdateRequest = req.body;

  if (locReq.latitude === undefined || locReq.longitude === undefined) {
    return res.status(400).json({ error: 'Latitude and longitude are required' });
  }

  const updatedSession = emergencyStore.addLocation(token, {
    latitude: locReq.latitude,
    longitude: locReq.longitude,
    accuracy: locReq.accuracy ?? 10,
    speed: locReq.speed,
    heading: locReq.heading,
    altitude: locReq.altitude,
    timestamp: locReq.timestamp || Date.now(),
    batteryLevel: locReq.batteryLevel,
    provider: locReq.provider || 'fused',
  });

  if (!updatedSession) {
    return res.status(404).json({ error: 'Active session not found' });
  }

  return res.json({
    success: true,
    session: updatedSession,
    lastLocation: updatedSession.lastLocation,
    batteryLevel: updatedSession.batteryLevel,
  });
});

/**
 * Batch location telemetry ingestion for offline queue flushes (Phase 06)
 */
app.post('/api/sos/:token/locations/batch', (req: Request, res: Response) => {
  const token = String(req.params.token);
  const { locations } = req.body;

  if (!Array.isArray(locations) || locations.length === 0) {
    return res.status(400).json({ error: 'locations array is required' });
  }

  const updatedSession = emergencyStore.addLocationsBatch(token, locations);
  if (!updatedSession) {
    return res.status(404).json({ error: 'Active session not found' });
  }

  return res.json({
    success: true,
    count: locations.length,
    lastLocation: updatedSession.lastLocation,
    batteryLevel: updatedSession.batteryLevel,
  });
});

/**
 * Update contact delivery state (called by phone after SMS sent/delivered or push receipt)
 */
app.post('/api/sos/:token/contact-status', (req: Request, res: Response) => {
  const token = String(req.params.token);
  const { contactId, deliveryState, deliveryEvidence }: ContactStatusUpdateRequest = req.body;

  if (!contactId || !deliveryState) {
    return res.status(400).json({ error: 'contactId and deliveryState are required' });
  }

  const result = emergencyStore.updateContactStatus(token, contactId, deliveryState, deliveryEvidence);
  if (!result.session) {
    return res.status(404).json({ error: 'Session not found' });
  }

  return res.json({
    success: true,
    contact: result.contact,
    contacts: result.session.contacts,
  });
});

/**
 * Contact responding action (called from web live viewer)
 */
app.post('/api/sos/:token/respond', (req: Request, res: Response) => {
  const token = String(req.params.token);
  const resp: ContactResponseRequest = req.body;

  const result = emergencyStore.recordContactResponse(token, resp);
  if (!result.session) {
    return res.status(404).json({ error: 'Session not found' });
  }

  return res.json({
    success: true,
    contact: result.contact,
    contacts: result.session.contacts,
  });
});

/**
 * Dispatch Cloud SMS fallback (Phase 05)
 */
app.post('/api/sos/:token/cloud-sms', async (req: Request, res: Response) => {
  const token = String(req.params.token);
  const { contactId, messageText } = req.body;

  const session = emergencyStore.getSessionByToken(token);
  if (!session) {
    return res.status(404).json({ error: 'Session not found' });
  }

  const contact = session.contacts.find(c => c.contactId === contactId || c.phone === contactId);
  if (!contact) {
    return res.status(404).json({ error: 'Contact not found' });
  }

  const result = await smsGateway.dispatchCloudSms({
    toPhone: contact.phone,
    contactId: contact.contactId,
    messageText: messageText || `EMERGENCY ALERT: ${session.userName} needs help! View: http://${req.get('host')}/live/${token}?cid=${contact.contactId}`,
    senderName: session.userName,
    isTestMode: session.isTestMode,
  });

  if (result.success) {
    emergencyStore.updateContactStatus(token, contact.contactId, result.deliveryState, result.deliveryEvidence);
  }

  return res.json({
    success: result.success,
    result,
    contacts: session.contacts,
  });
});

/**
 * Dispatch FCM push alert to trusted contact (Phase 04)
 */
app.post('/api/sos/:token/push-alert', async (req: Request, res: Response) => {
  const token = String(req.params.token);
  const { contactId, recipientToken } = req.body;

  const session = emergencyStore.getSessionByToken(token);
  if (!session) {
    return res.status(404).json({ error: 'Session not found' });
  }

  const contact = session.contacts.find(c => c.contactId === contactId || c.phone === contactId);
  if (!contact) {
    return res.status(404).json({ error: 'Contact not found' });
  }

  const result = await pushService.dispatchAlert({
    recipientToken: recipientToken || 'dummy_token',
    contactId: contact.contactId,
    victimName: session.userName,
    victimPhone: session.userPhone,
    liveUrl: `http://${req.get('host')}/live/${token}?cid=${contact.contactId}`,
    isEscalated: false,
    timestamp: Date.now(),
  });

  if (result.success && result.deliveryReceipt) {
    emergencyStore.updateContactStatus(token, contact.contactId, 'DELIVERED', result.deliveryReceipt);
  }

  return res.json({
    success: result.success,
    result,
    contacts: session.contacts,
  });
});

/**
 * Alert Escalation Ladder (Phase 04): Escalates unacknowledged alerts
 */
app.post('/api/sos/:token/escalate', async (req: Request, res: Response) => {
  const token = String(req.params.token);

  const session = emergencyStore.getSessionByToken(token);
  if (!session) {
    return res.status(404).json({ error: 'Session not found' });
  }

  // Find contacts that are not yet seen or responding
  const pendingContacts = session.contacts.filter(c => !c.seen && !c.responding);
  const escalatedResults = [];

  for (const c of pendingContacts) {
    const pushResult = await pushService.dispatchAlert({
      recipientToken: 'escalated_token',
      contactId: c.contactId,
      victimName: session.userName,
      victimPhone: session.userPhone,
      liveUrl: `http://${req.get('host')}/live/${token}?cid=${c.contactId}`,
      isEscalated: true,
      timestamp: Date.now(),
    });

    escalatedResults.push({
      contactId: c.contactId,
      name: c.name,
      pushResult,
    });
  }

  return res.json({
    success: true,
    escalatedCount: pendingContacts.length,
    escalatedResults,
    contacts: session.contacts,
  });
});

/**
 * Cancel or stop emergency session
 */
app.post('/api/sos/:token/stop', (req: Request, res: Response) => {
  const token = String(req.params.token);
  const { pin }: StopSosRequest = req.body;

  const result = emergencyStore.stopSession(token, pin);
  if (result.status === 'NOT_FOUND') {
    return res.status(404).json({ error: 'Session not found' });
  }

  if (result.status === 'INVALID_PIN') {
    return res.status(401).json({ error: 'Incorrect cancellation PIN' });
  }

  if (result.status === 'DURESS') {
    // Duress cancellation: silently flag contacts
    return res.json({
      success: true,
      status: 'DURESS_CANCELLED',
      message: 'SOS cancelled under duress alert.',
    });
  }

  return res.json({
    success: true,
    status: 'RESOLVED',
    message: 'SOS successfully resolved.',
  });
});

// Direct route to serve live tracking page
app.get('/live/:token', (req: Request, res: Response) => {
  res.sendFile(path.join(__dirname, '../public/live.html'));
});
