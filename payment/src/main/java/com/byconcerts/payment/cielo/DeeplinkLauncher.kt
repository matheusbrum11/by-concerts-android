package com.byconcerts.payment.cielo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Dispara a intent de deeplink. Abstraído por interface para (a) testar o
 * gateway sem Android e (b) tratar de forma explícita o caso "app de pagamento
 * da Cielo não instalado".
 */
interface DeeplinkLauncher {
    /** @return true se a intent foi disparada; false se não há app para tratá-la. */
    fun launch(uri: String): Boolean

    /** Há algum app capaz de tratar o scheme `lio://`? */
    fun isPaymentAppAvailable(): Boolean
}

class AndroidDeeplinkLauncher(private val context: Context) : DeeplinkLauncher {

    override fun launch(uri: String): Boolean = try {
        context.startActivity(intentFor(uri))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    /**
     * Exige a declaração `<queries>` no manifest: a partir do Android 11
     * (targetSdk 30+) a resolução de intents é filtrada por visibilidade de
     * pacotes, e sem ela `resolveActivity` devolveria null mesmo com o app da
     * Cielo instalado.
     */
    override fun isPaymentAppAvailable(): Boolean =
        intentFor("${CieloRequestCodec.SCHEME}://${CieloRequestCodec.AUTHORITY_PAYMENT}")
            .resolveActivity(context.packageManager) != null

    private fun intentFor(uri: String) = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
        // NEW_TASK porque o launch pode partir de um Context de aplicação;
        // CLEAR_TOP conforme o sample oficial da Cielo.
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
