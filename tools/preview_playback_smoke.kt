import br.com.uaicortes.PreviewPlayback
fun main() {
  check(PreviewPlayback.SPEEDS.size == 14)
  check(PreviewPlayback.SPEEDS.first() == 0.1f && PreviewPlayback.SPEEDS.last() == 6f)
  check(PreviewPlayback.label(.1f) == "0,1×")
  check(PreviewPlayback.label(1f) == "1×")
  check(PreviewPlayback.label(6f) == "6×")
  check(PreviewPlayback.previous(1f) == .75f)
  check(PreviewPlayback.next(1f) == 1.25f)
  check(PreviewPlayback.previous(.1f) == .1f && PreviewPlayback.next(6f) == 6f)
  check(runCatching { PreviewPlayback.label(7f) }.isFailure)
  println("PASS: 14 velocidades, +/- com limites, nomes pt-BR")
}
