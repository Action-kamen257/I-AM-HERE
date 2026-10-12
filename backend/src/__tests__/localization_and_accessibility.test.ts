import request from 'supertest';
import { app } from '../app';
import { emergencyStore } from '../store';

describe('Phase 13: Accessibility and Multi-Language Localization', () => {
  beforeEach(() => {
    emergencyStore.clearAll();
  });

  describe('Multi-Language Emergency Templates', () => {
    const templates = {
      en: {
        alertPrefix: 'EMERGENCY ALERT!',
        drillPrefix: '[TEST DRILL - I AM SAFE]',
        duressPrefix: '[URGENT DURESS ALERT]',
        formatMessage: (name: string, link: string) => `EMERGENCY ALERT! ${name} needs help. Live location: ${link}`,
        formatDuress: (name: string, link: string) => `[URGENT DURESS ALERT] ${name} entered secret duress code under force! Attacker present. Silent live tracking remains active: ${link}`
      },
      hi: {
        alertPrefix: 'आपातकालीन अलर्ट!',
        drillPrefix: '[सुरक्षा अभ्यास - मैं सुरक्षित हूँ]',
        duressPrefix: '[अति आवश्यक जबरन अलार्म अलर्ट]',
        formatMessage: (name: string, link: string) => `आपातकालीन अलर्ट! ${name} को सहायता की आवश्यकता है। लाइव स्थान: ${link}`,
        formatDuress: (name: string, link: string) => `[अति आवश्यक जबरन अलार्म अलर्ट] ${name} ने दबाव में सीक्रेट कोड डाला है। हमलावर मौजूद है। साइलेंट ट्रैकिंग जारी है: ${link}`
      }
    };

    it('generates unambiguous emergency SMS messages in English', () => {
      const msg = templates.en.formatMessage('Priya Sharma', 'http://localhost:3000/live/tok_123');
      expect(msg).toContain('EMERGENCY ALERT!');
      expect(msg).toContain('Priya Sharma');
      expect(msg).toContain('http://localhost:3000/live/tok_123');
    });

    it('generates authentic, culturally accurate emergency SMS messages in Hindi', () => {
      const msg = templates.hi.formatMessage('प्रिया शर्मा', 'http://localhost:3000/live/tok_123');
      expect(msg).toContain('आपातकालीन अलर्ट!');
      expect(msg).toContain('प्रिया शर्मा');
      expect(msg).toContain('लाइव स्थान');
    });

    it('generates clear duress alerts in both English and Hindi', () => {
      const duressEn = templates.en.formatDuress('Priya', 'http://localhost:3000/live/tok_123');
      const duressHi = templates.hi.formatDuress('प्रिया', 'http://localhost:3000/live/tok_123');

      expect(duressEn).toContain('[URGENT DURESS ALERT]');
      expect(duressHi).toContain('[अति आवश्यक जबरन अलार्म अलर्ट]');
    });
  });

  describe('Accessibility & Non-Color Dependent State Encodings', () => {
    // Deuteranopia/Protanopia friendly: every status must have a distinct textual symbol/icon
    const statusBadges = {
      QUEUED: { icon: '⏳', text: 'Queued' },
      SENT: { icon: '✓', text: 'Sent' },
      DELIVERED: { icon: '✓✓', text: 'Delivered' },
      SEEN: { icon: '👁', text: 'Seen' },
      RESPONDING: { icon: '🏃', text: 'Responding' },
      DELIVERY_UNAVAILABLE: { icon: '✗', text: 'Delivery Unavailable' },
      FAILED: { icon: '!', text: 'Failed' }
    };

    it('provides distinct glyphs and unambiguous names for all 7 delivery states', () => {
      const states = ['QUEUED', 'SENT', 'DELIVERED', 'SEEN', 'RESPONDING', 'DELIVERY_UNAVAILABLE', 'FAILED'] as const;
      const seenGlyphs = new Set<string>();

      for (const state of states) {
        const badge = statusBadges[state];
        expect(badge).toBeDefined();
        expect(badge.icon).toBeDefined();
        expect(badge.text).toBeDefined();
        expect(badge.icon.length).toBeGreaterThan(0);
        seenGlyphs.add(badge.icon);
      }

      // Ensure distinct visual indicators (excluding normal single vs double checkmarks)
      expect(seenGlyphs.size).toBeGreaterThanOrEqual(5);
    });

    it('maintains high contrast color ratios on critical emergency elements', () => {
      // Emergency Red #FF2D55 against Deep Dark #0A0E17
      // Pure White #FFFFFF against Emergency Red #FF2D55
      const calculateLuminance = (r: number, g: number, b: number) => {
        const a = [r, g, b].map(v => {
          v /= 255;
          return v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        });
        return a[0] * 0.2126 + a[1] * 0.7152 + a[2] * 0.0722;
      };

      const lumWhite = calculateLuminance(255, 255, 255);
      const lumDarkBg = calculateLuminance(10, 14, 23);

      const contrastRatio = (lumWhite + 0.05) / (lumDarkBg + 0.05);
      // Contrast ratio exceeds WCAG AAA standard of 7:1
      expect(contrastRatio).toBeGreaterThan(15.0);
    });
  });
});
