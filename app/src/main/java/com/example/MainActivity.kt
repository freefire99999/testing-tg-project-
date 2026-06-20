package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.SmsLog
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentWindowInsets = WindowInsets.statusBars
                ) { innerPadding ->
                    SmsForwarderAppScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SmsForwarderAppScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current

    // Observe flow states from ViewModel
    val logs by viewModel.logsState.collectAsStateWithLifecycle()
    val token by viewModel.token.collectAsStateWithLifecycle()
    val chatId by viewModel.chatId.collectAsStateWithLifecycle()
    val isForwardingEnabled by viewModel.isForwardingEnabled.collectAsStateWithLifecycle()
    val testStatus by viewModel.testStatus.collectAsStateWithLifecycle()
    val isTesting by viewModel.isTesting.collectAsStateWithLifecycle()

    // Permission state & launcher
    var hasSmsPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECEIVE_SMS
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermission = permissions[Manifest.permission.RECEIVE_SMS] == true
    }

    // Checking on startup / refresh
    LaunchedEffect(Unit) {
        hasSmsPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECEIVE_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Screen States
    var showExplanationGuide by remember { mutableStateOf(false) }
    var isTokenVisible by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 1. Custom App Title Banner with Telegram Blue accents
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF229ED9)) // Solid elegant Telegram Blue
                .padding(horizontal = 20.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "SMS to Telegram",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "এসএমএস টেলিগ্রাম ফরওয়ার্ড",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal
                )
            }

            // Real-time active status pill showing if rules are enabled
            val activePillColor = if (isForwardingEnabled && hasSmsPermission) Color(0xFF4CAF50) else Color(0xFFF44336)
            val activePillText = if (isForwardingEnabled && hasSmsPermission) "ACTIVE & RUNNING" else "STOPPED"
            
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(activePillColor)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = activePillText,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // LazyColumn container hosting configuration blocks and logs
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            
            // 2. Permission Prompt Card (Show if missing)
            if (!hasSmsPermission) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(2.dp, MaterialTheme.colorScheme.error, RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Sms Permission Exclamation",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "SMS captures are currently offline!",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "This application strictly requires SMS permissions to catch and forward messages instantly. Click below to grant safety access.",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECEIVE_SMS,
                                            Manifest.permission.READ_SMS
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Grant SMS Access (অনুমতি দিন)", color = Color.White)
                            }
                        }
                    }
                }
            }

            // 3. Settings Configuration Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Setup Telegram Bot",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Bot Settings",
                                tint = Color(0xFF229ED9),
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Bot Token Input field
                        OutlinedTextField(
                            value = token,
                            onValueChange = { viewModel.updateToken(it) },
                            label = { Text("Telegram Bot Token") },
                            placeholder = { Text("e.g. 123456:ABC-DEF1234ghIkl-zyx") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Token Lock",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            trailingIcon = {
                                TextButton(
                                    onClick = { isTokenVisible = !isTokenVisible },
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = if (isTokenVisible) "HIDE" else "SHOW",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF229ED9)
                                    )
                                }
                            },
                            visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("bot_token_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Chat ID Input field
                        OutlinedTextField(
                            value = chatId,
                            onValueChange = { viewModel.updateChatId(it) },
                            label = { Text("Telegram Chat ID / Client ID") },
                            placeholder = { Text("e.g. 987654321") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Chat ID Person",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("chat_id_input"),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Forwarding Enable state switcher
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Bridge Status / একটিভ করুন",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isForwardingEnabled) "Forwarding process is listening." else "Forwarding paused.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                            Switch(
                                checked = isForwardingEnabled,
                                onCheckedChange = { viewModel.toggleForwarding(it) },
                                modifier = Modifier.testTag("forwarding_toggle")
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Action Buttons: Send test message
                        Button(
                            onClick = { viewModel.triggerTestMessage() },
                            enabled = !isTesting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("test_conn_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF229ED9),
                                contentColor = Color.White
                            )
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sending Test SMS...", fontSize = 14.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "Send Test Message",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Test Connection", fontSize = 14.sp)
                            }
                        }

                        // Test Delivery Output Logs Banner
                        testStatus?.let { status ->
                            Spacer(modifier = Modifier.height(12.dp))
                            val bannerColor = if (status.startsWith("Success")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                            val bannerTextColor = if (status.startsWith("Success")) Color(0xFF2E7D32) else Color(0xFFC62828)

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bannerColor)
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (status.startsWith("Success")) Icons.Default.CheckCircle else Icons.Default.Info,
                                    contentDescription = "Status Banner Icon",
                                    tint = bannerTextColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = status,
                                    fontSize = 12.sp,
                                    color = bannerTextColor,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { viewModel.clearTestStatus() },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Dismiss Banner",
                                        tint = bannerTextColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. Instructions Setup Guide Helper Accordion
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    )
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showExplanationGuide = !showExplanationGuide }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = "Help Guide Info",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Bot Setup Instructions (সহায়িকা)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = if (showExplanationGuide) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "Dropdown Accordion Trigger"
                            )
                        }

                        AnimatedVisibility(
                            visible = showExplanationGuide,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f))
                                
                                Text(
                                    text = "🇬🇧 ENGLISH:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "1. Search for @BotFather on Telegram.\n" +
                                           "2. Send /newbot, give it a name and username. Copy the API Token.\n" +
                                           "3. Search for @userinfobot or @GetMyChatID_Bot on Telegram.\n" +
                                           "4. Send a message to get your numeric Personal Chat ID.\n" +
                                           "5. Input both into the fields above, tap 'Test Connection' and verify.",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    lineHeight = 18.sp
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "🇧🇩 বাংলা সাহয্য:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "১. টেলিগ্রামে গিয়ে @BotFather লিখে সার্চ করুন।\n" +
                                           "২. /newbot লিখে নতুন একটি বট বানিয়ে API Token টি কপি করুন।\n" +
                                           "৩. আইডি পাওয়ার জন্য টেলিগ্রামে @userinfobot বা @GetMyChatID_Bot এ স্টার্ট দিন।\n" +
                                           "৪. এখন আপনার টোকেন এবং চ্যাট আইডি উপরে বসিয়ে 'Test Connection' এ ক্লিক করে ট্রাই করুন।",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }

            // 5. Statistics Overview Banner
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val totalLogs = logs.size
                    val totalSent = logs.count { it.status == "SENT" }
                    val totalFailed = logs.count { it.status == "FAILED" }

                    // Total Count mini-stat
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL SMS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$totalLogs", fontSize = 18.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    // Forwarded Count mini-stat
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5E9))
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("FORWARDED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32).copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$totalSent", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                    }

                    // Failed Count mini-stat
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFEBEE))
                            .padding(vertical = 12.dp, horizontal = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("FAILED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFC62828).copy(alpha = 0.7f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("$totalFailed", fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color(0xFFC62828))
                    }
                }
            }

            // 6. Logs Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMS Transmission History",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    if (logs.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearLogs() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear logs icon",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear All", fontSize = 12.sp)
                        }
                    }
                }
            }

            // 7. Dynamic list states: Empty state vs Logs display
            if (logs.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Bell indicator",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "No history available",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Once your phone captures incoming SMS alerts, they will appear here.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            } else {
                items(
                    items = logs,
                    key = { it.id }
                ) { log ->
                    SmsLogItemRow(
                        log = log,
                        onDeleteClick = { viewModel.deleteLog(log.id) },
                        onRetryClick = { viewModel.retryForward(log) }
                    )
                }
            }
        }
    }
}

