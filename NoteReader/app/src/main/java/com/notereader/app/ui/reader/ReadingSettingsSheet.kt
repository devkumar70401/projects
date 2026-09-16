package com.notereader.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notereader.app.domain.model.PageTurnStyle
import com.notereader.app.domain.model.ReadingSettings
import com.notereader.app.domain.model.ReadingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    settings: ReadingSettings,
    onSettingsChanged: (ReadingSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                "Reading Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Themes
            Text("Themes", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ReadingTheme.values()) { th ->
                    val isSelected = th == settings.theme
                    val bg = Color(android.graphics.Color.parseColor(th.backgroundHex))
                    val txt = Color(android.graphics.Color.parseColor(th.textHex))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bg,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .size(width = 90.dp, height = 50.dp)
                            .clickable { onSettingsChanged(settings.copy(theme = th)) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = th.displayName,
                                color = txt,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Font Family
            Text("Font Family", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("SansSerif", "Serif", "Monospace").forEach { fam ->
                    val isSelected = fam.equals(settings.fontFamily, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSettingsChanged(settings.copy(fontFamily = fam)) },
                        label = { Text(fam) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Font Size Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Font Size", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${settings.fontSizeSp.toInt()} sp", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = settings.fontSizeSp,
                onValueChange = { onSettingsChanged(settings.copy(fontSizeSp = it)) },
                valueRange = 12f..32f,
                steps = 10
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Line Spacing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Line Spacing", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(String.format("%.2fx", settings.lineSpacingMultiplier), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = settings.lineSpacingMultiplier,
                onValueChange = { onSettingsChanged(settings.copy(lineSpacingMultiplier = it)) },
                valueRange = 1.0f..2.2f
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Margin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Page Margins", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${settings.marginDp.toInt()} dp", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = settings.marginDp,
                onValueChange = { onSettingsChanged(settings.copy(marginDp = it)) },
                valueRange = 12f..48f
            )
        }
    }
}
