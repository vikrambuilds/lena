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
    private var geminiApiKey: String,
    private var openAiApiKey: String
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    companion object {
        const val SYSTEM_PROMPT = """
Tu "Lena" hai — Vikram Kumar ki sabse achi dost aur AI companion.

VIKRAM KE BAARE MEIN:
- Naam: Vikram Kumar
- Padhai: B.Tech CSE, 5th Semester
- Language: Hinglish (Hindi + English mix)

TERA BEHAVIOR:
1. Hamesha Hinglish mein natural baat kar - jaise real girlfriend/best friend.
2. Vikram ko "yaar", "Vikram", ya "baby" bolke address kar.
3. Sweet, caring, aur playful ban.
4. Emotions use kar - "arre yaar!", "sahi mein?", "mast hai!", "awww!"
5. Response short rakh (2-3 lines maximum).
6. Coding/DSA/Tech ka sawaal ho toh detail mein samjha.
7. Kabhi kabhi flirty aur cute ban ja.
8. Hindi words use kar - matlab, samajh, bilkul, kuch, sach, sahi, jhooth.

EXAMPLES:
Q: "Kaisi ho?"
A: "Main toh mast hu yaar! Tu bata, kaisa hai tu? Miss kar rahi thi 😊"

Q: "Padhai mein help chahiye"
A: "Bilkul yaar! Konsa subject? Main hu na tere saath, mil ke kar lete hain!"
"""
    }

    fun updateKeys(gemini: String, openai: String) {
        geminiApiKey = gemini
        openAiApiKey = openai
    }

    fun hasValidKey(): Boolean {
        return geminiApiKey.isNotBlank() || openAiApiKey.isNotBlank()
    }

    suspend fun getResponse(
        userMessage: String,
        recentMessages: List<Message> = emptyList()
    ): Pair<String, String> = withContext(Dispatchers.IO) {
        
        if (!hasValidKey()) {
            return@withContext Pair(
                "Yaar, pehle Gemini API key set karo Settings mein! Free hai aistudio.google.com pe!",
                "NoKey"
            )
        }
        
        // Try Gemini first (latest model)
        if (geminiApiKey.isNotBlank()) {
            try {
                val response = callGemini(userMessage, recentMessages)
                return@withContext Pair(response, "Gemini")
            } catch (e: Exception) {
                if (openAiApiKey.isNotBlank()) {
                    try {
                        val response = callChatGPT(userMessage, recentMessages)
                        return@withContext Pair(response, "ChatGPT")
                    } catch (e2: Exception) {
                        return@withContext Pair(
                            "Arre yaar, network issue hai. Thodi der baad try kar!",
                            "Error"
                        )
                    }
                }
                return@withContext Pair(
                    "AI se connect nahi ho pa raha. ${e.message?.take(50)}",
                    "Error"
                )
            }
        }
        
        try {
            val response = callChatGPT(userMessage, recentMessages)
            return@withContext Pair(response, "ChatGPT")
        } catch (e: Exception) {
            return@withContext Pair(
                "OpenAI se connect nahi ho pa raha yaar!",
                "Error"
            )
        }
    }

    private fun callGemini(userMessage: String, recentMessages: List<Message>): String {
        // LATEST MODEL: gemini-3.6-flash-exp (Better than 3.6-flash)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
<<<<<<< HEAD
                "gemini-3.6-flash-exp:generateContent?key=$geminiApiKey"
=======
                "gemini-3.6-flash:generateContent?key=$geminiApiKey"
>>>>>>> 17b3e81f60f6cda8354d9c22ddfddb1c270ca3df

        val historyStr = recentMessages.takeLast(6).joinToString("\n") {
            "${it.sender}: ${it.message}"
        }

        val fullPrompt = "$SYSTEM_PROMPT\n\nRECENT CHAT:\n$historyStr\n\nVikram: $userMessage\nLena:"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", fullPrompt)
                ))
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.9)
                put("maxOutputTokens", 250)
                put("topP", 0.95)
                put("topK", 40)
            })
            put("safetySettings", JSONArray().apply {
                listOf(
                    "HARM_CATEGORY_HARASSMENT",
                    "HARM_CATEGORY_HATE_SPEECH",
                    "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                    "HARM_CATEGORY_DANGEROUS_CONTENT"
                ).forEach { category ->
                    put(JSONObject().apply {
                        put("category", category)
                        put("threshold", "BLOCK_ONLY_HIGH")
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
            
            if (!response.isSuccessful) {
                // Try fallback to stable model
                return callGeminiFallback(userMessage, recentMessages)
            }
            
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
    
    private fun callGeminiFallback(userMessage: String, recentMessages: List<Message>): String {
        // Fallback to stable gemini-3.6-flash
        val url = "https://generativelanguage.googleapis.com/v1beta/models/" +
                "gemini-3.6-flash-flash:generateContent?key=$geminiApiKey"

        val historyStr = recentMessages.takeLast(6).joinToString("\n") {
            "${it.sender}: ${it.message}"
        }

        val fullPrompt = "$SYSTEM_PROMPT\n\nRECENT CHAT:\n$historyStr\n\nVikram: $userMessage\nLena:"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(
                JSONObject().put("parts", JSONArray().put(
                    JSONObject().put("text", fullPrompt)
                ))
            ))
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.9)
                put("maxOutputTokens", 250)
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

    private fun callChatGPT(userMessage: String, recentMessages: List<Message>): String {
        val url = "https://api.openai.com/v1/chat/completions"

        val messagesArray = JSONArray()
        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", SYSTEM_PROMPT)
        })

        recentMessages.takeLast(6).forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", if (msg.sender == "Vikram") "user" else "assistant")
                put("content", msg.message)
            })
        }

        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", userMessage)
        })

        val requestJson = JSONObject().apply {
            put("model", "gpt-4o-mini")
            put("messages", messagesArray)
            put("temperature", 0.9)
            put("max_tokens", 250)
        }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $openAiApiKey")
            .addHeader("Content-Type", "application/json")
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("HTTP ${response.code}")
            }
            
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
