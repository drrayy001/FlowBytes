package com.ray.flowmeter.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

object LocaleHelper {
    fun applyLocale(context: Context, languageCode: String): Context {
        val locale = if (languageCode.isEmpty()) {
            val systemLocales = Resources.getSystem().configuration.locales
            if (!systemLocales.isEmpty) systemLocales.get(0) else Locale.getDefault()
        } else {
            Locale.forLanguageTag(languageCode)
        }

        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        @Suppress("DEPRECATION")
        context.resources.updateConfiguration(config, context.resources.displayMetrics)

        val appContext = context.applicationContext
        if (appContext != null && appContext !== context) {
            @Suppress("DEPRECATION")
            appContext.resources.updateConfiguration(config, appContext.resources.displayMetrics)
        }

        return context.createConfigurationContext(config)
    }
}
