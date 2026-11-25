package com.example

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.KeyboardPreferences
import com.example.ui.CallistoUiState
import com.example.ui.CallistoViewModel
import com.example.ui.LiveKeyboardPreviewCard
import com.example.ui.SettingsAndPrivacyScreen
import com.example.ui.theme.CallistoSuccess
import com.example.ui.theme.CallistoTeal
import com.example.ui.theme.CallistoViolet
import com.example.ui.theme.GitHubAccent
import com.example.ui.theme.GitHubBorder
import com.example.ui.theme.GitHubDarkTheme
import com.example.ui.theme.GitHubKeyBackground
import com.example.ui.theme.GitHubMainBackground
import com.example.ui.theme.GitHubPrimaryText
import com.example.ui.theme.GitHubSecondaryText
import com.example.ui.theme.GitHubSurface
import com.example.ui.theme.GitHubSurfaceElevated

class MainActivity : ComponentActivity() {

    private var requestedTab by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestedTab = intent?.getIntExtra("open_tab", 0) ?: 0
        enableEdgeToEdge()
        setContent {
            GitHubDarkTheme {
                CallistoCompanionApp(initialTab = requestedTab)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val tab = intent.getIntExtra("open_tab", -1)
        if (tab in 0..2) {
            requestedTab = tab
        }
    }
}

@Composable
fun CallistoCompanionApp(
    initialTab: Int = 0,
    vm: CallistoViewModel = viewModel()
) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    var showAddWordDialog by remember { mutableStateOf(false) }

    LaunchedEffect(initialTab) {
        if (initialTab in 0..2) {
            selectedTab = initialTab
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refreshAll()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(uiState.statusBannerMessage) {
        val msg = uiState.statusBannerMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            vm.dismissBanner()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = GitHubMainBackground,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = { showAddWordDialog = true },
                    containerColor = GitHubAccent,
                    contentColor = Color.Black,
                    modifier = Modifier.testTag("add_word_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Custom Word")
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = GitHubSurface,
                contentColor = GitHubPrimaryText,
                windowInsets = WindowInsets.navigationBars
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            if (selectedTab == 0) Icons.Filled.Keyboard else Icons.Outlined.Keyboard,
                            contentDescription = "Setup"
                        )
                    },
                    label = { Text("استودیو", fontSize = 11.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GitHubAccent,
                        selectedTextColor = GitHubAccent,
                        indicatorColor = Color(0xFF193458),
                        unselectedIconColor = GitHubSecondaryText,
                        unselectedTextColor = GitHubSecondaryText
                    ),
                    modifier = Modifier.testTag("nav_setup")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        vm.loadDictionaryData()
                    },
                    icon = {
                        Icon(
                            if (selectedTab == 1) Icons.Filled.Book else Icons.Outlined.Book,
                            contentDescription = "Dictionary"
                        )
                    },
                    label = { Text("دانشنامه", fontSize = 11.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GitHubAccent,
                        selectedTextColor = GitHubAccent,
                        indicatorColor = Color(0xFF193458),
                        unselectedIconColor = GitHubSecondaryText,
                        unselectedTextColor = GitHubSecondaryText
                    ),
                    modifier = Modifier.testTag("nav_dictionary")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        vm.loadDictionaryData()
                    },
                    icon = {
                        Icon(
                            if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings"
                        )
                    },
                    label = { Text("تنظیمات", fontSize = 11.sp, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GitHubAccent,
                        selectedTextColor = GitHubAccent,
                        indicatorColor = Color(0xFF193458),
                        unselectedIconColor = GitHubSecondaryText,
                        unselectedTextColor = GitHubSecondaryText
                    ),
                    modifier = Modifier.testTag("nav_settings")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Box(modifier = Modifier.widthIn(max = 640.dp).fillMaxSize()) {
                when (selectedTab) {
                    0 -> SetupAndPlaygroundScreen(uiState = uiState)
                    1 -> DictionaryScreen(
                        uiState = uiState,
                        onSearchChanged = vm::updateSearchQuery,
                        onFilterChanged = vm::updateLanguageFilter,
                        onDeleteWord = vm::deleteWord,
                        onDeletePhrase = vm::deletePhrase
                    )
                    2 -> SettingsAndPrivacyScreen(
                        uiState = uiState,
                        onSpellCheckChanged = vm::setSpellCheck,
                        onAutoCorrectChanged = vm::setAutoCorrect,
                        onSentenceSuggestionsChanged = vm::setSentenceSuggestions,
                        onLearningChanged = vm::setLearningEnabled,
                        onKeyPreviewChanged = vm::setKeyPreview,
                        onHapticChanged = vm::setHaptic,
                        onSoundChanged = vm::setSound,
                        onPersianDigitsChanged = vm::setUsePersianDigits,
                        onHeightModeChanged = vm::setKeyboardHeightMode,
                        onLongPressDelayChanged = vm::setLongPressDelayMs,
                        onClearHistory = vm::clearTypingHistoryOnly,
                        onClearAll = vm::clearAllLearnedData
                    )
                }
            }
        }
    }

