package org.breezyweather.weather.converters

import android.content.Context
import org.breezyweather.BreezyWeather
import org.breezyweather.common.basic.models.Location
import org.breezyweather.common.basic.models.options.provider.WeatherSource
import org.breezyweather.common.basic.models.weather.*
import org.breezyweather.common.utils.DisplayUtils
import org.breezyweather.weather.json.atmoaura.AtmoAuraPointResult
import org.breezyweather.weather.json.mf.MfCurrentResult
import org.breezyweather.weather.json.mf.MfEphemeris
import org.breezyweather.weather.json.mf.MfEphemerisResult
import org.breezyweather.weather.json.mf.MfForecastResult
import org.breezyweather.weather.json.mf.MfForecastDaily
import org.breezyweather.weather.json.mf.MfForecastHourly
import org.breezyweather.weather.json.mf.MfForecastProbability
import org.breezyweather.weather.json.mf.MfRainResult
import org.breezyweather.weather.json.mf.MfWarningsResult
import org.breezyweather.weather.services.WeatherService.WeatherResultWrapper
import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.roundToInt

fun convert(location: Location?, result: MfForecastResult): Location? {
    return if (result.properties == null || result.geometry == null
        || result.geometry.coordinates?.getOrNull(0) == null || result.geometry.coordinates.getOrNull(1) == null) {
        null
    } else if (location != null && !location.province.isNullOrEmpty()
        && location.city.isNotEmpty()
        && !location.district.isNullOrEmpty()
    ) {
        Location(
            cityId = result.geometry.coordinates[1].toString() + "," + result.geometry.coordinates[0],
            latitude = result.geometry.coordinates[1],
            longitude = result.geometry.coordinates[0],
            timeZone = TimeZone.getTimeZone(result.properties.timezone),
            country = result.properties.country,
            countryCode = result.properties.country.substring(0, 2),
            province = location.province, // Département
            provinceCode = location.provinceCode, // Département
            city = location.city,
            district = location.district,
            weatherSource = WeatherSource.MF,
            isChina = result.properties.country.isNotEmpty()
                    && (result.properties.country.startsWith("cn", ignoreCase = true)
                    || result.properties.country.startsWith("hk", ignoreCase = true)
                    || result.properties.country.startsWith("tw", ignoreCase = true))
        )
    } else {
        Location(
            cityId = result.geometry.coordinates[1].toString() + "," + result.geometry.coordinates[0],
            latitude = result.geometry.coordinates[1],
            longitude = result.geometry.coordinates[0],
            timeZone = TimeZone.getTimeZone(result.properties.timezone),
            country = result.properties.country,
            countryCode = result.properties.country.substring(0, 2),
            province = if (!result.properties.frenchDepartment.isNullOrEmpty())
                getFrenchDepartmentName(result.properties.frenchDepartment) else null, // Département
            provinceCode = result.properties.frenchDepartment, // Département
            city = result.properties.name,
            weatherSource = WeatherSource.MF,
            isChina = result.properties.country.isNotEmpty()
                    && (result.properties.country.startsWith("cn", ignoreCase = true)
                    || result.properties.country.startsWith("hk", ignoreCase = true)
                    || result.properties.country.startsWith("tw", ignoreCase = true))
        )
    }
}

