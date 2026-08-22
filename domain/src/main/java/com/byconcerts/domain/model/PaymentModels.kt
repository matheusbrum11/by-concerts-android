package com.byconcerts.domain.model

/**
 * Formas de pagamento suportadas. [wireValue] é a string exata esperada pela
 * Cielo no campo `paymentCode`. Para o case, CRÉDITO e DÉBITO à vista bastam;
 * a lista oficial tem mais valores (parcelado, PIX, voucher…).
 */
enum class PaymentCode(val wireValue: String) {
    CREDITO_AVISTA("CREDITO_AVISTA"),
    DEBITO_AVISTA("DEBITO_AVISTA"),
}

/** Item do pedido enviado à Cielo. Preço unitário em centavos. */
data class PaymentItem(
    val name: String,
    val quantity: Int,
    val sku: String,
    val unitPriceInCents: Long,
    val unitOfMeasure: String = "unidade",
)

/**
 * Requisição de pagamento — modelo de DOMÍNIO. O :payment mapeia isto para o
 * JSON/Base64 da Cielo; as telas nunca montam esse payload.
 *
 * @param reference chave de idempotência da tentativa de compra (vira `reference`
 *   no pedido Cielo). É o elo que evita cobrança duplicada.
 */
data class PaymentRequest(
    val reference: String,
    val paymentCode: PaymentCode,
    val totalInCents: Long,
    val items: List<PaymentItem>,
    val installments: Int = 0,
    val email: String = "",
)

/** Requisição de cancelamento/estorno (deeplink lio://payment-reversal). */
data class CancellationRequest(
    val purchaseId: String,
    val reference: String,
    val cieloCode: String,
    val authCode: String,
    val totalInCents: Long,
)

/**
 * Dados de pagamento retornados pela Cielo e persistidos no comprovante.
 * [cieloCode] é o NSU. Valor em centavos.
 */
data class PaymentInfo(
    val authCode: String,
    val cieloCode: String,
    val brand: String,
    val maskedCard: String,
    val amountInCents: Long,
)
