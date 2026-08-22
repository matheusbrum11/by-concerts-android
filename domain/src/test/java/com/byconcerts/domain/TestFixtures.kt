package com.byconcerts.domain

import com.byconcerts.domain.model.Event
import com.byconcerts.domain.model.PaymentCode
import com.byconcerts.domain.model.PaymentInfo
import com.byconcerts.domain.model.Purchase
import com.byconcerts.domain.model.PurchaseStatus

fun event(
    id: String = "evt-1",
    availableQuantity: Int = 10,
    unitPriceInCents: Long = 5000,
): Event = Event(
    id = id,
    title = "Show Teste",
    venue = "Arena",
    city = "São Paulo",
    dateEpochMillis = 1_800_000_000_000,
    unitPriceInCents = unitPriceInCents,
    availableQuantity = availableQuantity,
    imageUrl = null,
    description = null,
)

fun purchase(
    id: String = "pur-1",
    idempotencyKey: String = "key-1",
    quantity: Int = 2,
    status: PurchaseStatus = PurchaseStatus.PENDING,
    payment: PaymentInfo? = null,
): Purchase = Purchase(
    id = id,
    idempotencyKey = idempotencyKey,
    eventId = "evt-1",
    eventTitle = "Show Teste",
    quantity = quantity,
    unitPriceInCents = 5000,
    totalInCents = 5000L * quantity,
    status = status,
    paymentCode = PaymentCode.CREDITO_AVISTA,
    createdAtEpochMillis = 1,
    updatedAtEpochMillis = 1,
    payment = payment,
)

fun paymentInfo(): PaymentInfo = PaymentInfo(
    authCode = "AUTH123",
    cieloCode = "NSU456",
    brand = "VISA",
    maskedCard = "**** 1234",
    amountInCents = 10000,
)
