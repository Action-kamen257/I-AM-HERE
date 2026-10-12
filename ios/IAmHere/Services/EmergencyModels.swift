import Foundation
import CoreLocation

enum DeliveryState: String, Codable {
    case queued = "QUEUED"
    case sent = "SENT"
    case delivered = "DELIVERED"
    case deliveryUnavailable = "DELIVERY_UNAVAILABLE"
    case failed = "FAILED"
}

struct Contact: Identifiable, Codable {
    let id: String
    var name: String
    var phone: String
    var relationship: String
    var isPrimary: Bool
}

struct ContactAlertStatus: Identifiable, Codable {
    var id: String { contactId }
    let contactId: String
    let name: String
    let phone: String
    let relationship: String
    var deliveryState: DeliveryState
    var deliveryEvidence: String?
    var sentAt: Date?
    var deliveredAt: Date?
    var seen: Bool = false
    var seenAt: Date?
    var responding: Bool = false
    var respondingAt: Date?
    var responseNote: String?
}

struct LocationPoint: Codable {
    let latitude: Double
    let longitude: Double
    let accuracy: Double
    let speed: Double?
    let heading: Double?
    let altitude: Double?
    let timestamp: Int64
    let batteryLevel: Int?
    let provider: String
}

struct SosSession: Codable {
    let id: String
    let token: String
    let status: String
    let startedAt: Int64
    var endedAt: Int64?
    var lastLocation: LocationPoint?
    var contacts: [ContactAlertStatus]
}
