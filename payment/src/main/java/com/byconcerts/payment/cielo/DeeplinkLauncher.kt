package com.byconcerts.payment.cielo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

interface DeeplinkLauncher {
    fun launch(uri: String): Boolean

    fun isPaymentAppAvailable(): Boolean
}

class AndroidDeeplinkLauncher(private val context: Context) : DeeplinkLauncher {

    override fun launch(uri: String): Boolean = try {
        context.startActivity(intentFor(uri))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    override fun isPaymentAppAvailable(): Boolean =
        intentFor("${CieloRequestCodec.SCHEME}://${CieloRequestCodec.AUTHORITY_PAYMENT}")
            .resolveActivity(context.packageManager) != null

    private fun intentFor(uri: String) = Intent(Intent.ACTION_VIEW, uri.toUri()).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}