fun convert(
    context: Context,
    location: Location,
    currentResult: MfCurrentResult,
    forecastResult: MfForecastResult,
    ephemerisResult: MfEphemerisResult,
    rainResult: MfRainResult?,
    warningsResult: MfWarningsResult,
    aqiAtmoAuraResult: AtmoAuraPointResult?
): WeatherResultWrapper {
    // If the API doesn’t return hourly or daily, consider data as garbage and keep cached data
    if (forecastResult.properties == null || forecastResult.properties.forecast.isNullOrEmpty()
        || forecastResult.properties.dailyForecast.isNullOrEmpty()) {
        return WeatherResultWrapper(null);
    }

    return try {
        val hourlyByHalfDay: MutableMap<String, Map<String, MutableList<Hourly>>> = HashMap()
        val hourlyList: MutableList<Hourly> = mutableListOf()

        for (i in forecastResult.properties.forecast.indices) {
            val hourlyForecast = forecastResult.properties.forecast[i]
            val hourly = Hourly(
                date = hourlyForecast.time,
                weatherText = hourlyForecast.weatherDescription,
                weatherCode = getWeatherCode(hourlyForecast.weatherIcon),
                temperature = Temperature(
                    temperature = hourlyForecast.t?.roundToInt(),
                    windChillTemperature = hourlyForecast.tWindchill?.roundToInt()
                ),
                precipitation = getHourlyPrecipitation(hourlyForecast),
                precipitationProbability = if (forecastResult.properties.probabilityForecast != null) getHourlyPrecipitationProbability(
                    forecastResult.properties.probabilityForecast,
                    hourlyForecast.time
                ) else null,
                wind = Wind(
                    direction = hourlyForecast.windIcon,
                    degree = if (hourlyForecast.windDirection != null) WindDegree(
                        hourlyForecast.windDirection.toFloat(),
                        hourlyForecast.windDirection == -1
                    ) else null,
                    speed = hourlyForecast.windSpeed?.times(3.6f),
                    level = getWindLevel(context, hourlyForecast.windSpeed?.times(3.6f))
                ),
                airQuality = getAirQuality(hourlyForecast.time, aqiAtmoAuraResult)
            )

            // We shift by 6 hours the hourly date, otherwise nighttime (00:00 to 05:59) would be on the wrong day
            val theDayAtMidnight = DisplayUtils.toTimezoneNoHour(
                Date(hourlyForecast.time.time - (6 * 3600 * 1000)),
                location.timeZone
            )
            val theDayFormatted =
                DisplayUtils.getFormattedDate(theDayAtMidnight, location.timeZone, "yyyyMMdd")
            if (!hourlyByHalfDay.containsKey(theDayFormatted)) {
                hourlyByHalfDay[theDayFormatted] = hashMapOf(
                    "day" to ArrayList(),
                    "night" to ArrayList()
                )
            }
            if (hourlyForecast.time.time < theDayAtMidnight.time + 18 * 3600 * 1000) {
                // 06:00 to 17:59 is the day
                hourlyByHalfDay[theDayFormatted]!!["day"]!!.add(hourly)
            } else {
                // 18:00 to 05:59 is the night
                hourlyByHalfDay[theDayFormatted]!!["night"]!!.add(hourly)
            }

            // Add to the app only if starts in the current hour
            if (hourlyForecast.time.time >= System.currentTimeMillis() - 3600 * 1000) {
                hourlyList.add(hourly)
            }
        }
        val dailyList = getDailyList(context, location.timeZone, forecastResult.properties.dailyForecast, ephemerisResult.properties?.ephemeris, hourlyList, hourlyByHalfDay)
        val weather = Weather(
            base = Base(
                cityId = location.cityId,
                publishDate = forecastResult.updateTime ?: Date()
            ),
            current = Current(
                weatherText = currentResult.properties?.gridded?.weatherDescription,
                weatherCode = getWeatherCode(currentResult.properties?.gridded?.weatherIcon),
                temperature = Temperature(
                    temperature = currentResult.properties?.gridded?.temperature?.roundToInt() ?: hourlyList.getOrNull(1)?.temperature?.temperature
                ),
                wind = if (currentResult.properties?.gridded != null) Wind(
                    direction = currentResult.properties.gridded.windIcon,
                    degree = WindDegree(
                        degree = currentResult.properties.gridded.windDirection?.toFloat(),
                        isNoDirection = currentResult.properties.gridded.windDirection == -1
                    ),
                    speed = currentResult.properties.gridded.windSpeed?.times(3.6f),
                    level = getWindLevel(context, currentResult.properties.gridded.windSpeed?.times(3.6f))
                ) else null,
                uV = getCurrentUV(
                    context,
                    dailyList.getOrNull(0)?.uV?.index,
                    Date(),
                    dailyList.getOrNull(0)?.sun?.riseDate,
                    dailyList.getOrNull(0)?.sun?.setDate,
                    location.timeZone
                ),
                airQuality = hourlyList.getOrNull(1)?.airQuality,
            ),
            dailyForecast = dailyList,
            hourlyForecast = completeHourlyListFromDailyList(context, hourlyList, dailyList, location.timeZone, completeDaylight = true),
            minutelyForecast = getMinutelyList(rainResult),
            alertList = getWarningsList(warningsResult)
        )
        WeatherResultWrapper(weather)
    } catch (e: Exception) {
        if (BreezyWeather.instance.debugMode) {
            e.printStackTrace()
        }
        WeatherResultWrapper(null)
    }
}

