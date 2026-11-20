package com.conduit.msme.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.conduit.msme.data.model.ConduitLanguage
import com.conduit.msme.data.model.DigitalTwinSnapshot
import com.conduit.msme.data.model.MSMEProfile
import com.conduit.msme.data.model.MonthlyInflow
import com.conduit.msme.data.model.RepaymentProgress
import com.conduit.msme.data.remote.ApiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.conduitDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "conduit_msme_preferences"
)

class ConduitPreferencesDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val SESSION_TOKEN = stringPreferencesKey("msme_session_token")
        val ACTIVE_LOAN_ID = stringPreferencesKey("active_loan_id")
        val CACHED_PROFILE_JSON = stringPreferencesKey("cached_profile_json")
        val CACHED_DIGITAL_TWIN_JSON = stringPreferencesKey("cached_digital_twin_json")
        val REPAID_AMOUNT = doublePreferencesKey("repaid_amount")
        val TOTAL_LOAN_AMOUNT = doublePreferencesKey("total_loan_amount")
        val DAYS_REMAINING = intPreferencesKey("days_remaining")
        val INTEREST_SAVED = doublePreferencesKey("interest_saved")
    }

    val languageFlow: Flow<ConduitLanguage> = context.conduitDataStore.data.map { prefs ->
        ConduitLanguage.fromCode(prefs[Keys.LANGUAGE_CODE])
    }

    val sessionTokenFlow: Flow<String> = context.conduitDataStore.data.map { prefs ->
        prefs[Keys.SESSION_TOKEN] ?: "msme_sess_hubballi_ravi_01"
    }

    val activeLoanIdFlow: Flow<String> = context.conduitDataStore.data.map { prefs ->
        prefs[Keys.ACTIVE_LOAN_ID] ?: "LN-KRN-2026-0842"
    }

    val cachedProfileFlow: Flow<MSMEProfile> = context.conduitDataStore.data.map { prefs ->
        prefs[Keys.CACHED_PROFILE_JSON]?.let { raw ->
            runCatching { ApiClient.json.decodeFromString<MSMEProfile>(raw) }.getOrNull()
        } ?: defaultMerchantProfile()
    }

    val cachedDigitalTwinFlow: Flow<DigitalTwinSnapshot> = context.conduitDataStore.data.map { prefs ->
        prefs[Keys.CACHED_DIGITAL_TWIN_JSON]?.let { raw ->
            runCatching { ApiClient.json.decodeFromString<DigitalTwinSnapshot>(raw) }.getOrNull()
        } ?: defaultDigitalTwin()
    }

    val cachedRepaymentFlow: Flow<RepaymentProgress> = context.conduitDataStore.data.map { prefs ->
        val total = prefs[Keys.TOTAL_LOAN_AMOUNT] ?: 50_000.0
        val repaid = prefs[Keys.REPAID_AMOUNT] ?: 18_480.0
        val remaining = (total - repaid).coerceAtLeast(0.0)
        val daysLeft = prefs[Keys.DAYS_REMAINING] ?: 144
        val saved = prefs[Keys.INTEREST_SAVED] ?: 17_400.0
        RepaymentProgress(
            totalLoanAmount = total,
            amountRepaid = repaid,
            remainingBalance = remaining,
            dailySweepAmount = 220.0,
            interestSavedVsMoneylender = saved,
            daysRemaining = daysLeft
        )
    }

    suspend fun setLanguage(language: ConduitLanguage) {
        context.conduitDataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = language.code
        }
    }

    suspend fun saveSessionAndProfile(token: String, profile: MSMEProfile) {
        context.conduitDataStore.edit { prefs ->
            prefs[Keys.SESSION_TOKEN] = token
            prefs[Keys.CACHED_PROFILE_JSON] = ApiClient.json.encodeToString(profile)
        }
    }

    suspend fun saveDigitalTwin(snapshot: DigitalTwinSnapshot) {
        context.conduitDataStore.edit { prefs ->
            prefs[Keys.CACHED_DIGITAL_TWIN_JSON] = ApiClient.json.encodeToString(snapshot)
        }
    }

    suspend fun saveActiveLoan(loanId: String, progress: RepaymentProgress) {
        context.conduitDataStore.edit { prefs ->
            prefs[Keys.ACTIVE_LOAN_ID] = loanId
            prefs[Keys.TOTAL_LOAN_AMOUNT] = progress.totalLoanAmount
            prefs[Keys.REPAID_AMOUNT] = progress.amountRepaid
            prefs[Keys.DAYS_REMAINING] = progress.daysRemaining
            prefs[Keys.INTEREST_SAVED] = progress.interestSavedVsMoneylender
        }
    }

    companion object {
        fun defaultMerchantProfile(): MSMEProfile = MSMEProfile(
            id = "MSME-HUB-4821",
            businessName = "Hubballi Provision Store",
            ownerName = "Ravi Kulkarni",
            district = "Hubballi",
            upiVpa = "ravi.hubballi@okaxis",
            monthlyUpiVolume = 186_000.0
        )

        fun defaultDigitalTwin(): DigitalTwinSnapshot = DigitalTwinSnapshot(
            creditScore = 785,
            maxScore = 900,
            tierLabelEn = "Prime Tier",
            tierLabelKn = "ಪ್ರೈಮ್ ಶ್ರೇಣಿ",
            dailyUpiTurnover = 6_200.0,
            consistencyPercent = 98.2,
            totalInterestSaved = 17_400.0,
            socialCollateralScore = 98,
            shgEndorsementsCount = 14,
            credentialHash = "CND-HUB-785-9F2A-88C1",
            monthlyInflows = listOf(
                MonthlyInflow("Dec", "ಡಿ", 168f),
                MonthlyInflow("Jan", "ಜ", 174f),
                MonthlyInflow("Feb", "ಫೆ", 162f),
                MonthlyInflow("Mar", "ಮಾ", 215f, isFestivalPeak = true), // Ugadi
                MonthlyInflow("Apr", "ಏ", 182f),
                MonthlyInflow("May", "ಮೇ", 176f),
                MonthlyInflow("Jun", "ಜೂ", 170f),
                MonthlyInflow("Jul", "ಜು", 179f),
                MonthlyInflow("Aug", "ಆ", 198f, isFestivalPeak = true), // Gowri-Ganesha
                MonthlyInflow("Sep", "ಸೆ", 188f),
                MonthlyInflow("Oct", "ಅ", 232f, isFestivalPeak = true), // Dasara
                MonthlyInflow("Nov", "ನ", 248f, isFestivalPeak = true)  // Diwali
            )
        )
    }
}
