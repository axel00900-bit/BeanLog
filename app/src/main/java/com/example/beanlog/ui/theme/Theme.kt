package com.example.beanlog.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// BeanLog uses one light, warm background throughout the app.
private val BeanLogColorScheme = lightColorScheme(
    primary = DarkRoast,
    onPrimary = CoffeeCream,
    primaryContainer = CoffeeCard,
    onPrimaryContainer = CoffeeCardText,
    secondary = CoffeeCard,
    onSecondary = CoffeeCardText,
    secondaryContainer = Color(0xFF76543D),
    onSecondaryContainer = CoffeeCream,
    tertiary = Caramel,
    background = CoffeeCream,
    surface = LatteSurface,
    surfaceVariant = LatteVariant,
    onBackground = Espresso,
    onSurface = DarkRoast,
    onSurfaceVariant = MutedCoffee,
    outline = DarkRoast
)

@Composable
fun BeanLogTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BeanLogColorScheme,
        typography = Typography,
        content = content
    )
}
