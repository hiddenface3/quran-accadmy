package com.example.ui.screens.classroom

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.QuranVerse
import com.example.ui.theme.BrandDarkEmerald
import com.example.ui.theme.BrandDarkGold
import com.example.ui.theme.BrandGold
import com.example.ui.theme.BrandMint

@Composable
fun QuranCompanionCard(
    verse: QuranVerse,
    currentIndex: Int,
    totalCount: Int,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("quran_companion_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = BrandDarkEmerald.copy(alpha = 0.94f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandDarkGold),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        tint = BrandGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${verse.surahName} · Ayah ${verse.ayahNumber} of $totalCount",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = BrandGold
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = BrandMint)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Arabic text in large, readable script
            Text(
                text = verse.arabicText,
                fontSize = 24.sp,
                lineHeight = 36.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = verse.transliteration,
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                color = BrandMint.copy(alpha = 0.8f)
            )

            Text(
                text = verse.translation,
                fontSize = 13.sp,
                color = BrandGold,
                modifier = Modifier.padding(top = 2.dp)
            )

            if (verse.tajweedNote.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = "Tajweed: ${verse.tajweedNote}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandMint,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pagination Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPrev,
                    enabled = currentIndex > 0
                ) {
                    Icon(imageVector = Icons.Default.NavigateBefore, contentDescription = "Prev", tint = Color.White)
                }

                Text(
                    text = "Ayah ${currentIndex + 1} of $totalCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandMint
                )

                IconButton(
                    onClick = onNext,
                    enabled = currentIndex < totalCount - 1
                ) {
                    Icon(imageVector = Icons.Default.NavigateNext, contentDescription = "Next", tint = Color.White)
                }
            }
        }
    }
}
