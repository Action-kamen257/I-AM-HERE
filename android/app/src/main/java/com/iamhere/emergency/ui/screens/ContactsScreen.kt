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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iamhere.emergency.data.model.Contact
import com.iamhere.emergency.data.repository.EmergencyManager
import com.iamhere.emergency.ui.theme.*

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
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TRUSTED CIRCLE",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = TextGlassPrimary
                        )
                        Text(
                            text = "Contacts alerted instantly with live GPS telemetry",
                            fontSize = 12.sp,
                            color = TextGlassSecondary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Add Contact Button
                    GlassButton(
                        onClick = {
                            formName = ""
                            formPhone = ""
                            formRelation = "Mother"
                            formIsPrimary = true
                            formError = null
                            editingContact = null
                            showAddDialog = true
                        },
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        backgroundColor = Color(0xFF222329),
                        borderColor = Color.White,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Contact",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

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
                            Icon(
                                imageVector = Icons.Default.PeopleOutline,
                                contentDescription = null,
                                tint = TextGlassMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Text(
                                text = "No trusted contacts configured",
                                color = TextGlassSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap the '+' button above to add emergency contacts",
                                color = TextGlassMuted,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    // Contact Glass Cards List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 110.dp)
                    ) {
                        items(contacts, key = { it.id }) { contact ->
                            GlassCard(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        editingContact = contact
                                        formName = contact.name
                                        formPhone = contact.phone
                                        formRelation = contact.relationship
                                        formIsPrimary = contact.isPrimary
                                        formError = null
                                        showAddDialog = true
                                    },
                                shape = RoundedCornerShape(20.dp),
                                backgroundColor = Color(0xF2121317)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Monogram Avatar
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF222329))
                                                .border(1.dp, Color(0x30FFFFFF), RoundedCornerShape(14.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = (contact.name.firstOrNull() ?: 'U').uppercase(),
                                                color = Color.White,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = contact.name,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextGlassPrimary,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                GlassPill(
                                                    text = contact.relationship,
                                                    tintColor = Color(0xFFD4D4D8)
                                                )
                                                if (contact.isPrimary) {
                                                    GlassPill(
                                                        text = "PRIMARY",
                                                        tintColor = Color.White
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = contact.phone,
                                                fontSize = 13.sp,
                                                color = TextGlassSecondary,
                                                letterSpacing = 0.4.sp
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { manager.removeContact(contact.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = TextGlassMuted,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

        // Centered Root Add/Edit Contact Modal Dialog
        if (showAddDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xCC000000))
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
                    backgroundColor = Color(0xF8141519)
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
                                color = TextGlassPrimary,
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
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Full-width prominent phonebook importer
                        GlassButton(
                            onClick = { contactPickerLauncher.launch(null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp),
                            shape = RoundedCornerShape(14.dp),
                            backgroundColor = Color(0xFF222329),
                            borderColor = Color(0x35FFFFFF)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Contacts,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Pick from Device Contacts",
                                color = Color.White,
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
                            placeholder = { Text("Contact Full Name", color = TextGlassMuted, fontSize = 13.sp) },
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

                        // Phone Field
                        OutlinedTextField(
                            value = formPhone,
                            onValueChange = {
                                formPhone = it
                                formError = null
                            },
                            placeholder = { Text("Mobile (+91... or 10 digits)", color = TextGlassMuted, fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
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

                        // Relationship quick selector chips
                        Text(
                            text = "RELATIONSHIP",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGlassMuted,
                            letterSpacing = 0.8.sp
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
                                        .background(if (isSelected) Color(0xFF282930) else Color(0x10FFFFFF))
                                        .border(1.dp, if (isSelected) Color.White else Color(0x18FFFFFF), RoundedCornerShape(10.dp))
                                        .clickable { formRelation = rel },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rel,
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
                            relationships.drop(4).forEach { rel ->
                                val isSelected = formRelation == rel
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) Color(0xFF282930) else Color(0x10FFFFFF))
                                        .border(1.dp, if (isSelected) Color.White else Color(0x18FFFFFF), RoundedCornerShape(10.dp))
                                        .clickable { formRelation = rel },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = rel,
                                        color = if (isSelected) Color.White else TextGlassSecondary,
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
                                Text("Primary Emergency Contact", color = TextGlassPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Alerted with priority escalation", color = TextGlassSecondary, fontSize = 11.sp)
                            }
                            Switch(
                                checked = formIsPrimary,
                                onCheckedChange = { formIsPrimary = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = Color(0xFF282930),
                                    uncheckedThumbColor = TextGlassMuted,
                                    uncheckedTrackColor = Color(0x18FFFFFF)
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
                                backgroundColor = Color(0x14FFFFFF),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Cancel", color = TextGlassSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
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
                                backgroundColor = Color(0xFF222329),
                                borderColor = Color.White,
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
