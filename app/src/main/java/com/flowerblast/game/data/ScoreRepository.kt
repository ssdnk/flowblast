package com.flowerblast.game.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "flower_blast_prefs")

class ScoreRepository(private val context: Context) {

    private val highScoreKey = intPreferencesKey("high_score")

    val highScore: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[highScoreKey] ?: 0
    }

    suspend fun updateHighScore(score: Int) {
        context.dataStore.edit { preferences ->
            val currentHigh = preferences[highScoreKey] ?: 0
            if (score > currentHigh) {
                preferences[highScoreKey] = score
            }
        }
    }
}