import Foundation
import CoreLocation
import UIKit

class LocationManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    static let shared = LocationManager()

    private let manager = CLLocationManager()
    @Published var lastLocation: CLLocation?
    var onLocationUpdate: ((LocationPoint) -> Void)?

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = 5 // 5 meters
    }

    func requestPermissions() {
        manager.requestAlwaysAuthorization()
    }

    func startEmergencyTracking() {
        manager.allowsBackgroundLocationUpdates = true
        manager.showsBackgroundLocationIndicator = true
        manager.pausesLocationUpdatesAutomatically = false
        manager.startUpdatingLocation()
    }

    func stopEmergencyTracking() {
        manager.stopUpdatingLocation()
        manager.showsBackgroundLocationIndicator = false
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let location = locations.last else { return }
        
        // Filter out poor accuracy fixes (>60m)
        if location.horizontalAccuracy > 60 || location.horizontalAccuracy < 0 {
            return
        }

        self.lastLocation = location

        UIDevice.current.isBatteryMonitoringEnabled = true
        let batteryPct = Int(UIDevice.current.batteryLevel * 100)

        let point = LocationPoint(
            latitude: location.coordinate.latitude,
            longitude: location.coordinate.longitude,
            accuracy: location.horizontalAccuracy,
            speed: location.speed >= 0 ? location.speed : nil,
            heading: location.course >= 0 ? location.course : nil,
            altitude: location.altitude,
            timestamp: Int64(location.timestamp.timeIntervalSince1970 * 1000),
            batteryLevel: batteryPct >= 0 ? batteryPct : nil,
            provider: "gps"
        )

        onLocationUpdate?(point)
    }

    func locationManager(_ manager: CLLocationManager, didFailWithError error: Error) {
        print("[LocationManager] GPS error: \(error.localizedDescription)")
    }
}
