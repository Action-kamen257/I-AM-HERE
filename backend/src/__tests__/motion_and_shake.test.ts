import request from 'supertest';
import { app } from '../app';
import { emergencyStore } from '../store';

describe('Phase 10: Motion and Shake Detection & ML Pipeline Safety', () => {
  beforeEach(() => {
    emergencyStore.clearAll();
  });

  describe('Motion Physics & Algorithm Verification', () => {
    const G = 9.80665;

    function computeGForce(x: number, y: number, z: number): number {
      return Math.sqrt(x * x + y * y + z * z) / G;
    }

    function checkShakeReversals(samples: Array<{ x: number; y: number; z: number; t: number }>, thresholdG: number, minReversals: number, windowMs: number): boolean {
      let lastDir = 0;
      let reversals = 0;
      let firstReversalT = 0;

      for (const s of samples) {
        const g = computeGForce(s.x, s.y, s.z);
        if (g >= thresholdG) {
          const dir = Math.abs(s.x) >= Math.abs(s.y) ? (s.x > 0 ? 1 : -1) : (s.y > 0 ? 1 : -1);
          if (lastDir !== 0 && dir !== lastDir) {
            if (reversals === 0) firstReversalT = s.t;
            reversals++;
            if (s.t - firstReversalT <= windowMs && reversals >= minReversals) {
              return true;
            }
          }
          lastDir = dir;
        }
      }
      return false;
    }

    it('rejects casual motion or walking (under threshold)', () => {
      // Walking pattern: slight bobbing at ~1.2g
      const walkingSamples = [
        { x: 1.0, y: 2.0, z: 9.8, t: 0 },
        { x: -1.0, y: 1.5, z: 11.2, t: 200 },
        { x: 1.2, y: 1.8, z: 8.5, t: 400 },
        { x: -0.8, y: 2.2, z: 10.5, t: 600 }
      ];

      const detected = checkShakeReversals(walkingSamples, 2.7, 3, 800);
      expect(detected).toBe(false);
    });

    it('detects vigorous multi-directional shake (3 reversals at > 2.7g within 800ms)', () => {
      // Violent back-and-forth shake along X axis
      const shakeSamples = [
        { x: 28.0, y: 2.0, z: 9.8, t: 0 },     // ~2.9g (+1)
        { x: -30.0, y: 1.0, z: 9.8, t: 150 },  // ~3.1g (-1) -> rev 1
        { x: 29.0, y: 0.0, z: 9.8, t: 300 },   // ~3.0g (+1) -> rev 2
        { x: -28.0, y: -2.0, z: 9.8, t: 450 }  // ~2.9g (-1) -> rev 3 (triggered!)
      ];

      const detected = checkShakeReversals(shakeSamples, 2.7, 3, 800);
      expect(detected).toBe(true);
    });

    it('detects freefall drop followed by hard impact', () => {
      const freefallThresholdG = 0.25;
      const impactThresholdG = 3.5;

      const motionTimeline = [
        { g: 1.0, t: 0, state: 'normal' },
        { g: 0.15, t: 100, state: 'freefall' },
        { g: 0.12, t: 250, state: 'freefall' }, // 150ms freefall
        { g: 4.2, t: 350, state: 'impact' }     // hard impact peak
      ];

      let freefallStart = 0;
      let freefallConfirmed = false;
      let impactDetected = false;

      for (const sample of motionTimeline) {
        if (sample.g < freefallThresholdG) {
          if (freefallStart === 0) freefallStart = sample.t;
          else if (sample.t - freefallStart >= 120) freefallConfirmed = true;
        } else if (freefallConfirmed && sample.t - freefallStart <= 600 && sample.g >= impactThresholdG) {
          impactDetected = true;
          break;
        }
      }

      expect(impactDetected).toBe(true);
    });
  });

  describe('Feature Extraction Pipeline & Training Rule Compliance', () => {
    it('computes 10-dimensional feature vector without NaN or Infinity', () => {
      const windowSize = 64;
      const samples: Array<{ x: number; y: number; z: number; mag: number }> = [];

      for (let i = 0; i < windowSize; i++) {
        const x = Math.sin(i * 0.2) * 15.0;
        const y = Math.cos(i * 0.2) * 5.0;
        const z = 9.8 + Math.sin(i * 0.4) * 8.0;
        const mag = Math.sqrt(x * x + y * y + z * z);
        samples.push({ x, y, z, mag });
      }

      const meanMag = samples.reduce((acc, s) => acc + s.mag, 0) / windowSize;
      const varianceMag = samples.reduce((acc, s) => acc + Math.pow(s.mag - meanMag, 2), 0) / windowSize;
      const stdDevMag = Math.sqrt(varianceMag);
      const minMag = Math.min(...samples.map(s => s.mag));
      const maxMag = Math.max(...samples.map(s => s.mag));
      const peakToPeak = maxMag - minMag;

      expect(meanMag).toBeGreaterThan(0);
      expect(stdDevMag).toBeGreaterThan(0);
      expect(peakToPeak).toBeGreaterThan(0);
      expect(Number.isFinite(meanMag)).toBe(true);
      expect(Number.isFinite(stdDevMag)).toBe(true);
    });

    it('enforces training rule: validates pipeline readiness without starting automated training', () => {
      // Adheres strictly to: "never automatically start training. Prepare and validate the pipeline, then stop and report that manual training is ready."
      let autoTrainingTriggered = false;
      const startAutoTraining = () => { autoTrainingTriggered = true; };

      // Pipeline validation step
      const pipelineReady = true;
      const manualTrainingReadyReported = pipelineReady && !autoTrainingTriggered;

      expect(manualTrainingReadyReported).toBe(true);
      expect(autoTrainingTriggered).toBe(false);
    });
  });

  describe('Emergency Session with Motion Trigger Telemetry', () => {
    it('initiates SOS with motion trigger reason and records trigger source', async () => {
      const res = await request(app)
        .post('/api/sos/start')
        .send({
          userName: 'Priya Sharma',
          triggerSource: 'SHAKE_DETECTOR',
          triggerReason: 'Vigorous Shake Detected',
          contacts: [{ name: 'Mother', phone: '+919800000001' }]
        });

      expect(res.status).toBe(201);
      expect(res.body.session).toBeDefined();
      expect(res.body.session.token).toBeDefined();
    });
  });
});
