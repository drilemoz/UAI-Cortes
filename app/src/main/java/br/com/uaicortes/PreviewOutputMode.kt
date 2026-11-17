package br.com.uaicortes

/** Preview output choices use the same framing parameters as ExportScreen. */
internal enum class PreviewOutputMode(val label: String, val destination: ExportDestination,
    val frameMode: VerticalFrameMode) {
    ORIGINAL("Original", ExportDestination.ORIGINAL, VerticalFrameMode.FIT),
    VERTICAL_FIT("9:16 Ajustar", ExportDestination.VERTICAL, VerticalFrameMode.FIT),
    VERTICAL_FILL("9:16 Preencher", ExportDestination.VERTICAL, VerticalFrameMode.CENTER_CROP)
}