    if (showAddWordDialog) {
        AddCustomWordDialog(
            onDismiss = { showAddWordDialog = false },
            onAdd = { word, lang ->
                vm.addCustomWord(word, lang)
                showAddWordDialog = false
            }
        )
    }
}

@Composable
fun SetupAndPlaygroundScreen(uiState: CallistoUiState) {
    val context = LocalContext.current
    var testText by remember { mutableStateOf("") }
    var testPhone by remember { mutableStateOf("") }
    var testPassword by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Hero Card
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0xFF131921), Color(0xFF172436))
                    )
                )
                .border(1.dp, GitHubBorder, RoundedCornerShape(18.dp))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFF193458))
                        .border(1.dp, GitHubAccent.copy(alpha = 0.55f), RoundedCornerShape(15.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = "CallistoKey Logo",
                        tint = GitHubAccent,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.app_name),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = GitHubPrimaryText
                    )
                    Text(
                        text = "کیبورد دقیق، سریع و مدرن فارسی و انگلیسی",
                        fontSize = 12.5.sp,
                        color = GitHubSecondaryText
                    )
                }
            }
        }

        // Live Activation Status Pill
        StatusBadgeCard(isEnabled = uiState.isEnabled, isSelected = uiState.isSelected)

        // Activation Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("setup_card"),
            colors = CardDefaults.cardColors(containerColor = GitHubSurface),
            border = BorderStroke(1.dp, GitHubBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "راه‌اندازی دو مرحله‌ای",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GitHubPrimaryText
                )

                Button(
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("enable_keyboard_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isEnabled) GitHubKeyBackground else GitHubAccent,
                        contentColor = if (uiState.isEnabled) GitHubPrimaryText else Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isEnabled) Icons.Default.CheckCircle else Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isEnabled) "۱. کیبورد در سیستم فعال است ✓" else stringResource(R.string.enable_keyboard),
                        fontWeight = FontWeight.Medium
                    )
                }

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
                        contentColor = if (uiState.isSelected) GitHubAccent else GitHubPrimaryText
                    ),
                    border = BorderStroke(1.dp, if (uiState.isSelected) GitHubAccent else GitHubBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = if (uiState.isSelected) Icons.Default.CheckCircle else Icons.Default.Keyboard,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (uiState.isSelected) "۲. CallistoKey انتخاب شده است ✓" else stringResource(R.string.select_keyboard),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Live Keyboard Preview
        LiveKeyboardPreviewCard(
            heightMode = uiState.keyboardHeightMode,
            showSuggestions = uiState.spellCheckEnabled || uiState.sentenceSuggestionsEnabled
        )

        // Live Interactive Typing Playground
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("test_area_card"),
            colors = CardDefaults.cardColors(containerColor = GitHubSurface),
            border = BorderStroke(1.dp, GitHubBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "آزمایشگاه تایپ زنده",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GitHubPrimaryText
                    )
                    if (testText.isNotEmpty() || testPhone.isNotEmpty() || testPassword.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                testText = ""
                                testPhone = ""
                                testPassword = ""
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, tint = GitHubSecondaryText, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("پاک کردن", fontSize = 11.5.sp, color = GitHubSecondaryText)
                        }
                    }
                }
                OutlinedTextField(
                    value = testText,
                    onValueChange = { testText = it },
                    placeholder = { Text(stringResource(R.string.test_field_hint), color = GitHubSecondaryText, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_text_field"),
                    colors = callistoFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = testPhone,
                    onValueChange = { testPhone = it },
                    placeholder = { Text(stringResource(R.string.test_phone_hint), color = GitHubSecondaryText, fontSize = 13.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_phone_field"),
                    colors = callistoFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
                OutlinedTextField(
                    value = testPassword,
                    onValueChange = { testPassword = it },
                    placeholder = { Text(stringResource(R.string.test_password_hint), color = GitHubSecondaryText, fontSize = 13.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("test_password_field"),
                    colors = callistoFieldColors(),
                    shape = RoundedCornerShape(10.dp)
                )
            }
        }

        // Highlights
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GitHubSurface),
            border = BorderStroke(1.dp, GitHubBorder),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "امکانات کلیدی CallistoKey",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = GitHubPrimaryText
                )
                FeatureGuideRow(
                    icon = Icons.Default.Swipe,
                    title = "تغییر زبان با سوایپ Space",
                    subtitle = "انگشت خود را روی دکمه فاصله به چپ یا راست بکشید تا زبان فوراً تغییر کند."
                )
                FeatureGuideRow(
                    icon = Icons.Default.AutoAwesome,
                    title = "دقت تاچ صفر-نقطه-کور + اصلاح املا",
                    subtitle = "تمام سطح کیبورد بدون فضای خالی مرده لمس را ثبت می‌کند و غلط املایی را اصلاح می‌نماید."
                )
            }
        }
    }
}

