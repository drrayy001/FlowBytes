// Locale manager utility providing context wrapping to dynamically switch language resources at runtime.
package com.ray.flowmeter.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.Locale

class LocaleContextWrapper(
    base: Context,
    private val realContext: Context
) : android.content.ContextWrapper(base) {
    override fun getBaseContext(): Context {
        return realContext
    }
}

object LocaleHelper {
    fun applyLocale(context: Context, languageCode: String): Context {
        val originalContext = if (context is LocaleContextWrapper) context.baseContext else context

        val locale = if (languageCode.isEmpty()) {
            val systemLocales = Resources.getSystem().configuration.locales
            if (!systemLocales.isEmpty) systemLocales.get(0) else Locale.getDefault()
        } else {
            Locale.forLanguageTag(languageCode)
        }

        Locale.setDefault(locale)

        val config = Configuration(originalContext.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)

        @Suppress("DEPRECATION")
        originalContext.resources.updateConfiguration(config, originalContext.resources.displayMetrics)

        val appContext = originalContext.applicationContext
        if (appContext != null && appContext !== originalContext) {
            @Suppress("DEPRECATION")
            appContext.resources.updateConfiguration(config, appContext.resources.displayMetrics)
        }

        val configContext = originalContext.createConfigurationContext(config)
        return LocaleContextWrapper(configContext, originalContext)
    }
}
