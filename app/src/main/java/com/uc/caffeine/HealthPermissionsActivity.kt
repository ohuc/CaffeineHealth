package com.uc.caffeine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.uc.caffeine.data.SettingsRepository
import com.uc.caffeine.data.ThemeMode
import com.uc.caffeine.data.UserSettings
import com.uc.caffeine.ui.components.SettingsPageScaffold
import com.uc.caffeine.ui.theme.CaffeineTheme
import com.uc.caffeine.util.AppLocale

/** A separate task entry lets Health Connect return to its own permission screen on Back. */
class HealthPermissionsActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) = super.attachBaseContext(AppLocale.wrap(newBase))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val settingsRepository = SettingsRepository(applicationContext)
        setContent {
            val settings by settingsRepository.settingsFlow.collectAsStateWithLifecycle(initialValue = UserSettings())
            val dark = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            CaffeineTheme(darkTheme = dark, colorPalette = settings.colorPalette) {
                Surface {
                    SettingsPageScaffold(
                        title = stringResource(R.string.health_privacy_title),
                        showBackButton = true,
                        onBack = ::finish,
                    ) { _ ->
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState())
                                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom))
                                .padding(bottom = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                        ) {
                            Text(stringResource(R.string.health_privacy_intro), style = MaterialTheme.typography.bodyLarge)
                            listOf(
                                R.string.health_privacy_nutrition_title to R.string.health_privacy_nutrition_body,
                                R.string.health_privacy_hydration_title to R.string.health_privacy_hydration_body,
                                R.string.health_privacy_sleep_title to R.string.health_privacy_sleep_body,
                                R.string.health_privacy_storage_title to R.string.health_privacy_storage_body,
                                R.string.health_privacy_control_title to R.string.health_privacy_control_body,
                            ).forEach { (title, body) ->
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(title), style = MaterialTheme.typography.titleMediumEmphasized)
                                    Text(stringResource(body), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                            val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
                            OutlinedButton(
                                onClick = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyPolicyUrl))) },
                            ) {
                                Text(stringResource(R.string.privacy_policy_title))
                            }
                        }
                    }
                }
            }
        }
    }
}
