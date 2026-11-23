package com.example.domain.model

enum class AudioSourceType {
    ISLAMIC_NETWORK_128,
    ISLAMIC_NETWORK_64,
    EVERYAYAH,
    MP3QURAN_TIMED,
    MP3QURAN_FULL
}

data class Reciter(
    val id: String,
    val arabicName: String,
    val styleLabel: String = "مرتل",
    val sourceType: AudioSourceType,
    val editionOrFolder: String,
    val mp3QuranReadId: Int? = null,
    val mp3QuranServerUrl: String? = null,
    val quranComRecitationId: Int? = null,
    val isFavorite: Boolean = false
) {
    val supportsExactPerAyah: Boolean
        get() = sourceType == AudioSourceType.ISLAMIC_NETWORK_128 ||
            sourceType == AudioSourceType.ISLAMIC_NETWORK_64 ||
            sourceType == AudioSourceType.EVERYAYAH

    fun perAyahUrl(surahNumber: Int, ayahInSurah: Int, absoluteAyahNumber: Int): String? {
        return when (sourceType) {
            AudioSourceType.ISLAMIC_NETWORK_128 ->
                "https://cdn.islamic.network/quran/audio/128/$editionOrFolder/$absoluteAyahNumber.mp3"
            AudioSourceType.ISLAMIC_NETWORK_64 ->
                "https://cdn.islamic.network/quran/audio/64/$editionOrFolder/$absoluteAyahNumber.mp3"
            AudioSourceType.EVERYAYAH -> {
                val s = surahNumber.toString().padStart(3, '0')
                val a = ayahInSurah.toString().padStart(3, '0')
                "https://everyayah.com/data/$editionOrFolder/${s}${a}.mp3"
            }
            AudioSourceType.MP3QURAN_TIMED, AudioSourceType.MP3QURAN_FULL -> null
        }
    }

    fun fullSurahUrl(surahNumber: Int): String? {
        val base = mp3QuranServerUrl ?: return null
        val normalized = if (base.endsWith("/")) base else "$base/"
        val s = surahNumber.toString().padStart(3, '0')
        return "$normalized$s.mp3"
    }
}

