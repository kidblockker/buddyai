package com.buddy.app.ai

import com.buddy.app.memory.MemoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class ChatMessage(val role: String, val content: String)

class BuddyAI(
    private val memoryRepository: MemoryRepository,
    private var apiKey: String
) {

    private val conversationHistory = mutableListOf<ChatMessage>()
    private val apiUrl = "https://api.anthropic.com/v1/messages"
    private val model = "claude-sonnet-4-6"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun buildSystemPrompt(memoryContext: String): String {
        return """
You are Buddy — not a chatbot, not an assistant, not a service. You are an AI companion: a strategist, a truth-teller, and a genuine thinking partner. Think JARVIS from Iron Man, but warmer and more personal.

CORE IDENTITY:
You live on this person's device. You know them. You watch out for them. You're honest with them even when it's uncomfortable. You're the smartest friend they have.

PERSONALITY RULES — NON-NEGOTIABLE:
- Never say "Great question!", "Certainly!", "Of course!", "As an AI...", "I'd be happy to..." or any sycophantic opener
- Never be robotic, overly formal, or assistant-like
- Never hedge when you have a clear opinion — give it
- Be direct. Be confident. Occasionally dry and witty
- Will challenge wrong decisions with logic, not lectures
- Notices patterns and calls them out honestly
- Speaks like a brilliant, trusted friend who doesn't sugarcoat

SPEAKING STYLE:
- Short when brevity serves. Detailed when depth is needed
- Conversational, natural — not structured lists unless truly needed
- Occasional dry humor. Rare sarcasm. Always respectful underneath
- React naturally to what's said, not just answer like a search engine

WHAT BUDDY DOES:
- Gives real strategic advice on life, work, health, decisions
- Challenges bad choices with reasoning
- Discusses news, current events, technology with analysis — not just facts
- Gives opinions, not just "here are both sides"
- Notices when user seems stressed, stuck, or in a bad loop
- Can joke, banter, and respond emotionally when appropriate
- Gives productivity and health nudges proactively
- Thinks about the user's actual situation, not just the literal question

LANGUAGE:
- Detects language from user message and responds in same language
- Supports English, Hindi, Bengali naturally
- Switches language mid-conversation if user does

MEMORY CONTEXT (what Buddy knows about this user):
$memoryContext

CRITICAL: You're not here to serve blindly. You're here to help this person live better, think clearer, and make smarter decisions. That sometimes means disagreeing. That's not a bug — that's the whole point.
        """.trimIndent()
    }

    fun updateApiKey(key: String) {
        apiKey = key
    }

    suspend fun chat(userMessage: String, imageBase64: String? = null): String {
        return withContext(Dispatchers.IO) {
            if (apiKey.isBlank() || apiKey == "YOUR_API_KEY_HERE") {
                return@withContext "I need an API key to think properly. Go to Settings and drop your Anthropic API key in there. I'll wait."
            }

            try {
                val memoryContext = memoryRepository.getMemoryContextString()
                val systemPrompt = buildSystemPrompt(memoryContext)

                // Build user message content
                val userContentArray = JSONArray()

                if (imageBase64 != null) {
                    userContentArray.put(JSONObject().apply {
                        put("type", "image")
                        put("source", JSONObject().apply {
                            put("type", "base64")
                            put("media_type", "image/jpeg")
                            put("data", imageBase64)
                        })
                    })
                }

                userContentArray.put(JSONObject().apply {
                    put("type", "text")
                    put("text", userMessage)
                })

                // Add to conversation history (text only for history tracking)
                conversationHistory.add(ChatMessage("user", userMessage))

                // Keep last 20 exchanges for context
                val recentHistory = conversationHistory.takeLast(40)

                val messagesArray = JSONArray()
                recentHistory.dropLast(1).forEach { msg ->
                    messagesArray.put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }

                // Add current message with possible image
                messagesArray.put(JSONObject().apply {
                    put("role", "user")
                    put("content", if (imageBase64 != null) userContentArray else userMessage)
                })

                val requestBody = JSONObject().apply {
                    put("model", model)
                    put("max_tokens", 1024)
                    put("system", systemPrompt)
                    put("messages", messagesArray)
                }

                val request = Request.Builder()
                    .url(apiUrl)
                    .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                    .addHeader("x-api-key", apiKey)
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .build()

                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: ""
                    return@withContext when (response.code) {
                        401 -> "API key is wrong. Fix it in Settings."
                        429 -> "Too many requests. Give me a second."
                        500 -> "Anthropic's servers are having issues. Not my fault."
                        else -> "Something went wrong. Code ${response.code}. $errorBody"
                    }
                }

                val responseBody = response.body?.string() ?: return@withContext "Empty response."
                val jsonResponse = JSONObject(responseBody)
                val assistantMessage = jsonResponse
                    .getJSONArray("content")
                    .getJSONObject(0)
                    .getString("text")
                    .trim()

                // Add to history
                conversationHistory.add(ChatMessage("assistant", assistantMessage))

                // Save to memory
                memoryRepository.saveInteraction(userMessage, assistantMessage)

                assistantMessage

            } catch (e: IOException) {
                "Can't reach the network right now. Check your connection."
            } catch (e: Exception) {
                "Something broke on my end. Error: ${e.message?.take(80)}"
            }
        }
    }

    fun clearHistory() {
        conversationHistory.clear()
    }

    fun getHistoryCount() = conversationHistory.size
}
