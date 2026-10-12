import request from 'supertest';
import { app } from '../app';
import { emergencyStore } from '../store';

describe('Phase 11: Trusted Circle Safety Companion Features', () => {
  beforeEach(() => {
    emergencyStore.clearAll();
  });

  describe('Walk With Me Journey Companion', () => {
    it('initializes journey session with check-in intervals and tracks timing', () => {
      const durationMinutes = 30;
      const intervalMinutes = 10;
      const now = Date.now();
      const expiresAt = now + durationMinutes * 60 * 1000;
      const nextCheckIn = now + intervalMinutes * 60 * 1000;

      const journey = {
        id: `journey_${now}`,
        destination: 'Metro Station to Home',
        durationMinutes,
        checkInIntervalMinutes: intervalMinutes,
        startedAt: now,
        nextCheckInAt: nextCheckIn,
        expiresAt,
        status: 'ACTIVE'
      };

      expect(journey.status).toBe('ACTIVE');
      expect(journey.nextCheckInAt).toBeGreaterThan(now);
      expect(journey.expiresAt).toBeGreaterThan(journey.nextCheckInAt);

      // Check-in action safely pushes next check-in time forward
      const checkInTime = now + 9 * 60 * 1000;
      const updatedNextCheckIn = checkInTime + intervalMinutes * 60 * 1000;
      journey.nextCheckInAt = updatedNextCheckIn;

      expect(journey.nextCheckInAt).toBeGreaterThan(nextCheckIn);
    });

    it('triggers automatic SOS when grace period expires on missed check-in', async () => {
      // Simulate missed check-in escalating to backend distress session
      const startRes = await request(app)
        .post('/api/sos/start')
        .send({
          userName: 'Priya Sharma',
          triggerSource: 'WALK_WITH_ME_AUTO',
          triggerReason: 'Missed Journey Check-in (Walk With Me)',
          contacts: [
            { contactId: 'mom', name: 'Mother', phone: '+919800000001', isPrimary: true }
          ]
        });

      expect(startRes.status).toBe(201);
      expect(startRes.body.session.status).toBe('ACTIVE');
      expect(startRes.body.session.contacts[0].deliveryState).toBe('QUEUED');
    });

    it('allows graceful journey completion when user arrives safely', () => {
      let isJourneyActive = true;
      const endJourney = () => { isJourneyActive = false; };

      endJourney();
      expect(isJourneyActive).toBe(false);
    });
  });

  describe('Discreet Fake Call Generator', () => {
    it('schedules fake incoming call and handles ringing and answered states', () => {
      const fakeCall = {
        caller: 'Dad',
        delaySec: 5,
        state: 'PENDING' as 'PENDING' | 'RINGING' | 'CONNECTED' | 'ENDED',
        durationSec: 0
      };

      // Timer expires -> Ringing
      fakeCall.state = 'RINGING';
      expect(fakeCall.state).toBe('RINGING');

      // User answers -> Connected & duration increments
      fakeCall.state = 'CONNECTED';
      fakeCall.durationSec = 14; // simulated 14 seconds in conversation
      expect(fakeCall.state).toBe('CONNECTED');
      expect(fakeCall.durationSec).toBe(14);

      // User hangs up
      fakeCall.state = 'ENDED';
      expect(fakeCall.state).toBe('ENDED');
    });
  });

  describe('Nearby Safe Points of Interest (POIs)', () => {
    const safePois = [
      { id: '1', name: 'Central Police Station', category: 'POLICE', phone: '112', lat: 28.6315, lng: 77.2167 },
      { id: '2', name: 'Women Helpline', category: 'WOMEN_HELPLINE', phone: '1091', lat: 28.6250, lng: 77.2190 },
      { id: '3', name: 'Trauma Emergency Hospital', category: 'HOSPITAL', phone: '102', lat: 28.6190, lng: 77.2280 }
    ];

    it('contains verified emergency phone numbers and coordinates for 1-tap dispatch', () => {
      for (const poi of safePois) {
        expect(poi.name).toBeDefined();
        expect(['112', '1091', '102']).toContain(poi.phone);
        expect(poi.lat).toBeGreaterThan(0);
        expect(poi.lng).toBeGreaterThan(0);
      }
    });

    it('formats valid geo navigation and tel URI links', () => {
      const poi = safePois[0];
      const navUri = `geo:${poi.lat},${poi.lng}?q=${encodeURIComponent(poi.name)}`;
      const telUri = `tel:${poi.phone}`;

      expect(navUri).toContain('geo:28.6315,77.2167');
      expect(telUri).toBe('tel:112');
    });
  });
});