@Composable
fun SmsLogItemRow(
    log: SmsLog,
    onDeleteClick: () -> Unit,
    onRetryClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()) }
    val formattedTime = formatter.format(Date(log.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sms_log_item_${log.id}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Header Row: Sender and Status indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Mobile sender address block
                    Text(
                        text = log.sender,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Custom Status labels with beautiful high-contrast icons
                val statusContainerColor: Color
                val statusContentColor: Color
                val statusText: String
                val statusIcon: @Composable () -> Unit

                when (log.status) {
                    "SENT" -> {
                        statusContainerColor = Color(0xFFE8F5E9)
                        statusContentColor = Color(0xFF2E7D32)
                        statusText = "FORWARDED"
                        statusIcon = { Icon(Icons.Default.Check, contentDescription = "Success tick", tint = statusContentColor, modifier = Modifier.size(12.dp)) }
                    }
                    "FAILED" -> {
                        statusContainerColor = Color(0xFFFFEBEE)
                        statusContentColor = Color(0xFFC62828)
                        statusText = "FAILED"
                        statusIcon = { Icon(Icons.Default.Warning, contentDescription = "Failure warn", tint = statusContentColor, modifier = Modifier.size(12.dp)) }
                    }
                    "DISABLED" -> {
                        statusContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        statusContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                        statusText = "DISABLED"
                        statusIcon = { Icon(Icons.Default.Close, contentDescription = "Off cross", tint = statusContentColor, modifier = Modifier.size(12.dp)) }
                    }
                    else -> { // PENDING
                        statusContainerColor = Color(0xFFFFF3E0)
                        statusContentColor = Color(0xFFE65100)
                        statusText = "PENDING"
                        statusIcon = { Icon(Icons.Default.Refresh, contentDescription = "Sync loop", tint = statusContentColor, modifier = Modifier.size(12.dp)) }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusContainerColor)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        statusIcon()
                        Text(
                            text = statusText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusContentColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body content containing actual details of the SMS message
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(10.dp)
            ) {
                Text(
                    text = log.body,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    fontFamily = FontFamily.SansSerif,
                    lineHeight = 18.sp
                )
            }

            // If Failed, display troubleshooting error message and retry action buttons
            if (log.status == "FAILED" && !log.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFFFEBEE))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Error detail: ${log.errorMessage}",
                        fontSize = 11.sp,
                        color = Color(0xFFC62828),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom action tray: Delete or Retry
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (log.status == "FAILED") {
                    IconButton(
                        onClick = onRetryClick,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Retry manual dispatch",
                            tint = Color(0xFF229ED9),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                }
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Trash individual log",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
