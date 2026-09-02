package com.empire.server.etsy

import com.empire.server.config.AppConfig
import com.empire.server.storage.JsonFileStore
import java.io.File
import kotlinx.serialization.Serializable

/** Etsy access tokens live ~1 hour and each refresh returns a NEW refresh token that
 *  invalidates the old one -- so the current pair has to be persisted to survive a
 *  restart, not just held in memory. Seeded from AppConfig.etsyInitialRefreshToken the
 *  very first time; every refresh after that overwrites this file with the rotated pair. */
@Serializable
data class EtsyTokens(
    val accessToken: String = "",
    val refreshToken: String = "",
    val expiresAtEpochSeconds: Long = 0
)

class EtsyTokenStore(dataDir: File = AppConfig.dataDir) {
    private val store = JsonFileStore(
        file = File(dataDir, "etsy-tokens.json"),
        serializer = EtsyTokens.serializer(),
        default = { EtsyTokens(refreshToken = AppConfig.etsyInitialRefreshToken.orEmpty()) }
    )

    fun read(): EtsyTokens = store.read()

    fun write(tokens: EtsyTokens) = store.write(tokens)
}