private fun getDailyList(
    context: Context,
    timeZone: TimeZone,
    dailyForecasts: List<MfForecastDaily>,
    ephemerisResult: MfEphemeris?,
    hourlyList: List<Hourly>,
    hourlyListByHalfDay: Map<String, Map<String, MutableList<Hourly>>>
): List<Daily> {
    val dailyList: MutableList<Daily> = ArrayList(dailyForecasts.size)
    val hourlyListByDay = hourlyList.groupBy { DisplayUtils.getFormattedDate(it.date, timeZone, "yyyyMMdd") }
    for (dailyForecast in dailyForecasts) {
        // Given as UTC, we need to convert in the correct timezone at 00:00
        val dayInUTCCalendar = DisplayUtils.toCalendarWithTimeZone(dailyForecast.time, TimeZone.getTimeZone("UTC"))
        val dayInLocalCalendar = Calendar.getInstance(timeZone)
        dayInLocalCalendar[Calendar.YEAR] = dayInUTCCalendar[Calendar.YEAR]
        dayInLocalCalendar[Calendar.MONTH] = dayInUTCCalendar[Calendar.MONTH]
        dayInLocalCalendar[Calendar.DAY_OF_MONTH] = dayInUTCCalendar[Calendar.DAY_OF_MONTH]
        dayInLocalCalendar[Calendar.HOUR_OF_DAY] = 0
        dayInLocalCalendar[Calendar.MINUTE] = 0
        dayInLocalCalendar[Calendar.SECOND] = 0
        val theDayInLocal = dayInLocalCalendar.time
        val dailyDateFormatted = DisplayUtils.getFormattedDate(theDayInLocal, timeZone, "yyyyMMdd")
        dailyList.add(
            Daily(
                date = theDayInLocal,
                day = completeHalfDayFromHourlyList(
                    dailyDate = theDayInLocal,
                    initialHalfDay = HalfDay(
                        // Too complicated to get weather from hourly, so let's just use daily info for both day and night
                        weatherText = dailyForecast.dailyWeatherDescription,
                        weatherPhase = dailyForecast.dailyWeatherDescription,
                        weatherCode = getWeatherCode(dailyForecast.dailyWeatherIcon),
                        temperature = Temperature(temperature = dailyForecast.tMax?.roundToInt())
                        // TODO cloudCover with hourly data
                    ),
                    halfDayHourlyList = hourlyListByHalfDay.getOrDefault(dailyDateFormatted, null)?.get("day"),
                    isDay = true
                ),
                night = completeHalfDayFromHourlyList(
                    dailyDate = theDayInLocal,
                    initialHalfDay = HalfDay(
                        weatherText = dailyForecast.dailyWeatherDescription,
                        weatherPhase = dailyForecast.dailyWeatherDescription,
                        weatherCode = getWeatherCode(dailyForecast.dailyWeatherIcon),
                        temperature = Temperature(temperature = dailyForecast.tMin?.roundToInt())
                        // TODO cloudCover with hourly data
                    ),
                    halfDayHourlyList = hourlyListByHalfDay.getOrDefault(dailyDateFormatted, null)?.get("night"),
                    isDay = false
                ),
                sun = Astro(
                    riseDate = dailyForecast.sunriseTime,
                    setDate = dailyForecast.sunsetTime
                ),
                moon = Astro( // FIXME: It's valid only for the first day
                    riseDate = ephemerisResult?.moonriseTime,
                    setDate = ephemerisResult?.moonsetTime
                ),
                moonPhase = MoonPhase( // FIXME: It's valid only for the first day
                    angle = getMoonPhaseAngle(ephemerisResult?.moonPhaseDescription),
                    description = ephemerisResult?.moonPhaseDescription
                ),
                airQuality = getDailyAirQualityFromHourlyList(hourlyListByDay.getOrDefault(dailyDateFormatted, null)),
                uV = UV(
                    index = dailyForecast.uvIndex,
                    level = getUVLevel(context, dailyForecast.uvIndex)
                ),
                hoursOfSun = getHoursOfDay(dailyForecast.sunriseTime, dailyForecast.sunsetTime)
            )
        )
    }
    return dailyList
}

