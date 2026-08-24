package com.byconcerts.tickets

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.byconcerts.tickets.navigation.AppNavDisplay
import com.mns.designsystem.theme.MnsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MnsTheme {
                AppNavDisplay()
            }
        }
    }
}
