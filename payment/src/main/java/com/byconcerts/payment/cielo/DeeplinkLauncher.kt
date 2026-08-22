package com.byconcerts.payment.cielo

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Dispara a intent de deeplink. Abstraído por interface para (a) testar o gateway
 * sem Android e (b) detectar de forma explícita o caso "app Cielo não instalado".
 *
 * @return true se a intent foi disparada; false se não há app para tratá-la.
 */
fun interface DeeplinkLauncher {
    fun launch(uri: String): Boolean
}

class AndroidDeeplinkLauncher(private val context: Context) : DeeplinkLauncher {
    override fun launch(uri: String): Boolean = try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
