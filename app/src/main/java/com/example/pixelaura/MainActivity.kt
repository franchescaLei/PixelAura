package com.example.pixelaura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.view.WindowCompat
import com.example.pixelaura.pages.PixelAuraNavigation
import com.example.pixelaura.ui.theme.AuthViewModel
import com.example.pixelaura.ui.theme.PixelAuraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        enableEdgeToEdge()
        val authViewModel : AuthViewModel by viewModels()
        setContent {
            PixelAuraTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    PixelAuraNavigation(modifier = Modifier.padding(innerPadding), authViewModel)

                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    PixelAuraTheme {

    }
}






