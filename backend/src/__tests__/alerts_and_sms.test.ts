import request from 'supertest';
import { app } from '../app';
import { emergencyStore } from '../store';

describe('Phase 04 & Phase 05: Strong Alerts & Emergency SMS Gateway', () => {
  beforeEach(() => {
    emergencyStore.clearAll();
  });

  it('dispatches high-priority push alert with siren channel and evidence', async () => {
    const startRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Priya Sharma',
        contacts: [
          { contactId: 'mom', name: 'Mother', phone: '+919800000001' }
        ]
      });

    const token = startRes.body.session.token;

    const pushRes = await request(app)
      .post(`/api/sos/${token}/push-alert`)
      .send({
        contactId: 'mom',
        recipientToken: 'fcm_device_token_abc'
      });

    expect(pushRes.status).toBe(200);
    expect(pushRes.body.success).toBe(true);
    expect(pushRes.body.result.deliveryReceipt).toBeDefined();

    const mom = pushRes.body.contacts.find((c: any) => c.contactId === 'mom');
    expect(mom.deliveryState).toBe('DELIVERED');
    expect(mom.deliveryEvidence).toContain('PUSH_DELIVERY_RECEIPT_VERIFIED');
  });

  it('escalates unacknowledged alerts to secondary circle and louder notifications', async () => {
    const startRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Priya Sharma',
        contacts: [
          { contactId: 'mom', name: 'Mother', phone: '+919800000001' },
          { contactId: 'sis', name: 'Sister', phone: '+919800000002' }
        ]
      });

    const token = startRes.body.session.token;

    // Mom opens link (seen)
    await request(app).get(`/api/sos/${token}?cid=mom`);

    // Escalate triggered after 30s timeout
    const escRes = await request(app)
      .post(`/api/sos/${token}/escalate`);

    expect(escRes.status).toBe(200);
    expect(escRes.body.escalatedCount).toBe(1); // Only sister was pending
    expect(escRes.body.escalatedResults[0].contactId).toBe('sis');
  });

  it('dispatches cloud SMS fallback when device has cellular issues', async () => {
    const startRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Priya Sharma',
        contacts: [
          { contactId: 'c1', name: 'Mother', phone: '+919800000001' }
        ]
      });

    const token = startRes.body.session.token;

    const smsRes = await request(app)
      .post(`/api/sos/${token}/cloud-sms`)
      .send({
        contactId: 'c1'
      });

    expect(smsRes.status).toBe(200);
    expect(smsRes.body.success).toBe(true);
    expect(smsRes.body.result.deliveryEvidence).toContain('CLOUD_SMS_GATEWAY_ACCEPTED');

    const contact = smsRes.body.contacts.find((c: any) => c.contactId === 'c1');
    expect(contact.deliveryState).toBe('SENT');
  });

  it('records accurate failure state for invalid phone numbers without pretending success', async () => {
    const startRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Priya Sharma',
        contacts: [
          { contactId: 'bad_num', name: 'Invalid Contact', phone: '123' }
        ]
      });

    const token = startRes.body.session.token;

    const smsRes = await request(app)
      .post(`/api/sos/${token}/cloud-sms`)
      .send({
        contactId: 'bad_num'
      });

    expect(smsRes.status).toBe(200);
    expect(smsRes.body.success).toBe(false);
    expect(smsRes.body.result.deliveryState).toBe('FAILED');
    expect(smsRes.body.result.deliveryEvidence).toBe('INVALID_PHONE_NUMBER_FORMAT');
  });
});
