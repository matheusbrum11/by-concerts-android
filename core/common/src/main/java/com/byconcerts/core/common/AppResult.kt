package com.byconcerts.core.common

/**
 * Wrapper de resultado com erro TIPADO. Preferimos isto a exceptions soltas para
 * tornar o tratamento de erro explícito e verificável em tempo de compilação:
 * quem chama é obrigado a decidir o que fazer com [Failure].
 *
 * @param D tipo do dado em caso de sucesso.
 * @param E tipo do erro de domínio em caso de falha (um sealed, nunca Throwable cru).
 */
sealed interface AppResult<out D, out E> {
    data class Success<out D>(val data: D) : AppResult<D, Nothing>
    data class Failure<out E>(val error: E) : AppResult<Nothing, E>
}

inline fun <D, E, R> AppResult<D, E>.fold(
    onSuccess: (D) -> R,
    onFailure: (E) -> R,
): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Failure -> onFailure(error)
}

inline fun <D, E, R> AppResult<D, E>.map(transform: (D) -> R): AppResult<R, E> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <D, E> AppResult<D, E>.onSuccess(action: (D) -> Unit): AppResult<D, E> {
    if (this is AppResult.Success) action(data)
    return this
}

inline fun <D, E> AppResult<D, E>.onFailure(action: (E) -> Unit): AppResult<D, E> {
    if (this is AppResult.Failure) action(error)
    return this
}

fun <D, E> AppResult<D, E>.getOrNull(): D? = (this as? AppResult.Success)?.data
