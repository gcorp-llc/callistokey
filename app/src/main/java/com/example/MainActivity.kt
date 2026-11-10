package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.GitHubAccent
import com.example.ui.theme.GitHubBorder
import com.example.ui.theme.GitHubDarkTheme
import com.example.ui.theme.GitHubKeyBackground
import com.example.ui.theme.GitHubMainBackground
import com.example.ui.theme.GitHubPrimaryText
import com.example.ui.theme.GitHubSecondaryText
import com.example.ui.theme.GitHubSurface

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GitHubDarkTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = GitHubMainBackground
                ) { innerPadding ->
                    CompanionKeyboardScreen(
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
fun CompanionKeyboardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var isEnabled by remember { mutableStateOf(checkIsKeyboardEnabled(context)) }
    var isSelected by remember { mutableStateOf(checkIsKeyboardSelected(context)) }

    // Recheck status whenever activity resumes
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isEnabled = checkIsKeyboardEnabled(context)
                isSelected = checkIsKeyboardSelected(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var testText by remember { mutableStateOf("") }
    var testPhone by remember { mutableStateOf("") }
    var testPassword by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GitHubSurface)
                    .border(1.dp, GitHubBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Keyboard,
                    contentDescription = "Keyboard Icon",
                    tint = GitHubAccent,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = GitHubPrimaryText
                )
                Text(
                    text = "v1.0 • Persian (فارسی) & English",
                    fontSize = 12.sp,
                    color = GitHubSecondaryText
                )
            }
        }

        // Active Status Card
        StatusBadgeCard(isEnabled = isEnabled, isSelected = isSelected)

        // Setup Actions Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup_card"),
            colors = CardDefaults.cardColors(containerColor = GitHubSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Quick Activation",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GitHubPrimaryText
                )

                // Step 1: Enable
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("enable_keyboard_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEnabled) GitHubKeyBackground else GitHubAccent,
                        contentColor = if (isEnabled) GitHubPrimaryText else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isEnabled) Icons.Default.CheckCircle else Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isEnabled) "1. Keyboard Enabled ✓" else stringResource(R.string.enable_keyboard),
                        fontWeight = FontWeight.Medium
                    )
                }

                // Step 2: Select
                OutlinedButton(
                    onClick = {
                        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                        imm?.showInputMethodPicker()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("select_keyboard_button"),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isSelected) GitHubAccent else GitHubPrimaryText
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isSelected) GitHubAccent else GitHubBorder
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSelected) "2. Keyboard Active ✓" else stringResource(R.string.select_keyboard),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Live Interactive Typing Test Area
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("test_area_card"),
            colors = CardDefaults.cardColors(containerColor = GitHubSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Live Typing Playground",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GitHubPrimaryText
                )
                Text(
                    text = "Tap fields below to test English, Persian, Telephone keypad, and Symbols in real-time.",
                    fontSize = 12.sp,
                    color = GitHubSecondaryText
                )

                // Text field
                OutlinedTextField(
                    value = testText,
                    onValueChange = { testText = it },
                    placeholder = { Text(stringResource(R.string.test_field_hint), color = GitHubSecondaryText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_text_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GitHubAccent,
                        unfocusedBorderColor = GitHubBorder,
                        focusedTextColor = GitHubPrimaryText,
                        unfocusedTextColor = GitHubPrimaryText,
                        focusedContainerColor = GitHubKeyBackground,
                        unfocusedContainerColor = GitHubKeyBackground
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Numeric / Telephone test field
                OutlinedTextField(
                    value = testPhone,
                    onValueChange = { testPhone = it },
                    placeholder = { Text("Test 3x4 telephone keypad (Phone/Numbers)…", color = GitHubSecondaryText) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_phone_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GitHubAccent,
                        unfocusedBorderColor = GitHubBorder,
                        focusedTextColor = GitHubPrimaryText,
                        unfocusedTextColor = GitHubPrimaryText,
                        focusedContainerColor = GitHubKeyBackground,
                        unfocusedContainerColor = GitHubKeyBackground
                    ),
                    shape = RoundedCornerShape(8.dp)
                )

                // Password test field
                OutlinedTextField(
                    value = testPassword,
                    onValueChange = { testPassword = it },
                    placeholder = { Text(stringResource(R.string.test_password_hint), color = GitHubSecondaryText) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_password_field"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GitHubAccent,
                        unfocusedBorderColor = GitHubBorder,
                        focusedTextColor = GitHubPrimaryText,
                        unfocusedTextColor = GitHubPrimaryText,
                        focusedContainerColor = GitHubKeyBackground,
                        unfocusedContainerColor = GitHubKeyBackground
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Privacy Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("privacy_card"),
            colors = CardDefaults.cardColors(containerColor = GitHubSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Privacy Shield",
                    tint = GitHubAccent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.privacy_title),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GitHubPrimaryText
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.privacy_body),
                        fontSize = 12.sp,
                        color = GitHubSecondaryText,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Font Attribution Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("attribution_card"),
            colors = CardDefaults.cardColors(containerColor = GitHubSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, GitHubBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.font_attribution_title),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GitHubPrimaryText
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.font_attribution_body),
                    fontSize = 12.sp,
                    color = GitHubSecondaryText,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun StatusBadgeCard(isEnabled: Boolean, isSelected: Boolean) {
    val (statusText, badgeColor) = when {
        isSelected -> "● Active Default Keyboard" to Color(0xFF3FB950) // GitHub Green
        isEnabled -> "● Enabled • Needs Selection" to GitHubAccent
        else -> "○ Needs System Activation" to GitHubSecondaryText
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(GitHubSurface)
            .border(1.dp, GitHubBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .testTag("status_badge_box")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = GitHubPrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun checkIsKeyboardEnabled(context: Context): Boolean {
    return try {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager ?: return false
        val enabledList = imm.enabledInputMethodList
        enabledList.any { it.packageName == context.packageName }
    } catch (_: Exception) {
        false
    }
}

private fun checkIsKeyboardSelected(context: Context): Boolean {
    return try {
        val currentIme = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ) ?: return false
        currentIme.contains(context.packageName)
    } catch (_: Exception) {
        false
    }
}
