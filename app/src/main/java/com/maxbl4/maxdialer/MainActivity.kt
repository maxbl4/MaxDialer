package com.maxbl4.maxdialer

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.maxbl4.maxdialer.ui.PhoneApp
import com.maxbl4.maxdialer.ui.PhoneViewModel
import com.maxbl4.maxdialer.ui.theme.MaxDialerTheme

class MainActivity : ComponentActivity() {
    private val vm: PhoneViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleDial(intent)
        addOnNewIntentListener(::handleDial)
        setContent {
            MaxDialerTheme { PhoneApp(vm) }
        }
    }

    /** A tel: link or another app asked to dial a number: put it on the dialpad. */
    private fun handleDial(intent: Intent) {
        if (intent.action != Intent.ACTION_DIAL && intent.action != Intent.ACTION_VIEW) return
        val number = intent.data?.takeIf { it.scheme == "tel" }?.schemeSpecificPart ?: return
        vm.setQuery(number.filter { it.isDigit() || it in "+*#,;" })
    }
}
