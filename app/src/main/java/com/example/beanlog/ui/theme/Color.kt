package com.example.beanlog.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class BeanPalette(
    val coffeeCream: Color,
    val latteSurface: Color,
    val latteVariant: Color,
    val oatMilk: Color,
    val caramel: Color,
    val softCaramel: Color,
    val oatGold: Color,
    val espresso: Color,
    val mutedCoffee: Color,
    val coffeeOutline: Color,
    val darkRoast: Color,
    val coffeeCard: Color,
    val coffeeCardText: Color,
    val pageTop: Color,
    val navButtonSurface: Color,
    val navButtonText: Color,
    val fieldSurface: Color,
    val tipSurface: Color
)

val LightBeanPalette = BeanPalette(
    coffeeCream = Color(0xFFFFFCF6),
    latteSurface = Color(0xFFFFFEFB),
    latteVariant = Color(0xFFF5EBDD),
    oatMilk = Color(0xFFECE0CD),
    caramel = Color(0xFF986744),
    softCaramel = Color(0xFFB98961),
    oatGold = Color(0xFFD8BC91),
    espresso = Color(0xFF3D2B20),
    mutedCoffee = Color(0xFF5A4030),
    coffeeOutline = Color(0xFF9A785D),
    darkRoast = Color(0xFF29180F),
    coffeeCard = Color(0xFF563827),
    coffeeCardText = Color(0xFFFFF5E8),
    pageTop = Color(0xFFF1E5D7),
    navButtonSurface = Color(0xFF493323),
    navButtonText = Color(0xFFFFF8EC),
    fieldSurface = Color(0xFFF1E3D0),
    tipSurface = Color(0xFFEADCCB)
)

val DarkBeanPalette = BeanPalette(
    coffeeCream = Color(0xFF211B18),
    latteSurface = Color(0xFF2B231F),
    latteVariant = Color(0xFF342923),
    oatMilk = Color(0xFF49382D),
    caramel = Color(0xFFB98961),
    softCaramel = Color(0xFFC99A70),
    oatGold = Color(0xFF9A795A),
    espresso = Color(0xFFF2E8DA),
    mutedCoffee = Color(0xFFD0BEAC),
    coffeeOutline = Color(0xFF8E7664),
    darkRoast = Color(0xFFF2E8DA),
    coffeeCard = Color(0xFF594335),
    coffeeCardText = Color(0xFFF7EEE3),
    pageTop = Color(0xFF211B18),
    navButtonSurface = Color(0xFF49382D),
    navButtonText = Color(0xFFF7EEE3),
    fieldSurface = Color(0xFF49382D),
    tipSurface = Color(0xFF49382D)
)

val LocalBeanPalette = staticCompositionLocalOf { LightBeanPalette }

val CoffeeCream: Color @Composable get() = LocalBeanPalette.current.coffeeCream
val LatteSurface: Color @Composable get() = LocalBeanPalette.current.latteSurface
val LatteVariant: Color @Composable get() = LocalBeanPalette.current.latteVariant
val OatMilk: Color @Composable get() = LocalBeanPalette.current.oatMilk
val Caramel: Color @Composable get() = LocalBeanPalette.current.caramel
val SoftCaramel: Color @Composable get() = LocalBeanPalette.current.softCaramel
val OatGold: Color @Composable get() = LocalBeanPalette.current.oatGold
val Espresso: Color @Composable get() = LocalBeanPalette.current.espresso
val MutedCoffee: Color @Composable get() = LocalBeanPalette.current.mutedCoffee
val CoffeeOutline: Color @Composable get() = LocalBeanPalette.current.coffeeOutline
val DarkRoast: Color @Composable get() = LocalBeanPalette.current.darkRoast
val CoffeeCard: Color @Composable get() = LocalBeanPalette.current.coffeeCard
val CoffeeCardText: Color @Composable get() = LocalBeanPalette.current.coffeeCardText

val BeanLogPageTop: Color @Composable get() = LocalBeanPalette.current.pageTop
val BeanLogNavButtonSurface: Color @Composable get() = LocalBeanPalette.current.navButtonSurface
val BeanLogNavButtonText: Color @Composable get() = LocalBeanPalette.current.navButtonText
val BeanLogFieldSurface: Color @Composable get() = LocalBeanPalette.current.fieldSurface
val BeanLogTipSurface: Color @Composable get() = LocalBeanPalette.current.tipSurface
