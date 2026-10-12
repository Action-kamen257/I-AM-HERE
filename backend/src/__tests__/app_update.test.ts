import request from 'supertest';
import { app } from '../app';

describe('App Auto-Update & Version Discovery API', () => {
  it('returns valid app version metadata from /api/app/version', async () => {
    const res = await request(app).get('/api/app/version');
    expect(res.status).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.versionCode).toBeGreaterThanOrEqual(1);
    expect(typeof res.body.versionName).toBe('string');
    expect(res.body.apkUrl).toBe('/api/app/latest.apk');
    expect(typeof res.body.releaseNotes).toBe('string');
  });

  it('handles /api/app/latest.apk request properly', async () => {
    const res = await request(app).get('/api/app/latest.apk');
    // If the APK exists on disk, it should return 200 with content-type application/vnd.android.package-archive
    // Otherwise 404 with error message
    if (res.status === 200) {
      expect(res.headers['content-type']).toContain('application/vnd.android.package-archive');
      expect(res.headers['content-disposition']).toContain('iamhere-latest.apk');
    } else {
      expect(res.status).toBe(404);
      expect(res.body.error).toBeDefined();
    }
  });
});
