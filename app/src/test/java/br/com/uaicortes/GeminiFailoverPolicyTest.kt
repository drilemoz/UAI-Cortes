package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class GeminiFailoverPolicyTest {
    @Test fun onlyDistinctProjectsAfter429() {
        val tried = setOf("video-prod")
        assertTrue(GeminiFailoverPolicy.canSwitch(429, true, "video-prod", "narracao-backup", tried, false))
        assertFalse(GeminiFailoverPolicy.canSwitch(429, true, "video-prod", "video-prod", tried, false))
        assertFalse(GeminiFailoverPolicy.canSwitch(429, true, "", "narracao-backup", tried, false))
        assertFalse(GeminiFailoverPolicy.canSwitch(429, true, "video-prod", "", tried, false))
        assertFalse(GeminiFailoverPolicy.canSwitch(403, true, "video-prod", "narracao-backup", tried, false))
        assertFalse(GeminiFailoverPolicy.canSwitch(402, true, "video-prod", "narracao-backup", tried, false))
        assertFalse(GeminiFailoverPolicy.canSwitch(429, false, "video-prod", "narracao-backup", tried, false))
        assertFalse(GeminiFailoverPolicy.canSwitch(429, true, "video-prod", "narracao-backup", tried, true))
        assertFalse(GeminiFailoverPolicy.canSwitch(429, true, "video-prod", "narracao-backup", setOf("narracao-backup"), false))
    }
}