object DefaultReciters {
    val list: List<Reciter> = listOf(
        Reciter(
            id = "alafasy",
            arabicName = "مشاري راشد العفاسي",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_128,
            editionOrFolder = "ar.alafasy",
            mp3QuranReadId = 123,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/mishary-alafasy/r2/",
            quranComRecitationId = 7
        ),
        Reciter(
            id = "sudais",
            arabicName = "عبد الرحمن السديس",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_64,
            editionOrFolder = "ar.abdurrahmaansudais",
            mp3QuranReadId = 54,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/abdulrahman-sudais/r1/",
            quranComRecitationId = 3
        ),
        Reciter(
            id = "ghamdi",
            arabicName = "سعد الغامدي",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.EVERYAYAH,
            editionOrFolder = "Ghamadi_40kbps",
            mp3QuranReadId = 30,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/saad-ghamdi/r1/"
        ),
        Reciter(
            id = "muaiqly",
            arabicName = "ماهر المعيقلي",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_128,
            editionOrFolder = "ar.mahermuaiqly",
            mp3QuranReadId = 133,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/maher-muaiqly/r3/"
        ),
        Reciter(
            id = "abdulbasit",
            arabicName = "عبد الباسط عبد الصمد",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_64,
            editionOrFolder = "ar.abdulbasitmurattal",
            mp3QuranReadId = 51,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/abdulbasit-abdulsamad/r3/",
            quranComRecitationId = 2
        ),
        Reciter(
            id = "minshawi",
            arabicName = "محمد صديق المنشاوي",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_128,
            editionOrFolder = "ar.minshawi",
            mp3QuranReadId = 112,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/muhammad-minshawi/r4/",
            quranComRecitationId = 9
        ),
        Reciter(
            id = "husary",
            arabicName = "محمود خليل الحصري",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_128,
            editionOrFolder = "ar.husary",
            mp3QuranReadId = 118,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/mahmoud-husary/r5/",
            quranComRecitationId = 6
        ),
        Reciter(
            id = "dosari",
            arabicName = "ياسر الدوسري",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.EVERYAYAH,
            editionOrFolder = "Yasser_Ad-Dussary_128kbps",
            mp3QuranReadId = 92,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/yasser-dosari/r1/"
        ),
        Reciter(
            id = "shuraim",
            arabicName = "سعود الشريم",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_64,
            editionOrFolder = "ar.saoodshuraym",
            mp3QuranReadId = 31,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/saud-shuraim/r1/",
            quranComRecitationId = 10
        ),
        Reciter(
            id = "juhany",
            arabicName = "عبد الله عواد الجهني",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.EVERYAYAH,
            editionOrFolder = "Abdullaah_3awwaad_Al-Juhaynee_128kbps",
            mp3QuranReadId = 62,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/abdullah-juhani/r1/"
        ),
        Reciter(
            id = "ajmi",
            arabicName = "أحمد بن علي العجمي",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_128,
            editionOrFolder = "ar.ahmedajamy",
            mp3QuranReadId = 5,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/ahmad-ajmi/r1/"
        ),
        Reciter(
            id = "jalil",
            arabicName = "خالد الجليل",
            styleLabel = "توقيت آيات معتمد",
            sourceType = AudioSourceType.MP3QURAN_TIMED,
            editionOrFolder = "khalid-jalil",
            mp3QuranReadId = 20,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/khalid-jalil/r1/"
        ),
        Reciter(
            id = "abbad",
            arabicName = "فارس عباد",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.EVERYAYAH,
            editionOrFolder = "Fares_Abbad_64kbps",
            mp3QuranReadId = 81,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/fares-abbad/r1/"
        ),
        Reciter(
            id = "qatami",
            arabicName = "ناصر القطامي",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.EVERYAYAH,
            editionOrFolder = "Nasser_Alqatami_128kbps",
            mp3QuranReadId = 86,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/nasser-qatami/r1/"
        ),
        Reciter(
            id = "luhaidan",
            arabicName = "محمد اللحيدان",
            styleLabel = "مصحف كامل",
            sourceType = AudioSourceType.MP3QURAN_FULL,
            editionOrFolder = "muhammad-luhaidan",
            mp3QuranReadId = 107,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/muhammad-luhaidan/r1/"
        ),
        Reciter(
            id = "hudhaify",
            arabicName = "علي عبد الرحمن الحذيفي",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_128,
            editionOrFolder = "ar.hudhaify",
            mp3QuranReadId = 74,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/ali-hudhaify/r1/"
        ),
        Reciter(
            id = "budair",
            arabicName = "صلاح البدير",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.EVERYAYAH,
            editionOrFolder = "Salah_Al_Budair_128kbps",
            mp3QuranReadId = 43,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/salah-budair/r1/"
        ),
        Reciter(
            id = "baleela",
            arabicName = "بندر بليلة",
            styleLabel = "توقيت آيات معتمد",
            sourceType = AudioSourceType.MP3QURAN_TIMED,
            editionOrFolder = "bandar-baleela",
            mp3QuranReadId = 217,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/bandar-baleela/r1/"
        ),
        Reciter(
            id = "ayyub",
            arabicName = "محمد أيوب",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_128,
            editionOrFolder = "ar.muhammadayyoub",
            mp3QuranReadId = 109,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/muhammad-ayyub/r2/"
        ),
        Reciter(
            id = "akhdar",
            arabicName = "إبراهيم الأخضر",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.EVERYAYAH,
            editionOrFolder = "Ibrahim_Akhdar_32kbps",
            mp3QuranReadId = 1,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/ibrahim-akhdar/r1/"
        ),
        Reciter(
            id = "alijaber",
            arabicName = "علي جابر",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.EVERYAYAH,
            editionOrFolder = "Ali_Jaber_64kbps",
            mp3QuranReadId = 76,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/ali-jaber/r1/"
        ),
        Reciter(
            id = "rifai",
            arabicName = "هاني الرفاعي",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_64,
            editionOrFolder = "ar.hanirifai",
            mp3QuranReadId = 89,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/hani-rifai/r1/",
            quranComRecitationId = 5
        ),
        Reciter(
            id = "abkar",
            arabicName = "إدريس أبكر",
            styleLabel = "مصحف كامل",
            sourceType = AudioSourceType.MP3QURAN_FULL,
            editionOrFolder = "idrees-abkar",
            mp3QuranReadId = 12,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/idrees-abkar/r1/"
        ),
        Reciter(
            id = "kurdi",
            arabicName = "رعد محمد الكردي",
            styleLabel = "توقيت آيات معتمد",
            sourceType = AudioSourceType.MP3QURAN_TIMED,
            editionOrFolder = "raad-kurdi",
            mp3QuranReadId = 221,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/raad-kurdi/r1/"
        ),
        // Extra famous reciters
        Reciter(
            id = "shatri",
            arabicName = "أبو بكر الشاطري",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_128,
            editionOrFolder = "ar.shaatree",
            mp3QuranReadId = 4,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/abu-bakr-shatri/r1/",
            quranComRecitationId = 4
        ),
        Reciter(
            id = "jibreel",
            arabicName = "محمد جبريل",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_128,
            editionOrFolder = "ar.muhammadjibreel",
            mp3QuranReadId = 111,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/muhammad-jibreel/r1/"
        ),
        Reciter(
            id = "basfar",
            arabicName = "عبد الله بصفر",
            styleLabel = "مرتل - آية بآية",
            sourceType = AudioSourceType.ISLAMIC_NETWORK_128,
            editionOrFolder = "ar.abdullahbasfar",
            mp3QuranReadId = 60,
            mp3QuranServerUrl = "https://cdn.mp3quran.net/audio/abdullah-basfar/r1/"
        )
    )
}
