package com.byconcerts.payment.cielo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/** Credenciais da Cielo (injetadas a partir de BuildConfig). */
data class CieloCredentials(
    val clientId: String,
    val accessToken: String,
    val merchantCode: String = "",
)

// ─────────────────────────────────────────────────────────────────────────────
//  REQUEST — campos conforme o sample oficial da Cielo (LIO-Hybrid-Integration-
//  Sample-Flutter → PaymentRequest). Valores monetários em CENTAVOS, numéricos.
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
internal data class CieloRequestDto(
    val accessToken: String,
    // A doc usa "clientID" (D maiúsculo) — não é typo.
    @SerialName("clientID") val clientId: String,
    val reference: String,
    val email: String,
    val installments: Int,
    val items: List<CieloItemDto>,
    val merchantCode: String,
    val paymentCode: String,
    val value: Long,
)

@Serializable
internal data class CieloItemDto(
    val name: String,
    val quantity: Int,
    val sku: String,
    val unitOfMeasure: String,
    val unitPrice: Long,
)

/** Request de cancelamento/estorno (deeplink `lio://payment-reversal`). */
@Serializable
internal data class CieloReversalRequestDto(
    val id: String,
    @SerialName("clientID") val clientId: String,
    val accessToken: String,
    val cieloCode: String,
    val authCode: String,
    val value: Long,
)

// ─────────────────────────────────────────────────────────────────────────────
//  RESPONSE
//
//  SUCESSO: a Order vem na RAIZ do JSON (não há envelope). Campos conforme
//  PaymentCheckoutResponseSuccess do sample oficial.
//  ERRO/CANCELAMENTO: { "code": int, "reason": string, "order": {...} }
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
internal data class CieloOrderDto(
    val id: String? = null,
    val reference: String? = null,
    val status: String? = null,
    val number: String? = null,
    val notes: String? = null,
    val price: Long? = null,
    val paidAmount: Long? = null,
    val pendingAmount: Long? = null,
    val payments: List<CieloPaymentDto>? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val type: String? = null,
)

@Serializable
internal data class CieloPaymentDto(
    val id: String? = null,
    val amount: Long? = null,
    val authCode: String? = null,
    val brand: String? = null,
    val cieloCode: String? = null,
    val mask: String? = null,
    val installments: Int? = null,
    val terminal: String? = null,
    val primaryCode: String? = null,
    val secondaryCode: String? = null,
    val requestDate: String? = null,
    val paymentFields: CieloPaymentFieldsDto? = null,
)

/**
 * No contrato da Cielo TODOS os paymentFields são STRING (inclusive
 * `statusCode`). Usamos JsonPrimitive para aceitar tanto `"1"` quanto `1` sem
 * quebrar o parse — a normalização fica em [statusCodeOrNull].
 */
@Serializable
internal data class CieloPaymentFieldsDto(
    val statusCode: JsonPrimitive? = null,
    val paymentTypeCode: String? = null,
    val entranceMode: String? = null,
    val pan: String? = null,
    val productName: String? = null,
    val originalTransactionId: String? = null,
) {
    val statusCodeOrNull: Int?
        get() = statusCode?.content?.trim()?.toIntOrNull()
}

/** Envelope de falha: `{ code, reason, order }`. */
@Serializable
internal data class CieloErrorDto(
    val code: Int? = null,
    val reason: String? = null,
    val order: CieloOrderDto? = null,
)
