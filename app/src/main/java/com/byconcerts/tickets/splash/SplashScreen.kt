package com.byconcerts.tickets.splash

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.style.TextAlign
import com.mns.designsystem.component.layout.MnsSurface
import com.mns.designsystem.component.status.MnsCircularProgress
import com.mns.designsystem.component.text.MnsText
import com.mns.designsystem.theme.MnsTheme
import org.koin.androidx.compose.koinViewModel

@Composable
fun SplashRoute(
    onReady: () -> Unit,
    viewModel: SplashViewModel = koinViewModel(),
) {
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    LaunchedEffect(ready) {
        if (ready) onReady()
    }
    SplashScreen()
}

@Composable
internal fun SplashScreen() {
    MnsSurface(
        modifier = Modifier.fillMaxSize(),
        shape = MnsTheme.shapes.none,
        color = MnsTheme.colors.primary,
        contentColor = MnsTheme.colors.onPrimary,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MnsText(
                    text = "By Concerts",
                    style = MnsTheme.typography.displaySmall,
                    color = MnsTheme.colors.onPrimary,
                    textAlign = TextAlign.Center,
                )
                MnsText(
                    text = "Ingressos para eventos locais",
                    style = MnsTheme.typography.bodyLarge,
                    color = MnsTheme.colors.onPrimary,
                    textAlign = TextAlign.Center,
                )
                MnsCircularProgress(
                    color = MnsTheme.colors.onPrimary,
                    contentDescription = "Carregando",
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
