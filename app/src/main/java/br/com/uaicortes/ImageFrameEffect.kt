package br.com.uaicortes

import android.graphics.Matrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation

/** Media3 aplica a mesma geometria da prévia após Presentation FIT.
 * O eixo Y é invertido porque a origem do GL é na parte inferior.
 */
@UnstableApi
internal class ImageFrameEffect(
    private val clip: Clip,
    private val sourceAspect: Float,
    private val canvasAspect: Float
) : MatrixTransformation {
    override fun getMatrix(presentationTimeUs: Long): Matrix {
        val t = ImageFraming.resolve(clip.imageFrameMode, clip.imageZoom,
            clip.imagePanX, clip.imagePanY, sourceAspect, canvasAspect)
        return Matrix().apply {
            setScale(t.scale, t.scale)
            postTranslate(t.offsetX, -t.offsetY)
        }
    }
}
