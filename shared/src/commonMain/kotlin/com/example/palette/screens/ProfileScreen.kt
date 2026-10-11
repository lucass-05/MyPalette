package com.example.palette.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// NOTA !! ahora mismo la mayoría de elementos son placeholders (para ver la estructura)
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- CONTENEDOR DE HEADER Y FOTO DE PERFIL ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            // HEADER
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            ) {
                Text("header", color = Color.DarkGray)
            }

            // FOTO DE PERFIL
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomCenter)
                    .clip(CircleShape)
                    .background(Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                Text("pic", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- NOMBRE Y USUARIO ---
        Text(text = "name", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = "@username", fontSize = 16.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(10.dp))

        // --- BIOGRAFÍA ---
        Text(
            text = "biography",
            fontSize = 14.sp,
            color = Color.DarkGray,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // --- SECCIÓN FAVORITOS ---
        Column(
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
        ) {

            Text(text = "favs", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(modifier = Modifier.height(8.dp))

            // fondo de la paleta
            val paletteColor = Color.LightGray

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(color = paletteColor, shape = RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MediaBox(title = "film", coverColor = Color(0xFFFFCDD2))
                        MediaBox(title = "book", coverColor = Color(0xFFC8E6C9))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MediaBox(title = "game", coverColor = Color(0xFFBBDEFB))
                        MediaBox(title = "music", coverColor = Color(0xFFFFF9C4))
                    }
                }
            }
        }
    }
}

@Composable
fun MediaBox(title: String, coverColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .background(color = coverColor, shape = RoundedCornerShape(8.dp))
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}