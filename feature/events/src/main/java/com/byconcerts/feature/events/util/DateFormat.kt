package com.byconcerts.feature.events.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val formatter = SimpleDateFormat("dd MMM yyyy · HH'h'", Locale("pt", "BR"))

/** Formata epoch millis para uma data curta em pt-BR. SimpleDateFormat funciona
 * em qualquer minSdk (não exige java.time / desugaring). */
fun Long.toEventDateLabel(): String = formatter.format(Date(this))
