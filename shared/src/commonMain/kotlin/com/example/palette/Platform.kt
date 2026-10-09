package com.example.palette

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform