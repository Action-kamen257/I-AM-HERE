import SwiftUI

@main
struct IAmHereApp: App {
    init() {
        LocationManager.shared.requestPermissions()
    }

    var body: some Scene {
        WindowGroup {
            SosView()
        }
    }
}
