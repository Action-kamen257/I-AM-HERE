import request from 'supertest';
import { app } from '../app';
import { emergencyStore } from '../store';

describe('Phase 02: Emergency Backend Foundation & Evidence Engine', () => {
  beforeEach(() => {
    emergencyStore.clearAll();
  });

  it('starts an SOS session and creates tracking token', async () => {
    const res = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Aanya Sharma',
        userPhone: '+919876543210',
        medicalNotes: 'Asthmatic, inhaler in bag',
        bloodGroup: 'O+',
        cancelPin: '1234',
        duressPin: '9999',
        initialLocation: {
          latitude: 19.0760,
          longitude: 72.8777,
          accuracy: 5.2,
          batteryLevel: 88,
          timestamp: Date.now(),
          provider: 'fused',
        },
        contacts: [
          { contactId: 'mom', name: 'Mother', phone: '+919111111111', relationship: 'Mother' },
          { contactId: 'dad', name: 'Father', phone: '+919222222222', relationship: 'Father' },
        ],
      });

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.session.token).toBeDefined();
    expect(res.body.session.contacts).toHaveLength(2);
    expect(res.body.session.contacts[0].deliveryState).toBe('QUEUED');
    expect(res.body.session.contacts[0].seen).toBe(false);
    expect(res.body.session.contacts[0].responding).toBe(false);
  });

  it('marks contact SEEN when opening live tracking link with cid', async () => {
    const startRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Aanya Sharma',
        userPhone: '+919876543210',
        contacts: [
          { contactId: 'mom', name: 'Mother', phone: '+919111111111' },
        ],
      });

    const token = startRes.body.session.token;

    // Contact opens link
    const viewRes = await request(app)
      .get(`/api/sos/${token}?cid=mom`);

    expect(viewRes.status).toBe(200);
    const momContact = viewRes.body.session.contacts.find((c: any) => c.contactId === 'mom');
    expect(momContact.seen).toBe(true);
    expect(momContact.seenAt).toBeDefined();
  });

  it('updates contact delivery state based on carrier evidence', async () => {
    const startRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Test User',
        contacts: [
          { contactId: 'friend', name: 'Friend', phone: '+919333333333' },
        ],
      });

    const token = startRes.body.session.token;

    // Simulate SMS sent
    await request(app)
      .post(`/api/sos/${token}/contact-status`)
      .send({
        contactId: 'friend',
        deliveryState: 'SENT',
        deliveryEvidence: 'SMS_SENT_BROADCAST_OK',
      });

    // Simulate SMS delivered
    const delRes = await request(app)
      .post(`/api/sos/${token}/contact-status`)
      .send({
        contactId: 'friend',
        deliveryState: 'DELIVERED',
        deliveryEvidence: 'PDU_DELIVERY_ACK_RECEIVED',
      });

    expect(delRes.status).toBe(200);
    const updated = delRes.body.contacts.find((c: any) => c.contactId === 'friend');
    expect(updated.deliveryState).toBe('DELIVERED');
    expect(updated.deliveryEvidence).toBe('PDU_DELIVERY_ACK_RECEIVED');
    expect(updated.deliveredAt).toBeDefined();
  });

  it('records contact responding action from live map', async () => {
    const startRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Test User',
        contacts: [
          { contactId: 'mom', name: 'Mother', phone: '+919111111111' },
        ],
      });

    const token = startRes.body.session.token;

    const respRes = await request(app)
      .post(`/api/sos/${token}/respond`)
      .send({
        contactId: 'mom',
        action: 'ON_MY_WAY',
        note: 'Leaving home now, 5 mins away',
      });

    expect(respRes.status).toBe(200);
    const mom = respRes.body.contacts.find((c: any) => c.contactId === 'mom');
    expect(mom.responding).toBe(true);
    expect(mom.respondingAt).toBeDefined();
    expect(mom.responseNote).toBe('Leaving home now, 5 mins away');
  });

  it('ingests live location stream and updates breadcrumb history', async () => {
    const startRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Test User',
        contacts: [{ name: 'Contact 1', phone: '123' }],
      });

    const token = startRes.body.session.token;

    // Send location 1
    await request(app)
      .post(`/api/sos/${token}/location`)
      .send({
        latitude: 19.0761,
        longitude: 72.8778,
        accuracy: 4.1,
        speed: 3.5,
        batteryLevel: 85,
      });

    // Send location 2
    const loc2 = await request(app)
      .post(`/api/sos/${token}/location`)
      .send({
        latitude: 19.0763,
        longitude: 72.8780,
        accuracy: 3.8,
        speed: 4.2,
        batteryLevel: 84,
      });

    expect(loc2.status).toBe(200);
    expect(loc2.body.lastLocation.latitude).toBe(19.0763);
    expect(loc2.body.batteryLevel).toBe(84);

    // Verify session contains both points
    const checkRes = await request(app).get(`/api/sos/${token}`);
    expect(checkRes.body.session.locations).toHaveLength(2);
  });

  it('handles regular cancel PIN and secret Duress PIN', async () => {
    const startRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Test User',
        cancelPin: '1234',
        duressPin: '9999',
        contacts: [{ name: 'Contact 1', phone: '123' }],
      });

    const token = startRes.body.session.token;

    // Wrong PIN
    const wrongRes = await request(app)
      .post(`/api/sos/${token}/stop`)
      .send({ pin: '0000' });
    expect(wrongRes.status).toBe(401);

    // Duress PIN entered under threat
    const duressRes = await request(app)
      .post(`/api/sos/${token}/stop`)
      .send({ pin: '9999' });
    expect(duressRes.status).toBe(200);
    expect(duressRes.body.status).toBe('DURESS_CANCELLED');
  });
});
