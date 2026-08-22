package com.byconcerts.data.mapper

import com.byconcerts.data.local.entity.PurchaseEntity
import com.byconcerts.domain.model.PaymentCode
import com.byconcerts.domain.model.PaymentInfo
import com.byconcerts.domain.model.Purchase
import com.byconcerts.domain.model.PurchaseStatus

fun PurchaseEntity.toDomain(): Purchase = Purchase(
    id = id,
    idempotencyKey = idempotencyKey,
    eventId = eventId,
    eventTitle = eventTitle,
    quantity = quantity,
    unitPriceInCents = unitPriceInCents,
    totalInCents = totalInCents,
    status = PurchaseStatus.valueOf(status),
    paymentCode = PaymentCode.valueOf(paymentCode),
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    payment = toPaymentInfoOrNull(),
    failureReason = failureReason,
)

private fun PurchaseEntity.toPaymentInfoOrNull(): PaymentInfo? {
    // Só reconstrói o PaymentInfo quando os campos essenciais existem (APPROVED).
    if (authCode == null || cieloCode == null || brand == null ||
        maskedCard == null || amountInCents == null
    ) {
        return null
    }
    return PaymentInfo(
        authCode = authCode,
        cieloCode = cieloCode,
        brand = brand,
        maskedCard = maskedCard,
        amountInCents = amountInCents,
    )
}

fun Purchase.toEntity(): PurchaseEntity = PurchaseEntity(
    id = id,
    idempotencyKey = idempotencyKey,
    eventId = eventId,
    eventTitle = eventTitle,
    quantity = quantity,
    unitPriceInCents = unitPriceInCents,
    totalInCents = totalInCents,
    status = status.name,
    paymentCode = paymentCode.name,
    createdAtEpochMillis = createdAtEpochMillis,
    updatedAtEpochMillis = updatedAtEpochMillis,
    authCode = payment?.authCode,
    cieloCode = payment?.cieloCode,
    brand = payment?.brand,
    maskedCard = payment?.maskedCard,
    amountInCents = payment?.amountInCents,
    failureReason = failureReason,
)
