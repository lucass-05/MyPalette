package com.example.palette2.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.palette2.screens.HomeScreen
import com.example.palette2.screens.ProfileScreen

enum class AppPage {
    HOME,
    PROFILE
}

@Composable
fun AppNavigation() {

    var currentPage by rememberSaveable {
        mutableStateOf(AppPage.HOME)
    }

    Scaffold(
        bottomBar = {
            NavigationBar {

                NavigationBarItem(
                    selected = currentPage == AppPage.HOME,
                    onClick = {
                        currentPage = AppPage.HOME
                    },
                    icon = {
                        Text("⌂")
                    },
                    label = {
                        Text("Inicio")
                    }
                )

                NavigationBarItem(
                    selected = currentPage == AppPage.PROFILE,
                    onClick = {
                        currentPage = AppPage.PROFILE
                    },
                    icon = {
                        Text("👤")
                    },
                    label = {
                        Text("Perfil")
                    }
                )
            }
        }
    ) { paddingValues ->

        when (currentPage) {

            AppPage.HOME -> {
                HomeScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }

            AppPage.PROFILE -> {
                ProfileScreen(
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}
