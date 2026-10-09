package com.example.palette

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.palette.navigation.AppNavigation

@Composable
fun App() {
    MaterialTheme {
        AppNavigation()
    }
}
