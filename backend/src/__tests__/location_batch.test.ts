import request from 'supertest';
import { app } from '../app';
import { emergencyStore } from '../store';

describe('Phase 06: Configurable Live Location & Offline Batch Ingestion', () => {
  beforeEach(() => {
    emergencyStore.clearAll();
  });

  it('ingests offline buffered batch points in chronological order', async () => {
    const startRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Priya Sharma',
        contacts: [{ name: 'Mother', phone: '+919800000001' }]
      });

    const token = startRes.body.session.token;

    // Simulate batch of 3 buffered locations from an offline elevator/tunnel traversal
    const batch = [
      {
        latitude: 18.5205,
        longitude: 73.8568,
        accuracy: 8.0,
        speed: 1.5,
        timestamp: 1000,
        batteryLevel: 90,
        provider: 'gps'
      },
      {
        latitude: 18.5210,
        longitude: 73.8575,
        accuracy: 12.0,
        speed: 2.0,
        timestamp: 2000,
        batteryLevel: 89,
        provider: 'gps'
      },
      {
        latitude: 18.5218,
        longitude: 73.8582,
        accuracy: 6.0,
        speed: 2.8,
        timestamp: 3000,
        batteryLevel: 89,
        provider: 'gps'
      }
    ];

    const batchRes = await request(app)
      .post(`/api/sos/${token}/locations/batch`)
      .send({ locations: batch });

    expect(batchRes.status).toBe(200);
    expect(batchRes.body.success).toBe(true);
    expect(batchRes.body.count).toBe(3);
    expect(batchRes.body.lastLocation.latitude).toBe(18.5218);
    expect(batchRes.body.batteryLevel).toBe(89);

    // Verify session breadcrumbs history
    const sessionRes = await request(app).get(`/api/sos/${token}`);
    expect(sessionRes.body.session.locations).toHaveLength(3);
    expect(sessionRes.body.session.locations[0].timestamp).toBe(1000);
    expect(sessionRes.body.session.locations[2].timestamp).toBe(3000);
  });

  it('filters out erratic low-accuracy location fixes (>60m)', async () => {
    const startRes = await request(app)
      .post('/api/sos/start')
      .send({
        userName: 'Priya Sharma',
        contacts: [{ name: 'Mother', phone: '+919800000001' }]
      });

    const token = startRes.body.session.token;

    const batch = [
      {
        latitude: 18.5205,
        longitude: 73.8568,
        accuracy: 10.0,
        timestamp: 1000,
        batteryLevel: 90
      },
      {
        // Wild GPS jump / cellular cell-tower bounce with 120m accuracy error
        latitude: 18.5500,
        longitude: 73.8900,
        accuracy: 120.0,
        timestamp: 1500,
        batteryLevel: 90
      },
      {
        latitude: 18.5210,
        longitude: 73.8570,
        accuracy: 8.0,
        timestamp: 2000,
        batteryLevel: 90
      }
    ];

    await request(app)
      .post(`/api/sos/${token}/locations/batch`)
      .send({ locations: batch });

    const sessionRes = await request(app).get(`/api/sos/${token}`);
    // The point with accuracy 120 should be discarded
    expect(sessionRes.body.session.locations).toHaveLength(2);
    expect(sessionRes.body.session.locations.some((l: any) => l.accuracy === 120.0)).toBe(false);
  });
});
