package com.byconcerts.core.common

import java.util.UUID

/** Fonte de tempo injetável — testes usam um clock fixo. */
fun interface Clock {
    fun nowMillis(): Long
}

class SystemClock : Clock {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

/** Gerador de identificadores/chaves de idempotência — testes usam sequência fixa. */
fun interface IdGenerator {
    fun newId(): String
}

class UuidGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
