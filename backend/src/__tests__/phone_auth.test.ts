import request from 'supertest';
import { app } from '../app';
import { emergencyStore } from '../store';

describe('Phone Authentication & Victim Specific Emergency Binding', () => {
  beforeEach(() => {
    emergencyStore.clearAll();
  });

  it('registers a user via /api/auth/phone-login and persists profile', async () => {
    const res = await request(app)
      .post('/api/auth/phone-login')
      .send({
        phoneNumber: '+919988776655',
        name: 'Aanya Verma',
        bloodGroup: 'O+',
        medicalNotes: 'Asthmatic, carries inhaler'
      });

    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.user.phoneNumber).toBe('+919988776655');
    expect(res.body.user.name).toBe('Aanya Verma');
    expect(res.body.user.bloodGroup).toBe('O+');

    const profileRes = await request(app)
      .get('/api/user/profile/+919988776655');
    expect(profileRes.status).toBe(200);
    expect(profileRes.body.user.name).toBe('Aanya Verma');
  });

  it('rejects phone login with invalid phone number format', async () => {
    const res = await request(app)
      .post('/api/auth/phone-login')
      .send({
        phoneNumber: '123',
        name: 'Invalid'
      });

    expect(res.status).toBe(400);
    expect(res.body.error).toContain('Valid 10+ digit phone number is required');
  });

  it('links SOS session specifically to the registered victim phone number', async () => {
    const victimPhone = '+919811223344';

    // Register victim
    await request(app)
      .post('/api/auth/phone-login')
      .send({
        phoneNumber: victimPhone,
        name: 'Kavita Patel'
      });

    // Start SOS session for this victim
    const sosRes = await request(app)
      .post('/api/sos/start')
      .send({
        userId: victimPhone,
        userPhone: victimPhone,
        userName: 'Kavita Patel',
        initialLocation: {
          latitude: 28.6139,
          longitude: 77.2090,
          accuracy: 12.0
        },
        trustedContacts: [
          { name: 'Papa', phone: '+919800000001', isPrimary: true }
        ]
      });

    expect(sosRes.status).toBe(201);
    expect(sosRes.body.token).toBeDefined();

    // Query active SOS by victim phone
    const activeRes = await request(app)
      .get(`/api/victim/${encodeURIComponent(victimPhone)}/active-sos`);

    expect(activeRes.status).toBe(200);
    expect(activeRes.body.hasActiveSos).toBe(true);
    expect(activeRes.body.session.token).toBe(sosRes.body.token);
    expect(activeRes.body.session.userId).toBe(victimPhone);
  });
});