@Composable
fun DictionaryScreen(
    uiState: CallistoUiState,
    onSearchChanged: (String) -> Unit,
    onFilterChanged: (String) -> Unit,
    onDeleteWord: (String, String) -> Unit,
    onDeletePhrase: (String, String, String) -> Unit
) {
    var subTab by rememberSaveable { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "دانشنامه کلمات پرتکرار",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = GitHubPrimaryText
        )

        // Bento Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BentoStatCard(
                label = "کلمات آموخته",
                value = "${uiState.storageStats.learnedWordsCount}",
                accent = GitHubAccent,
                modifier = Modifier.weight(1f)
            )
            BentoStatCard(
                label = "الگوهای جمله",
                value = "${uiState.storageStats.learnedPhrasesCount}",
                accent = CallistoViolet,
                modifier = Modifier.weight(1f)
            )
            val kb = String.format("%.1f KB", uiState.storageStats.databaseSizeBytes / 1024f)
            BentoStatCard(
                label = "حجم حافظه",
                value = kb,
                accent = CallistoTeal,
                modifier = Modifier.weight(1f)
            )
        }

        // Sub-tabs: Words vs Phrases
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                label = { Text("کلمات پرتکرار (${uiState.learnedWords.size})") },
                colors = callistoChipColors(),
                modifier = Modifier.testTag("dict_tab_words")
            )
            FilterChip(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                label = { Text("ترکیب‌های جمله (${uiState.learnedPhrases.size})") },
                colors = callistoChipColors(),
                modifier = Modifier.testTag("dict_tab_phrases")
            )
        }

        if (subTab == 0) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchChanged,
                placeholder = { Text("جستجو در کلمات ذخیره شده…", color = GitHubSecondaryText, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GitHubSecondaryText) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dict_search_field"),
                colors = callistoFieldColors(),
                shape = RoundedCornerShape(10.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "همه", KeyboardPreferences.LANG_FA to "فارسی", KeyboardPreferences.LANG_EN to "English").forEach { (code, title) ->
                FilterChip(
                    selected = uiState.languageFilter == code,
                    onClick = { onFilterChanged(code) },
                    label = { Text(title, fontSize = 12.sp) },
                    colors = callistoChipColors()
                )
            }
        }

        if (subTab == 0) {
            if (uiState.learnedWords.isEmpty()) {
                EmptyDictionaryState("هنوز کلمه‌ای در دانشنامه شخصی شما ذخیره نشده است.\nبا تایپ روزمره یا زدن دکمه + کلمات پرتکرار به صورت خودکار یاد گرفته می‌شوند.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.learnedWords, key = { "${it.lang}_${it.word}" }) { entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GitHubSurface)
                                .border(1.dp, GitHubBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.word,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GitHubPrimaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${if (entry.lang == KeyboardPreferences.LANG_FA) "فارسی" else "English"} • تکرار: ${entry.frequency}",
                                    fontSize = 11.sp,
                                    color = GitHubSecondaryText
                                )
                            }
                            IconButton(
                                onClick = { onDeleteWord(entry.word, entry.lang) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete word",
                                    tint = GitHubSecondaryText
                                )
                            }
                        }
                    }
                }
            }
        } else {
            if (uiState.learnedPhrases.isEmpty()) {
                EmptyDictionaryState("هنوز الگوی جمله‌سازی ذخیره نشده است.\nپس از تایپ عبارات متوالی، کیبورد جملات پیشنهادی را یاد می‌گیرد.")
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(uiState.learnedPhrases, key = { "${it.lang}_${it.prefix}_${it.nextWord}" }) { phrase ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(GitHubSurface)
                                .border(1.dp, GitHubBorder, RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${phrase.prefix}  →  ${phrase.nextWord}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = GitHubPrimaryText
                                )
                                Text(
                                    text = "${if (phrase.lang == KeyboardPreferences.LANG_FA) "فارسی" else "English"} • تکرار: ${phrase.frequency}",
                                    fontSize = 11.sp,
                                    color = GitHubSecondaryText
                                )
                            }
                            IconButton(
                                onClick = { onDeletePhrase(phrase.prefix, phrase.nextWord, phrase.lang) },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete phrase",
                                    tint = GitHubSecondaryText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BentoStatCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(GitHubSurface)
            .border(1.dp, GitHubBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Text(text = value, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = accent)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = label, fontSize = 11.sp, color = GitHubSecondaryText, maxLines = 1)
    }
}

@Composable
fun AddCustomWordDialog(
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit
) {
    var wordText by remember { mutableStateOf("") }
    var selectedLang by remember { mutableStateOf(KeyboardPreferences.LANG_FA) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = GitHubSurfaceElevated,
        titleContentColor = GitHubPrimaryText,
        title = { Text("افزودن کلمه به دانشنامه", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = wordText,
                    onValueChange = { wordText = it },
                    placeholder = { Text("کلمه مورد نظر را بنویسید…", color = GitHubSecondaryText) },
                    singleLine = true,
                    colors = callistoFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedLang == KeyboardPreferences.LANG_FA,
                        onClick = { selectedLang = KeyboardPreferences.LANG_FA },
                        label = { Text("فارسی") },
                        colors = callistoChipColors()
                    )
                    FilterChip(
                        selected = selectedLang == KeyboardPreferences.LANG_EN,
                        onClick = { selectedLang = KeyboardPreferences.LANG_EN },
                        label = { Text("English") },
                        colors = callistoChipColors()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (wordText.isNotBlank()) onAdd(wordText, selectedLang) }
            ) {
                Text("ذخیره", color = GitHubAccent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("لغو", color = GitHubSecondaryText)
            }
        }
    )
}

@Composable
fun StatusBadgeCard(isEnabled: Boolean, isSelected: Boolean) {
    val (statusText, badgeColor) = when {
        isSelected -> "CallistoKey فعال و پیش‌فرض سیستم است" to CallistoSuccess
        isEnabled -> "کیبورد فعال شد • مرحله ۲ (انتخاب) را انجام دهید" to GitHubAccent
        else -> "کیبورد هنوز در تنظیمات اندروید فعال نشده است" to GitHubSecondaryText
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GitHubSurface)
            .border(1.dp, GitHubBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("status_badge_box")
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = statusText,
                color = GitHubPrimaryText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun FeatureGuideRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GitHubAccent,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(18.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = GitHubPrimaryText)
            Text(text = subtitle, fontSize = 12.sp, color = GitHubSecondaryText, lineHeight = 17.sp)
        }
    }
}

@Composable
private fun EmptyDictionaryState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(GitHubSurface)
            .border(1.dp, GitHubBorder, RoundedCornerShape(14.dp))
            .padding(28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Outlined.Book,
                contentDescription = null,
                tint = GitHubSecondaryText,
                modifier = Modifier.size(36.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = GitHubSecondaryText,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun callistoFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = GitHubAccent,
    unfocusedBorderColor = GitHubBorder,
    focusedTextColor = GitHubPrimaryText,
    unfocusedTextColor = GitHubPrimaryText,
    focusedContainerColor = GitHubKeyBackground,
    unfocusedContainerColor = GitHubKeyBackground
)

@Composable
private fun callistoChipColors() = FilterChipDefaults.filterChipColors(
    selectedContainerColor = Color(0xFF193458),
    selectedLabelColor = GitHubAccent,
    containerColor = GitHubSurface,
    labelColor = GitHubSecondaryText
)
