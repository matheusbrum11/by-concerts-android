package com.byconcerts.payment.cielo

import kotlinx.serialization.Serializable

/** Credenciais da Cielo (injetadas a partir de BuildConfig). */
data class CieloCredentials(
    val clientId: String,
    val accessToken: String,
)

// ── Request (montado por nós, enviado à Cielo) ───────────────────────────────

@Serializable
internal data class CieloRequestDto(
    val accessToken: String,
    val clientID: String,
    val reference: String,
    val email: String,
    val installments: Int,
    val items: List<CieloItemDto>,
    val paymentCode: String,
    val value: String,
)

@Serializable
internal data class CieloItemDto(
    val name: String,
    val quantity: Int,
    val sku: String,
    val unitOfMeasure: String,
    val unitPrice: Long,
)

// ── Response (recebido no callback) ──────────────────────────────────────────

@Serializable
internal data class CieloResponseDto(
    val payments: List<CieloPaymentDto>? = null,
    val pendingAmount: Long? = null,
    // Forma de erro/cancelamento: { "code": <int>, "reason": "<motivo>" }
    val code: Int? = null,
    val reason: String? = null,
)

@Serializable
internal data class CieloPaymentDto(
    val authCode: String? = null,
    val cieloCode: String? = null,
    val brand: String? = null,
    val mask: String? = null,
    val amount: Long? = null,
    val paymentFields: CieloPaymentFieldsDto? = null,
)

@Serializable
internal data class CieloPaymentFieldsDto(
    // 0 = PIX, 1 = autorizada, 2 = cancelamento
    val statusCode: Int? = null,
)
