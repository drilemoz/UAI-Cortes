package br.com.uaicortes

import android.content.Context
import org.json.JSONObject

internal class WatermarkDefaultsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("watermark_style_v1", Context.MODE_PRIVATE)
    fun load(): TextWatermark? = prefs.getString("preset", null)?.let {
        runCatching { TextWatermark.read(JSONObject(it)) }.getOrNull()
    }
    fun save(value: TextWatermark): Boolean = prefs.edit()
        .putString("preset", value.normalized().json().toString()).commit()
}
