package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ModelInfo
import com.example.data.model.ModelStatus
import com.example.ui.theme.MintEmerald
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple

@Composable
fun ModelCard(
    model: ModelInfo,
    isLoaded: Boolean,
    onSelect: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedDetails by remember { mutableStateOf(false) }

    val borderColor = if (isLoaded) NeonCyan else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(if (isLoaded) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .testTag("model_card_${model.id}")
    ) {
        // Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = model.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isLoaded) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MintEmerald.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MintEmerald
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = model.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }

            IconButton(
                onClick = { expandedDetails = !expandedDetails },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (expandedDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand GGUF metadata",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Specs Pills Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpecBadge(label = "Params", value = model.parameterCount, color = NeonPurple)
            SpecBadge(label = "Quant", value = model.quantization, color = NeonCyan)
            SpecBadge(label = "Size", value = model.formattedSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
            SpecBadge(label = "Min RAM", value = "${model.recommendedRamMb}MB", color = MintEmerald)
        }

        // Downloading state
        if (model.status == ModelStatus.DOWNLOADING) {
            Spacer(modifier = Modifier.height(12.dp))
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Downloading GGUF weights...",
                        fontSize = 12.sp,
                        color = NeonCyan
                    )
                    Text(
                        text = "${(model.downloadProgress * 100).toInt()}% (${String.format("%.1f", model.downloadSpeedMb)} MB/s)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { model.downloadProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = NeonCyan,
                    trackColor = MaterialTheme.colorScheme.surface
                )
            }
        }

        // Expanded GGUF Details
        AnimatedVisibility(visible = expandedDetails) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(12.dp)
            ) {
                Text(
                    text = "GGUF Header Metadata",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                MetadataRow(label = "Architecture", value = model.metadata?.architecture ?: model.architecture)
                MetadataRow(label = "Format Version", value = model.metadata?.version?.toString() ?: "3")
                MetadataRow(label = "Context Window", value = "${model.metadata?.contextLength ?: model.defaultContextLength} tokens")
                MetadataRow(label = "Tensor Count", value = model.metadata?.tensorCount?.toString() ?: "120")
                MetadataRow(label = "File Path", value = model.localFilePath ?: "Not saved locally")
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Action Buttons Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (model.status == ModelStatus.READY || isLoaded) {
                if (!isLoaded) {
                    Button(
                        onClick = onSelect,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("load_model_button_${model.id}")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load Model", color = Color(0xFF003549), fontWeight = FontWeight.Bold)
                    }
                } else {
                    OutlinedButton(
                        onClick = {},
                        enabled = false,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MintEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Active & Ready", color = MintEmerald)
                    }
                }

                if (model.isCustom || model.localFilePath != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete Model",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            } else if (model.status == ModelStatus.NOT_DOWNLOADED || model.status == ModelStatus.ERROR) {
                Button(
                    onClick = onDownload,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("download_model_button_${model.id}")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download GGUF", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SpecBadge(label: String, value: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "$label: ",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = value,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
