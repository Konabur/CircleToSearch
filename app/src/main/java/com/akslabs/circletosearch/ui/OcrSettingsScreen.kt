package com.akslabs.circletosearch.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.*
import com.akslabs.circletosearch.ocr.TesseractEngine
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("OcrSettings", Context.MODE_PRIVATE)

    // Get selected languages as a comma-separated string, default to "eng"
    val savedLangs = prefs.getString("selected_langs", "eng") ?: "eng"
    val selectedLangsList = savedLangs.split(",").filter { it.isNotBlank() }.map { it.trim() }.toMutableSet()
    
    var selectedLanguages by remember { mutableStateOf(selectedLangsList) }
    var availableModels by remember { mutableStateOf(TesseractEngine.getAvailableModels(context)) }
    var isNoteVisible by remember { mutableStateOf(prefs.getBoolean("ocr_note_dismissed", false).not()) }
    val uriHandler = LocalUriHandler.current

    androidx.activity.compose.BackHandler(onBack = onBack)

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            TesseractEngine.importModel(context, uri) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                if (success) {
                    availableModels = TesseractEngine.getAvailableModels(context)
                }
            }
        }
    }

    // Save selected languages whenever they change
    LaunchedEffect(selectedLanguages) {
        val langsStr = selectedLanguages.sorted().joinToString(",")
        prefs.edit().putString("selected_langs", langsStr).apply()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OCR Language Models", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { filePickerLauncher.launch("*/*") },
                icon = { Icon(Icons.Default.Add, contentDescription = "Import Model") },
                text = { Text("Import Model") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                if (isNoteVisible) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Box(modifier = Modifier.padding(16.dp)) {
                            Column(modifier = Modifier
                                .fillMaxWidth()
                                .padding(end = 32.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Language Models Guide",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                val annotatedString = buildAnnotatedString {
                                    append("This app uses language models to detect text. By default, it uses a fast English model which may miss some text. To use high-accuracy models or detect other languages, request them from the ")
                                    
                                    pushStringAnnotation(tag = "URL", annotation = "https://t.me/AKSLabs")
                                    withStyle(style = SpanStyle(
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        textDecoration = TextDecoration.Underline
                                    )) {
                                        append("developer's Telegram group")
                                    }
                                    pop()
                                    
                                    append(" with /model command, e.g. /model english and import them below. You can select multiple languages at once!")
                                }

                                ClickableText(
                                    text = annotatedString,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    onClick = { offset ->
                                        annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                                            .firstOrNull()?.let { annotation ->
                                                uriHandler.openUri(annotation.item)
                                            }
                                    }
                                )
                            }
                            
                            IconButton(
                                onClick = {
                                    isNoteVisible = false
                                    prefs.edit().putBoolean("ocr_note_dismissed", true).apply()
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Installed Models (Select Multiple)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            items(availableModels) { lang ->
                val isSelected = lang in selectedLanguages
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) 
                            MaterialTheme.colorScheme.primaryContainer 
                        else 
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                ) {
                    ListItem(
                        headlineContent = { Text(lang.uppercase(), fontWeight = FontWeight.Bold) },
                        supportingContent = { Text("$lang.traineddata", style = MaterialTheme.typography.labelSmall) },
                        leadingContent = {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = null,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        trailingContent = {
                            Icon(
                                Icons.Default.Translate, 
                                contentDescription = null,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable {
                            if (isSelected) {
                                // Don't allow deselecting all languages
                                if (selectedLanguages.size > 1) {
                                    selectedLanguages = selectedLanguages.minus(lang).toMutableSet()
                                }
                            } else {
                                selectedLanguages = selectedLanguages.plus(lang).toMutableSet()
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }

            // Show selected languages summary
            if (selectedLanguages.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Selected Languages",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = selectedLanguages.sorted().joinToString(" + ") { it.uppercase() },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "OCR will use all selected languages combined for better accuracy",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
