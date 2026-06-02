package com.example.eposappexample.poslink.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.eposappexample.poslink.TeyaUtils
import com.example.eposappexample.poslink.ui.order.OrderScreen
import com.example.eposappexample.poslink.ui.theme.EposAppExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        TeyaUtils.setUp()

        setContent {
            EposAppExampleTheme {
                OrderScreen()
            }
        }
    }
}
