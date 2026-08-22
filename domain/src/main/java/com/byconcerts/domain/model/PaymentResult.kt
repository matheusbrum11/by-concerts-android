package com.byconcerts.domain.model

/**
 * Resultado de uma tentativa de pagamento, já mapeado para o domínio. As telas
 * conhecem SÓ este tipo — nunca o JSON da Cielo.
 */
sealed interface PaymentResult {
    /** Pagamento aprovado e integralmente pago. */
    data class Approved(val payment: PaymentInfo) : PaymentResult

    /** Negado pela adquirente/emissor. [code] é o código bruto quando houver. */
    data class Denied(val reason: String, val code: Int? = null) : PaymentResult

    /** Cancelado pelo usuário no fluxo da Cielo. */
    data object Canceled : PaymentResult

    /** Falha técnica ou de negócio — ver [PaymentError]. */
    data class Error(val type: PaymentError) : PaymentResult
}

/**
 * Tipos de erro de pagamento como domínio (sealed), não exceptions soltas.
 */
sealed interface PaymentError {
    /** App da Cielo não instalado / intent não pôde ser disparada. */
    data object GatewayNotAvailable : PaymentError

    /** Callback ausente, corrompido ou que não parseia. */
    data object InvalidResponse : PaymentError

    /** pendingAmount != 0: pedido NÃO foi totalmente pago. */
    data class PartialPayment(val pendingInCents: Long) : PaymentError

    /** Sem retorno da Cielo dentro do tempo esperado. */
    data object Timeout : PaymentError

    /** Qualquer retorno inesperado não coberto acima. */
    data class Unknown(val message: String? = null) : PaymentError
}
