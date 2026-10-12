export type DeliveryState = 
  | 'QUEUED' 
  | 'SENT' 
  | 'DELIVERED' 
  | 'DELIVERY_UNAVAILABLE' 
  | 'FAILED';

export interface LocationPoint {
  latitude: number;
  longitude: number;
  accuracy: number;
  speed?: number;
  heading?: number;
  altitude?: number;
  timestamp: number;
  batteryLevel?: number;
  provider: 'gps' | 'network' | 'fused' | 'simulated';
}

export interface ContactAlertStatus {
  contactId: string;
  name: string;
  phone: string;
  relationship: string;
  deliveryState: DeliveryState;
  deliveryEvidence?: string;
  sentAt?: number;
  deliveredAt?: number;
  seen: boolean;
  seenAt?: number;
  responding: boolean;
  respondingAt?: number;
  responseNote?: string;
}

export type SosStatus = 'ACTIVE' | 'RESOLVED' | 'CANCELLED' | 'DURESS_CANCELLED';

export interface SosSession {
  id: string;
  token: string;
  userId: string;
  userName: string;
  userPhone: string;
  medicalNotes?: string;
  bloodGroup?: string;
  status: SosStatus;
  startedAt: number;
  endedAt?: number;
  lastLocation?: LocationPoint;
  locations: LocationPoint[];
  contacts: ContactAlertStatus[];
  batteryLevel?: number;
  isTestMode: boolean;
  cancelPinHash?: string;
  duressPinHash?: string;
}

export interface StartSosRequest {
  userId?: string;
  userName?: string;
  userPhone?: string;
  medicalNotes?: string;
  bloodGroup?: string;
  initialLocation?: LocationPoint;
  contacts: Array<{
    contactId?: string;
    name: string;
    phone: string;
    relationship?: string;
  }>;
  isTestMode?: boolean;
  cancelPin?: string;
  duressPin?: string;
}

export interface LocationUpdateRequest {
  latitude: number;
  longitude: number;
  accuracy: number;
  speed?: number;
  heading?: number;
  altitude?: number;
  timestamp?: number;
  batteryLevel?: number;
  provider?: 'gps' | 'network' | 'fused' | 'simulated';
}

export interface ContactStatusUpdateRequest {
  contactId: string;
  deliveryState: DeliveryState;
  deliveryEvidence?: string;
}

export interface ContactResponseRequest {
  contactId?: string;
  contactName?: string;
  action: 'ON_MY_WAY' | 'CALLED_POLICE' | 'SAFE_CONFIRMED' | 'CUSTOM';
  note?: string;
}

export interface StopSosRequest {
  pin?: string;
  reason?: string;
}
