package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.Height
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Spellcheck
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.KeyboardPreferences
import com.example.ui.theme.CallistoAmber
import com.example.ui.theme.CallistoRose
import com.example.ui.theme.CallistoSuccess
import com.example.ui.theme.CallistoTeal
import com.example.ui.theme.CallistoViolet
import com.example.ui.theme.GitHubAccent
import com.example.ui.theme.GitHubBorder
import com.example.ui.theme.GitHubKeyBackground
import com.example.ui.theme.GitHubMainBackground
import com.example.ui.theme.GitHubPrimaryText
import com.example.ui.theme.GitHubSecondaryText
import com.example.ui.theme.GitHubSpecialKey
import com.example.ui.theme.GitHubSurface
import com.example.ui.theme.GitHubSurfaceElevated

@Composable
fun SettingsAndPrivacyScreen(
    uiState: CallistoUiState,
    onSpellCheckChanged: (Boolean) -> Unit,
    onAutoCorrectChanged: (Boolean) -> Unit,
    onSentenceSuggestionsChanged: (Boolean) -> Unit,
    onLearningChanged: (Boolean) -> Unit,
    onKeyPreviewChanged: (Boolean) -> Unit,
    onHapticChanged: (Boolean) -> Unit,
    onSoundChanged: (Boolean) -> Unit,
    onPersianDigitsChanged: (Boolean) -> Unit,
    onHeightModeChanged: (String) -> Unit,
    onLongPressDelayChanged: (Int) -> Unit,
    onClearHistory: () -> Unit,
    onClearAll: () -> Unit
) {
    var confirmAction by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pro Header Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF131921), Color(0xFF172233))
                    )
                )
                .border(1.dp, GitHubBorder, RoundedCornerShape(18.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "مرکز کنترل CallistoKey",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = GitHubPrimaryText
                        )
                        Text(
                            text = "تنظیمات حرفه‌ای ارگونومی، هوش تایپ و حریم خصوصی",
                            fontSize = 12.sp,
                            color = GitHubSecondaryText
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CallistoSuccess.copy(alpha = 0.14f))
                            .border(1.dp, CallistoSuccess.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "● آفلاین",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = CallistoSuccess
                        )
                    }
                }

                // Engine telemetry chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EngineBadge(text = "Zero Dead-Zone", color = GitHubAccent)
                    EngineBadge(text = "120Hz Canvas", color = CallistoTeal)
                    EngineBadge(text = "Vazirmatn", color = CallistoViolet)
                }
            }
        }

        // Live Interactive Keyboard Preview Card
        LiveKeyboardPreviewCard(
            heightMode = uiState.keyboardHeightMode,
            showSuggestions = uiState.spellCheckEnabled || uiState.sentenceSuggestionsEnabled
        )

        // SECTION 1: Ergonomics & Touch Precision
        SettingsSectionHeader(title = "ارگونومی، دقت لمس و ظاهر")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GitHubSurface),
            border = BorderStroke(1.dp, GitHubBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                // Segmented Keyboard Height Picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SquircleIcon(icon = Icons.Default.Height, tint = CallistoAmber)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "ارتفاع کیبورد",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GitHubPrimaryText
                            )
                            Text(
                                text = "تنظیم فاصله عمودی کلیدها متناسب با دست شما",
                                fontSize = 11.5.sp,
                                color = GitHubSecondaryText
                            )
                        }
                    }

                    SegmentedOptionRow(
                        options = listOf(
                            KeyboardPreferences.HEIGHT_COMPACT to "فشرده",
                            KeyboardPreferences.HEIGHT_NORMAL to "استاندارد",
                            KeyboardPreferences.HEIGHT_TALL to "بلند"
                        ),
                        selectedKey = uiState.keyboardHeightMode,
                        onSelect = onHeightModeChanged
                    )
                }

                HorizontalDivider(color = GitHubBorder)

                // Segmented Long-Press Speed Picker
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SquircleIcon(icon = Icons.Default.TouchApp, tint = CallistoTeal)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "سرعت نگه‌داشتن کلید (Long-Press)",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = GitHubPrimaryText
                            )
                            Text(
                                text = "مدت زمان نمایش اعداد و علائم فرعی روی کلیدها",
                                fontSize = 11.5.sp,
                                color = GitHubSecondaryText
                            )
                        }
                    }

                    SegmentedOptionRow(
                        options = listOf(
                            240 to "سریع (240ms)",
                            310 to "متعادل (310ms)",
                            420 to "آرام (420ms)"
                        ),
                        selectedKey = uiState.longPressDelayMs,
                        onSelect = onLongPressDelayChanged
                    )
                }

                HorizontalDivider(color = GitHubBorder)

                ProSettingToggleRow(
                    icon = Icons.Default.Visibility,
                    iconTint = GitHubAccent,
                    title = "پیش‌نمایش شناور کلید",
                    subtitle = "نمایش حباب بزرگ حرف لمس‌شده بالای کلید بدون تاخیر",
                    checked = uiState.keyPreviewEnabled,
                    onCheckedChange = onKeyPreviewChanged,
                    testTag = "toggle_key_preview"
                )

                HorizontalDivider(color = GitHubBorder)

                ProSettingToggleRow(
                    icon = Icons.Default.Vibration,
                    iconTint = CallistoViolet,
                    title = "لرزش ملایم (Haptic Feedback)",
                    subtitle = "بازخورد لمسی آنی برای حس تایپ مکانیکی",
                    checked = uiState.hapticEnabled,
                    onCheckedChange = onHapticChanged,
                    testTag = "toggle_haptic"
                )

                HorizontalDivider(color = GitHubBorder)

                ProSettingToggleRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconTint = CallistoTeal,
                    title = "صدای کلیک کلیدها",
                    subtitle = "پخش صدای کوتاه سیستم هنگام تایپ",
                    checked = uiState.soundEnabled,
                    onCheckedChange = onSoundChanged,
                    testTag = "toggle_sound"
                )

                HorizontalDivider(color = GitHubBorder)

                ProSettingToggleRow(
                    icon = Icons.Default.Dialpad,
                    iconTint = CallistoAmber,
                    title = "اعداد پیش‌فرض فارسی (۱۲۳)",
                    subtitle = "استفاده از ارقام فارسی در صفحه‌کلید عددی ۳×۴ تلفن",
                    checked = uiState.usePersianDigits,
                    onCheckedChange = onPersianDigitsChanged,
                    testTag = "toggle_persian_digits"
                )
            }
        }

        // SECTION 2: Intelligence & Predictions
        SettingsSectionHeader(title = "هوش تایپ و اصلاح املا")
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GitHubSurface),
            border = BorderStroke(1.dp, GitHubBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProSettingToggleRow(
                    icon = Icons.Default.Spellcheck,
                    iconTint = GitHubAccent,
                    title = "اصلاح غلط املایی",
                    subtitle = "پیشنهاد اصلاح کلمات اشتباه بر اساس فاصله کلیدها (فارسی و انگلیسی)",
                    checked = uiState.spellCheckEnabled,
                    onCheckedChange = onSpellCheckChanged,
                    testTag = "toggle_spell_check"
                )

                HorizontalDivider(color = GitHubBorder)

                ProSettingToggleRow(
                    icon = Icons.Default.Bolt,
                    iconTint = CallistoSuccess,
                    title = "اصلاح خودکار با دکمه Space",
                    subtitle = "تصحیح خودکار هنگام زدن فاصله + بازگشت فوری با Backspace",
                    checked = uiState.autoCorrectEnabled,
                    onCheckedChange = onAutoCorrectChanged,
                    testTag = "toggle_auto_correct"
                )

                HorizontalDivider(color = GitHubBorder)

                ProSettingToggleRow(
                    icon = Icons.Default.AutoAwesome,
                    iconTint = CallistoViolet,
                    title = "پیشنهاد جملات و کلمه بعدی",
                    subtitle = "نمایش جملات آماده و پیش‌بینی ادامه عبارت در نوار بالا",
                    checked = uiState.sentenceSuggestionsEnabled,
                    onCheckedChange = onSentenceSuggestionsChanged,
                    testTag = "toggle_sentence_suggestions"
                )

                HorizontalDivider(color = GitHubBorder)

                ProSettingToggleRow(
                    icon = Icons.Default.Psychology,
                    iconTint = CallistoTeal,
                    title = "یادگیری کلمات پرتکرار",
                    subtitle = "افزودن خودکار لغات و جملات پرکاربرد شما به دانشنامه محلی",
                    checked = uiState.learningEnabled,
                    onCheckedChange = onLearningChanged,
                    testTag = "toggle_learning"
                )
            }
        }

        // SECTION 3: Privacy Vault & Storage Control
        SettingsSectionHeader(title = "گاوصندوق حریم خصوصی و مدیریت حافظه")
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("storage_management_card"),
            colors = CardDefaults.cardColors(containerColor = GitHubSurface),
            border = BorderStroke(1.dp, GitHubBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SquircleIcon(icon = Icons.Default.Memory, tint = GitHubAccent)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "حافظه دانشنامه محلی",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GitHubPrimaryText
                        )
                        val kb = String.format("%.1f KB", uiState.storageStats.databaseSizeBytes / 1024f)
                        Text(
                            text = "${uiState.storageStats.learnedWordsCount} کلمه • ${uiState.storageStats.learnedPhrasesCount} الگوی جمله • $kb",
                            fontSize = 11.5.sp,
                            color = GitHubSecondaryText
                        )
                    }
                }

                // Visual storage bar
                val totalEntries = uiState.storageStats.learnedWordsCount + uiState.storageStats.learnedPhrasesCount
                val fillProgress = (totalEntries / 500f).coerceIn(0.03f, 1f)
                LinearProgressIndicator(
                    progress = { fillProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GitHubAccent,
                    trackColor = GitHubKeyBackground
                )

                // Security guarantees pills
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(GitHubMainBackground)
                        .border(1.dp, GitHubBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    PrivacyCheckItem("بدون مجوز اینترنت (INTERNET) — پردازش ۱۰۰٪ درون دستگاه")
                    PrivacyCheckItem("توقف خودکار یادگیری و پیشنهادها در فیلدهای رمز عبور")
                    PrivacyCheckItem("امکان پاک‌سازی فوری تاریخچه و دانشنامه در هر لحظه")
                }

                OutlinedButton(
                    onClick = { confirmAction = "history" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("clear_history_button"),
                    border = BorderStroke(1.dp, CallistoAmber.copy(alpha = 0.7f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CallistoAmber),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("پاک‌سازی تاریخچه جملات و الگوهای تایپ", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = { confirmAction = "all" },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("wipe_all_storage_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = CallistoRose.copy(alpha = 0.16f),
                        contentColor = Color(0xFFFF7B72)
                    ),
                    border = BorderStroke(1.dp, CallistoRose.copy(alpha = 0.45f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("حذف کامل تاریخچه تایپ و دانشنامه ذخیره شده", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Privacy & Font Attribution Footer Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("privacy_card"),
            colors = CardDefaults.cardColors(containerColor = GitHubSurface),
            border = BorderStroke(1.dp, GitHubBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = CallistoSuccess,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.font_attribution_title),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GitHubPrimaryText
                    )
                }
                Text(
                    text = stringResource(R.string.font_attribution_body),
                    fontSize = 12.sp,
                    color = GitHubSecondaryText,
                    lineHeight = 18.sp
                )
            }
        }
    }

    if (confirmAction != null) {
        val isAll = confirmAction == "all"
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            containerColor = GitHubSurfaceElevated,
            titleContentColor = GitHubPrimaryText,
            textContentColor = GitHubSecondaryText,
            title = {
                Text(
                    text = if (isAll) "حذف کامل دانشنامه و حافظه؟" else "حذف تاریخچه جملات؟",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = if (isAll) {
                        "تمامی کلمات پرتکرار ذخیره شده، الگوهای جملات و پایگاه داده محلی پاک خواهند شد. این عمل قابل بازگشت نیست."
                    } else {
                        "تاریخچه ارتباط کلمات و پیشنهادهای جمله‌سازی پاک می‌شود، اما کلمات تکی دانشنامه حفظ می‌شوند."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (isAll) onClearAll() else onClearHistory()
                        confirmAction = null
                    }
                ) {
                    Text("تایید و حذف", color = Color(0xFFFF7B72), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) {
                    Text("انصراف", color = GitHubPrimaryText)
                }
            }
        )
    }
}

/**
 * Interactive miniature live keyboard preview showing real-time height, suggestion pills,
 * and EN/FA layout aesthetics inside the app.
 */
@Composable
fun LiveKeyboardPreviewCard(
    heightMode: String,
    showSuggestions: Boolean
) {
    var previewPersian by remember { mutableStateOf(false) }

    val rowHeight by animateDpAsState(
        targetValue = when (heightMode) {
            KeyboardPreferences.HEIGHT_COMPACT -> 24.dp
            KeyboardPreferences.HEIGHT_TALL -> 32.dp
            else -> 28.dp
        },
        animationSpec = spring(),
        label = "previewKeyHeight"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_keyboard_preview"),
        colors = CardDefaults.cardColors(containerColor = GitHubSurface),
        border = BorderStroke(1.dp, GitHubBorder),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "پیش‌نمایش زنده ظاهر کیبورد",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GitHubPrimaryText
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(GitHubKeyBackground)
                        .border(1.dp, GitHubBorder, RoundedCornerShape(8.dp))
                        .clickable { previewPersian = !previewPersian }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (previewPersian) "نمایش: فارسی ⇄" else "نمایش: English ⇄",
                        fontSize = 11.sp,
                        color = GitHubAccent,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Miniature Keyboard Container
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0E1319))
                    .border(1.dp, GitHubBorder, RoundedCornerShape(12.dp))
                    .padding(5.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (showSuggestions) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val pills = if (previewPersian) listOf("سلام", "خوبی؟", "ممنون") else listOf("Hello", "How are you?", "Thanks")
                        pills.forEachIndexed { idx, txt ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (idx == 0) Color(0xFF193458) else Color(0xFF1B222C))
                                    .border(
                                        0.7.dp,
                                        if (idx == 0) GitHubAccent.copy(alpha = 0.6f) else GitHubBorder,
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = txt,
                                    fontSize = 9.5.sp,
                                    color = if (idx == 0) GitHubAccent else GitHubPrimaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                val r1 = if (previewPersian) {
                    listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح", "ج", "چ")
                } else {
                    listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p")
                }
                val r2 = if (previewPersian) {
                    listOf("ش", "س", "ی", "ب", "ل", "ا", "ت", "ن", "م", "ک", "گ")
                } else {
                    listOf("a", "s", "d", "f", "g", "h", "j", "k", "l")
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    r1.forEach { ch ->
                        MiniKeycap(text = ch, modifier = Modifier.weight(1f).height(rowHeight))
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = if (previewPersian) 6.dp else 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    r2.forEach { ch ->
                        MiniKeycap(text = ch, modifier = Modifier.weight(1f).height(rowHeight))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    MiniKeycap(text = if (previewPersian) "۱۲۳" else "123", isSpecial = true, modifier = Modifier.weight(1.2f).height(rowHeight))
                    MiniKeycap(text = if (previewPersian) "؟#؛" else "!#?", isSpecial = true, modifier = Modifier.weight(1.2f).height(rowHeight))
                    MiniKeycap(text = if (previewPersian) "CallistoKey • فارسی" else "CallistoKey • EN", isSpace = true, modifier = Modifier.weight(3.8f).height(rowHeight))
                    MiniKeycap(text = "⏎", isAccent = true, modifier = Modifier.weight(1.4f).height(rowHeight))
                }
            }
        }
    }
}

@Composable
private fun MiniKeycap(
    text: String,
    modifier: Modifier = Modifier,
    isSpecial: Boolean = false,
    isSpace: Boolean = false,
    isAccent: Boolean = false
) {
    val bg = when {
        isAccent -> Color(0xFF193458)
        isSpace -> Color(0xFF1B222C)
        isSpecial -> GitHubSpecialKey
        else -> GitHubKeyBackground
    }
    val border = if (isAccent) GitHubAccent.copy(alpha = 0.6f) else GitHubBorder
    val fg = when {
        isAccent -> GitHubAccent
        isSpecial || isSpace -> GitHubSecondaryText
        else -> GitHubPrimaryText
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(5.dp))
            .background(bg)
            .border(0.7.dp, border, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = if (isSpace) 8.5.sp else 10.sp,
            color = fg,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun <T> SegmentedOptionRow(
    options: List<Pair<T, String>>,
    selectedKey: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GitHubMainBackground)
            .border(1.dp, GitHubBorder, RoundedCornerShape(12.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (key, label) ->
            val selected = key == selectedKey
            val bg by animateColorAsState(
                targetValue = if (selected) Color(0xFF193458) else Color.Transparent,
                label = "segBg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(bg)
                    .then(
                        if (selected) Modifier.border(1.dp, GitHubAccent.copy(alpha = 0.55f), RoundedCornerShape(9.dp))
                        else Modifier
                    )
                    .clickable { onSelect(key) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 11.5.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = if (selected) GitHubAccent else GitHubSecondaryText,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = GitHubAccent,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

@Composable
private fun SquircleIcon(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(tint.copy(alpha = 0.14f))
            .border(1.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun EngineBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(0.8.dp, color.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun PrivacyCheckItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(CallistoSuccess.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = CallistoSuccess,
                modifier = Modifier.size(11.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 11.5.sp,
            color = GitHubPrimaryText
        )
    }
}

@Composable
private fun ProSettingToggleRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SquircleIcon(icon = icon, tint = iconTint)
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = GitHubPrimaryText)
            Text(text = subtitle, fontSize = 11.5.sp, color = GitHubSecondaryText, lineHeight = 16.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(testTag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = GitHubAccent,
                uncheckedThumbColor = GitHubSecondaryText,
                uncheckedTrackColor = GitHubKeyBackground,
                uncheckedBorderColor = GitHubBorder
            )
        )
    }
}
