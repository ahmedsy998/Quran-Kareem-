package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Mp3QuranRecitersResponse(
    val reciters: List<Mp3QuranReciterDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Mp3QuranReciterDto(
    val id: Int,
    val name: String,
    val moshaf: List<Mp3QuranMoshafDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class Mp3QuranMoshafDto(
    val id: Int = 0,
    val name: String = "",
    val server: String = "",
    @Json(name = "surah_total") val surahTotal: Int = 114,
    @Json(name = "surah_list") val surahList: String = ""
)

@JsonClass(generateAdapter = true)
data class AyahTimingDto(
    val ayah: Int,
    @Json(name = "start_time") val startTimeMs: Long = 0L,
    @Json(name = "end_time") val endTimeMs: Long = 0L
)

interface Mp3QuranService {
    @GET("api/v3/reciters")
    suspend fun getReciters(@Query("language") language: String = "ar"): Mp3QuranRecitersResponse

    @GET("api/v3/ayat_timing")
    suspend fun getAyahTiming(
        @Query("surah") surah: Int,
        @Query("read") readId: Int
    ): List<AyahTimingDto>
}

object NetworkModule {
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val mp3QuranService: Mp3QuranService by lazy {
        Retrofit.Builder()
            .baseUrl("https://www.mp3quran.net/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(Mp3QuranService::class.java)
    }
}
