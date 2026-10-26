package com.iamhere.emergency.ui.screens

import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamhere.emergency.data.model.Contact
import com.iamhere.emergency.data.repository.EmergencyManager
import com.iamhere.emergency.ui.theme.*

/**
 * SCREEN 3: CONTACTS SCREEN (Matching Master Reference UI Screen 3)
 */
@Composable
fun ContactsScreen() {
    val context = LocalContext.current
    val manager = remember { EmergencyManager.getInstance() }
    val contacts by manager.contacts.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<Contact?>(null) }

    var formName by remember { mutableStateOf("") }
    var formPhone by remember { mutableStateOf("") }
    var formRelation by remember { mutableStateOf("Family") }
    var formIsPrimary by remember { mutableStateOf(true) }
    var formError by remember { mutableStateOf<String?>(null) }

    val relationships = remember {
        listOf("Mother", "Father", "Sister", "Brother", "Partner", "Friend", "Doctor", "Other")
    }

    // Phonebook Contact Picker
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { contactUri: Uri? ->
        if (contactUri != null) {
            try {
                val cursor = context.contentResolver.query(contactUri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val idIndex = it.getColumnIndex(ContactsContract.Contacts._ID)
                        val name = if (nameIndex >= 0) it.getString(nameIndex) else ""
                        val id = if (idIndex >= 0) it.getString(idIndex) else ""

                        if (name.isNotBlank()) formName = name

                        // Query phone number
                        val phoneCursor = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(id),
                            null
                        )
                        phoneCursor?.use { pCursor ->
                            if (pCursor.moveToFirst()) {
                                val pIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (pIndex >= 0) {
                                    val phone = pCursor.getString(pIndex)
                                    formPhone = phone.replace(Regex("[^0-9+]"), "")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 12.dp)
        ) {
            // Header Row: Reference Title "Contact"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Contact",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimaryLight
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // "+ Add new" Prominent Card (Master Reference UI Screen 3)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        formName = ""
                        formPhone = ""
                        formRelation = "Mother"
                        formIsPrimary = true
                        formError = null
                        editingContact = null
                        showAddDialog = true
                    },
                shape = RoundedCornerShape(20.dp),
                backgroundColor = SurfaceWhite,
                shadowElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(CoralPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Contact",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Add new",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Max 5 contacts (${contacts.size}/5 armed)",
                            fontSize = 12.sp,
                            color = TextSecondaryLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Contacts List
            if (contacts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(SoftBlush),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PeopleOutline,
                                contentDescription = null,
                                tint = CoralPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text(
                            text = "No trusted contacts configured",
                            color = TextPrimaryLight,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tap 'Add new' above to configure emergency contacts",
                            color = TextSecondaryLight,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    items(contacts, key = { it.id }) { contact ->
                        ContactRowCard(
                            contact = contact,
                            onEdit = {
                                editingContact = contact
                                formName = contact.name
                                formPhone = contact.phone
                                formRelation = contact.relationship
                                formIsPrimary = contact.isPrimary
                                formError = null
                                showAddDialog = true
                            },
                            onDelete = {
                                manager.removeContact(contact.id)
                            }
                        )
                    }
                }
            }
        }

        // Add/Edit Contact Modal Dialog
        if (showAddDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showAddDialog = false }
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* prevent click-through */ }
                        ),
                    shape = RoundedCornerShape(26.dp),
                    shadowElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (editingContact != null) "Edit Trusted Contact" else "Add Trusted Contact",
                                color = TextPrimaryLight,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )

                            IconButton(
                                onClick = { showAddDialog = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = TextSecondaryLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Pick from device contacts button
                        GlassButton(
                            onClick = { contactPickerLauncher.launch(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = SoftBlush,
                            borderColor = SoftBlushBorder
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                tint = CoralPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pick from Device Contacts",
                                color = CoralPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // Name Field
                        OutlinedTextField(
                            value = formName,
                            onValueChange = {
                                formName = it
                                formError = null
                            },
                            placeholder = { Text("Contact Full Name", color = TextMutedLight, fontSize = 13.sp) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CoralPrimary,
                                unfocusedBorderColor = BorderSubtleLight,
                                focusedTextColor = TextPrimaryLight,
                                unfocusedTextColor = TextPrimaryLight,
                                focusedContainerColor = SurfaceLightNeutral,
                                unfocusedContainerColor = SurfaceLightNeutral
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Phone Field
                        OutlinedTextField(
                            value = formPhone,
                            onValueChange = {
                                formPhone = it
                                formError = null
                            },
                            placeholder = { Text("Mobile (+91... or 10 digits)", color = TextMutedLight, fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CoralPrimary,
                                unfocusedBorderColor = BorderSubtleLight,
                                focusedTextColor = TextPrimaryLight,
                                unfocusedTextColor = TextPrimaryLight,
                                focusedContainerColor = SurfaceLightNeutral,
                                unfocusedContainerColor = SurfaceLightNeutral
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Relationship selector
                        Text(
                            text = "RELATIONSHIP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondaryLight,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            relationships.take(4).forEach { rel ->
                                val isSelected = formRelation == rel
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CoralPrimary else SurfaceLightNeutral)
                                        .border(1.dp, if (isSelected) CoralPrimary else BorderSubtleLight, RoundedCornerShape(10.dp))
                                        .clickable { formRelation = rel },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rel,
                                        color = if (isSelected) Color.White else TextSecondaryLight,
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
                            relationships.drop(4).forEach { rel ->
                                val isSelected = formRelation == rel
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) CoralPrimary else SurfaceLightNeutral)
                                        .border(1.dp, if (isSelected) CoralPrimary else BorderSubtleLight, RoundedCornerShape(10.dp))
                                        .clickable { formRelation = rel },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rel,
                                        color = if (isSelected) Color.White else TextSecondaryLight,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }

                        // Primary Contact Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Primary Emergency Contact", color = TextPrimaryLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Alerted with priority escalation", color = TextSecondaryLight, fontSize = 11.sp)
                            }
                            Switch(
                                checked = formIsPrimary,
                                onCheckedChange = { formIsPrimary = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = CoralPrimary,
                                    uncheckedThumbColor = TextMutedLight,
                                    uncheckedTrackColor = SurfaceLightNeutral
                                )
                            )
                        }

                        if (formError != null) {
                            Text(
                                text = formError!!,
                                color = EmergencyRed,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassButton(
                                onClick = { showAddDialog = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                backgroundColor = SurfaceLightNeutral,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Cancel", color = TextSecondaryLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }

                            GlassButton(
                                onClick = {
                                    val digits = formPhone.filter { it.isDigit() }
                                    if (formName.isBlank()) {
                                        formError = "Please enter contact name"
                                    } else if (digits.length < 10) {
                                        formError = "Please enter a valid 10-digit mobile number"
                                    } else {
                                        val cleanPhone = if (formPhone.startsWith("+")) formPhone.trim() else "+91${digits.takeLast(10)}"
                                        val existing = editingContact
                                        if (existing != null) {
                                            manager.updateContact(
                                                existing.copy(
                                                    name = formName.trim(),
                                                    phone = cleanPhone,
                                                    relationship = formRelation.trim(),
                                                    isPrimary = formIsPrimary
                                                )
                                            )
                                        } else {
                                            manager.addContact(
                                                Contact(
                                                    id = "c_${System.currentTimeMillis()}",
                                                    name = formName.trim(),
                                                    phone = cleanPhone,
                                                    relationship = formRelation.trim(),
                                                    isPrimary = formIsPrimary
                                                )
                                            )
                                        }
                                        showAddDialog = false
                                        editingContact = null
                                    }
                                },
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(48.dp),
                                backgroundColor = CoralPrimary,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = if (editingContact != null) "Save Changes" else "Add Contact",
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
    }
}

/**
 * Individual Contact Row Card (Master Reference UI Screen 3)
 * Displays:
 * - Circular avatar image/monogram
 * - Contact Name (e.g. "Dad", "Mom", "Kevin")
 * - Status Pill: "Safe" (green) or "Need help" (coral)
 * - Phone icon + Phone number (e.g. 297-682-3661)
 * - Three dots ⋮ options menu
 */
@Composable
fun ContactRowCard(
    contact: Contact,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        backgroundColor = SurfaceWhite,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Circular Avatar Monogram (Stylized with Soft Blush tone)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(SoftBlush)
                        .border(1.5.dp, SoftBlushBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (contact.name.firstOrNull() ?: 'U').uppercase(),
                        color = CoralPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = contact.name,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimaryLight,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Status Badge: "Safe" (Green) or "Need help" (Coral) from Reference Screen 3
                        if (contact.relationship.contains("Friend", ignoreCase = true)) {
                            GlassPill(
                                text = "Need help",
                                tintColor = EmergencyRed,
                                icon = Icons.Default.Warning
                            )
                        } else {
                            GlassPill(
                                text = "Safe",
                                tintColor = StatusSafeGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = TextSecondaryLight,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = contact.phone,
                            fontSize = 13.sp,
                            color = TextSecondaryLight,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }

            // Three dots menu (⋮)
            Box {
                IconButton(
                    onClick = { expandedMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = TextSecondaryLight,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = expandedMenu,
                    onDismissRequest = { expandedMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit Contact") },
                        onClick = {
                            expandedMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = CoralPrimary)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Contact", color = EmergencyRed) },
                        onClick = {
                            expandedMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = EmergencyRed)
                        }
                    )
                }
            }
        }
    }
}
