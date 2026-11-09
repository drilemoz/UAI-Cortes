#!/usr/bin/env python3
"""Compila e executa regras reais de recorte do quadro fora de Android."""
from pathlib import Path
import shutil
import subprocess
import tempfile

root=Path(__file__).resolve().parents[1]
source=root/'app/src/main/java/br/com/uaicortes/FrameSnapshotTiming.kt'
assert source.is_file()
compiler=shutil.which('kotlinc')
if compiler is None:
    print('SKIP: kotlinc indisponível; teste Kotlin/JVM permanece no CI')
else:
    with tempfile.TemporaryDirectory() as temp:
        main=Path(temp)/'SnapshotTimingMain.kt'
        main.write_text('''package br.com.uaicortes
fun main() {
    val x = FrameSnapshotTiming.slice(1000,5000,3250)!!
    check(x.startMs==3180L && x.endMs==3440L && x.frameAtMs==70L)
    val y = FrameSnapshotTiming.slice(1000,2000,1999)!!
    check(y.endMs==2000L && y.frameAtMs==70L)
    check(FrameSnapshotTiming.slice(100,500,100)!!.frameAtMs==0L)
    check(FrameSnapshotTiming.slice(400,300,350)==null)
    check(FrameSnapshotTiming.overlap(190,50,200,250)==200L..239L)
    check(FrameSnapshotTiming.overlap(0,30,100,200)==null)
    check(FrameSnapshotTiming.overlap(100,30,100,200)==100L..129L)
    println("OK: 7 verificações Kotlin/JVM do recorte para quadro")
}
''')
        jar=Path(temp)/'snapshot.jar'
        subprocess.run([compiler, str(source),str(main),'-include-runtime','-d',str(jar)],check=True,timeout=45)
        subprocess.run(['java','-jar',str(jar)],check=True,timeout=30)
