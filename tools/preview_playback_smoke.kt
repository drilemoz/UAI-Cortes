import br.com.uaicortes.PreviewPlayback
fun main() {
  check(PreviewPlayback.SPEEDS == listOf(.25f, .5f, .75f, 1f, 1.25f, 1.5f, 2f))
  check(PreviewPlayback.label(.25f) == "0,25×")
  check(PreviewPlayback.label(1f) == "1×")
  check(PreviewPlayback.label(1.5f) == "1,5×")
  check(PreviewPlayback.label(2f) == "2×")
  val invalid = runCatching { PreviewPlayback.label(3f) }.exceptionOrNull()
  check(invalid is IllegalArgumentException)
  println("PASS: PreviewPlayback 7 velocidades, formatação pt-BR e validação")
}
