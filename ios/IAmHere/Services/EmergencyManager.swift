import Foundation
import Combine
import AudioToolbox
import UIKit

class EmergencyManager: ObservableObject {
    static let shared = EmergencyManager()

    @Published var userName: String = "Priya Sharma"
    @Published var userPhone: String = "+919876543210"
    @Published var bloodGroup: String = "B+"
    @Published var cancelPin: String = "1234"
    @Published var duressPin: String = "9999"

    @Published var contacts: [Contact] = [
        Contact(id: "c1", name: "Mother", phone: "+919800000001", relationship: "Mother", isPrimary: true),
        Contact(id: "c2", name: "Sister", phone: "+919800000002", relationship: "Sister", isPrimary: true),
        Contact(id: "c3", name: "Friend (Aditi)", phone: "+919800000003", relationship: "Friend", isPrimary: false)
    ]

    @Published var isEmergencyActive: Bool = false
    @Published var countdown: Int? = nil
    @Published var currentSession: SosSession? = nil
    @Published var contactStatuses: [ContactAlertStatus] = []

    var baseUrl: String = "http://localhost:3000/api"
    private var syncTimer: Timer?
    private var countdownTimer: Timer?

    init() {
        LocationManager.shared.onLocationUpdate = { [weak self] point in
            self?.sendLocationUpdate(point)
        }
    }

    func startCountdown() {
        guard !isEmergencyActive else { return }
        var current = 3
        self.countdown = current

        countdownTimer?.invalidate()
        countdownTimer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak self] timer in
            guard let self = self else { return }
            current -= 1
            if current > 0 {
                self.countdown = current
                AudioServicesPlaySystemSound(1057) // Tactile beep
            } else {
                timer.invalidate()
                self.countdown = nil
                self.triggerSosImmediate()
            }
        }
    }

    func cancelCountdown() {
        countdownTimer?.invalidate()
        countdownTimer = nil
        countdown = nil
    }

    func triggerSosImmediate() {
        guard !isEmergencyActive else { return }
        isEmergencyActive = true

        self.contactStatuses = contacts.map {
            ContactAlertStatus(
                contactId: $0.id,
                name: $0.name,
                phone: $0.phone,
                relationship: $0.relationship,
                deliveryState: .queued
            )
        }

        // Start iOS background location tracking
        LocationManager.shared.startEmergencyTracking()

        // Initialize backend session
        startBackendSession()
    }

    private func startBackendSession() {
        guard let url = URL(string: "\(baseUrl)/sos/start") else { return }

        let payload: [String: Any] = [
            "userName": userName,
            "userPhone": userPhone,
            "bloodGroup": bloodGroup,
            "cancelPin": cancelPin,
            "duressPin": duressPin,
            "contacts": contacts.map { [
                "contactId": $0.id,
                "name": $0.name,
                "phone": $0.phone,
                "relationship": $0.relationship
            ] }
        ]

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONSerialization.data(withJSONObject: payload)

        URLSession.shared.dataTask(with: request) { [weak self] data, response, error in
            guard let data = data, let self = self else { return }
            do {
                if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                   let sessionDict = json["session"] as? [String: Any],
                   let token = sessionDict["token"] as? String {

                    DispatchQueue.main.async {
                        self.startStatusSync(token: token)
                    }
                }
            } catch {
                print("[EmergencyManager] Session start parse error: \(error)")
            }
        }.resume()
    }

    private func sendLocationUpdate(_ point: LocationPoint) {
        guard let token = currentSession?.token, let url = URL(string: "\(baseUrl)/sos/\(token)/location") else { return }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.httpBody = try? JSONEncoder().encode(point)

        URLSession.shared.dataTask(with: request).resume()
    }

    private func startStatusSync(token: String) {
        syncTimer?.invalidate()
        syncTimer = Timer.scheduledTimer(withTimeInterval: 3.0, repeats: true) { [weak self] _ in
            guard let self = self, let url = URL(string: "\(self.baseUrl)/sos/\(token)") else { return }

            URLSession.shared.dataTask(with: url) { data, _, _ in
                guard let data = data else { return }
                // Parse contacts status
            }.resume()
        }
    }

    func stopSos(enteredPin: String, completion: @escaping (Bool) -> Void) {
        let isDuress = (enteredPin == duressPin)

        if !isDuress {
            LocationManager.shared.stopEmergencyTracking()
            syncTimer?.invalidate()
            syncTimer = nil
            isEmergencyActive = false
            currentSession = nil
            completion(true)
        } else {
            // Duress PIN entered: visually dismiss, but keep LocationManager tracking silently!
            isEmergencyActive = false
            completion(true)
        }
    }
}
