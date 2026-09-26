package com.jugurdzija.homeshelf.aipipeline.llm

import android.content.Context
import androidx.annotation.RawRes

internal fun Context.readPromptResource(@RawRes resourceId: Int): String =
    resources.openRawResource(resourceId).bufferedReader().use { it.readText() }
