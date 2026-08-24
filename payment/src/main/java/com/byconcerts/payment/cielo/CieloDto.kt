package com.byconcerts.payment.cielo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

data class CieloCredentials(
    val clientId: String,
    val accessToken: String,
    val merchantCode: String = "",
)

@Serializable
internal data class CieloRequestDto(
    val accessToken: String,
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

@Serializable
internal data class CieloReversalRequestDto(
    val id: String,
    @SerialName("clientID") val clientId: String,
    val accessToken: String,
    val cieloCode: String,
    val authCode: String,
    val value: Long,
)

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

@Serializable
internal data class CieloErrorDto(
    val code: Int? = null,
    val reason: String? = null,
    val order: CieloOrderDto? = null,
)
