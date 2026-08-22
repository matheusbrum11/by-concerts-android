package com.byconcerts.domain.error

import com.byconcerts.core.common.AppResult
import com.byconcerts.domain.model.PaymentError

/**
 * Erros de negócio/domínio como sealed — nunca exceptions soltas cruzando
 * camadas. A UI mapeia cada caso para uma mensagem acionável.
 */
sealed interface DomainError {
    /** Evento não encontrado pelo id informado. */
    data class EventNotFound(val eventId: String) : DomainError

    /** Quantidade solicitada indisponível. */
    data class OutOfStock(val eventId: String, val requested: Int, val available: Int) : DomainError

    /** Quantidade inválida (<= 0). */
    data object InvalidQuantity : DomainError

    /** Compra não encontrada pela chave/id. */
    data object PurchaseNotFound : DomainError

    /** Falha ao persistir/ler dados locais. */
    data class Storage(val message: String? = null) : DomainError

    /** Erro proveniente do gateway de pagamento. */
    data class Payment(val error: PaymentError) : DomainError
}

/** Result de domínio: sucesso tipado ou [DomainError]. */
typealias DomainResult<D> = AppResult<D, DomainError>
