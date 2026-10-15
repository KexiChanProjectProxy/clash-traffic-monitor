package com.zenkexi.clashmonitor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.zenkexi.clashmonitor.data.AppSettings
import com.zenkexi.clashmonitor.data.ClashApiClient
import com.zenkexi.clashmonitor.data.ControllerConfig
import com.zenkexi.clashmonitor.data.ThemeMode
import com.zenkexi.clashmonitor.ui.theme.LocalMetricColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initial: AppSettings,
    canGoBack: Boolean,
    onSave: (AppSettings) -> Unit,
    onBack: () -> Unit,
) {
    var host by remember { mutableStateOf(initial.controller.host) }
    var port by remember { mutableStateOf(initial.controller.port.toString()) }
    var secret by remember { mutableStateOf(initial.controller.secret) }
    var https by remember { mutableStateOf(initial.controller.useHttps) }
    var showSecret by remember { mutableStateOf(false) }
    var theme by remember { mutableStateOf(initial.themeMode) }
    var keepOn by remember { mutableStateOf(initial.keepScreenOn) }
    var testResult by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var testing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val colors = LocalMetricColors.current

    val portNum = port.toIntOrNull()?.takeIf { it in 1..65535 }
    val hostClean = host.trim().removePrefix("http://").removePrefix("https://").trimEnd('/')
    val valid = hostClean.isNotEmpty() && portNum != null
    fun config() = ControllerConfig(hostClean, portNum ?: 9090, secret.trim(), https)

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (canGoBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            } else {
                Spacer(Modifier.width(16.dp))
            }
            Text("Clash controller", style = MaterialTheme.typography.titleLarge)
        }

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(Modifier.widthIn(max = 720.dp).fillMaxWidth()) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it; testResult = null },
                        label = { Text("Host or IP") },
                        placeholder = { Text("192.168.1.1") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.weight(2f),
                    )
                    OutlinedTextField(
                        value = port,
                        onValueChange = { port = it.filter(Char::isDigit).take(5); testResult = null },
                        label = { Text("Port") },
                        isError = portNum == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = secret,
                    onValueChange = { secret = it; testResult = null },
                    label = { Text("Secret (optional)") },
                    singleLine = true,
                    visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        TextButton(onClick = { showSecret = !showSecret }) {
                            Text(if (showSecret) "Hide" else "Show")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                SettingRow("Use HTTPS") { Switch(checked = https, onCheckedChange = { https = it; testResult = null }) }
                SettingRow("Keep screen on") { Switch(checked = keepOn, onCheckedChange = { keepOn = it }) }
                SettingRow("Theme") {
                    SingleChoiceSegmentedButtonRow {
                        ThemeMode.entries.forEachIndexed { i, m ->
                            SegmentedButton(
                                selected = theme == m,
                                onClick = { theme = m },
                                shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                            ) {
                                Text(
                                    when (m) {
                                        ThemeMode.SYSTEM -> "Auto"
                                        ThemeMode.LIGHT -> "Light"
                                        ThemeMode.DARK -> "Dark"
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        enabled = valid && !testing,
                        onClick = {
                            testing = true
                            testResult = null
                            scope.launch {
                                testResult = try {
                                    true to "Connected: ${ClashApiClient(config()).version()}"
                                } catch (e: Exception) {
                                    false to ClashApiClient.describe(e)
                                }
                                testing = false
                            }
                        },
                    ) { Text(if (testing) "Testing…" else "Test connection") }
                    Spacer(Modifier.width(12.dp))
                    Button(
                        enabled = valid,
                        onClick = { onSave(AppSettings(config(), theme, keepOn)) },
                    ) { Text("Save") }
                    Spacer(Modifier.width(16.dp))
                    testResult?.let { (ok, msg) ->
                        Text(
                            msg,
                            color = if (ok) colors.good else colors.critical,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Uses the Clash / mihomo external controller (external-controller in config.yaml). " +
                        "New connections per second is derived from connection IDs sampled once a second, " +
                        "so connections shorter than a second may be missed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingRow(label: String, control: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        control()
    }
}
