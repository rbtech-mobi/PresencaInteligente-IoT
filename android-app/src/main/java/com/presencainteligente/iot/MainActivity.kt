package com.presencainteligente.iot

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.presencainteligente.iot.ui.LocationSetupScreen
import com.presencainteligente.iot.ui.PresenceScreen
import com.presencainteligente.iot.ui.PresenceViewModel
import com.presencainteligente.iot.ui.theme.MqttMobileIotTheme

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        Log.d("MainActivity", "Permissões resultado: $permissions")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val presenceViewModel = ViewModelProvider(
            this,
            ViewModelProvider.AndroidViewModelFactory.getInstance(application)
        )[PresenceViewModel::class.java]

        checkPermissions()

        setContent {
            MqttMobileIotTheme {
                AppNavigation(presenceViewModel = presenceViewModel)
            }
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val toRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (toRequest.isNotEmpty()) {
            Log.d("MainActivity", "Pedindo permissões: $toRequest")
            requestPermissionLauncher.launch(toRequest.toTypedArray())
        } else {
            Log.d("MainActivity", "Todas as permissões já concedidas")
        }
    }
}

@Composable
fun AppNavigation(presenceViewModel: PresenceViewModel) {
    val currentScreen = remember { mutableStateOf("presence") }

    Log.d("AppNavigation", "Renderizando tela: ${currentScreen.value}")

    when (currentScreen.value) {
        "presence" -> {
            Log.d("AppNavigation", "Mostrando PresenceScreen")
            PresenceScreen(
                viewModel = presenceViewModel,
                onNavigateToLocationSetup = {
                    Log.d("AppNavigation", "Navegando para LocationSetupScreen")
                    currentScreen.value = "location_setup"
                }
            )
        }

        "location_setup" -> {
            Log.d("AppNavigation", "Mostrando LocationSetupScreen")
            LocationSetupScreen(
                onLocationSaved = { latitude, longitude ->
                    Log.d("AppNavigation", "Localização salva: lat=$latitude, lng=$longitude")
                    presenceViewModel.setHomeLocation(latitude, longitude)
                    currentScreen.value = "presence"
                },
                onNavigateBack = {
                    Log.d("AppNavigation", "Voltando para PresenceScreen")
                    currentScreen.value = "presence"
                }
            )
        }
    }
}
