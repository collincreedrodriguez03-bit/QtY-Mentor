package com.example.repository

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun explainChart(chartSummary: String, isBeginnerMode: Boolean): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "Gemini API key is not configured. Please add your Gemini API key in the AI Studio Secrets panel to enable AI explanations."
        }

        val prompt = buildString {
            append("You are an educational AI technical analysis assistant for beginners learning Bitcoin price action. ")
            append("Analyze the following chart data summary and provide observations vs possible interpretations. ")
            append("CRITICAL RULES: Never guarantee future price movement (never say 'BTC will go up' or 'buy now'). ")
            append("Always distinguish observation from interpretation. Keep explanations clear, structured, and beginner-friendly.\n\n")
            append("Chart Data Summary:\n$chartSummary\n\n")
            if (isBeginnerMode) {
                append("Explain in very simple terms with definitions for any technical terms used.")
            } else {
                append("Provide standard technical analysis educational breakdown.")
            }
        }

        try {
            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", prompt)
                    ))
                ))
            }

            val body = jsonBody.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Failed to generate AI explanation: ${response.code} ${response.message}"
                }
                val responseString = response.body?.string() ?: return@withContext "Empty response"
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No text found")
                    }
                }
                return@withContext "No explanation generated."
            }
        } catch (e: Exception) {
            return@withContext "Error connecting to Gemini API: ${e.localizedMessage}"
        }
    }
}
