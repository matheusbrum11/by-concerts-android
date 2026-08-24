package com.byconcerts.domain.model

enum class PaymentCode {
    CARTAO_LOJA_AVISTA,
    CARTAO_LOJA_PAGTO_FATURA_CHEQUE,
    CARTAO_LOJA_PAGTO_FATURA_DINHEIRO,
    CARTAO_LOJA_PARCELADO,
    CARTAO_LOJA_PARCELADO_BANCO,
    CARTAO_LOJA_PARCELADO_LOJA,
    CREDIARIO_SIMULACAO,
    CREDIARIO_VENDA,
    CREDITO_AVISTA,
    CREDITO_CREDIARIO_CREDITO,
    CREDITO_PARCELADO_ADM,
    CREDITO_PARCELADO_BNCO,
    CREDITO_PARCELADO_LOJA,
    DEBITO_AVISTA,
    DEBITO_PAGTO_FATURA_DEBITO,
    FROTAS,
    PIX,
    PRE_AUTORIZACAO,
    VOUCHER_ALIMENTACAO,
    VOUCHER_AUTO,
    VOUCHER_AUTOMOTIVO,
    VOUCHER_BENEFICIOS,
    VOUCHER_CONSULTA_SALDO,
    VOUCHER_CULTURA,
    VOUCHER_PEDAGIO,
    VOUCHER_REFEICAO,
    VOUCHER_VALE_PEDAGIO,
    ;

    val wireValue: String get() = name

    companion object {
        val selectableInCheckout: List<PaymentCode> =
            listOf(CREDITO_AVISTA, DEBITO_AVISTA, PIX)
    }
}

data class PaymentItem(
    val name: String,
    val quantity: Int,
    val sku: String,
    val unitPriceInCents: Long,
    val unitOfMeasure: String = "unidade",
)

data class PaymentRequest(
    val reference: String,
    val paymentCode: PaymentCode,
    val totalInCents: Long,
    val items: List<PaymentItem>,
    val installments: Int = 0,
    val email: String = "",
    val merchantCode: String = "",
)

data class CancellationRequest(
    val purchaseId: String,
    val reference: String,
    val cieloCode: String,
    val authCode: String,
    val totalInCents: Long,
)

data class PaymentInfo(
    val authCode: String,
    val cieloCode: String,
    val brand: String,
    val maskedCard: String,
    val amountInCents: Long,
    val paymentId: String = "",
    val installments: Int = 0,
)

data class PaymentCallback(
    val reference: String?,
    val result: PaymentResult,
)
