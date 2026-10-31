package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test

class ProjectNamingTest {
    @Test fun trimsAndRejectsEmptyNames() {
        assertEquals("Cortes de viagem", ProjectNaming.title("  Cortes   de   viagem  "))
        assertFalse(ProjectNaming.validTitle(" \n "))
        assertTrue(ProjectNaming.validTitle("Vídeo 01"))
    }
    @Test fun fileNameUsesProjectTitleAndSanitizes() {
        assertEquals("Meu vídeo.mp4", ProjectNaming.exportName("Meu vídeo"))
        assertEquals("Parte_01.mp4", ProjectNaming.exportName("Parte/01"))
        assertEquals("UaiCortes.mp4", ProjectNaming.exportName("..."))
    }
}
