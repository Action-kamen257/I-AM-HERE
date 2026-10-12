package com.iamhere.emergency.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamhere.emergency.R
import com.iamhere.emergency.data.repository.EmergencyManager
import com.iamhere.emergency.ui.theme.*

@Composable
fun PhoneLoginScreen(
    onLoginSuccess: () -> Unit
) {
    val manager = remember { EmergencyManager.getInstance() }

    var fullName by remember { mutableStateOf(manager.userName.ifBlank { "Priya Sharma" }) }
    var phoneNumber by remember { mutableStateOf("") }
    var bloodGroup by remember { mutableStateOf(manager.bloodGroup.ifBlank { "B+" }) }
    var medicalNotes by remember { mutableStateOf(manager.medicalNotes) }
    var cancelPin by remember { mutableStateOf(manager.cancelPin.ifBlank { "1234" }) }

    // Step state: 0 = Profile & Phone Form, 1 = OTP Verification Code
    var currentStep by remember { mutableIntStateOf(0) }
    var enteredOtp by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    val bloodGroups = remember { listOf("A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-") }

    GlassBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(10.dp))

                // Brand Emblem
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.app_logo),
                        contentDescription = "App Logo",
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0x35FFFFFF), RoundedCornerShape(16.dp))
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "I AM HERE",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = TextGlassPrimary
                        )
                        Text(
                            text = "EMERGENCY IDENTITY SETUP",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp,
                            color = EmergencyRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                AnimatedContent(
                    targetState = currentStep,
                    label = "loginStepAnimation"
                ) { step ->
                    if (step == 0) {
                        // Step 0: Phone Number & Victim Profile Setup
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            backgroundColor = Color(0xF2121317)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                GlassPill(
                                    text = "VICTIM PHONE AUTHENTICATION",
                                    tintColor = Color.White,
                                    icon = Icons.Default.Phone
                                )

                                Text(
                                    text = "Initialize Your Protection",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGlassPrimary
                                )

                                Text(
                                    text = "All emergency SOS broadcasts, multi-part SMS dispatches, and live GPS streaming will operate specifically under this victim identity.",
                                    fontSize = 12.sp,
                                    color = TextGlassSecondary,
                                    lineHeight = 17.sp
                                )

                                HorizontalDivider(color = Color(0x18FFFFFF))

                                // Full Name
                                Text(
                                    text = "LEGAL FULL NAME",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGlassMuted,
                                    letterSpacing = 0.8.sp
                                )
                                OutlinedTextField(
                                    value = fullName,
                                    onValueChange = {
                                        fullName = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("e.g. Priya Sharma", color = TextGlassMuted, fontSize = 13.sp) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = TextGlassPrimary,
                                        unfocusedTextColor = TextGlassPrimary,
                                        focusedContainerColor = Color(0x18FFFFFF),
                                        unfocusedContainerColor = Color(0x0CFFFFFF)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Mobile Number
                                Text(
                                    text = "VICTIM PHONE NUMBER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmergencyRed,
                                    letterSpacing = 0.8.sp
                                )
                                OutlinedTextField(
                                    value = phoneNumber,
                                    onValueChange = {
                                        phoneNumber = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("e.g. 9876543210 or +919876543210", color = TextGlassMuted, fontSize = 13.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = EmergencyRed,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = TextGlassPrimary,
                                        unfocusedTextColor = TextGlassPrimary,
                                        focusedContainerColor = Color(0x18FFFFFF),
                                        unfocusedContainerColor = Color(0x0CFFFFFF)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Blood Group Quick Pills
                                Text(
                                    text = "BLOOD GROUP",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGlassMuted,
                                    letterSpacing = 0.8.sp
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    bloodGroups.take(4).forEach { bg ->
                                        val isSelected = bloodGroup == bg
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) Color(0xFF282930) else Color(0x10FFFFFF))
                                                .border(1.dp, if (isSelected) Color.White else Color(0x18FFFFFF), RoundedCornerShape(10.dp))
                                                .clickable { bloodGroup = bg }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = bg,
                                                color = if (isSelected) Color.White else TextGlassSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    bloodGroups.drop(4).forEach { bg ->
                                        val isSelected = bloodGroup == bg
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (isSelected) Color(0xFF282930) else Color(0x10FFFFFF))
                                                .border(1.dp, if (isSelected) Color.White else Color(0x18FFFFFF), RoundedCornerShape(10.dp))
                                                .clickable { bloodGroup = bg }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = bg,
                                                color = if (isSelected) Color.White else TextGlassSecondary,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            )
                                        }
                                    }
                                }

                                // 4-Digit Cancel PIN
                                Text(
                                    text = "SAFETY DEACTIVATION PIN (4 DIGITS)",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGlassMuted,
                                    letterSpacing = 0.8.sp
                                )
                                OutlinedTextField(
                                    value = cancelPin,
                                    onValueChange = {
                                        if (it.length <= 4) cancelPin = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("1234", color = TextGlassMuted, fontSize = 13.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = TextGlassPrimary,
                                        unfocusedTextColor = TextGlassPrimary,
                                        focusedContainerColor = Color(0x18FFFFFF),
                                        unfocusedContainerColor = Color(0x0CFFFFFF)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                if (errorMessage != null) {
                                    Text(
                                        text = errorMessage!!,
                                        color = EmergencyRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Proceed Button
                                GlassButton(
                                    onClick = {
                                        val digitsOnly = phoneNumber.filter { it.isDigit() }
                                        if (fullName.isBlank()) {
                                            errorMessage = "Please enter your name"
                                        } else if (digitsOnly.length < 10) {
                                            errorMessage = "Please enter a valid 10-digit mobile number"
                                        } else if (cancelPin.length != 4) {
                                            errorMessage = "Please enter a 4-digit safety cancel PIN"
                                        } else {
                                            errorMessage = null
                                            currentStep = 1
                                            enteredOtp = "1234" // Pre-fill mock OTP for smooth verification
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    backgroundColor = Color(0xFF222329),
                                    borderColor = Color.White,
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = "Send Verification Code",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.4.sp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Step 1: OTP Confirmation
                        GlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(26.dp),
                            backgroundColor = Color(0xF2121317)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                GlassPill(
                                    text = "VERIFY PHONE NUMBER",
                                    tintColor = Color.White,
                                    icon = Icons.Default.CheckCircle
                                )

                                Text(
                                    text = "Confirm Victim Phone",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGlassPrimary
                                )

                                Text(
                                    text = "We sent a 4-digit verification code to $phoneNumber to activate emergency telemetry.",
                                    fontSize = 13.sp,
                                    color = TextGlassSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 18.sp
                                )

                                OutlinedTextField(
                                    value = enteredOtp,
                                    onValueChange = {
                                        if (it.length <= 4) enteredOtp = it
                                        errorMessage = null
                                    },
                                    placeholder = { Text("4-digit OTP", color = TextGlassMuted, textAlign = TextAlign.Center) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(16.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = Color.White,
                                        unfocusedBorderColor = GlassBorder,
                                        focusedTextColor = TextGlassPrimary,
                                        unfocusedTextColor = TextGlassPrimary,
                                        focusedContainerColor = Color(0x18FFFFFF),
                                        unfocusedContainerColor = Color(0x0CFFFFFF)
                                    ),
                                    modifier = Modifier.width(180.dp)
                                )

                                if (errorMessage != null) {
                                    Text(
                                        text = errorMessage!!,
                                        color = EmergencyRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    GlassButton(
                                        onClick = { currentStep = 0 },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp),
                                        backgroundColor = Color(0x14FFFFFF),
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text("Back", color = TextGlassSecondary, fontSize = 13.sp)
                                    }

                                    GlassButton(
                                        onClick = {
                                            if (enteredOtp.length < 4) {
                                                errorMessage = "Please enter the 4-digit code"
                                            } else {
                                                isSubmitting = true
                                                manager.login(
                                                    phone = phoneNumber,
                                                    name = fullName,
                                                    blood = bloodGroup,
                                                    notes = medicalNotes,
                                                    pin = cancelPin
                                                ) {
                                                    isSubmitting = false
                                                    onLoginSuccess()
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1.4f)
                                            .height(50.dp),
                                        backgroundColor = Color(0xFF222329),
                                        borderColor = Color.White,
                                        shape = RoundedCornerShape(16.dp)
                                    ) {
                                        Text(
                                            text = if (isSubmitting) "Activating..." else "Verify & Activate",
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
