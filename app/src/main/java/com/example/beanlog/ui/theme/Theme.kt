package com.example.beanlog.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = OatGold,
    onPrimary = Espresso,
    primaryContainer = Color(0xFF604832),
    onPrimaryContainer = CoffeeCream,
    secondary = SoftCaramel,
    onSecondary = Espresso,
    secondaryContainer = Color(0xFF594332),
    onSecondaryContainer = CoffeeCream,
    tertiary = OatGold,
    background = Espresso,
    surface = Color(0xFF30251E),
    surfaceVariant = Color(0xFF514236),
    onBackground = CoffeeCream,
    onSurface = CoffeeCream,
    onSurfaceVariant = Color(0xFFE2D2BE),
    outline = CoffeeOutline
)

private val LightColorScheme = lightColorScheme(
    primary = Caramel,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEBD8BF),
    onPrimaryContainer = Espresso,
    secondary = SoftCaramel,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF1E3D0),
    onSecondaryContainer = Espresso,
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
