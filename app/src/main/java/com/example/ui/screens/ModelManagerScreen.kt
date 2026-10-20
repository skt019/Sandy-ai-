package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ModelStatus
import com.example.ui.components.ModelCard
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.viewmodel.AppScreen
import com.example.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val models by viewModel.modelRepository.models.collectAsState()
    val activeModel by viewModel.modelRepository.activeModel.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            var fileName = "imported_model.gguf"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
            viewModel.importCustomGgufFile(uri, fileName)
            Toast.makeText(context, "Importing & parsing $fileName", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        viewModel.navigateTo(AppScreen.CHAT)
    }

    val installedModels = models.filter { it.status == ModelStatus.READY || it.status == ModelStatus.LOADED }
    val catalogModels = models

    val totalStorageBytes = installedModels.sumOf { it.sizeBytes }
    val totalStorageMb = totalStorageBytes / (1024.0 * 1024.0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GGUF Model Hub", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(AppScreen.CHAT) },
                        modifier = Modifier.testTag("back_to_chat_from_models")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Storage & Active Model Summary Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MintEmerald)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Active Model:",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activeModel?.name ?: "None selected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${installedModels.size} models installed • ${String.format("%.1f MB", totalStorageMb)} used",
                            fontSize = 12.sp,
                            color = NeonCyan
                        )
                    }

                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("import_custom_gguf_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Import .GGUF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Installed (${installedModels.size})", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("installed_models_tab")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Hugging Face Catalog", fontWeight = FontWeight.SemiBold) },
                    modifier = Modifier.testTag("catalog_models_tab")
                )
            }

            // Model List
            val displayList = if (selectedTab == 0) installedModels else catalogModels

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
            ) {
                if (displayList.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No installed models yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Download a model from the catalog or import any .gguf file from your storage.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                items(displayList, key = { it.id }) { model ->
                    ModelCard(
                        model = model,
                        isLoaded = activeModel?.id == model.id,
                        onSelect = {
                            viewModel.modelRepository.selectModel(model.id)
                            Toast.makeText(context, "Loaded ${model.name}", Toast.LENGTH_SHORT).show()
                        },
                        onDownload = {
                            viewModel.downloadModel(model.id)
                        },
                        onDelete = {
                            viewModel.modelRepository.deleteLocalModel(model.id)
                            Toast.makeText(context, "Deleted ${model.name}", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}
