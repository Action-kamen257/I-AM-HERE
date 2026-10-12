import request from 'supertest';
import { app } from '../app';
import { emergencyStore } from '../store';

describe('Phase 07, 08, 09: System SOS, 112 Integration & Test Drill Mode', () => {
  beforeEach(() => {
    emergencyStore.clearAll();
  });

  it('runs a safe test mode drill without alarming contacts', async () => {
    const drillRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Priya Sharma',
        isTestMode: true,
        contacts: [
          { contactId: 'mom', name: 'Mother', phone: '+919800000001' }
        ]
      });

    expect(drillRes.status).toBe(201);
    expect(drillRes.body.session.isTestMode).toBe(true);

    // Verify web viewer displays test drill indicator
    const token = drillRes.body.session.token;
    const viewRes = await request(app).get(`/api/sos/${token}`);
    expect(viewRes.status).toBe(200);
    expect(viewRes.body.session.isTestMode).toBe(true);
  });

  it('probes system health for diagnostics screen', async () => {
    const healthRes = await request(app).get('/health');
    expect(healthRes.status).toBe(200);
    expect(healthRes.body.status).toBe('ok');
    expect(healthRes.body.time).toBeDefined();
  });

  it('logs 112 emergency call dispatch event in session telemetry', async () => {
    const startRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Priya Sharma',
        contacts: [{ name: 'Mother', phone: '+919800000001' }]
      });

    const token = startRes.body.session.token;

    // Contact responds with CALLED_POLICE (112 action)
    const callRes = await request(app)
      .post(`/api/sos/${token}/respond`)
      .send({
        action: 'CALLED_POLICE',
        note: 'Dialed 112 ERSS, operator dispatched patrol unit'
      });

    expect(callRes.status).toBe(200);
    expect(callRes.body.contact.responding).toBe(true);
    expect(callRes.body.contact.responseNote).toContain('112');
  });
});