// This can be improved by adding results from other regions
private fun getAirQuality(requestedDate: Date, aqiAtmoAuraResult: AtmoAuraPointResult?): AirQuality? {
    if (aqiAtmoAuraResult == null) return null

    var pm25: Float? = null
    var pm10: Float? = null
    var so2: Float? = null
    var no2: Float? = null
    var o3: Float? = null
    aqiAtmoAuraResult.polluants
        ?.filter { p -> p.horaires?.firstOrNull { it.datetimeEcheance == requestedDate } != null }
        ?.forEach { p -> when (p.polluant) {
                "o3" -> o3 = p.horaires?.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                "no2" -> no2 = p.horaires?.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                "pm2.5" -> pm25 = p.horaires?.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                "pm10" -> pm10 = p.horaires?.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
                "so2" -> so2 = p.horaires?.firstOrNull { it.datetimeEcheance == requestedDate }?.concentration?.toFloat()
            }
        }

    // Return null instead of an object initialized with null values to ease the filtering later when aggregating for daily
    return if (pm25 != null || pm10 != null || so2 != null || no2 != null || o3 != null) AirQuality(
        pM25 = pm25,
        pM10 = pm10,
        sO2 = so2,
        nO2 = no2,
        o3 = o3
    ) else null
}

private fun getHourlyPrecipitation(hourlyForecast: MfForecastHourly): Precipitation {
    val rainCumul = with (hourlyForecast) {
        rain1h ?: rain3h ?: rain6h ?: rain12h ?: rain24h
    }
    val snowCumul = with (hourlyForecast) {
        snow1h ?: snow3h ?: snow6h ?: snow12h ?: snow24h
    }
    val cumul: List<Float> = listOfNotNull(rainCumul, snowCumul)
    return Precipitation(
        total = if (cumul.isNotEmpty()) cumul.sum() else null,
        rain = rainCumul,
        snow = snowCumul
    )
}

/**
 * TODO: Needs to be reviewed
 */
