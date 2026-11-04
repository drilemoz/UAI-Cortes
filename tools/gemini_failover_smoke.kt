// Teste JVM puro e independente do Android/Google. Executável com kotlinc.
package br.com.uaicortes

fun main() {
    fun ok(value: Boolean, reason: String) { check(value) { reason } }
    val tried = setOf("projeto-original")
    ok(GeminiFailoverPolicy.canSwitch(429,true,"projeto-original","outro-projeto",tried,false), "Projeto alternativo elegível")
    ok(!GeminiFailoverPolicy.canSwitch(429,true,"projeto-original","projeto-original",tried,false), "Mesmo projeto não pode rodar")
    ok(!GeminiFailoverPolicy.canSwitch(429,true,"projeto-original","outro-projeto",setOf("outro-projeto"),false), "Não repetir projeto")
    ok(!GeminiFailoverPolicy.canSwitch(429,true,"projeto-original","outro-projeto",tried,true), "Pausa deve ser respeitada")
    ok(!GeminiFailoverPolicy.canSwitch(429,false,"projeto-original","outro-projeto",tried,false), "Opt-in obrigatório")
    ok(!GeminiFailoverPolicy.canSwitch(401,true,"projeto-original","outro-projeto",tried,false), "Não retentar autenticação")
    ok(!GeminiFailoverPolicy.canSwitch(402,true,"projeto-original","outro-projeto",tried,false), "Não retentar cobrança")
    ok(!GeminiFailoverPolicy.canSwitch(403,true,"projeto-original","outro-projeto",tried,false), "Não retentar permissão")
    ok(!GeminiFailoverPolicy.canSwitch(503,true,"projeto-original","outro-projeto",tried,false), "Não retentar servidor")
    ok(!GeminiFailoverPolicy.canSwitch(429,true,"","outro-projeto",tried,false), "ID original requerido")
    ok(!GeminiFailoverPolicy.canSwitch(429,true,"projeto-original","",tried,false), "ID reserva requerido")
    println("OK: 11 cenários GeminiFailoverPolicy")
}
