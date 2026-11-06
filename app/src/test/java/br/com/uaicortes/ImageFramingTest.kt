package br.com.uaicortes

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class ImageFramingTest {
    @Test fun legacyImageHasNoUnexpectedCrop() {
        val image = Clip(uri="content://sample/photo", title="Capa",sourceDurationMs=3000,
            startMs=0, endMs=3000,isImage=true)
        val old = JSONObject().put("id",image.id).put("uri",image.uri)
            .put("title",image.title).put("sourceDurationMs",3000)
            .put("startMs",0).put("endMs",3000).put("isImage",true)
        val restored=Clip.read(old)
        assertEquals(ImageFraming.FIT,restored.imageFrameMode)
        assertEquals(1f,restored.imageZoom,0f)
        assertEquals(0f,restored.imagePanX,0f)
        assertEquals(1f,ImageFraming.resolve(restored.imageFrameMode,restored.imageZoom,
            restored.imagePanX,restored.imagePanY,16f/9f,9f/16f).scale,0f)
    }
    @Test fun fillIsAspectCorrectWithoutStretch() {
        val t=ImageFraming.resolve("fill",1f,0f,0f,16f/9f,9f/16f)
        assertEquals(256f/81f,t.scale,.001f)
        assertEquals(0f,t.availableY,.001f)
        assertTrue(t.availableX>2f)
    }
    @Test fun invalidNumbersNeverEnterSavedProject() {
        val clip=Clip(uri="content://example/photo",title="Teste",sourceDurationMs=3000,
            isImage=true,imageZoom=Float.NaN,imagePanX=Float.POSITIVE_INFINITY,
            imagePanY=9f,imageFrameMode="corrupt")
        val read=Clip.read(clip.json())
        assertEquals(1f,read.imageZoom,0f)
        assertEquals(0f,read.imagePanX,0f)
        assertEquals(1f,read.imagePanY,0f)
        assertEquals("fit",read.imageFrameMode)
    }
    @Test fun panIsBoundedByAvailablePhotoContent() {
        val t=ImageFraming.resolve("fill",1f,1f,1f,16f/9f,9f/16f)
        assertEquals(0f,t.offsetY,.001f)
        assertTrue(t.offsetX<=t.availableX)
        assertEquals(.4f,ImageFraming.dragPan(.4f,200f,300,0f),0f)
    }
}
