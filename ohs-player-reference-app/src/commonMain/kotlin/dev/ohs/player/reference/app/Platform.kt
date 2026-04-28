package dev.ohs.player.reference.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform