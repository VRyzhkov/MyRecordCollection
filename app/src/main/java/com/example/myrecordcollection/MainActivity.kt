package com.example.myrecordcollection

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Modifier
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.example.myrecordcollection.data.update.UpdatePhase
import com.example.myrecordcollection.data.update.UpdateViewModel
import com.example.myrecordcollection.input.SteeringWheelController
import com.example.myrecordcollection.ui.collection.CollectionRoute
import com.example.myrecordcollection.ui.theme.MyRecordCollectionTheme
import com.example.myrecordcollection.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    private lateinit var steeringWheelController: SteeringWheelController
    private val updateViewModel: UpdateViewModel by viewModels()
    private val installPermission = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (packageManager.canRequestPackageInstalls()) installUpdate()
        else updateViewModel.showInstallError("Установка не разрешена. Нажмите «Установить» и разрешите установку из MyRecordCollection в настройках Android.")
    }

    private fun installUpdate() {
        try {
            if (!packageManager.canRequestPackageInstalls()) {
                updateViewModel.showInstallError("Разрешите установку из MyRecordCollection, затем вернитесь в приложение.")
                installPermission.launch(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            } else {
                startActivity(updateViewModel.installerIntent())
                updateViewModel.showInstallError("APK готов. Завершите установку в окне Android. Если вы отменили её, нажмите «Установить» ещё раз.")
            }
        } catch (_: Exception) {
            updateViewModel.showInstallError("Не удалось открыть установщик. Проверьте разрешение на установку и наличие системного установщика APK.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        steeringWheelController = SteeringWheelController(this)
        enableEdgeToEdge()
        setContent {
            val updateState by updateViewModel.state.collectAsStateWithLifecycle()
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

            MyRecordCollectionTheme(darkTheme = darkTheme) {
                CollectionRoute(
                    steeringWheelController = steeringWheelController,
                    themeMode = themeMode,
                    onThemeModeChanged = { newMode ->
                        themeMode = newMode
                        preferences.edit().putString("theme_mode", newMode.name).apply()
                    },
                    modifier = Modifier.fillMaxSize(),
                )
                if (updateState.phase != UpdatePhase.Hidden) {
                    AlertDialog(
                        onDismissRequest = { if (updateState.phase != UpdatePhase.Downloading) updateViewModel.dismiss() },
                        title = { Text("Обновление ${updateState.update?.versionName.orEmpty()}") },
                        text = {
                            Text(if (updateState.phase == UpdatePhase.Offer) "Скачать новую версию с GitHub? После загрузки появится кнопка установки." else updateState.message)
                        },
                        confirmButton = {
                            when (updateState.phase) {
                                UpdatePhase.Offer, UpdatePhase.Failed -> TextButton(onClick = updateViewModel::download) {
                                    Text(if (updateState.phase == UpdatePhase.Failed) "Повторить" else "Скачать")
                                }
                                UpdatePhase.Ready -> TextButton(onClick = ::installUpdate) { Text("Установить") }
                                else -> Unit
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = updateViewModel::dismiss) {
                                Text(if (updateState.phase == UpdatePhase.Downloading) "Отменить загрузку" else "Позже")
                            }
                        },
                    )
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
        steeringWheelController.handleKeyEvent(event) || super.dispatchKeyEvent(event)

    override fun onResume() {
        super.onResume()
        steeringWheelController.setActivityResumed(true)
    }

    override fun onPause() {
        steeringWheelController.setActivityResumed(false)
        super.onPause()
    }

    override fun onDestroy() {
        steeringWheelController.release()
        super.onDestroy()
    }
}
