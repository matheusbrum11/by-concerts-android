package com.byconcerts.domain.usecase

import com.byconcerts.core.common.AppResult
import com.byconcerts.core.common.Clock
import com.byconcerts.domain.error.DomainError
import com.byconcerts.domain.error.DomainResult
import com.byconcerts.domain.model.PaymentError
import com.byconcerts.domain.model.PaymentResult
import com.byconcerts.domain.model.Purchase
import com.byconcerts.domain.model.PurchaseStatus
import com.byconcerts.domain.repository.EventRepository
import com.byconcerts.domain.repository.PurchaseRepository

/**
 * Concilia o resultado do pagamento com a compra PENDING, identificada pela
 * chave de idempotência (= `reference` da Cielo).
 *
 * IDEMPOTÊNCIA (critério crítico): se a compra JÁ está em estado terminal
 * (APPROVED/DENIED/CANCELED), um callback repetido é um NO-OP — devolvemos a
 * compra existente sem reprocessar (sem baixar estoque duas vezes, sem
 * sobrescrever dados de aprovação).
 *
 * Erros transitórios (timeout, gateway indisponível, resposta inválida) e
 * pagamento parcial MANTÊM a compra PENDING, permitindo retentar com a MESMA
 * chave — nunca criando uma nova cobrança.
 */
class ReconcilePaymentUseCase(
    private val purchaseRepository: PurchaseRepository,
    private val eventRepository: EventRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(
        idempotencyKey: String,
        result: PaymentResult,
    ): DomainResult<Purchase> {
        val existing = purchaseRepository.findByIdempotencyKey(idempotencyKey)
            ?: return AppResult.Failure(DomainError.PurchaseNotFound)

        if (existing.isTerminal) {
            // Callback duplicado após um desfecho definitivo: no-op idempotente.
            return AppResult.Success(existing)
        }

        val now = clock.nowMillis()
        val updated = when (result) {
            is PaymentResult.Approved -> existing.copy(
                status = PurchaseStatus.APPROVED,
                payment = result.payment,
                failureReason = null,
                updatedAtEpochMillis = now,
            )

            is PaymentResult.Denied -> existing.copy(
                status = PurchaseStatus.DENIED,
                failureReason = result.reason,
                updatedAtEpochMillis = now,
            )

            PaymentResult.Canceled -> existing.copy(
                status = PurchaseStatus.CANCELED,
                failureReason = "Cancelado pelo usuário",
                updatedAtEpochMillis = now,
            )

            // Transitórios/negócio: permanecem PENDING (retentáveis com a mesma chave).
            is PaymentResult.Error -> existing.copy(
                status = PurchaseStatus.PENDING,
                failureReason = result.type.toReason(),
                updatedAtEpochMillis = now,
            )
        }

        purchaseRepository.update(updated)

        // Baixa de estoque acontece UMA vez, só na transição para APPROVED.
        if (updated.status == PurchaseStatus.APPROVED) {
            eventRepository.decrementAvailability(updated.eventId, updated.quantity)
        }

        return AppResult.Success(updated)
    }
}

private fun PaymentError.toReason(): String = when (this) {
    PaymentError.GatewayNotAvailable -> "App de pagamento Cielo não encontrado"
    PaymentError.InvalidResponse -> "Retorno de pagamento inválido"
    is PaymentError.PartialPayment -> "Pagamento parcial: faltam ${pendingInCents} centavos"
    PaymentError.Timeout -> "Tempo esgotado aguardando o pagamento"
    is PaymentError.Unknown -> message ?: "Erro desconhecido no pagamento"
}
