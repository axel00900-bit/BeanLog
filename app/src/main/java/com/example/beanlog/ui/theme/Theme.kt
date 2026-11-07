package com.example.beanlog.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

@Composable
fun BeanLogTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    val palette = if (darkTheme) DarkBeanPalette else LightBeanPalette
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = palette.caramel,
            onPrimary = palette.coffeeCream,
            primaryContainer = palette.coffeeCard,
            onPrimaryContainer = palette.coffeeCardText,
            secondary = palette.coffeeCard,
            onSecondary = palette.coffeeCardText,
            secondaryContainer = palette.oatMilk,
            onSecondaryContainer = palette.espresso,
            tertiary = palette.softCaramel,
            background = palette.coffeeCream,
            surface = palette.latteSurface,
            surfaceVariant = palette.latteVariant,
            onBackground = palette.espresso,
            onSurface = palette.darkRoast,
            onSurfaceVariant = palette.mutedCoffee,
            outline = palette.coffeeOutline
        )
    } else {
        lightColorScheme(
            primary = palette.darkRoast,
            onPrimary = palette.coffeeCream,
            primaryContainer = palette.coffeeCard,
            onPrimaryContainer = palette.coffeeCardText,
            secondary = palette.coffeeCard,
            onSecondary = palette.coffeeCardText,
            secondaryContainer = androidx.compose.ui.graphics.Color(0xFF76543D),
            onSecondaryContainer = palette.coffeeCream,
            tertiary = palette.caramel,
            background = palette.coffeeCream,
            surface = palette.latteSurface,
            surfaceVariant = palette.latteVariant,
            onBackground = palette.espresso,
            onSurface = palette.darkRoast,
            onSurfaceVariant = palette.mutedCoffee,
            outline = palette.darkRoast
        )
    }
    CompositionLocalProvider(LocalBeanPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
    }
}
