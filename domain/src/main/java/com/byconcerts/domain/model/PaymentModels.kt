package com.byconcerts.domain.model

/**
 * Valores aceitos no campo `paymentCode` da Cielo. Lista conforme a
 * documentação oficial (docs.cielo.com.br → "Valores aceitos no campo
 * paymentCode"). O nome do enum é idêntico ao valor enviado no JSON, por isso
 * [wireValue] usa o próprio `name`.
 *
 * A UI expõe apenas os códigos de [selectableInCheckout]; os demais existem
 * para que o contrato do gateway cubra a lista oficial sem exigir mudança de
 * modelo quando um novo meio for habilitado.
 */
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

    /** Valor exato enviado no campo `paymentCode` do JSON da Cielo. */
    val wireValue: String get() = name

    companion object {
        /** Meios oferecidos na tela de checkout deste app. */
        val selectableInCheckout: List<PaymentCode> =
            listOf(CREDITO_AVISTA, DEBITO_AVISTA, PIX)
    }
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
    /** Opcional; identifica o estabelecimento em cenários multi-EC. */
    val merchantCode: String = "",
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
    /** `id` do payment na Order — necessário para o cancelamento/estorno. */
    val paymentId: String = "",
    val installments: Int = 0,
)

/**
 * Resultado do callback já correlacionado ao pedido. A Cielo devolve o campo
 * `reference` dentro da Order, e é ele — a nossa chave de idempotência — que
 * amarra o retorno à compra local, mesmo que o processo do app tenha sido
 * morto enquanto o app de pagamento estava em primeiro plano.
 */
data class PaymentCallback(
    val reference: String?,
    val result: PaymentResult,
)
