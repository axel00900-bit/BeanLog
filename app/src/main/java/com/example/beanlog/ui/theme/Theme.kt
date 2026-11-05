package com.example.beanlog.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = OatGold,
    onPrimary = Espresso,
    secondary = SoftCaramel,
    tertiary = OatGold,
    background = Espresso,
    surface = Color(0xFF30231C),
    surfaceVariant = Color(0xFF514034),
    onBackground = CoffeeCream,
    onSurface = CoffeeCream,
    outline = CoffeeOutline
)

private val LightColorScheme = lightColorScheme(
    primary = Caramel,
    onPrimary = Color.White,
    secondary = SoftCaramel,
    onSecondary = Color.White,
    tertiary = OatGold,
    background = CoffeeCream,
    surface = LatteSurface,
    surfaceVariant = LatteVariant,
    onBackground = Espresso,
    onSurface = Espresso,
    onSurfaceVariant = MutedCoffee,
    outline = CoffeeOutline
)

@Composable
fun BeanLogTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
