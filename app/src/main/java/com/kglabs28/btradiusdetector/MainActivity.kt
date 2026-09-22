package com.kglabs28.btradiusdetector

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kglabs28.btradiusdetector.data.BleRssiRepository
import com.kglabs28.btradiusdetector.data.CompassRepository
import com.kglabs28.btradiusdetector.navigation.AppNavigation
import com.kglabs28.btradiusdetector.ui.MainViewModel
import com.kglabs28.btradiusdetector.ui.theme.BTRadiusDetectorTheme
import com.kglabs28.btradiusdetector.utils.AppUtils
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val bleRepository = BleRssiRepository(applicationContext)
        val compassRepository = CompassRepository(applicationContext)

        val viewModelFactory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(bleRepository, compassRepository) as T
            }
        }

        setContent {
            val viewModel: MainViewModel = viewModel(factory = viewModelFactory)
            val showOnboarding by AppUtils.observeShowOnboarding(applicationContext)
                .collectAsState(initial = true)

            BTRadiusDetectorTheme {
                AppNavigation(
                    viewModel = viewModel,
                    showOnboarding = false,
                    onOnboardingComplete = {
                        lifecycleScope.launch {
                            AppUtils.setShowOnboarding(applicationContext, false)
                        }
                    }
                )
            }
        }
    }
}