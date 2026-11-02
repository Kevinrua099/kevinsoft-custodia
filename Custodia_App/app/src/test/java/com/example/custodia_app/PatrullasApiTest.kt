package com.example.custodia_app

import com.example.custodia_app.network.FotoPatrulla
import com.example.custodia_app.network.PatrullasResponse
import com.example.custodia_app.network.urlFotoPatrulla
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PatrullasApiTest {
    @Test
    fun interpretaRespuestaConFotosYCamposOpcionales() {
        val json = """{"status":"success","data":[{"id_inspeccion":"7","placa":"ABC-123","disco":"42","kilometraje":null,"fecha_hora_recepcion":"2026-11-01 12:30:00","usuario_nombre":"Operador","fotos":[{"id":"3","guardada":true,"ruta_anterior":null}]}]}"""
        val resultado = Gson().fromJson(json, PatrullasResponse::class.java)
        val inspeccion = resultado.data!!.single()

        assertEquals(7L, inspeccion.id)
        assertEquals("Operador", inspeccion.usuarioNombre)
        assertNull(inspeccion.kilometraje)
        assertEquals("${BuildConfig.API_BASE_URL}patrullas.php?foto=3", urlFotoPatrulla(inspeccion.fotos!!.single()))
    }

    @Test
    fun aceptaSoloRutasAnterioresDePatrullas() {
        val fotoAnterior = FotoPatrulla(1, false, "uploads/patrullas/foto_1.jpg")
        assertEquals(java.net.URI(BuildConfig.API_BASE_URL).resolve("../web/uploads/patrullas/foto_1.jpg").toString(), urlFotoPatrulla(fotoAnterior))
        assertNull(urlFotoPatrulla(FotoPatrulla(2, false, "../../api/login.php")))
    }
}