private fun getHourlyPrecipitationProbability(
    probabilityForecastResult: List<MfForecastProbability>,
    dt: Date
): PrecipitationProbability {
    var rainProbability: Float? = null
    var snowProbability: Float? = null
    var iceProbability: Float? = null
    for (probabilityForecast in probabilityForecastResult) {
        /*
         * Probablity are given every 3 hours, sometimes every 6 hours.
         * Sometimes every 3 hour-schedule give 3 hours probability AND 6 hours probability,
         * sometimes only one of them
         * It's not very clear, but we take all hours in order.
         */
        if (probabilityForecast.time.time == dt.time || probabilityForecast.time.time + 3600 * 1000 == dt.time || probabilityForecast.time.time + 3600 * 2 * 1000 == dt.time) {
            if (probabilityForecast.rainHazard3h != null) {
                rainProbability = probabilityForecast.rainHazard3h.toFloat()
            } else if (probabilityForecast.rainHazard6h != null) {
                rainProbability = probabilityForecast.rainHazard6h.toFloat()
            }
            if (probabilityForecast.snowHazard3h != null) {
                snowProbability = probabilityForecast.snowHazard3h.toFloat()
            } else if (probabilityForecast.snowHazard6h != null) {
                snowProbability = probabilityForecast.snowHazard6h.toFloat()
            }
            if (probabilityForecast.freezingHazard != null) {
                iceProbability = probabilityForecast.freezingHazard.toFloat()
            }
        }

        /*
         * If it's found as part of the "6 hour schedule" and we find later a "3 hour schedule"
         * the "3 hour schedule" will overwrite the "6 hour schedule" below with the above
         */
        if (probabilityForecast.time.time + 3600 * 3 * 1000 == dt.time || probabilityForecast.time.time + 3600 * 4 * 1000 == dt.time || probabilityForecast.time.time + 3600 * 5 * 1000 == dt.time) {
            if (probabilityForecast.rainHazard6h != null) {
                rainProbability = probabilityForecast.rainHazard6h.toFloat()
            }
            if (probabilityForecast.snowHazard6h != null) {
                snowProbability = probabilityForecast.snowHazard6h.toFloat()
            }
            if (probabilityForecast.freezingHazard != null) {
                iceProbability = probabilityForecast.freezingHazard.toFloat()
            }
        }
    }
    return PrecipitationProbability(
        maxOf(rainProbability ?: 0f, snowProbability ?: 0f, iceProbability ?: 0f),
        null,
        rainProbability,
        snowProbability,
        iceProbability
    )
}

private fun getMinutelyList(rainResult: MfRainResult?): List<Minutely> {
    val minutelyList: MutableList<Minutely> = arrayListOf()
    rainResult?.properties?.rainForecasts?.forEachIndexed { i, rainForecast ->
        minutelyList.add(
            Minutely(
                rainForecast.time,
                rainForecast.rainIntensityDescription,
                if (rainForecast.rainIntensity != null && rainForecast.rainIntensity > 1) WeatherCode.RAIN else null,
                if (i < rainResult.properties.rainForecasts.size - 1) {
                    ((rainResult.properties.rainForecasts[i + 1].time.time - rainForecast.time.time) / (60 * 1000)).toDouble()
                        .roundToInt()
                } else ((rainForecast.time.time - rainResult.properties.rainForecasts[i - 1].time.time) / (60 * 1000)).toDouble()
                    .roundToInt(),
                if (rainForecast.rainIntensity != null) getPrecipitationIntensity(rainForecast.rainIntensity) else null,
                null
            )
        )
    }
    return minutelyList
}

private fun getWarningsList(warningsResult: MfWarningsResult): List<Alert> {
    val alertList: MutableList<Alert> = arrayListOf()
    warningsResult.timelaps?.forEach { timelaps ->
        timelaps.timelapsItems
            ?.filter { it.colorId > 1 }
            ?.forEach { timelapsItem ->
                alertList.add(
                    Alert(
                        // Create unique ID from alert type ID concatenated with start time
                        (timelaps.phenomenonId + timelapsItem.beginTime.time.toString()).toLong(),
                        timelapsItem.beginTime,
                        timelapsItem.endTime,
                        getWarningType(timelaps.phenomenonId) + " — " + getWarningText(timelapsItem.colorId),
                        if (timelapsItem.colorId >= 3) getWarningContent(
                            timelaps.phenomenonId,
                            warningsResult
                        ) else null,
                        getWarningType(timelaps.phenomenonId),
                        timelapsItem.colorId.times(-1) // Reverse, as lower is better
                    )
                )
            }
    }
    return alertList.sortedWith(compareBy({ it.priority }, { it.startDate }))
}

private fun getPrecipitationIntensity(rain: Int): Double {
    return when (rain) {
        4 -> 10.0
        3 -> 5.5
        2 -> 2.0
        else -> 0.0
    }
}

