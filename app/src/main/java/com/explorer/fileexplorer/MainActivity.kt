package com.explorer.fileexplorer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.explorer.fileexplorer.core.designsystem.FileExplorerTheme
import com.explorer.fileexplorer.core.designsystem.R as DesignSystemR
import com.explorer.fileexplorer.core.designsystem.ThemeMode
import com.explorer.fileexplorer.core.storage.PermissionHelper
import com.explorer.fileexplorer.feature.settings.SettingsRepository
import com.explorer.fileexplorer.navigation.AppNavigation
import com.explorer.fileexplorer.navigation.Routes
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var permissionHelper: PermissionHelper
    @Inject lateinit var settingsRepository: SettingsRepository

    private val hasPermissionState = mutableStateOf(false)
    private val limitedStorageState = mutableStateOf(false)
    private val shortcutRouteState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hasPermissionState.value = permissionHelper.hasFullStorageAccess()
        shortcutRouteState.value = extractShortcutRoute(intent)
        publishDynamicShortcuts()

        setContent {
            val settingsFlow = remember(settingsRepository) {
                settingsRepository.settings
            }
            val settings by settingsFlow
                .collectAsStateWithLifecycle(initialValue = com.explorer.fileexplorer.feature.settings.SettingsState())

            FileExplorerTheme(themeMode = settings.themeMode, fontScale = settings.textScale) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    if (hasPermissionState.value || limitedStorageState.value) {
                        AppNavigation(
                            initialPath = if (limitedStorageState.value) {
                                permissionHelper.scopedStorageRootPath()
                            } else null,
                            initialRoute = shortcutRouteState.value,
                        )
                    } else {
                        PermissionScreen(
                            onGrantPermission = {
                                startActivity(permissionHelper.getManageStorageIntent())
                            },
                            onContinueWithLimitedAccess = {
                                limitedStorageState.value = true
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        shortcutRouteState.value = extractShortcutRoute(intent)
    }

    override fun onResume() {
        super.onResume()
        hasPermissionState.value = permissionHelper.hasFullStorageAccess()
    }

    private fun extractShortcutRoute(intent: Intent?): String? {
        val route = intent?.getStringExtra("shortcut_route") ?: return null
        return when (route) {
            "search" -> Routes.SEARCH
            "network" -> Routes.NETWORK
            "cloud" -> Routes.CLOUD
            "apps" -> Routes.APPS
            "analyzer" -> Routes.ANALYZER
            "transfers" -> Routes.TRANSFERS
            else -> null
        }
    }

    private fun publishDynamicShortcuts() {
        val context = applicationContext
        val shortcuts = listOf(
            ShortcutInfoCompat.Builder(context, "dynamic_search")
                .setShortLabel(getString(R.string.shortcut_search_short))
                .setLongLabel(getString(R.string.shortcut_search_long))
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_search))
                .setIntent(Intent(context, MainActivity::class.java).apply {
                    action = "com.explorer.fileexplorer.ACTION_SHORTCUT"
                    putExtra("shortcut_route", "search")
                })
                .build(),
            ShortcutInfoCompat.Builder(context, "dynamic_network")
                .setShortLabel(getString(R.string.shortcut_network_short))
                .setLongLabel(getString(R.string.shortcut_network_long))
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_share))
                .setIntent(Intent(context, MainActivity::class.java).apply {
                    action = "com.explorer.fileexplorer.ACTION_SHORTCUT"
                    putExtra("shortcut_route", "network")
                })
                .build(),
            ShortcutInfoCompat.Builder(context, "dynamic_cloud")
                .setShortLabel(getString(R.string.shortcut_cloud_short))
                .setLongLabel(getString(R.string.shortcut_cloud_long))
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_compass))
                .setIntent(Intent(context, MainActivity::class.java).apply {
                    action = "com.explorer.fileexplorer.ACTION_SHORTCUT"
                    putExtra("shortcut_route", "cloud")
                })
                .build(),
            ShortcutInfoCompat.Builder(context, "dynamic_apps")
                .setShortLabel(getString(R.string.shortcut_apps_short))
                .setLongLabel(getString(R.string.shortcut_apps_long))
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_manage))
                .setIntent(Intent(context, MainActivity::class.java).apply {
                    action = "com.explorer.fileexplorer.ACTION_SHORTCUT"
                    putExtra("shortcut_route", "apps")
                })
                .build(),
            ShortcutInfoCompat.Builder(context, "dynamic_analyzer")
                .setShortLabel(getString(R.string.shortcut_analyzer_short))
                .setLongLabel(getString(R.string.shortcut_analyzer_long))
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_info_details))
                .setIntent(Intent(context, MainActivity::class.java).apply {
                    action = "com.explorer.fileexplorer.ACTION_SHORTCUT"
                    putExtra("shortcut_route", "analyzer")
                })
                .build(),
            ShortcutInfoCompat.Builder(context, "dynamic_transfers")
                .setShortLabel(getString(R.string.shortcut_transfers_short))
                .setLongLabel(getString(R.string.shortcut_transfers_long))
                .setIcon(IconCompat.createWithResource(context, android.R.drawable.ic_menu_upload))
                .setIntent(Intent(context, MainActivity::class.java).apply {
                    action = "com.explorer.fileexplorer.ACTION_SHORTCUT"
                    putExtra("shortcut_route", "transfers")
                })
                .build()
        )
        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (_: Exception) {
        }
    }
}

@Composable
private fun PermissionScreen(
    onGrantPermission: () -> Unit,
    onContinueWithLimitedAccess: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.Storage,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(DesignSystemR.string.storage_access_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(DesignSystemR.string.storage_access_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onGrantPermission,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(DesignSystemR.string.grant_storage_access))
        }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = onContinueWithLimitedAccess,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(DesignSystemR.string.continue_limited_storage))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(DesignSystemR.string.storage_access_limited_message),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(DesignSystemR.string.storage_access_settings),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
