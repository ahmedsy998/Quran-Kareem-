package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AppThemeStyle
import com.example.domain.model.ExportResolution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "quran_video_maker_prefs")

class PreferencesRepository(private val context: Context) {

    private object Keys {
        val ACTIVE_PROJECT_ID = stringPreferencesKey("active_project_id")
        val APP_THEME_STYLE = stringPreferencesKey("app_theme_style")
        val PREFERRED_RESOLUTION = stringPreferencesKey("preferred_resolution")
    }

    val activeProjectIdFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_PROJECT_ID]
    }

    val themeStyleFlow: Flow<AppThemeStyle> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.APP_THEME_STYLE]
        AppThemeStyle.entries.firstOrNull { it.name == raw } ?: AppThemeStyle.EMERALD_GOLD
    }

    val preferredResolutionFlow: Flow<ExportResolution> = context.dataStore.data.map { prefs ->
        val raw = prefs[Keys.PREFERRED_RESOLUTION]
        ExportResolution.entries.firstOrNull { it.name == raw } ?: ExportResolution.RES_720P
    }

    suspend fun getActiveProjectId(): String? = activeProjectIdFlow.first()

    suspend fun setActiveProjectId(projectId: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.ACTIVE_PROJECT_ID] = projectId
        }
    }

    suspend fun setThemeStyle(style: AppThemeStyle) {
        context.dataStore.edit { prefs ->
            prefs[Keys.APP_THEME_STYLE] = style.name
        }
    }

    suspend fun setPreferredResolution(resolution: ExportResolution) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PREFERRED_RESOLUTION] = resolution.name
        }
    }
}
