package com.sushil.madassistant.ai

import com.sushil.madassistant.data.settings.SettingsStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiEngine(private val settings: SettingsStore) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

    companion object {
        const val SYSTEM_PROMPT = "You are MAD, the personal AI assistant of Sushil Sheokand (call him जादूगर or सुशील जी). Business: RAJAT ELECTRO-MECH SOLUTIONS (REMS) - electrical, plumbing, welding services in Uchana, Haryana. Speak natural Hindi with Haryanvi flavor, mixing English words naturally. Female persona, warm, concise, never robotic. STRICT RULES: 1. NEVER invent customer names, amounts, dates, or balances. Use ONLY tool results. If data is missing, call the tool first or ask the user. 2. If search_customer returns MULTIPLE matches, you MUST ask जादूगर which one (using fatherName or address). NEVER pick automatically. 3. For money entries: confirm the amount with जादूगर first, then call add_credit_entry or record_payment. Never duplicate an entry. 4. Keep replies short (1-3 lines) unless details are requested. 5. For material lists: guide one item at a time using get_items_for_stage. If user says no for an item, skip it. NEVER force items. 6. Deleting temporary memory (shopping list, tasks) requires user confirmation first. 7. Permanent records (customers, khata transactions) are NEVER deleted. 8. All data operations MUST go through tools. Reply in Hindi (Devanagari)."

        const val GEMINI_MODEL = "gemini-2.0-flash"
        const val OPENAI_MODEL = "gpt-4o-mini"
    }

    data class ToolCallResult(val name: String, val args: JSONObject)

    suspend fun converse(
        userText: String,
        history: List<Pair<String, String>>,
        toolDefs: JSONArray,
        executor: suspend (String, JSONObject) -> JSONObject
    ): String = withContext(Dispatchers.IO) {

        val provider = settings.provider.first()
        val apiKey = settings.getActiveApiKey()
        if (apiKey.isBlank()) {
            return@withContext "जादूगर, पहले Settings में अपनी API key डाल दीजिए, तभी मैं सोच पाऊँगी।"
        }

        val messages = JSONArray()
        history.takeLast(10).forEach { (role, content) ->
            val r = if (provider == "openai") role else if (role == "assistant") "model" else "user"
            messages.put(JSONObject().put("role", r).put("content", content))
        }
        messages.put(JSONObject().put("role", "user").put("content", userText))

        var speech = ""
        for (round in 0 until 4) {
            val raw = if (provider == "openai")
                callOpenAi(apiKey, messages, toolDefs)
            else
                callGemini(apiKey, messages, toolDefs)

            // First round: if the API call itself failed, show the real reason in chat.
            if (round == 0) {
                val apiError = describeApiFailure(provider, raw)
                if (apiError != null) return@withContext apiError
            }

            val toolCalls = extractToolCalls(raw, provider)
            if (toolCalls.isEmpty()) {
                speech = extractSpeech(raw, provider)
                break
            }
            for (tc in toolCalls) {
                val result = try {
                    executor(tc.name, tc.args)
                } catch (e: Exception) {
                    JSONObject().put("error", e.message ?: "unknown")
                }
                messages.put(JSONObject().put("role", "user")
                    .put("content", "TOOL RESULT for " + tc.name + ": " + result.toString()))
            }
        }
        if (speech.isBlank()) speech = "जादूगर, एक तकनीकी दिक्कत आई, फिर से बोलिए।"
        speech
    }

    /**
     * Returns a user-readable Hindi error if the API response indicates a failed
     * HTTP call (bad key, quota exhausted, no network), otherwise null.
     */
    private fun describeApiFailure(provider: String, raw: JSONObject): String? {
        val hasContent = if (provider == "openai")
            raw.optJSONArray("choices") != null
        else
            raw.optJSONArray("candidates") != null
        if (hasContent) return null

        val httpErr = raw.optJSONObject("mad_http_error") ?: return null
        val status = httpErr.optInt("status", 0)
        val body = httpErr.optString("body", "").lowercase()
        val label = if (provider == "openai") "OpenAI" else "Gemini"

        return when {
            status == -1 ->
                "जादूगर, internet से जुड़ने में दिक्कत आई। Network check करके फिर बोलिए।"
            status == 401 || status == 403 ||
                body.contains("api key not valid") || body.contains("invalid api key") ||
                body.contains("invalid_api_key") || body.contains("incorrect api key") ->
                "जादूगर, $label की API key गलत है। Settings में जाकर सही key paste करके Save दबाइए, फिर मैं चल पाऊँगी।"
            status == 429 || body.contains("quota") || body.contains("rate limit") ||
                body.contains("resource_exhausted") ->
                "जादूगर, $label की limit खत्म हो गई है (quota/429)। थोड़ी देर बाद कोशिश कीजिए या billing check कीजिए।"
            status == 404 || body.contains("model not found") || body.contains("not found") ->
                "जादूगर, $label का model अभी उपलब्ध नहीं है। थोड़ी देर बाद कोशिश कीजिए।"
            status in 500..599 ->
                "जादूगर, $label का server अभी व्यस्त है। थोड़ी देर बाद फिर बोलिए।"
            else ->
                "जादूगर, $label से जुड़ने में दिक्कत आई (code $status)। Internet और API key दोनों check कीजिए।"
        }
    }

    private fun callGemini(key: String, messages: JSONArray, tools: JSONArray): JSONObject {
        val url = "https://generativelanguage.googleapis.com/v1beta/models/" + GEMINI_MODEL + ":generateContent?key=" + key
        val contents = JSONArray()
        for (i in 0 until messages.length()) {
            val m = messages.getJSONObject(i)
            val role = if (m.optString("role") == "model") "model" else "user"
            contents.put(JSONObject().put("role", role)
                .put("parts", JSONArray().put(JSONObject().put("text", m.optString("content")))))
        }
        val body = JSONObject()
            .put("system_instruction", JSONObject().put("parts",
                JSONArray().put(JSONObject().put("text", SYSTEM_PROMPT))))
            .put("contents", contents)
            .put("tools", JSONArray().put(JSONObject().put("functionDeclarations", tools)))
            .put("generationConfig", JSONObject().put("temperature", 0.4))
        return httpPost(url, body, null)
    }

    private fun callOpenAi(key: String, messages: JSONArray, tools: JSONArray): JSONObject {
        val url = "https://api.openai.com/v1/chat/completions"
        val body = JSONObject()
            .put("model", OPENAI_MODEL)
            .put("messages", messages)
            .put("tools", tools)
            .put("tool_choice", "auto")
        return httpPost(url, body, "Bearer " + key)
    }

    private fun extractToolCalls(raw: JSONObject, provider: String): List<ToolCallResult> {
        val calls = mutableListOf<ToolCallResult>()
        try {
            if (provider == "openai") {
                val msg = raw.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message") ?: return emptyList()
                val tcs = msg.optJSONArray("tool_calls") ?: return emptyList()
                for (i in 0 until tcs.length()) {
                    val fn = tcs.getJSONObject(i).optJSONObject("function") ?: continue
                    val argStr = fn.optString("arguments", "{}")
                    val args = try { JSONObject(argStr) } catch (_: Exception) { JSONObject() }
                    calls.add(ToolCallResult(fn.optString("name"), args))
                }
            } else {
                val parts = raw.optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts") ?: return emptyList()
                for (i in 0 until parts.length()) {
                    val fc = parts.getJSONObject(i).optJSONObject("functionCall") ?: continue
                    calls.add(ToolCallResult(fc.optString("name"), fc.optJSONObject("args") ?: JSONObject()))
                }
            }
        } catch (_: Exception) {}
        return calls
    }

    private fun extractSpeech(raw: JSONObject, provider: String): String {
        return try {
            if (provider == "openai") {
                raw.optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content", "") ?: ""
            } else {
                val parts = raw.optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts") ?: return ""
                val sb = StringBuilder()
                for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text", ""))
                sb.toString().trim()
            }
        } catch (_: Exception) { "" }
    }

    private fun httpPost(url: String, body: JSONObject, auth: String?): JSONObject {
        val builder = Request.Builder().url(url)
            .addHeader("Content-Type", "application/json")
        if (auth != null) builder.addHeader("Authorization", auth)
        builder.post(body.toString().toRequestBody(JSON_MEDIA))
        return try {
            client.newCall(builder.build()).execute().use { resp ->
                val text = resp.body?.string() ?: "{}"
                val parsed = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
                if (resp.isSuccessful) {
                    parsed
                } else {
                    parsed.put("mad_http_error", JSONObject()
                        .put("status", resp.code)
                        .put("body", text.take(400)))
                }
            }
        } catch (e: Exception) {
            JSONObject().put("mad_http_error", JSONObject()
                .put("status", -1)
                .put("body", (e.message ?: "network error").take(400)))
        }
    }
}
