import request from 'supertest';
import { app } from '../app';
import { emergencyStore } from '../store';

describe('Phase 03: First Real SOS Vertical Slice Integration Test', () => {
  beforeEach(() => {
    emergencyStore.clearAll();
  });

  it('executes the full end-to-end SOS vertical slice', async () => {
    // 1. Android Phone Triggers SOS
    const startResponse = await request(app)
      .post('/api/sos/start')
      .send({
        userId: 'user_priya_sharma',
        userName: 'Priya Sharma',
        userPhone: '+919876543210',
        medicalNotes: 'Blood Group: B+, No drug allergies',
        bloodGroup: 'B+',
        cancelPin: '1234',
        duressPin: '9999',
        initialLocation: {
          latitude: 18.5204,
          longitude: 73.8567,
          accuracy: 4.5,
          speed: 1.2,
          batteryLevel: 92,
          provider: 'fused',
        },
        contacts: [
          { contactId: 'c1', name: 'Mother', phone: '+919800000001', relationship: 'Mother' },
          { contactId: 'c2', name: 'Sister', phone: '+919800000002', relationship: 'Sister' },
          { contactId: 'c3', name: 'Friend (Aditi)', phone: '+919800000003', relationship: 'Friend' },
        ],
      });

    expect(startResponse.status).toBe(201);
    const session = startResponse.body.session;
    const token = session.token;
    expect(token).toBeDefined();
    expect(session.status).toBe('ACTIVE');
    expect(session.contacts).toHaveLength(3);

    // Initial state: All contacts are QUEUED, not seen, not responding
    session.contacts.forEach((c: any) => {
      expect(c.deliveryState).toBe('QUEUED');
      expect(c.seen).toBe(false);
      expect(c.responding).toBe(false);
    });

    // 2. Android Phone dispatches SMS and updates Carrier Delivery Evidence
    // Mother: Sent and Delivered
    await request(app)
      .post(`/api/sos/${token}/contact-status`)
      .send({
        contactId: 'c1',
        deliveryState: 'SENT',
        deliveryEvidence: 'CARRIER_SENT_OK',
      });
    const delMom = await request(app)
      .post(`/api/sos/${token}/contact-status`)
      .send({
        contactId: 'c1',
        deliveryState: 'DELIVERED',
        deliveryEvidence: 'PDU_DELIVERY_ACK_RECEIVED',
      });
    expect(delMom.status).toBe(200);

    // Sister: Sent, but delivery receipt unavailable
    await request(app)
      .post(`/api/sos/${token}/contact-status`)
      .send({
        contactId: 'c2',
        deliveryState: 'SENT',
        deliveryEvidence: 'CARRIER_SENT_OK',
      });
    const unavailSis = await request(app)
      .post(`/api/sos/${token}/contact-status`)
      .send({
        contactId: 'c2',
        deliveryState: 'DELIVERY_UNAVAILABLE',
        deliveryEvidence: 'CARRIER_DELIVERY_UNAVAILABLE',
      });
    expect(unavailSis.status).toBe(200);

    // 3. Mother receives SMS and opens Live Tracking URL in browser
    // Browser GET request with ?cid=c1
    const webViewerResponse = await request(app)
      .get(`/api/sos/${token}?cid=c1`);
    expect(webViewerResponse.status).toBe(200);
    const momContactAfterSeen = webViewerResponse.body.session.contacts.find((c: any) => c.contactId === 'c1');
    expect(momContactAfterSeen.seen).toBe(true);
    expect(momContactAfterSeen.seenAt).toBeDefined();

    // 4. Phone streams updated live GPS location as user moves
    const locationUpdate = await request(app)
      .post(`/api/sos/${token}/location`)
      .send({
        latitude: 18.5210,
        longitude: 73.8572,
        accuracy: 3.2,
        speed: 2.1,
        batteryLevel: 91,
        provider: 'gps',
      });
    expect(locationUpdate.status).toBe(200);
    expect(locationUpdate.body.lastLocation.latitude).toBe(18.5210);
    expect(locationUpdate.body.batteryLevel).toBe(91);

    // 5. Mother clicks "I'm on my way" on web live viewer
    const respondResponse = await request(app)
      .post(`/api/sos/${token}/respond`)
      .send({
        contactId: 'c1',
        action: 'ON_MY_WAY',
        note: "I'm leaving now, ETA 4 mins",
      });
    expect(respondResponse.status).toBe(200);
    const momFinal = respondResponse.body.contacts.find((c: any) => c.contactId === 'c1');
    expect(momFinal.responding).toBe(true);
    expect(momFinal.responseNote).toBe("I'm leaving now, ETA 4 mins");

    // 6. Android Phone polls / checks status and displays the exact verified matrix
    const phoneCheck = await request(app).get(`/api/sos/${token}`);
    expect(phoneCheck.status).toBe(200);
    const phoneSession = phoneCheck.body.session;

    const c1 = phoneSession.contacts.find((c: any) => c.contactId === 'c1');
    expect(c1.deliveryState).toBe('DELIVERED');
    expect(c1.seen).toBe(true);
    expect(c1.responding).toBe(true);

    const c2 = phoneSession.contacts.find((c: any) => c.contactId === 'c2');
    expect(c2.deliveryState).toBe('DELIVERY_UNAVAILABLE');
    expect(c2.seen).toBe(false);

    // 7. Safe Resolution via Master PIN
    const stopResponse = await request(app)
      .post(`/api/sos/${token}/stop`)
      .send({ pin: '1234' });
    expect(stopResponse.status).toBe(200);
    expect(stopResponse.body.status).toBe('RESOLVED');
  });
});
