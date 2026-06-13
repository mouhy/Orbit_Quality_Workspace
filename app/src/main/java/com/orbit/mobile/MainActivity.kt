package com.orbit.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.core.datastore.OrbitDataStore
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.ThemeMode
import com.orbit.mobile.feature.shell.OrbitNavHost
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

// Host activity
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: OrbitDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by dataStore.themeFlow
                .collectAsStateWithLifecycle(initialValue = ThemeMode.DARK)
            OrbitTheme(themeMode) {
                OrbitNavHost()
            }
        }
    }
}
