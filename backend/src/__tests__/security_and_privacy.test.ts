import request from 'supertest';
import { app } from '../app';
import { emergencyStore } from '../store';

describe('Phase 12: Security and Privacy Hardening', () => {
  beforeEach(() => {
    emergencyStore.clearAll();
  });

  describe('Duress PIN & Silent Covert Tracking', () => {
    it('accepts duress PIN, enters DURESS_CANCELLED status, and continues location ingestion', async () => {
      const startRes = await request(app)
        .post('/api/sos/start')
        .send({
          userName: 'Priya Sharma',
          cancelPin: '1234',
          duressPin: '9999',
          contacts: [{ name: 'Mother', phone: '+919800000001' }]
        });

      const token = startRes.body.session.token;

      // Attacker forces user to stop SOS -> User enters secret Duress PIN '9999'
      const stopRes = await request(app)
        .post(`/api/sos/${token}/stop`)
        .send({ pin: '9999' });

      expect(stopRes.status).toBe(200);
      expect(stopRes.body.status).toBe('DURESS_CANCELLED');

      // Security hardening verification: Phone continues streaming silent background GPS!
      const duressLocationRes = await request(app)
        .post(`/api/sos/${token}/location`)
        .send({
          latitude: 28.6139,
          longitude: 77.2090,
          accuracy: 10,
          provider: 'gps',
          timestamp: Date.now()
        });

      expect(duressLocationRes.status).toBe(200);
      expect(duressLocationRes.body.session.status).toBe('DURESS_CANCELLED');
      expect(duressLocationRes.body.session.lastLocation.latitude).toBe(28.6139);

      // Contact opening live map sees DURESS ALERT
      const contactViewRes = await request(app).get(`/api/sos/${token}`);
      expect(contactViewRes.status).toBe(200);
      expect(contactViewRes.body.session.status).toBe('DURESS_CANCELLED');
    });

    it('rejects incorrect PIN without leaking whether duress exists', async () => {
      const startRes = await request(app)
        .post('/api/sos/start')
        .send({
          userName: 'Priya Sharma',
          cancelPin: '1234',
          duressPin: '9999',
          contacts: [{ name: 'Mother', phone: '+919800000001' }]
        });

      const token = startRes.body.session.token;

      const wrongPinRes = await request(app)
        .post(`/api/sos/${token}/stop`)
        .send({ pin: '0000' });

      expect(wrongPinRes.status).toBe(401);
      expect(wrongPinRes.body.error).toContain('PIN');
    });
  });

  describe('Zero Passive Tracking & Token Revocation', () => {
    it('normal resolution terminates location ingestion immediately', async () => {
      const startRes = await request(app)
        .post('/api/sos/start')
        .send({
          userName: 'Priya Sharma',
          cancelPin: '1234',
          contacts: [{ name: 'Mother', phone: '+919800000001' }]
        });

      const token = startRes.body.session.token;

      // Legitimate cancel by user
      const stopRes = await request(app)
        .post(`/api/sos/${token}/stop`)
        .send({ pin: '1234' });

      expect(stopRes.status).toBe(200);
      expect(stopRes.body.status).toBe('RESOLVED');

      // Attempt to stream location post-resolution must fail
      const postStopLoc = await request(app)
        .post(`/api/sos/${token}/location`)
        .send({
          latitude: 28.6140,
          longitude: 77.2091,
          accuracy: 8,
          provider: 'gps',
          timestamp: Date.now()
        });

      expect(postStopLoc.status).toBe(404);
    });

    it('enforces token TTL expiration to prevent permanent location tracking links', async () => {
      const session = emergencyStore.createSession({
        userName: 'Priya Sharma',
        userPhone: '+919876543210',
        contacts: [{ name: 'Mother', phone: '+919800000001' }]
      });

      // 1. Mark resolved 3 hours ago (TTL is 2 hours post-resolution)
      session.status = 'RESOLVED';
      session.endedAt = Date.now() - (3 * 60 * 60 * 1000);

      const expiredSession = emergencyStore.getSessionByToken(session.token);
      expect(expiredSession).toBeUndefined();

      // 2. HTTP GET returns 404 for expired token
      const res = await request(app).get(`/api/sos/${session.token}`);
      expect(res.status).toBe(404);
    });

    it('enforces absolute 24-hour maximum TTL on any emergency session', async () => {
      const session = emergencyStore.createSession({
        userName: 'Priya Sharma',
        userPhone: '+919876543210',
        contacts: [{ name: 'Mother', phone: '+919800000001' }]
      });

      // Session started 25 hours ago
      session.startedAt = Date.now() - (25 * 60 * 60 * 1000);

      const expired = emergencyStore.getSessionByToken(session.token);
      expect(expired).toBeUndefined();
    });
  });
});
