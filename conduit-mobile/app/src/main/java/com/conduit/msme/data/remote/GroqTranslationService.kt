package com.conduit.msme.data.remote

import com.conduit.msme.data.model.VoiceIntentResponse
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

@Serializable
data class GroqChatRequest(
    val model: String = "qwen/qwen3.8-27b",
    val messages: List<GroqChatMessage>,
    val temperature: Double = 0.1,
    @SerialName("response_format")
    val responseFormat: GroqResponseFormat = GroqResponseFormat("json_object")
)

@Serializable
data class GroqChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class GroqResponseFormat(
    val type: String
)

@Serializable
data class GroqChatResponse(
    val choices: List<GroqChoice>
)

@Serializable
data class GroqChoice(
    val message: GroqChoiceMessage
)

@Serializable
data class GroqChoiceMessage(
    val content: String
)

@Serializable
data class GroqExtractedLoanIntent(
    val detectedLanguage: String = "Kannada",
    val transcribedText: String = "",
    val requestedAmount: Double = 50000.0,
    val inferredTenureMonths: Int = 3,
    val businessPurpose: String = "Festival working capital",
    val englishTranslation: String = ""
)

@Serializable
data class VoiceOnboardingPhase1Result(
    val merchantName: String = "",
    val shopName: String = "",
    val businessCategory: String = "General Store / Retail",
    val cityOrDistrict: String = "Hubballi",
    val estimatedMonthlyTurnover: Double = 150000.0,
    val preferredLanguage: String = "Kannada",
    val englishSummary: String = ""
)

@Serializable
data class VoiceOnboardingPhase2Result(
    val udyamRegNumber: String = "",
    val panOrGst: String = "",
    val businessVintageYears: Int = 3,
    val employeeCount: Int = 2,
    val eligibleSchemes: List<String> = emptyList(),
    val subsidyEstimatedInr: Double = 50000.0,
    val schemeEligibilityNotes: String = ""
)

/**
 * Direct Groq API client handling high-speed translation and intent extraction
 * across English, Hindi, and Kannada.
 */
