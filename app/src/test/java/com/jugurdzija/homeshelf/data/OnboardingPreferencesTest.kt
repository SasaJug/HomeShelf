package com.jugurdzija.homeshelf.data

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OnboardingPreferencesTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var onboardingPreferences: OnboardingPreferencesImpl

    @Before
    fun setUp() {
        prefs = mockk()
        editor = mockk(relaxed = true)
        every { editor.putBoolean(any(), any()) } returns editor
        every { prefs.edit() } returns editor

        val context = mockk<Context>()
        every { context.getSharedPreferences(any(), any()) } returns prefs

        onboardingPreferences = OnboardingPreferencesImpl(context)
    }

    @Test
    fun `hasSeenIntro defaults to false when nothing persisted`() {
        every { prefs.getBoolean(any(), false) } returns false

        assertFalse(onboardingPreferences.hasSeenIntro())
    }

    @Test
    fun `hasSeenIntro returns a persisted true value`() {
        every { prefs.getBoolean(any(), false) } returns true

        assertTrue(onboardingPreferences.hasSeenIntro())
    }

    @Test
    fun `markIntroSeen persists true`() {
        onboardingPreferences.markIntroSeen()

        verify { editor.putBoolean(any(), true) }
    }
}
