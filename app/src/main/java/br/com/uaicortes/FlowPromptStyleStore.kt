package br.com.uaicortes

import android.content.Context

internal class FlowPromptStyleStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("flow_style_v1", Context.MODE_PRIVATE)
    fun load(): String = FlowPromptPolicy.normalizeStyle(prefs.getString("base", null).orEmpty())
    fun save(style: String): Boolean = prefs.edit()
        .putString("base", FlowPromptPolicy.normalizeStyle(style)).commit()
}
