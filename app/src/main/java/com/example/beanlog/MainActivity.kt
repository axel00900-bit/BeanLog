package com.example.beanlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.beanlog.ui.theme.BeanLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDb.create(applicationContext).dao()
        setContent { BeanLogTheme { App(dao) } }
    }
}