package ly.com.tahaben.flavorizr.util

import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

object ImageResizer {

    /**
     * Resizes [sourceFile] to [targetSize] x [targetSize] pixels and writes to [outputFile].
     * Creates parent directories if needed.
     */
    fun resize(sourceFile: File, outputFile: File, targetSize: Int) {
        require(sourceFile.exists()) { "Source image does not exist: $sourceFile" }

        val sourceImage = ImageIO.read(sourceFile)
            ?: throw IllegalStateException("Could not read image: $sourceFile")

        val resized = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB)
        val g2d = resized.createGraphics()
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2d.drawImage(sourceImage, 0, 0, targetSize, targetSize, null)
        g2d.dispose()

        outputFile.parentFile?.mkdirs()
        ImageIO.write(resized, "png", outputFile)
    }
}
