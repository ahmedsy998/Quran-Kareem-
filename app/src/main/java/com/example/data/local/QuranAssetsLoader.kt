package com.example.data.local

import android.content.Context
import com.example.domain.model.Ayah
import com.example.domain.model.Surah
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray

class QuranAssetsLoader(private val context: Context) {
    private val loadMutex = Mutex()
    @Volatile
    private var cachedSurahs: List<Surah>? = null

    suspend fun getAllSurahs(): List<Surah> {
        cachedSurahs?.let { return it }
        return loadMutex.withLock {
            cachedSurahs?.let { return@withLock it }
            val loaded = withContext(Dispatchers.IO) {
                val jsonString = context.assets.open("quran/quran_uthmani.json")
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                val rootArray = JSONArray(jsonString)
                val result = ArrayList<Surah>(rootArray.length())
                for (i in 0 until rootArray.length()) {
                    val sObj = rootArray.getJSONObject(i)
                    val ayahsArr = sObj.getJSONArray("ayahs")
                    val ayahs = ArrayList<Ayah>(ayahsArr.length())
                    for (j in 0 until ayahsArr.length()) {
                        val aObj = ayahsArr.getJSONObject(j)
                        ayahs.add(
                            Ayah(
                                absoluteNumber = aObj.getInt("n"),
                                numberInSurah = aObj.getInt("i"),
                                text = aObj.getString("t"),
                                page = aObj.optInt("p", 1),
                                juz = aObj.optInt("j", 1)
                            )
                        )
                    }
                    result.add(
                        Surah(
                            number = sObj.getInt("n"),
                            arabicName = sObj.getString("name"),
                            englishName = sObj.optString("en", ""),
                            revelationType = sObj.optString("type", "Meccan"),
                            ayahs = ayahs
                        )
                    )
                }
                result
            }
            cachedSurahs = loaded
            loaded
        }
    }

    suspend fun getSurah(number: Int): Surah {
        val all = getAllSurahs()
        return all.firstOrNull { it.number == number } ?: all.first()
    }

    companion object {
        const val BISMILLAH_TEXT = "بِسْمِ ٱللَّهِ ٱلرَّحْمَٰنِ ٱلرَّحِيمِ"
    }
}