class GroqTranslationService(
    private val apiKey: String = BuildConfig.GROQ_API_KEY.ifBlank { "" }
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .writeTimeout(12, TimeUnit.SECONDS)
        .build()

    suspend fun processVoiceOnboardingPhase1(
        spokenText: String
    ): VoiceOnboardingPhase1Result = withContext(Dispatchers.IO) {
        val prompt = spokenText.ifBlank {
            "ನನ್ನ ಹೆಸರು ರವಿ, ನನ್ನ ಅಂಗಡಿ ಹುಬ್ಬಳ್ಳಿ ಪ್ರಾವಿಷನ್ ಸ್ಟೋರ್, ತಿಂಗಳಿಗೆ ಸುಮಾರು 1.5 ಲಕ್ಷ ವ್ಯಾಪಾರ ಇದೆ."
        }
        val systemPrompt = """
            You are an AI onboarding agent for Indian MSMEs supporting Kannada, Hindi, and English.
            Extract basic merchant profile info from spoken text into strict JSON:
            {
              "merchantName": "Name of the merchant owner",
              "shopName": "Name of shop or business",
              "businessCategory": "Category (e.g. Grocery / Retail / Kirana)",
              "cityOrDistrict": "City or District in India (e.g. Hubballi)",
              "estimatedMonthlyTurnover": number in INR,
              "preferredLanguage": "Kannada" or "Hindi" or "English",
              "englishSummary": "One sentence summary in English"
            }
        """.trimIndent()

        val requestData = GroqChatRequest(
            model = "qwen/qwen3.8-27b",
            messages = listOf(
                GroqChatMessage(role = "system", content = systemPrompt),
                GroqChatMessage(role = "user", content = prompt)
            )
        )
        val jsonString = json.encodeToString(GroqChatRequest.serializer(), requestData)
        val body = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        httpClient.newCall(req).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            val groqResp = json.decodeFromString<GroqChatResponse>(responseBody)
            val jsonContent = groqResp.choices.firstOrNull()?.message?.content ?: "{}"
            json.decodeFromString<VoiceOnboardingPhase1Result>(jsonContent)
        }
    }

    suspend fun processVoiceOnboardingPhase2(
        spokenText: String
    ): VoiceOnboardingPhase2Result = withContext(Dispatchers.IO) {
        val prompt = spokenText.ifBlank {
            "ನನ್ನ ಉದ್ಯಮ್ ನಂಬರ್ UDYAM-KR-03-0021482, 3 ವರ್ಷದಿಂದ ಅಂಗಡಿ ನಡೆಸುತ್ತಿದ್ದೇನೆ, 2 ಜನ ಕೆಲಸಗಾರರಿದ್ದಾರೆ, ಮುದ್ರಾ ಮತ್ತು PMEGP ಸ್ಕೀಮ್ ಬೇಕು."
        }
        val systemPrompt = """
            You are an expert on Indian Government MSME Schemes (PMMY Mudra, PMEGP, CGTMSE, PM Vishwakarma).
            From the user's spoken details in Kannada, Hindi, or English, extract Udyam & MSME scheme eligibility into strict JSON:
            {
              "udyamRegNumber": "Udyam Registration Number e.g. UDYAM-KR-03-0021482 or formatted string",
              "panOrGst": "PAN or GST if mentioned, or empty string",
              "businessVintageYears": integer number of years in business,
              "employeeCount": integer count of workers/employees,
              "eligibleSchemes": ["Pradhan Mantri MUDRA Yojana (Kishore)", "CGTMSE Credit Guarantee", "PMEGP Capital Subsidy"],
              "subsidyEstimatedInr": estimated subsidy or collateral-free limit in INR (e.g. 50000),
              "schemeEligibilityNotes": "Clear explanation in English of why they qualify for central & state MSME schemes"
            }
        """.trimIndent()

        val requestData = GroqChatRequest(
            model = "qwen/qwen3.8-27b",
            messages = listOf(
                GroqChatMessage(role = "system", content = systemPrompt),
                GroqChatMessage(role = "user", content = prompt)
            )
        )
        val jsonString = json.encodeToString(GroqChatRequest.serializer(), requestData)
        val body = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        httpClient.newCall(req).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            val groqResp = json.decodeFromString<GroqChatResponse>(responseBody)
            val jsonContent = groqResp.choices.firstOrNull()?.message?.content ?: "{}"
            json.decodeFromString<VoiceOnboardingPhase2Result>(jsonContent)
        }
    }

    suspend fun translateAndExtractVoiceIntent(
        rawSpokenText: String? = null,
        languageHint: String = "kn"
    ): VoiceIntentResponse = withContext(Dispatchers.IO) {
        val userPrompt = rawSpokenText?.ifBlank { null }
            ?: "ನನಗೆ ದೀಪಾವಳಿ ಹಬ್ಬದ ಸಾಮಾನಿಗೆ 50,000 ರೂಪಾಯಿ 3 ತಿಂಗಳಿಗೆ ಬೇಕು."

        val systemPrompt = """
            You are a multilingual translation and loan extraction engine powered by Groq API.
            You handle English, Hindi, and Kannada.
            Translate the user's speech and extract all structured loan parameters.
            Always return a strict JSON object with EXACTLY these fields:
            {
              "detectedLanguage": "Kannada" or "Hindi" or "English",
              "transcribedText": "Original spoken text in Kannada, Hindi, or English",
              "requestedAmount": number in INR (e.g. 50000),
              "inferredTenureMonths": integer number of months (e.g. 3),
              "businessPurpose": "Purpose translated to English (e.g. Diwali Grocery Stock)",
              "englishTranslation": "Accurate English translation of the spoken statement"
            }
        """.trimIndent()

        val requestBodyData = GroqChatRequest(
            model = "qwen/qwen3.8-27b",
            messages = listOf(
                GroqChatMessage(role = "system", content = systemPrompt),
                GroqChatMessage(role = "user", content = userPrompt)
            )
        )

        val jsonString = json.encodeToString(GroqChatRequest.serializer(), requestBodyData)
        val body = jsonString.toRequestBody("application/json; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("https://api.groq.com/openai/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("Content-Type", "application/json")
            .post(body)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                val errorBody = response.body?.string()
                throw IllegalStateException("Groq API error HTTP ${response.code}: $errorBody")
            }

            val responseBody = response.body?.string() ?: throw IllegalStateException("Empty Groq response")
            val groqResp = json.decodeFromString<GroqChatResponse>(responseBody)
            val jsonContent = groqResp.choices.firstOrNull()?.message?.content
                ?: throw IllegalStateException("No choices in Groq response")

            val extracted = json.decodeFromString<GroqExtractedLoanIntent>(jsonContent)

            VoiceIntentResponse(
                detectedLanguage = extracted.detectedLanguage.ifBlank { "kn" },
                transcribedText = extracted.transcribedText.ifBlank { userPrompt },
                requestedAmount = if (extracted.requestedAmount > 0) extracted.requestedAmount else 50000.0,
                inferredTenureMonths = if (extracted.inferredTenureMonths > 0) extracted.inferredTenureMonths else 3,
                businessPurpose = extracted.businessPurpose.ifBlank { "Festival Grocery Stock" },
                englishTranslation = extracted.englishTranslation.ifBlank {
                    "I need ₹50,000 for Diwali festival grocery stock for 3 months."
                }
            )
        }
    }
}
