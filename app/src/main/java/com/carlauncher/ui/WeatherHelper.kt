package com.carlauncher.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

object WeatherHelper {

    data class WeatherData(val tempC: Int, val description: String, val icon: String)

    private val WMO_MAP = mapOf(
        0  to Pair("晴天",   "☀"), 1  to Pair("基本晴", "🌤"),
        2  to Pair("多云",   "⛅"), 3  to Pair("阴天",   "☁"),
        45 to Pair("雾",     "🌫"), 51 to Pair("毛毛雨", "🌦"),
        61 to Pair("小雨",   "🌧"), 63 to Pair("中雨",   "🌧"),
        65 to Pair("大雨",   "🌧"), 71 to Pair("小雪",   "🌨"),
        73 to Pair("中雪",   "❄"),  75 to Pair("大雪",   "❄"),
        80 to Pair("阵雨",   "🌦"), 95 to Pair("雷阵雨", "⛈")
    )

    suspend fun fetch(lat: Double, lon: Double): WeatherData? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$lat&longitude=$lon&current_weather=true&temperature_unit=celsius"
            val json = JSONObject(URL(url).readText())
            val cur  = json.getJSONObject("current_weather")
            val temp = cur.getDouble("temperature").toInt()
            val code = cur.getInt("weathercode")
            val (desc, icon) = WMO_MAP[code] ?: Pair("未知", "🌡")
            WeatherData(temp, desc, icon)
        } catch (e: Exception) { null }
    }
}
