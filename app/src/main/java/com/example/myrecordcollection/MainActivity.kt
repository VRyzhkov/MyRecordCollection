package com.example.myrecordcollection

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.myrecordcollection.data.update.AvailableUpdate
import com.example.myrecordcollection.data.update.GitHubUpdateChecker
import com.example.myrecordcollection.ui.collection.CollectionRoute
import com.example.myrecordcollection.ui.theme.MyRecordCollectionTheme
import com.example.myrecordcollection.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var availableUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }
            val preferences = remember {
                getSharedPreferences("appearance", MODE_PRIVATE)
            }
            var themeMode by remember {
                mutableStateOf(
                    ThemeMode.fromStoredValue(preferences.getString("theme_mode", null)),
                )
            }
            val systemDarkTheme = isSystemInDarkTheme()
            val darkTheme = when (themeMode) {
                ThemeMode.System -> systemDarkTheme
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            LaunchedEffect(Unit) {
                val currentVersion = packageManager
                    .getPackageInfo(packageName, 0)
                    .versionName
                    .orEmpty()
                availableUpdate = runCatching {
                    GitHubUpdateChecker().findUpdate(currentVersion)
                }.getOrNull()
            }

            MyRecordCollectionTheme(darkTheme = darkTheme) {
                CollectionRoute(
                    themeMode = themeMode,
                    onThemeModeChanged = { newMode ->
                        themeMode = newMode
                        preferences.edit().putString("theme_mode", newMode.name).apply()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                availableUpdate?.let { update ->
                    AlertDialog(
                        onDismissRequest = { availableUpdate = null },
                        title = { Text("Доступно обновление") },
                        text = {
                            Text("Доступна версия ${update.versionName}. Скачать APK с GitHub?")
                        },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(update.downloadUrl)),
                                    )
                                    availableUpdate = null
                                },
                            ) {
                                Text("Скачать")
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { availableUpdate = null }) {
                                Text("Позже")
                            }
                        },
                    )
                }
            }
        }
    }
}
