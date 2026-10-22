package net.mfuns.watch

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import net.mfuns.watch.ui.AppNavHost
import net.mfuns.watch.ui.AppViewModel
import net.mfuns.watch.ui.screens.LoginScreen
import net.mfuns.watch.ui.theme.MfunsWatchTheme

class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            MfunsWatchTheme {
                val session by appViewModel.session.collectAsStateWithLifecycle()
                val loginState by appViewModel.loginState.collectAsStateWithLifecycle()

                if (session.loggedIn) {
                    AppNavHost(
                        appViewModel = appViewModel,
                        session = session,
                    )
                } else {
                    LoginScreen(
                        state = loginState,
                        onLogin = appViewModel::login,
                    )
                }
            }
        }
    }
}
