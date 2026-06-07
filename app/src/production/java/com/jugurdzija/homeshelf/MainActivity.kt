package com.jugurdzija.homeshelf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.jugurdzija.homeshelf.ui.common.AuthGate
import com.jugurdzija.homeshelf.ui.nav.HomeShelfNavGraph
import com.jugurdzija.homeshelf.ui.theme.HomeShelfTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HomeShelfTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AuthGate { onLogout ->
                        HomeShelfNavGraph(onLogout = onLogout)
                    }
                }
            }
        }
    }
}
