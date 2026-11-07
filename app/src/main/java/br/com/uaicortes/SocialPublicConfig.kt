package br.com.uaicortes

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Importação/exportação APENAS de identificadores públicos (nunca tokens OAuth). */
internal object SocialPublicConfig {
    fun export(data: SocialData): String = JSONObject().put("format", "uai-social-public-v1")
        .put("accounts", JSONArray().also { a -> data.accounts.forEach { account ->
            a.put(JSONObject().put("destination", account.destination.name)
                .put("label", account.label))
        } }).toString(2)

    fun merge(data: SocialData, bytes: ByteArray): SocialData {
        require(bytes.size in 1..100_000)
        val json = JSONObject(String(bytes, Charsets.UTF_8))
        require(json.length() == 2 && json.getString("format") == "uai-social-public-v1")
        val accounts = json.getJSONArray("accounts")
        require(accounts.length() <= 30)
        val merged = data.accounts.toMutableList()
        repeat(accounts.length()) { index ->
            val entry = accounts.getJSONObject(index)
            require(entry.length() == 2) { "Configuração inválida." }
            val dest = requireNotNull(SocialDestination.parse(entry.getString("destination")))
            val label = entry.getString("label").trim()
            require(label.length in 1..70)
            if (merged.none { it.destination == dest && it.label == label })
                merged.add(SocialAccount(id = UUID.randomUUID().toString(), destination = dest,
                    label = label))
        }
        require(merged.size <= 30)
        return data.copy(accounts = merged)
    }
}

