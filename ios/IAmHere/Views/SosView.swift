import SwiftUI

struct SosView: View {
    @ObservedObject var manager = EmergencyManager.shared
    @State private var isPressing = false
    @State private var showPinSheet = false
    @State private var enteredPin = ""

    var body: some View {
        ZStack {
            Color(red: 10/255, green: 14/255, blue: 23/255)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                // Header
                HStack {
                    VStack(alignment: .leading) {
                        Text("I AM HERE")
                            .font(.system(size: 22, weight: .black))
                            .foregroundColor(.white)
                        Text(manager.isEmergencyActive ? "DISTRESS BROADCAST ACTIVE" : "EMERGENCY READY")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(manager.isEmergencyActive ? Color(red: 255/255, green: 45/255, blue: 85/255) : Color(red: 0/255, green: 230/255, blue: 153/255))
                    }
                    Spacer()
                }
                .padding(.horizontal, 20)

                Spacer()

                // Giant Tactile SOS Button
                if let count = manager.countdown {
                    // Countdown Display
                    VStack(spacing: 12) {
                        Text("\(count)")
                            .font(.system(size: 72, weight: .heavy))
                            .foregroundColor(Color(red: 255/255, green: 45/255, blue: 85/255))
                        Text("Sending SOS to trusted circle...")
                            .font(.system(size: 14))
                            .foregroundColor(.gray)

                        Button(action: { manager.cancelCountdown() }) {
                            Text("CANCEL")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(.white)
                                .frame(width: 140, height: 44)
                                .background(Color.gray.opacity(0.3))
                                .cornerRadius(22)
                        }
                    }
                } else if !manager.isEmergencyActive {
                    // Inactive SOS Trigger Button
                    Button(action: {
                        manager.startCountdown()
                    }) {
                        ZStack {
                            Circle()
                                .fill(Color(red: 255/255, green: 45/255, blue: 85/255).opacity(0.2))
                                .frame(width: 220, height: 220)

                            Circle()
                                .fill(Color(red: 255/255, green: 45/255, blue: 85/255))
                                .frame(width: 180, height: 180)
                                .shadow(color: Color(red: 255/255, green: 45/255, blue: 85/255).opacity(0.5), radius: 20, x: 0, y: 10)

                            VStack(spacing: 6) {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .font(.system(size: 40))
                                    .foregroundColor(.white)
                                Text("SOS")
                                    .font(.system(size: 32, weight: .black))
                                    .foregroundColor(.white)
                            }
                        }
                    }
                    .accessibilityLabel("Activate Emergency SOS")
                } else {
                    // Active Emergency Card
                    VStack(spacing: 16) {
                        Image(systemName: "antenna.radiowaves.left.and.right")
                            .font(.system(size: 48))
                            .foregroundColor(Color(red: 255/255, green: 45/255, blue: 85/255))

                        Text("BROADCASTING LIVE LOCATION")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.white)

                        Button(action: { showPinSheet = true }) {
                            Text("CANCEL DISTRESS (ENTER PIN)")
                                .font(.system(size: 14, weight: .bold))
                                .foregroundColor(.white)
                                .frame(maxWidth: .infinity)
                                .frame(height: 50)
                                .background(Color.red.opacity(0.8))
                                .cornerRadius(12)
                        }
                        .padding(.horizontal, 32)
                    }
                }

                Spacer()

                // Contact Delivery Evidence Section
                VStack(alignment: .leading, spacing: 10) {
                    Text("TRUSTED CONTACT RESPONSE")
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.gray)
                        .padding(.horizontal, 20)

                    ForEach(manager.contactStatuses) { contact in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(contact.name)
                                    .font(.system(size: 14, weight: .bold))
                                    .foregroundColor(.white)
                                Text(contact.relationship)
                                    .font(.system(size: 11))
                                    .foregroundColor(.gray)
                            }
                            Spacer()
                            Text(contact.deliveryState.rawValue)
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(Color(red: 0/255, green: 230/255, blue: 153/255))
                                .padding(.horizontal, 8)
                                .padding(.vertical, 4)
                                .background(Color(red: 0/255, green: 230/255, blue: 153/255).opacity(0.15))
                                .cornerRadius(6)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 10)
                        .background(Color(red: 19/255, green: 26/255, blue: 42/255))
                        .cornerRadius(10)
                        .padding(.horizontal, 20)
                    }
                }
                .padding(.bottom, 24)
            }
        }
        .sheet(isPresented: $showPinSheet) {
            VStack(spacing: 20) {
                Text("Enter Safety PIN")
                    .font(.headline)
                SecureField("4-digit PIN", text: $enteredPin)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                    .keyboardType(.numberPad)
                    .padding(.horizontal, 40)

                Button("Confirm Cancellation") {
                    manager.stopSos(enteredPin: enteredPin) { success in
                        if success {
                            showPinSheet = false
                            enteredPin = ""
                        }
                    }
                }
                .buttonStyle(.borderedProminent)
            }
            .padding()
        }
    }
}
