package io.rippledown.main

import androidx.compose.ui.awt.ComposeWindow
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

private const val ICON_RESOURCE_PATH = "drawable/water_wave_icon.png"

/**
 * Application icon, decoded synchronously from the classpath at class-load time.
 * Loading via the Compose Multiplatform [painterResource] is not reliable for
 * window icons because it is a `@Composable` that loads asynchronously and may
 * leave AWT's title-bar / taskbar icons stuck on the JVM default.
 */
val WindowIconImage: BufferedImage? = run {
    val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(ICON_RESOURCE_PATH)
    if (stream == null) {
        System.err.println("[icon] resource not found on classpath: $ICON_RESOURCE_PATH")
        null
    } else {
        stream.use { ImageIO.read(it) }
    }
}

/**
 * Applies [WindowIconImage] to the given [ComposeWindow]. Sets both the singular
 * (Frame.setIconImage) and plural (Window.setIconImages) APIs so that Windows'
 * title-bar and taskbar both pick up the application icon.
 */
fun applyAppIcon(window: ComposeWindow) {
    val image = WindowIconImage ?: return
    window.setIconImage(image)
    window.iconImages = listOf(image)
}
