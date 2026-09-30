package com.example.palette2

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform