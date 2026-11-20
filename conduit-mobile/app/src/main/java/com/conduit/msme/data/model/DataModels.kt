package com.conduit.msme.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MSMEProfile(
    val id: String,
    val businessName: String,
    val ownerName: String,
    val district: String,
    val upiVpa: String,
    val monthlyUpiVolume: Double,
    val businessCategory: String = "General Store / Retail",
    val udyamRegNumber: String = "UDYAM-KR-03-0021482",
    val businessVintageYears: Int = 3,
    val employeeCount: Int = 2,
    val eligibleSchemes: List<String> = listOf("PMMY Mudra (Kishore)", "CGTMSE Guarantee", "PMEGP"),
    val subsidyEstimatedInr: Double = 50000.0,
    val onboardingCompleted: Boolean = false
)

@Serializable
data class VoiceIntentResponse(
    val detectedLanguage: String,
    val transcribedText: String,
    val requestedAmount: Double,
    val inferredTenureMonths: Int,
    val businessPurpose: String,
    val englishTranslation: String = "I need ₹50,000 for Diwali festival grocery stock for 3 months."
)

@Serializable
data class AAConsentRequest(
    val msmeId: String,
    val phone: String,
    val aaHandle: String
)

@Serializable
data class AAConsentStatus(
    val consentId: String,
    val isGranted: Boolean,
    val accountAggregatorName: String
)

@Serializable
data class CreditAssessmentResponse(
    val loanId: String,
    val approvedAmount: Double,
    val interestRateApr: Double,
    val dailySweepAmount: Double,
    val tenureDays: Int,
    val status: String,
    val zkCredentialHash: String
)

@Serializable
data class RepaymentProgress(
    val totalLoanAmount: Double,
    val amountRepaid: Double,
    val remainingBalance: Double,
    val dailySweepAmount: Double,
    val interestSavedVsMoneylender: Double,
    val daysRemaining: Int
)

@Serializable
data class MonthlyInflow(
    val monthCode: String,
    val monthKannada: String,
    val amountThousands: Float,
    val isFestivalPeak: Boolean = false
)

@Serializable
data class DigitalTwinSnapshot(
    val creditScore: Int,
    val maxScore: Int = 900,
    val tierLabelEn: String,
    val tierLabelKn: String,
    val dailyUpiTurnover: Double,
    val consistencyPercent: Double,
    val totalInterestSaved: Double,
    val socialCollateralScore: Int,
    val shgEndorsementsCount: Int,
    val credentialHash: String,
    val monthlyInflows: List<MonthlyInflow>
)

@Serializable
data class SweepTransaction(
    val id: String,
    val dateLabelEn: String,
    val dateLabelKn: String,
    val timeLabel: String,
    val amount: Double,
    val upiRef: String,
    val status: String
)

enum class ConduitLanguage(val code: String) {
    ENGLISH("en"),
    KANNADA("kn");

    companion object {
        fun fromCode(code: String?): ConduitLanguage =
            if (code == KANNADA.code) KANNADA else ENGLISH
    }
}
