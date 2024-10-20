package org.breezyweather.weather.json.mf

import kotlinx.serialization.Serializable

/**
 * Mf current result.
 */
@Serializable
data class MfCurrentResult(
    val properties: MfCurrentProperties?
)