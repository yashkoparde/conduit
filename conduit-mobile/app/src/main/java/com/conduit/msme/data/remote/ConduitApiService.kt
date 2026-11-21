package com.conduit.msme.data.remote

import com.conduit.msme.data.model.AAConsentRequest
import com.conduit.msme.data.model.AAConsentStatus
import com.conduit.msme.data.model.CreditAssessmentResponse
import com.conduit.msme.data.model.RepaymentProgress
import com.conduit.msme.data.model.VoiceIntentResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ConduitApiService {

    @POST("voice/transcribe-and-intent")
    suspend fun parseVoiceNote(
        @Body audioBase64: Map<String, String>
    ): VoiceIntentResponse

    @POST("aa/initiate-consent")
    suspend fun initiateAAConsent(
        @Body request: AAConsentRequest
    ): AAConsentStatus

    @GET("underwrite/assess/{msmeId}")
    suspend fun getUnderwritingAssessment(
        @Path("msmeId") msmeId: String
    ): CreditAssessmentResponse

    @GET("repayment/status/{loanId}")
    suspend fun getRepaymentProgress(
        @Path("loanId") loanId: String
    ): RepaymentProgress
}
