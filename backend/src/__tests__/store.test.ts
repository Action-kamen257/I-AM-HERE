import { emergencyStore } from '../store';
import { StartSosRequest, LocationPoint } from '../types';

describe('EmergencyStore Unit Tests', () => {
  beforeEach(() => {
    emergencyStore.clearAll();
  });

  describe('User Registration & Retrieval', () => {
    it('should register and retrieve user profile by cleaned phone number', () => {
      const profile = {
        phone: '+91 98765 43210',
        name: 'Tushar Patil',
        bloodGroup: 'O+',
        medicalNotes: 'Asthma inhaler required'
      };

      const registered = emergencyStore.registerUser(profile);
      expect(registered.phone).toBe('+919876543210');
      expect(registered.name).toBe('Tushar Patil');

      const retrieved = emergencyStore.getUserByPhone('+919876543210');
      expect(retrieved).toBeDefined();
      expect(retrieved?.name).toBe('Tushar Patil');
      expect(retrieved?.bloodGroup).toBe('O+');
    });
  });

  describe('Session Lifecycle & Tracking', () => {
    const mockRequest: StartSosRequest = {
      userId: 'usr_001',
      userName: 'Tushar',
      userPhone: '+919876543210',
      cancelPin: '1234',
      duressPin: '9999',
      contacts: [
        { name: 'Mom', phone: '+919999999999', relationship: 'Mother' },
        { name: 'Friend', phone: '+918888888888', relationship: 'Friend' }
      ],
      initialLocation: {
        latitude: 20.9042,
        longitude: 74.7749,
        accuracy: 10,
        timestamp: Date.now(),
        provider: 'gps',
        batteryLevel: 85
      }
    };

    it('should create an active SOS session with generated token and contacts', () => {
      const session = emergencyStore.createSession(mockRequest);

      expect(session.id).toBeDefined();
      expect(session.token).toHaveLength(16);
      expect(session.status).toBe('ACTIVE');
      expect(session.contacts).toHaveLength(2);
      expect(session.contacts[0].deliveryState).toBe('QUEUED');
      expect(session.batteryLevel).toBe(85);

      const byToken = emergencyStore.getSessionByToken(session.token);
      expect(byToken?.id).toBe(session.id);
    });

    it('should update location and battery level', () => {
      const session = emergencyStore.createSession(mockRequest);
      const newLoc: LocationPoint = {
        latitude: 20.9050,
        longitude: 74.7755,
        accuracy: 15,
        timestamp: Date.now() + 5000,
        provider: 'fused',
        batteryLevel: 83
      };

      const updated = emergencyStore.addLocation(session.token, newLoc);
      expect(updated?.locations).toHaveLength(2);
      expect(updated?.lastLocation?.latitude).toBe(20.9050);
      expect(updated?.batteryLevel).toBe(83);
    });

    it('should filter inaccurate locations in batch updates', () => {
      const session = emergencyStore.createSession(mockRequest);
      const batch: LocationPoint[] = [
        { latitude: 20.91, longitude: 74.78, accuracy: 25, timestamp: 100, provider: 'gps' },
        { latitude: 20.92, longitude: 74.79, accuracy: 120, timestamp: 200, provider: 'network' }, // should be filtered (>60m)
        { latitude: 20.93, longitude: 74.80, accuracy: 40, timestamp: 300, provider: 'fused' }
      ];

      const updated = emergencyStore.addLocationsBatch(session.token, batch);
      // initial location (1) + valid batch points (2) = 3 total points
      expect(updated?.locations).toHaveLength(3);
    });
  });

  describe('Contact Response & Status Updates', () => {
    it('should track delivery state transitions', () => {
      const session = emergencyStore.createSession({
        userId: 'usr_002',
        userName: 'Alex',
        userPhone: '+1234567890',
        contacts: [{ name: 'Dad', phone: '+1987654321' }]
      });

      const contactId = session.contacts[0].contactId;

      const { contact: sentContact } = emergencyStore.updateContactStatus(session.token, contactId, 'SENT', 'SMS Gateway Ref 123');
      expect(sentContact?.deliveryState).toBe('SENT');
      expect(sentContact?.sentAt).toBeDefined();

      const { contact: delContact } = emergencyStore.updateContactStatus(session.token, contactId, 'DELIVERED');
      expect(delContact?.deliveryState).toBe('DELIVERED');
      expect(delContact?.deliveredAt).toBeDefined();
    });

    it('should record contact seen and response actions', () => {
      const session = emergencyStore.createSession({
        userId: 'usr_003',
        userName: 'Sam',
        userPhone: '+1122334455',
        contacts: [{ contactId: 'c_01', name: 'Partner', phone: '+1122334466' }]
      });

      emergencyStore.markContactSeen(session.token, 'c_01');
      expect(session.contacts[0].seen).toBe(true);

      emergencyStore.recordContactResponse(session.token, {
        contactId: 'c_01',
        action: 'ON_MY_WAY',
        note: 'On my way with help'
      });

      expect(session.contacts[0].responding).toBe(true);
      expect(session.contacts[0].responseNote).toBe('On my way with help');
    });
  });

  describe('PIN Resolution & Duress Safety', () => {
    it('should resolve session with correct PIN', () => {
      const session = emergencyStore.createSession({
        userId: 'usr_004',
        cancelPin: '4321',
        duressPin: '9999',
        contacts: []
      });

      const invalidAttempt = emergencyStore.stopSession(session.token, '0000');
      expect(invalidAttempt.status).toBe('INVALID_PIN');

      const successAttempt = emergencyStore.stopSession(session.token, '4321');
      expect(successAttempt.status).toBe('SUCCESS');
      expect(session.status).toBe('RESOLVED');
      expect(session.endedAt).toBeDefined();
    });

    it('should handle duress cancellation securely', () => {
      const session = emergencyStore.createSession({
        userId: 'usr_005',
        cancelPin: '1234',
        duressPin: '8888',
        contacts: []
      });

      const duressAttempt = emergencyStore.stopSession(session.token, '8888');
      expect(duressAttempt.status).toBe('DURESS');
      expect(session.status).toBe('DURESS_CANCELLED');
    });
  });
});
