package com.uc.caffeine.ui.screens

import androidx.compose.foundation.layout.Box
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.uc.caffeine.data.model.DrinkPreset
import com.uc.caffeine.data.model.RecentDrink
import com.uc.caffeine.ui.viewmodel.CaffeineViewModel

@Composable
fun HomeScreen(viewModel: CaffeineViewModel = viewModel()) {

    val totalCaffeine by viewModel.todayTotalMg.collectAsStateWithLifecycle()
    val recentDrinks  by viewModel.recentDrinks.collectAsStateWithLifecycle()
    val todayEntries  by viewModel.todayEntries.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // ── Caffeine counter ──────────────────────────────────────
        Text(
            text = "Today's Caffeine",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${totalCaffeine}mg",
            fontSize = 72.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )

        val statusMessage = when {
            totalCaffeine == 0   -> "No caffeine yet today"
            totalCaffeine < 100  -> "You're just waking up ☀️"
            totalCaffeine < 200  -> "Feeling good ✅"
            totalCaffeine < 400  -> "Getting there ⚠️"
            else                 -> "Consider slowing down 🛑"
        }
        Text(
            text = statusMessage,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // ── Quick Add — only shown after the user has logged at least one drink ──
        if (recentDrinks.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Add",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "recently used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            // Row of max 2 recent drink cards side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                recentDrinks.forEach { recent ->
                    RecentDrinkCard(
                        recent = recent,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.logRecentDrink(recent)
                    }
                }
                // If only 1 recent drink, fill the other half with empty space
                if (recentDrinks.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
        }

        // ── First time prompt — shown before any drink is logged ─────────────
        if (recentDrinks.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = "☕", fontSize = 40.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Log your first drink",
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Tap Add to browse all drinks",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (todayEntries.isNotEmpty()) {
            TextButton(onClick = { viewModel.resetToday() }) {
                Text("Reset Today")
            }
        }
    }
}

// ── Quick Add card — built from RecentDrink (log history), not DrinkPreset ──
@Composable
fun RecentDrinkCard(
    recent: RecentDrink,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    // RecentDrink doesn't have imageName — look it up by drinkName convention
    // e.g. "Dhak Blend" → we try "img_bluetokai_dhak_blend" won't work directly
    // So we just use emoji for Quick Add cards for now
    // When we wire up imageName into ConsumptionEntry later, images will appear here too

    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = recent.emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = recent.drinkName,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${recent.caffeineMg}mg",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ── Full drink catalog card — used on the Add screen ──────────────────────
@Composable
fun DrinkCard(drink: DrinkPreset, onClick: () -> Unit) {
    val context = LocalContext.current

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (drink.imageName.isNotBlank()) {
                // Coil loads from assets/items/<itemId>.png
                // file:///android_asset/ is the URI scheme for the assets folder
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data("file:///android_asset/items/${drink.imageName}.png")
                        .build(),
                    contentDescription = drink.name,
                    modifier = Modifier.size(80.dp),
                    contentScale = ContentScale.Fit,
                    error = null,   // fall through to emoji on error
                    placeholder = null
                )
            } else {
                Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                    Text(text = drink.emoji, fontSize = 40.sp, textAlign = TextAlign.Center)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = drink.name,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (drink.brand.isNotBlank()) {
                Text(
                    text = drink.brand,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${drink.defaultCaffeineMg}mg",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = drink.defaultUnit,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
