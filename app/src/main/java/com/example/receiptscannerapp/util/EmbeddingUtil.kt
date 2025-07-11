package com.example.receiptscannerapp.util

import android.util.Log
import com.example.receiptscannerapp.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object EmbeddingUtil {

    private const val TAG = "EmbeddingUtil"
    private const val API_URL =
        "https://api-inference.huggingface.co/pipeline/feature-extraction/sentence-transformers/all-MiniLM-L6-v2"

    private val client = OkHttpClient()

    suspend fun getEmbedding(text: String): List<Float>? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.HF_API_KEY

        Log.d(TAG, "Starting embedding for text: ${text.take(50)}...")

        if (apiKey.isBlank()) {
            Log.e(TAG, "❌ Hugging Face API key is missing in BuildConfig.HF_API_KEY")
            return@withContext null
        }

        val json = JSONObject().put("inputs", text).toString()
        val mediaType = "application/json".toMediaType()
        val requestBody = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(API_URL)
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        try {
            Log.d(TAG, "Sending request to Hugging Face API...")
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "❌ API call failed with HTTP code: ${response.code}")
                    return@withContext null
                }

                val responseBody = response.body?.string()
                if (responseBody.isNullOrEmpty()) {
                    Log.e(TAG, "❌ API returned empty response body")
                    return@withContext null
                }

                Log.d(TAG, "✅ API call succeeded, parsing embedding response")

                val jsonArray = JSONArray(responseBody)
                val innerArray = jsonArray.getJSONArray(0)

                val embedding = List(innerArray.length()) { i ->
                    innerArray.getDouble(i).toFloat()
                }

                Log.d(TAG, "✅ Embedding extracted: length=${embedding.size}")
                return@withContext embedding
            }
        } catch (e: IOException) {
            Log.e(TAG, "❌ IOException occurred: ${e.message}", e)
            return@withContext null
        } catch (e: Exception) {
            Log.e(TAG, "❌ Unexpected error: ${e.message}", e)
            return@withContext null
        }
    }
}
