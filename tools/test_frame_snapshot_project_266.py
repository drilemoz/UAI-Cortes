#!/usr/bin/env python3
"""Compila os helpers REAIS de snapshot com stubs mínimos e executa casos de ramo.

Não substitui build Android/Media3; valida rotas e geometria da lógica pura.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile

root=Path(__file__).resolve().parents[1]
src=root/'app/src/main/java/br/com/uaicortes'
compiler=shutil.which('kotlinc')
if not compiler:
    print('SKIP: kotlinc ausente')
else:
    stubs = '''package br.com.uaicortes

data class VoiceTone(val enabled:Boolean=false)
data class Clip(val uri:String, val title:String="V",val id:String="C",val isImage:Boolean=false,
    val startMs:Long=0L,val endMs:Long=10000L,val volume:Float=1f,
    val voiceTone:VoiceTone=VoiceTone()) {
    val durationMs:Long get()=endMs-startMs
}
data class Overlay(val uri:String,val startAtMs:Long,val durationMs:Long,
    val isImage:Boolean=false,val sourceStartMs:Long=0L,val volume:Float=1f,val muted:Boolean=false)
data class SubtitleWordTiming(val startMs:Long,val endMs:Long)
data class SubtitleCue(val text:String,val startMs:Long,val endMs:Long,
    val wordTimings:List<SubtitleWordTiming> = emptyList())
data class TextWatermark(val label:String="watermark") {
    fun toSubtitle(durationMs: Long):SubtitleCue = SubtitleCue(label,0L,durationMs)
}
data class ClipTransition(val kind:String="corte",val durationMs:Long=0L)
data class VideoProject(val clips:List<Clip>,val overlays:List<Overlay> = emptyList(),
    val subtitles:List<SubtitleCue> = emptyList(),val textWatermark:TextWatermark?=null,
    val transitions:List<ClipTransition> = emptyList(),val music:String?=null,
    val audioTracks:List<String> = emptyList()) {
    val durationMs:Long get()=clips.sumOf { it.durationMs }
}
object SubtitlePreviewPolicy {
    fun firstAt(cues:List<SubtitleCue>,at:Long)=cues.firstOrNull { at>=it.startMs && at<it.endMs }
}
object ProjectNaming { fun exportName(title:String)=title }
object TransitionEffects { const val CUT="corte" }
object EditFeatures {
    fun boundary(project:VideoProject,index:Int):ClipTransition? =project.transitions.getOrNull(index)
    fun transitionMs(t:ClipTransition?,a:Long,b:Long):Long =
        if (t==null || t.kind==TransitionEffects.CUT) 0 else minOf(t.durationMs,a,b)
}
data class Seek(val clipIndex:Int,val localMs:Long)
object TimelinePlaybackCoordinator {
    fun target(durations:List<Long>,projectMs:Long):Seek? {
        if (durations.isEmpty()) return null
        var consumed=0L
        for ((i,d) in durations.withIndex()) {
            if (projectMs<=consumed+d) return Seek(i,(projectMs-consumed).coerceIn(0L,d))
            consumed+=d
        }
        return Seek(durations.lastIndex,durations.last())
    }
}
'''
    checks='''package br.com.uaicortes
fun main() {
    val clip=Clip(uri="content://v",endMs=6000)
    val plain=VideoProject(clips=listOf(clip))
    val simple=FrameSnapshotProject.prepare(plain,4200L,1080)!!
    check(simple.directSourceFrame)
    check(simple.sourceFrame.uri=="content://v")
    check(simple.sourceFrame.sourceTimeMs==4200L)
    check(simple.project.clips.single().durationMs==1600L)
    val pip=Overlay(uri="content://pip",startAtMs=2000L,durationMs=4000L)
    val composite=FrameSnapshotProject.prepare(plain.copy(overlays=listOf(pip)),3200L,720)!!
    check(!composite.directSourceFrame)
    check(composite.targetInVideoMs==500L)
    check(composite.project.overlays.single().sourceStartMs==700L)
    check(composite.project.overlays.single().durationMs==1600L)
    val sub=SubtitleCue("fala",3800,4700)
    check(!FrameSnapshotProject.prepare(plain.copy(subtitles=listOf(sub)),4200L,720)!!.directSourceFrame)
    check(FrameSnapshotProject.prepare(plain.copy(subtitles=listOf(sub)),5100L,720)!!.directSourceFrame)
    check(!FrameSnapshotProject.prepare(plain.copy(textWatermark=TextWatermark()),5100L,720)!!.directSourceFrame)
    val clip2=Clip(uri="content://second",id="b",endMs=5000)
    check(FrameSnapshotProject.prepare(plain.copy(clips=listOf(clip,clip2)),6000L,720)!!.sourceFrame.uri=="content://v")
    println("OK: 10 verificações Kotlin/JVM do snapshot real (original, PiP, legenda, marca, borda)")
}
'''
    with tempfile.TemporaryDirectory(prefix='uaicortes-snapshot-266-') as tmp:
        d=Path(tmp); (d/'Stubs.kt').write_text(stubs);(d/'Main.kt').write_text(checks)
        jar=d/'snapshot-test.jar'
        subprocess.run([compiler,str(src/'FrameSnapshotProject.kt'),str(src/'FrameSnapshotTiming.kt'),
            str(src/'FrameCapturePlan.kt'),str(d/'Stubs.kt'),str(d/'Main.kt'),'-include-runtime','-d',str(jar)],
            check=True, timeout=45)
        subprocess.run(['java','-jar',str(jar)],check=True,timeout=15)
