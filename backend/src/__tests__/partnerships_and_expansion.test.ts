import crypto from 'crypto';
import { partnershipManager } from '../partnerships';
import { SosSession } from '../types';

describe('Phase 16: Future Partnerships and Standardized Alert Expansion', () => {
  const dummySession: SosSession = {
    id: 'sos_178900',
    token: 'tok_partner_test',
    userId: 'user_1',
    userName: 'Priya Sharma',
    userPhone: '+919876543210',
    medicalNotes: 'Asthma inhaler required',
    status: 'ACTIVE',
    startedAt: Date.now(),
    isTestMode: false,
    contacts: [],
    locations: [
      {
        latitude: 28.6139,
        longitude: 77.2090,
        accuracy: 12,
        timestamp: Date.now(),
        provider: 'gps'
      }
    ],
    lastLocation: {
      latitude: 28.6139,
      longitude: 77.2090,
      accuracy: 12,
      timestamp: Date.now(),
      provider: 'gps'
    }
  };

  describe('Common Alerting Protocol (EDXL-CAP) Integration', () => {
    it('generates compliant CAP 1.2 payload for national emergency interoperability', () => {
      const cap = partnershipManager.generateCapPayload(dummySession, 'Alert');

      expect(cap.identifier).toContain('CAP_IAMHERE_');
      expect(cap.sender).toBe('urn:iamhere:safety:service');
      expect(cap.status).toBe('Actual');
      expect(cap.msgType).toBe('Alert');
      expect(cap.info.category).toBe('Safety');
      expect(cap.info.urgency).toBe('Immediate');
      expect(cap.info.severity).toBe('Extreme');
      expect(cap.info.headline).toContain('Priya Sharma');
      expect(cap.info.area.point).toEqual({ latitude: 28.6139, longitude: 77.2090 });
      expect(cap.info.area.circle).toBe('28.6139,77.209,12');
    });

    it('flags test mode drill events as Exercise status in CAP standard', () => {
      const drillSession: SosSession = { ...dummySession, isTestMode: true };
      const cap = partnershipManager.generateCapPayload(drillSession, 'Alert');

      expect(cap.status).toBe('Exercise');
    });
  });

  describe('Partner Webhook Dispatch & Cryptographic Signatures', () => {
    it('generates verifiable HMAC-SHA256 signatures for webhook integrity', () => {
      const secret = 'test_secret_key_123';
      const payload = JSON.stringify({ event: 'SOS_STARTED', victim: 'Priya' });

      const signature = partnershipManager.signPayload(payload, secret);
      const expectedSignature = crypto.createHmac('sha256', secret).update(payload).digest('hex');

      expect(signature).toBe(expectedSignature);
      expect(signature.length).toBe(64); // 256 bits in hex
    });

    it('dispatches partner notifications and skips disabled or unregistered events', async () => {
      partnershipManager.registerPartner({
        id: 'shelter_desk',
        name: 'Women Crisis Support Desk',
        url: 'https://shelter.org/webhook',
        secret: 'shelter_secret_449',
        enabled: true,
        events: ['SOS_STARTED', 'DURESS_TRIGGERED']
      });

      const results = await partnershipManager.notifyPartners('SOS_STARTED', dummySession);
      const shelterResult = results.find(r => r.partnerId === 'shelter_desk');

      expect(shelterResult).toBeDefined();
      expect(shelterResult?.status).toBe('SUCCESS');
      expect(shelterResult?.signature).toBeDefined();

      // Test event filtering: LOCATION_UPDATE is not in shelter_desk's subscribed events
      const skipResults = await partnershipManager.notifyPartners('LOCATION_UPDATE', dummySession);
      const shelterSkip = skipResults.find(r => r.partnerId === 'shelter_desk');
      expect(shelterSkip?.status).toBe('SKIPPED');
    });
  });
});