private fun getWarningType(phemononId: String): String {
    return when (phemononId) {
        "1" -> "Vent"
        "2" -> "Pluie-Inondation"
        "3" -> "Orages"
        "4" -> "Crues"
        "5" -> "Neige-Verglas"
        "6" -> "Canicule"
        "7" -> "Grand Froid"
        "8" -> "Avalanches"
        "9" -> "Vagues-Submersion"
        else -> "Divers"
    }
}

private fun getWarningText(colorId: Int): String {
    return when (colorId) {
        4 -> "Vigilance absolue"
        3 -> "Soyez très vigilant"
        2 -> "Soyez attentif"
        else -> "Pas de vigilance particulière"
    }
}

private fun getWarningContent(phenomenonId: String, warningsResult: MfWarningsResult): String? {
    val consequences = warningsResult.consequences?.firstOrNull { it.phenomenonId == phenomenonId }?.textConsequence?.replace("<br>", "\n")
    val advices = warningsResult.advices?.firstOrNull { it.phenomenonId == phenomenonId }?.textAdvice?.replace("<br>", "\n")

    val content = StringBuilder()
    if (!consequences.isNullOrEmpty()) {
        content
            .append("CONSÉQUENCES POSSIBLES\n")
            .append(consequences)
    }
    if (!advices.isNullOrEmpty()) {
        if (content.toString().isNotEmpty()) {
            content.append("\n\n")
        }
        content
            .append("CONSEILS DE COMPORTEMENT\n")
            .append(advices)
    }

    // There are also text blocks with hour by hour evaluation, but it’s way too detailed

    return content.toString().ifEmpty { null }
}

private fun getWeatherCode(icon: String?): WeatherCode? {
    return if (icon == null) {
        null
    } else with (icon) {
        when {
            // We need to take care of two-digits first
            startsWith("p32") || startsWith("p33")
                    || startsWith("p34") -> WeatherCode.WIND
            startsWith("p31") -> null // What is this?
            startsWith("p26") || startsWith("p27") || startsWith("p28")
                    || startsWith("p29") -> WeatherCode.THUNDER
            startsWith("p26") || startsWith("p27") || startsWith("p28")
                    || startsWith("p29") -> WeatherCode.THUNDER
            startsWith("p21") || startsWith("p22")
                    || startsWith("p23") -> WeatherCode.SNOW
            startsWith("p19") || startsWith("p20") -> WeatherCode.HAIL
            startsWith("p17") || startsWith("p18") -> WeatherCode.SLEET
            startsWith("p16") || startsWith("p24")
                    || startsWith("p25") || startsWith("p30") -> WeatherCode.THUNDERSTORM
            startsWith("p9") || startsWith("p10") || startsWith("p11")
                    || startsWith("p12") || startsWith("p13")
                    || startsWith("p14") || startsWith("p15") -> WeatherCode.RAIN
            startsWith("p6") || startsWith("p7")
                    || startsWith("p8") -> WeatherCode.FOG
            startsWith("p4") || startsWith("p5") -> WeatherCode.HAZE
            startsWith("p3") -> WeatherCode.CLOUDY
            startsWith("p2") -> WeatherCode.PARTLY_CLOUDY
            startsWith("p1") -> WeatherCode.CLEAR
            else -> null
        }
    }
}

fun getFrenchDepartmentName(frenchDepartmentCode: String): String? {
    return getFrenchDepartments().firstOrNull { it.first == frenchDepartmentCode }?.second
}

fun getFrenchDepartmentCode(frenchDepartmentName: String): String? {
    return getFrenchDepartments().firstOrNull { it.second == frenchDepartmentName }?.first
}

