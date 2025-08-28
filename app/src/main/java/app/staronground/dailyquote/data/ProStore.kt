package app.staronground.dailyquote.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

object ProStore {
    private val KEY = booleanPreferencesKey("is_pro")
    fun flow(ctx: Context) = ctx.dataStore.data.map { it[KEY] ?: false }
    suspend fun set(ctx: Context, v: Boolean) { ctx.dataStore.edit { it[KEY] = v } }
}
