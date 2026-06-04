package com.example.eposappexample.poslink.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.teya.lemonade.LemonadeTheme

@Composable
fun EposAppExampleTheme(
    content: @Composable () -> Unit
) {
    LemonadeTheme {
        val backgroundColors = LemonadeTheme.colors.background
        val contentColors = LemonadeTheme.colors.content
        val borderColors = LemonadeTheme.colors.border

        MaterialTheme(
            colorScheme = lightColorScheme(
                primary = backgroundColors.bgBrand,
                onPrimary = contentColors.contentOnBrandHigh,
                primaryContainer = backgroundColors.bgBrandSubtle,
                onPrimaryContainer = contentColors.contentBrand,
                secondary = backgroundColors.bgNeutral,
                onSecondary = contentColors.contentPrimary,
                secondaryContainer = backgroundColors.bgBrandSubtle,
                onSecondaryContainer = contentColors.contentBrand,
                tertiary = backgroundColors.bgBrand,
                onTertiary = contentColors.contentOnBrandHigh,
                tertiaryContainer = backgroundColors.bgBrandSubtle,
                onTertiaryContainer = contentColors.contentBrand,
                inversePrimary = backgroundColors.bgBrand,
                background = backgroundColors.bgDefault,
                onBackground = contentColors.contentPrimary,
                surface = backgroundColors.bgSubtle,
                onSurface = contentColors.contentPrimary,
                surfaceVariant = backgroundColors.bgSubtle,
                onSurfaceVariant = contentColors.contentSecondary,
                surfaceTint = backgroundColors.bgElevated,
                surfaceBright = backgroundColors.bgElevated,
                surfaceDim = backgroundColors.bgSubtle,
                surfaceContainerLowest = backgroundColors.bgDefault,
                surfaceContainerLow = backgroundColors.bgDefault,
                surfaceContainer = backgroundColors.bgElevated,
                surfaceContainerHigh = backgroundColors.bgElevated,
                surfaceContainerHighest = backgroundColors.bgSubtle,
                error = contentColors.contentCritical,
                onError = contentColors.contentCriticalOnColor,
                errorContainer = backgroundColors.bgCriticalSubtle,
                onErrorContainer = contentColors.contentCritical,
                outline = borderColors.borderNeutralMedium,
                outlineVariant = borderColors.borderNeutralLow,
            ),
            content = content
        )
    }
}
