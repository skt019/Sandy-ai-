package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.NeonCyan

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val context = LocalContext.current
    val sections = parseMarkdownSections(text)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (section in sections) {
            when (section) {
                is Section.TextSection -> {
                    val annotated = buildAnnotatedText(section.text, color)
                    Text(
                        text = annotated,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
                is Section.CodeBlock -> {
                    CodeBlockView(
                        language = section.language,
                        code = section.code,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("code", section.code)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Code copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CodeBlockView(
    language: String,
    code: String,
    onCopy: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0D1117))
            .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp))
    ) {
        // Code Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifBlank { "code" }.uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ElectricViolet
            )
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Code Body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(12.dp)
        ) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFFE6EDF3)
            )
        }
    }
}

private sealed class Section {
    data class TextSection(val text: String) : Section()
    data class CodeBlock(val language: String, val code: String) : Section()
}

private fun parseMarkdownSections(markdown: String): List<Section> {
    val sections = mutableListOf<Section>()
    val codeBlockRegex = Regex("```([a-zA-Z0-9_]*)\\n([\\s\\S]*?)```")

    var lastIndex = 0
    val matches = codeBlockRegex.findAll(markdown)

    for (match in matches) {
        if (match.range.first > lastIndex) {
            val textBefore = markdown.substring(lastIndex, match.range.first).trim()
            if (textBefore.isNotEmpty()) {
                sections.add(Section.TextSection(textBefore))
            }
        }
        val lang = match.groupValues[1]
        val code = match.groupValues[2].trimEnd()
        sections.add(Section.CodeBlock(lang, code))
        lastIndex = match.range.last + 1
    }

    if (lastIndex < markdown.length) {
        val remaining = markdown.substring(lastIndex).trim()
        if (remaining.isNotEmpty()) {
            sections.add(Section.TextSection(remaining))
        }
    }

    if (sections.isEmpty() && markdown.isNotEmpty()) {
        sections.add(Section.TextSection(markdown))
    }

    return sections
}

private fun buildAnnotatedText(text: String, defaultColor: Color) = buildAnnotatedString {
    var cursor = 0
    val boldRegex = Regex("\\*\\*([^*]+)\\*\\*")
    val inlineCodeRegex = Regex("`([^`]+)`")

    // Simple parser for bold and inline code
    val combinedRegex = Regex("(\\*\\*([^*]+)\\*\\*)|(`([^`]+)`)")
    val matches = combinedRegex.findAll(text)

    for (match in matches) {
        if (match.range.first > cursor) {
            withStyle(SpanStyle(color = defaultColor)) {
                append(text.substring(cursor, match.range.first))
            }
        }

        val raw = match.value
        if (raw.startsWith("**") && raw.endsWith("**")) {
            val inner = raw.substring(2, raw.length - 2)
            withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor)) {
                append(inner)
            }
        } else if (raw.startsWith("`") && raw.endsWith("`")) {
            val inner = raw.substring(1, raw.length - 1)
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan,
                    background = Color(0x3338BDF8)
                )
            ) {
                append(" $inner ")
            }
        }

        cursor = match.range.last + 1
    }

    if (cursor < text.length) {
        withStyle(SpanStyle(color = defaultColor)) {
            append(text.substring(cursor))
        }
    }
}
