package com.example.beanlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.beanlog.ui.theme.BeanLogTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = AppDb.create(applicationContext).dao()
        val beanId = intent.getLongExtra("bean_id", -1L).takeIf { it > 0L }
        val bagId = intent.getLongExtra("bag_id", -1L).takeIf { it > 0L }
        val shotId = intent.getLongExtra("shot_id", -1L).takeIf { it > 0L }
        setContent { BeanLogTheme { App(dao, beanId, bagId, shotId) } }
    }
}