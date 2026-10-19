package br.com.uaicortes

import kotlin.math.roundToInt

/** Precisão temporal e mapeamento proporcional entre legenda, prévia e exportação. */
object TimelinePrecision {
    fun step(positionMs: Long, deltaMs: Long, durationMs: Long): Long {
        val max = durationMs.coerceAtLeast(0L)
        val value = when {
            deltaMs > 0 && positionMs > Long.MAX_VALUE - deltaMs -> Long.MAX_VALUE
            deltaMs < 0 && positionMs < Long.MIN_VALUE - deltaMs -> Long.MIN_VALUE
            else -> positionMs + deltaMs
        }
        return value.coerceIn(0L,max)
    }

    /** Retorna o clipe e a posição relativa da legenda na timeline. */
    fun sourceAt(clips: List<Pair<String,Long>>, atMs: Long): Pair<Int,Long>? {
        if (clips.isEmpty()) return null
        var remaining = atMs.coerceAtLeast(0L)
        clips.forEachIndexed { index, (_,duration) ->
            val length=duration.coerceAtLeast(0L)
            if (remaining < length || index==clips.lastIndex)
                return index to remaining.coerceIn(0L,length)
            remaining-=length
        }
        return null
    }
}

object SubtitlePlacement {
    /** O texto tem âncora horizontal central e âncora vertical inferior no Media3. */
    fun anchorPixels(width:Int,height:Int,textWidth:Int,textHeight:Int,
                     x:Float,y:Float): Pair<Int,Int> =
        ((x.coerceIn(0f,1f)*width-textWidth/2f).roundToInt() to
         (y.coerceIn(0f,1f)*height-textHeight).roundToInt())

    fun drag(x:Float,y:Float,deltaX:Float,deltaY:Float,width:Int,height:Int):Pair<Float,Float> {
        if (width<=0 || height<=0 || !deltaX.isFinite() || !deltaY.isFinite()) return x to y
        return nudge(x,y,deltaX/width,deltaY/height)
    }

    fun nudge(x:Float,y:Float,deltaX:Float,deltaY:Float):Pair<Float,Float> =
        (x+deltaX).coerceIn(.1f,.9f) to (y+deltaY).coerceIn(.1f,.9f)
}
