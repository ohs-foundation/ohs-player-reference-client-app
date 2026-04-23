package dev.ohs.player.reference.client.app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform