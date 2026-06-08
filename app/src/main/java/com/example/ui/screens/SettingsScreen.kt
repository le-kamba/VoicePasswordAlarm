package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VoiceOverOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AlarmSettings
import com.example.ui.AlarmViewModel
import com.example.ui.ScreenState
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: AlarmViewModel,
    settings: AlarmSettings
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isRegisteringListening by viewModel.isRegisteringListening.collectAsStateWithLifecycle()
    val registeringSpeechText by viewModel.registeringSpeechText.collectAsStateWithLifecycle()
    val registeringSpeechError by viewModel.registeringSpeechError.collectAsStateWithLifecycle()

    // Cancel registration speech when the screen is left
    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopRegistrationSpeechListening()
        }
    }

    // System back behavior for settings screen
    BackHandler(enabled = true) {
        viewModel.navigateTo(ScreenState.MAIN)
    }

    // 登録された合い言葉の編集・登録用ダイアログ制御
    var showEditDialog by remember { mutableStateOf(false) }
    var dialogIndexToEdit by remember { mutableStateOf(0) }
    var dialogTextValue by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("アラーム設定詳細", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(ScreenState.MAIN) },
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "戻る",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // SECTION: ALARM SOUND
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "音符",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "アラーム音選択",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                val soundsList = listOf(
                    "CHIME_SYNTH" to "和風チャイム (心地よい連打)",
                    "BEEP_PULSE" to "デジタル電子音 (連続ダブルアップ)",
                    "ZEN_MEDITATION" to "瞑想風シンセ (深い波の響き)"
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    soundsList.forEach { (type, description) ->
                        val isSelected = settings.alarmSound == type
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                    else MaterialTheme.colorScheme.surface
                                )
                                .clickable { viewModel.updateAlarmSound(type) }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = description,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            RadioButton(
                                selected = isSelected,
                                onClick = { viewModel.updateAlarmSound(type) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                }
            }

            // SECTION: VOLUME CONTROL
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "音量",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "アラーム音量",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "音量割合",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${(settings.volume * 100).roundToInt()}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("volume_percent")
                            )
                        }
                        Slider(
                            value = settings.volume,
                            onValueChange = { viewModel.updateVolume(it) },
                            valueRange = 0.0f..1.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.testTag("volume_slider")
                        )
                    }
                }
            }

            // SECTION: VIBRATION SWITCH
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Vibration,
                        contentDescription = "振動",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "バイブレーション",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "バイブのオン・オフ",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "固定パターンで振動します",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = settings.isVibrationEnabled,
                        onCheckedChange = { viewModel.updateVibrationEnabled(it) },
                        modifier = Modifier.testTag("vibration_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // SECTION: STOP PHRASE & ACCURACY
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VoiceOverOff,
                        contentDescription = "音声停止ワード",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "合い言葉（アラーム停止フレーズ）",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "合い言葉リスト (事前に3つまで登録可能)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val phraseList = listOf(settings.stopPhrase1, settings.stopPhrase2, settings.stopPhrase3)
                        val nonEmptyCount = phraseList.count { it.isNotBlank() }

                        phraseList.forEachIndexed { index, phrase ->
                            if (phrase.isNotBlank()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(
                                            if (!settings.useRandomPhrase && !settings.requireAllPhrasesRandomOrder && settings.selectedPhraseIndex == index)
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (!settings.useRandomPhrase && !settings.requireAllPhrasesRandomOrder && settings.selectedPhraseIndex == index)
                                                MaterialTheme.colorScheme.primary
                                            else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            if (!settings.useRandomPhrase && !settings.requireAllPhrasesRandomOrder) {
                                                viewModel.updateSelectedPhraseIndex(index)
                                            }
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        RadioButton(
                                            selected = !settings.useRandomPhrase && !settings.requireAllPhrasesRandomOrder && settings.selectedPhraseIndex == index,
                                            onClick = { viewModel.updateSelectedPhraseIndex(index) },
                                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = phrase,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(
                                            onClick = {
                                                dialogIndexToEdit = index
                                                dialogTextValue = phrase
                                                showEditDialog = true
                                            },
                                            modifier = Modifier.testTag("phrase_edit_button_$index")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "編集",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deleteStopPhrase(index) },
                                            enabled = nonEmptyCount > 1,
                                            modifier = Modifier.testTag("phrase_delete_button_$index")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "削除",
                                                tint = if (nonEmptyCount > 1) MaterialTheme.colorScheme.error 
                                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                                            )
                                        }
                                    }
                                }
                            } else {
                                OutlinedButton(
                                    onClick = {
                                        dialogIndexToEdit = index
                                        dialogTextValue = ""
                                        showEditDialog = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("phrase_add_button_$index"),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "追加",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("合い言葉を登録する", fontSize = 14.sp)
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                        // Selection usage mode options
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "使用する合い言葉の選択方法:",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Fixed
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.updateUseRandomPhrase(false) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !settings.useRandomPhrase && !settings.requireAllPhrasesRandomOrder,
                                    onClick = { viewModel.updateUseRandomPhrase(false) }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "1つ選択して固定で使用",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "上記リストのチェックが入った合い言葉に固定します",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Random
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.updateUseRandomPhrase(true) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.useRandomPhrase,
                                    onClick = { viewModel.updateUseRandomPhrase(true) }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "登録された中から毎回ランダムで使用",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "アラーム鳴動ごとに登録リストからランダムに選ばれます",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // All Random Sequence
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { viewModel.updateRequireAllPhrasesRandomOrder(true) }
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = settings.requireAllPhrasesRandomOrder,
                                    onClick = { viewModel.updateRequireAllPhrasesRandomOrder(true) }
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "登録したすべての順番をランダムに、すべて一致させる",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "登録されているすべての合い言葉をランダムな順番で、順番にすべて一致させます",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f))

                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "音声認識精度（許容度）",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${(settings.matchAccuracy * 100).roundToInt()}%",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("accuracy_percent")
                                )
                            }
                            Text(
                                text = "パーセンテージを下げると一致判定が緩くなります。高い場合は厳格な発声一致が必要です。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Slider(
                                value = settings.matchAccuracy,
                                onValueChange = { viewModel.updateMatchAccuracy(it) },
                                valueRange = 0.5f..1.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.testTag("accuracy_slider")
                            )
                        }
                    }
                }
            }

            // Edit dialog
            if (showEditDialog) {
                val phraseCurrentList = listOf(settings.stopPhrase1, settings.stopPhrase2, settings.stopPhrase3)
                AlertDialog(
                    onDismissRequest = { 
                        viewModel.stopRegistrationSpeechListening()
                        showEditDialog = false 
                    },
                    title = {
                        Text(
                            text = if (phraseCurrentList[dialogIndexToEdit].isNotBlank()) "合い言葉の編集" else "新しい合い言葉の登録",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // VOICE RECOGNITION (PRIORITY 1)
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isRegisteringListening) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                ),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = "【推奨】音声入力で登録 (優先)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    
                                    // Big Mic Button (Action)
                                    IconButton(
                                        onClick = {
                                            if (isRegisteringListening) {
                                                viewModel.stopRegistrationSpeechListening()
                                            } else {
                                                viewModel.startRegistrationSpeechListening(context) { text ->
                                                    dialogTextValue = text
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .size(72.dp)
                                            .clip(RoundedCornerShape(36.dp))
                                            .background(
                                                if (isRegisteringListening) MaterialTheme.colorScheme.errorContainer
                                                else MaterialTheme.colorScheme.primaryContainer
                                            )
                                            .testTag("dialog_mic_button")
                                    ) {
                                        Icon(
                                            imageVector = if (isRegisteringListening) Icons.Default.Stop else Icons.Default.Mic,
                                            contentDescription = if (isRegisteringListening) "録音を停止" else "音声入力を開始",
                                            tint = if (isRegisteringListening) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                    
                                    // Voice State Message & Real-time Output
                                    if (isRegisteringListening) {
                                        Text(
                                            text = "マイクに向かって話してください...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = registeringSpeechText.ifEmpty { "（聞き取り中...）" },
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.testTag("dialog_realtime_transcript")
                                        )
                                    } else {
                                        Text(
                                            text = "マイクボタンをタップして発音テストができます。\n認識しやすい言葉が自動で入力されます。",
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            textAlign = TextAlign.Center
                                        )
                                        if (registeringSpeechError != null) {
                                            Text(
                                                text = registeringSpeechError ?: "",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // MANUAL ADJUSTMENT / FALLBACK (PRIORITY 2)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "確認・キーボードで微調整:",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = dialogTextValue,
                                    onValueChange = { dialogTextValue = it },
                                    singleLine = true,
                                    placeholder = { Text("例: おはようございます") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("dialog_stop_phrase_input"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                Text(
                                    text = "※ 漢字表記でも裏側で音声照合されますが、より認識をスムーズにするために「ひらがな」での登録をおすすめします。",
                                    fontSize = 10.sp,
                                    lineHeight = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.stopRegistrationSpeechListening()
                                if (dialogTextValue.isNotBlank()) {
                                    viewModel.updateStopPhrase(dialogIndexToEdit, dialogTextValue.trim())
                                    showEditDialog = false
                                }
                            },
                            enabled = dialogTextValue.isNotBlank(),
                            modifier = Modifier.testTag("dialog_confirm_button")
                        ) {
                            Text("保存")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                viewModel.stopRegistrationSpeechListening()
                                showEditDialog = false 
                            }
                        ) {
                            Text("キャンセル")
                        }
                    },
                    shape = RoundedCornerShape(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Done Button
            Button(
                onClick = { viewModel.navigateTo(ScreenState.MAIN) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("save_settings_btn"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "設定を保存して戻る",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
