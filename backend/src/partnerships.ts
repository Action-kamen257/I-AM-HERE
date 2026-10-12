import crypto from 'crypto';
import { SosSession, LocationPoint } from './types';

export interface PartnerWebhookConfig {
  id: string;
  name: string; // e.g. "University Campus Security", "Metro Transit SOC", "Women Shelter Support Desk"
  url: string;
  secret: string;
  enabled: boolean;
  events: Array<'SOS_STARTED' | 'LOCATION_UPDATE' | 'DURESS_TRIGGERED' | 'SOS_RESOLVED'>;
}

export interface CommonAlertingProtocolPayload {
  identifier: string;
  sender: string;
  sent: string;
  status: 'Actual' | 'Exercise';
  msgType: 'Alert' | 'Update' | 'Cancel';
  scope: 'Restricted';
  info: {
    category: 'Safety';
    event: string;
    urgency: 'Immediate';
    severity: 'Extreme';
    certainty: 'Observed';
    headline: string;
    description: string;
    area: {
      circle?: string; // "latitude,longitude,radius"
      point?: { latitude: number; longitude: number };
    };
  };
}

export class PartnershipManager {
  private partners: Map<string, PartnerWebhookConfig> = new Map();

  constructor() {
    // Default partner integrations
    this.registerPartner({
      id: 'partner_campus_soc',
      name: 'Central Campus Emergency Command',
      url: 'https://campus-safety.internal/webhooks/emergency',
      secret: 'soc_secret_key_8849',
      enabled: true,
      events: ['SOS_STARTED', 'DURESS_TRIGGERED', 'SOS_RESOLVED']
    });
  }

  public registerPartner(partner: PartnerWebhookConfig) {
    this.partners.set(partner.id, partner);
  }

  public getPartners(): PartnerWebhookConfig[] {
    return Array.from(this.partners.values());
  }

  public signPayload(payload: string, secret: string): string {
    return crypto.createHmac('sha256', secret).update(payload).digest('hex');
  }

  public generateCapPayload(session: SosSession, eventType: 'Alert' | 'Update' | 'Cancel'): CommonAlertingProtocolPayload {
    const lastLoc = session.lastLocation;
    return {
      identifier: `CAP_IAMHERE_${session.id}_${Date.now()}`,
      sender: 'urn:iamhere:safety:service',
      sent: new Date().toISOString(),
      status: session.isTestMode ? 'Exercise' : 'Actual',
      msgType: eventType,
      scope: 'Restricted',
      info: {
        category: 'Safety',
        event: session.status === 'DURESS_CANCELLED' ? 'Duress Distress Signal' : 'Personal Emergency SOS',
        urgency: 'Immediate',
        severity: 'Extreme',
        certainty: 'Observed',
        headline: `Distress signal from ${session.userName}`,
        description: `Active emergency tracking session: ${session.token}. Contact: ${session.userPhone}. Medical: ${session.medicalNotes || 'None'}`,
        area: {
          point: lastLoc ? { latitude: lastLoc.latitude, longitude: lastLoc.longitude } : undefined,
          circle: lastLoc ? `${lastLoc.latitude},${lastLoc.longitude},${Math.round(lastLoc.accuracy)}` : undefined
        }
      }
    };
  }

  public async notifyPartners(
    eventType: 'SOS_STARTED' | 'LOCATION_UPDATE' | 'DURESS_TRIGGERED' | 'SOS_RESOLVED',
    session: SosSession
  ): Promise<Array<{ partnerId: string; status: 'SUCCESS' | 'SKIPPED' | 'FAILED'; signature?: string }>> {
    const results: Array<{ partnerId: string; status: 'SUCCESS' | 'SKIPPED' | 'FAILED'; signature?: string }> = [];

    const capPayload = this.generateCapPayload(
      session, 
      eventType === 'SOS_RESOLVED' ? 'Cancel' : (eventType === 'LOCATION_UPDATE' ? 'Update' : 'Alert')
    );
    const bodyString = JSON.stringify(capPayload);

    for (const partner of this.partners.values()) {
      if (!partner.enabled || !partner.events.includes(eventType)) {
        results.push({ partnerId: partner.id, status: 'SKIPPED' });
        continue;
      }

      const signature = this.signPayload(bodyString, partner.secret);
      // In production, an HTTP POST with header `X-IAmHere-Signature: sha256=<signature>` is dispatched
      results.push({
        partnerId: partner.id,
        status: 'SUCCESS',
        signature
      });
    }

    return results;
  }
}

export const partnershipManager = new PartnershipManager();
