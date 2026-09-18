package com.jugurdzija.homeshelf.data.onboarding

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface OnboardingRepository {
    fun hasSeenIntro(): Boolean
    fun markIntroSeen()
}

private const val PREFS_NAME = "onboarding_preferences"
private const val KEY_HAS_SEEN_INTRO = "has_seen_intro"

@Singleton
class OnboardingRepositoryImpl @Inject constructor(
    @ApplicationContext context: Context
) : OnboardingRepository {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override fun hasSeenIntro(): Boolean = prefs.getBoolean(KEY_HAS_SEEN_INTRO, false)

    override fun markIntroSeen() {
        prefs.edit { putBoolean(KEY_HAS_SEEN_INTRO, true) }
    }
}
