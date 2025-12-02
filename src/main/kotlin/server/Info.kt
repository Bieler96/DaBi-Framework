package server

import kotlinx.serialization.Serializable

@Serializable
data class Info(
    val title: String,
    val version: String,
    val description: String? = null
)