fun getFrenchDepartments(): List<Pair<String, String>> {
    return listOf(
        Pair("01", "Ain"),
        Pair("02", "Aisne"),
        Pair("03", "Allier"),
        Pair("04", "Alpes de Hautes-Provence"),
        Pair("05", "Hautes-Alpes"),
        Pair("06", "Alpes-Maritimes"),
        Pair("07", "Ardèche"),
        Pair("08", "Ardennes"),
        Pair("09", "Ariège"),
        Pair("10", "Aube"),
        Pair("11", "Aude"),
        Pair("12", "Aveyron"),
        Pair("13", "Bouches-du-Rhône"),
        Pair("14", "Calvados"),
        Pair("15", "Cantal"),
        Pair("16", "Charente"),
        Pair("17", "Charente-Maritime"),
        Pair("18", "Cher"),
        Pair("19", "Corrèze"),
        Pair("21", "Côte-d'Or"),
        Pair("22", "Côtes d'Armor"),
        Pair("23", "Creuse"),
        Pair("24", "Dordogne"),
        Pair("25", "Doubs"),
        Pair("26", "Drôme"),
        Pair("27", "Eure"),
        Pair("28", "Eure-et-Loir"),
        Pair("29", "Finistère"),
        Pair("2A", "Corse-du-Sud"),
        Pair("2B", "Haute-Corse"),
        Pair("30", "Gard"),
        Pair("31", "Haute-Garonne"),
        Pair("32", "Gers"),
        Pair("33", "Gironde"),
        Pair("34", "Hérault"),
        Pair("35", "Ille-et-Vilaine"),
        Pair("36", "Indre"),
        Pair("37", "Indre-et-Loire"),
        Pair("38", "Isère"),
        Pair("39", "Jura"),
        Pair("40", "Landes"),
        Pair("41", "Loir-et-Cher"),
        Pair("42", "Loire"),
        Pair("43", "Haute-Loire"),
        Pair("44", "Loire-Atlantique"),
        Pair("45", "Loiret"),
        Pair("46", "Lot"),
        Pair("47", "Lot-et-Garonne"),
        Pair("48", "Lozère"),
        Pair("49", "Maine-et-Loire"),
        Pair("50", "Manche"),
        Pair("51", "Marne"),
        Pair("52", "Haute-Marne"),
        Pair("53", "Mayenne"),
        Pair("54", "Meurthe-et-Moselle"),
        Pair("55", "Meuse"),
        Pair("56", "Morbihan"),
        Pair("57", "Moselle"),
        Pair("58", "Nièvre"),
        Pair("59", "Nord"),
        Pair("60", "Oise"),
        Pair("61", "Orne"),
        Pair("62", "Pas-de-Calais"),
        Pair("63", "Puy-de-Dôme"),
        Pair("64", "Pyrénées-Atlantiques"),
        Pair("65", "Hautes-Pyrénées"),
        Pair("66", "Pyrénées-Orientales"),
        Pair("67", "Bas-Rhin"),
        Pair("68", "Haut-Rhin"),
        Pair("69", "Rhône"),
        Pair("70", "Haute-Saône"),
        Pair("71", "Saône-et-Loire"),
        Pair("72", "Sarthe"),
        Pair("73", "Savoie"),
        Pair("74", "Haute-Savoie"),
        Pair("75", "Paris"),
        Pair("76", "Seine-Maritime"),
        Pair("77", "Seine-et-Marne"),
        Pair("78", "Yvelines"),
        Pair("79", "Deux-Sèvres"),
        Pair("80", "Somme"),
        Pair("81", "Tarn"),
        Pair("82", "Tarn-et-Garonne"),
        Pair("83", "Var"),
        Pair("84", "Vaucluse"),
        Pair("85", "Vendée"),
        Pair("86", "Vienne"),
        Pair("87", "Haute-Vienne"),
        Pair("88", "Vosges"),
        Pair("89", "Yonne"),
        Pair("90", "Territoire-de-Belfort"),
        Pair("91", "Essonne"),
        Pair("92", "Hauts-de-Seine"),
        Pair("93", "Seine-Saint-Denis"),
        Pair("94", "Val-de-Marne"),
        Pair("95", "Val-d'Oise"),
        Pair("99", "Andorre")
    )
}