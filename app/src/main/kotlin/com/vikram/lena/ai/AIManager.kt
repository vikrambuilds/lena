package com.vikram.lena.ai

import com.vikram.lena.data.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AIManager(
    private val geminiApiKey: String,
    private val openAiApiKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val promptBuilder = PromptBuilder()

    /** Smart routing with fallback */
    suspend fun getResponse(
        userMessage: String,
        recentMessages: List<Message>,
        taskResult: String? = null
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        
        val useGPT = shouldUseGPT(userMessage)
        
        return@withContext try {
            if (useGPT && openAiApiKey.isNotEmpty()) {
                Pair(callChatGPT(userMessage, recentMessages, taskResult), "ChatGPT")
            } else {
                Pair(callGemini(userMessage, recentMessages, taskResult), "Gemini")
            }
        } catch (e: Exception) {
            // Fallback to other model
            try {
                if (useGPT) {
                    Pair(callGemini(userMessage, recentMessages, taskResult), "Gemini")
                } else if (openAiApiKey.isNotEmpty()) {
                    Pair(callChatGPT(userMessage, recentMessages, taskResult), "ChatGPT")
                } else {
                    throw e
                }
            } catch (e2: Exception) {
                Pair(
                    "Arre yaar, network issue ho raha hai. " +
                    "Thodi der baad try kar! 😅 Error: ${e2.message}",
                    "Error"
                )
            }
        }
    }

    private fun shouldUseGPT(message: String): Boolean {
        val complexKeywords = listOf(
            "code", "program", "algorithm", "debug", "error",
            "explain in detail", "solve this", "calculate",
            "logic", "function", "data structure", "dsa",
            "operating system", "os ", "dbms", "database",
            "networking", "compiler", "assembly"
        )
        return complexKeywords.any { message.lowercase().contains(it) }
    }

    /** Gemini API */
    private fun callGemini(
        userMessage: String,
        recentMessages: List<Message>,
        taskResult: String?
    ): String {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
                "gemini-2.0-flash:generateContent?key=$geminiApiKey"

        val fullPrompt = promptBuilder.buildPrompt(
            userMessage, recentMessages, taskResult
        )

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", fullPrompt)
                ))
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.9)
                put("maxOutputTokens", 400)
                put("topP", 0.95)
            })
            put("safetySettings", JSONArray().apply {
                val categories = listOf(
                    "HARM_CATEGORY_HARASSMENT",
                    "HARM_CATEGORY_HATE_SPEECH",
                    "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                    "HARM_CATEGORY_DANGEROUS_CONTENT"
                )
                categories.forEach { category ->
                    put(JSONObject().apply {
                        put("category", category)
                        put("threshold", "BLOCK_NONE")
                    })
                }
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw Exception("Empty response")
            val json = JSONObject(body)
            
            if (json.has("error")) {
                throw Exception(json.getJSONObject("error").getString("message"))
            }
            
            return json.getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
        }
    }

    /** ChatGPT API */
    private fun callChatGPT(
        userMessage: String,
        recentMessages: List<Message>,
        taskResult: String?
    ): String {
        val url = "https://api.openai.com/v1/chat/completions"

        val messagesArray = JSONArray()
        
        // System prompt
        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", PromptBuilder.SYSTEM_PROMPT)
        })

        // Task context
        if (taskResult != null) {
            messagesArray.put(JSONObject().apply {
                put("role", "system")
                put("content", "TASK COMPLETED: $taskResult. " +
                    "Iske baare mein friendly way mein bata.")
            })
        }

        // Recent history
        recentMessages.takeLast(10).forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.sender == "Vikram") "user" else "assistant")
                put("content", msg.message)
            })
        }

        // Current message
        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })

        val requestJson = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", messagesArray)
            put("temperature", 0.9)
            put("max_tokens", 400)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $openAiApiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw Exception("Empty response")
            val json = JSONObject(body)
            
            if (json.has("error")) {
                throw Exception(json.getJSONObject("error").getString("message"))
            }
            
            return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
        }
    }
}