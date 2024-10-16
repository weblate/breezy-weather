package org.breezyweather.weather.apis

import io.reactivex.rxjava3.core.Observable
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import org.breezyweather.weather.json.accu.*

/**
 * Accu api.
 */
interface AccuWeatherApi {
    @GET("locations/v1/translate")
    fun callWeatherLocation(
        @Query("apikey") apikey: String,
        @Query("q") q: String,
        @Query("language") language: String,
        @Query("details") details: Boolean,
        @Query("alias") alias: String
    ): Call<List<AccuLocationResult>>

    @GET("locations/v1/translate")
    fun getWeatherLocation(
        @Query("apikey") apikey: String,
        @Query("q") q: String,
        @Query("language") language: String,
        @Query("details") details: Boolean,
        @Query("alias") alias: String
    ): Observable<List<AccuLocationResult>>

    @GET("locations/v1/cities/geoposition/search")
    fun getWeatherLocationByGeoPosition(
        @Query("apikey") apikey: String,
        @Query("language") language: String,
        @Query("details") details: Boolean,
        @Query("q") q: String
    ): Observable<AccuLocationResult>

    @GET("currentconditions/v1/{city_key}")
    fun getCurrent(
        @Path("city_key") city_key: String,
        @Query("apikey") apikey: String,
        @Query("language") language: String,
        @Query("details") details: Boolean
    ): Observable<List<AccuCurrentResult>>

    @GET("forecasts/v1/daily/15day/{city_key}")
    fun getDaily(
        @Path("city_key") city_key: String,
        @Query("apikey") apikey: String,
        @Query("language") language: String,
        @Query("details") details: Boolean,
        @Query("metric") metric: Boolean
    ): Observable<AccuForecastDailyResult>

    @GET("forecasts/v1/hourly/120hour/{city_key}")
    fun getHourly(
        @Path("city_key") city_key: String,
        @Query("apikey") apikey: String,
        @Query("language") language: String,
        @Query("details") details: Boolean,
        @Query("metric") metric: Boolean
    ): Observable<List<AccuForecastHourlyResult>>

    @GET("forecasts/v1/minute/1minute")
    fun getMinutely(
        @Query("apikey") apikey: String,
        @Query("q") q: String,
        @Query("language") language: String,
        @Query("details") details: Boolean
    ): Observable<AccuMinutelyResult>

    @GET("alerts/v1/geoposition")
    fun getAlert(
        @Query("apikey") apikey: String,
        @Query("q") q: String,
        @Query("language") language: String,
        @Query("details") details: Boolean
    ): Observable<List<AccuAlertResult>>

    /*@GET("climo/v1/summary/{year}/{month}/{city_key}")
    fun getClimo(
        @Path("year") year: Int,
        @Path("month") month: Int,
        @Path("city_key") city_key: String,
        @Query("apikey") apikey: String,
        @Query("language") language: String,
        @Query("details") details: Boolean
    ): Observable<List<AccuClimoResult>>*/
}