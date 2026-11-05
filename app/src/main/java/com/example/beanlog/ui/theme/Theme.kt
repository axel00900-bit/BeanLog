package com.example.beanlog.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// BeanLog uses one light, warm background throughout the app.
private val BeanLogColorScheme = lightColorScheme(
    primary = SoftCaramel,
    onPrimary = Espresso,
    primaryContainer = Color(0xFFF1E3D0),
    onPrimaryContainer = Espresso,
    secondary = OatGold,
    onSecondary = Espresso,
    secondaryContainer = LatteVariant,
    onSecondaryContainer = Espresso,
    tertiary = Caramel,
    background = CoffeeCream,
    surface = LatteSurface,
    surfaceVariant = LatteVariant,
    onBackground = Espresso,
    onSurface = Espresso,
    onSurfaceVariant = MutedCoffee,
    outline = CoffeeOutline
)

@Composable
fun BeanLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BeanLogColorScheme,
        typography = Typography,
        content = content
    )
}
