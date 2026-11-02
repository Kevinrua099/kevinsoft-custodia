package com.example.custodia_app.network

import com.example.custodia_app.BuildConfig
import com.google.gson.annotations.SerializedName
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

data class LoginRequest(val cedula: String, val password: String)
data class LoginUsuario(val nombres: String, val id: Int? = null)
data class LoginResponse(val status: String, val mensaje: String, val usuario: LoginUsuario? = null)
data class ApiResponse(val status: String, val mensaje: String)
data class Ruta(
    @SerializedName("numero_registro") val numero: String,
    @SerializedName("fecha_emision") val fecha: String? = null,
    val origen: String? = null,
    val destino: String? = null,
    @SerializedName("estado_ruta") val estado: String,
    val placa: String? = null,
    val disco: String? = null
)
data class RutasResponse(val status: String, val mensaje: String? = null, val data: List<Ruta>? = null)
data class FotoPatrulla(
    val id: Long,
    val guardada: Boolean,
    @SerializedName("ruta_anterior") val rutaAnterior: String? = null
)
data class InspeccionPatrulla(
    @SerializedName("id_inspeccion") val id: Long,
    val placa: String? = null,
    val disco: String? = null,
    val kilometraje: Int? = null,
    val observaciones: String? = null,
    @SerializedName("fecha_hora_recepcion") val fechaRecepcion: String? = null,
    @SerializedName("usuario_nombre") val usuarioNombre: String? = null,
    @SerializedName("tipo_inspeccion") val tipoInspeccion: String? = null,
    val fotos: List<FotoPatrulla>? = null
)
data class PatrullasResponse(
    val status: String,
    val mensaje: String? = null,
    val data: List<InspeccionPatrulla>? = null
)

fun urlFotoPatrulla(foto: FotoPatrulla): String? {
    if (foto.guardada) return "${BuildConfig.API_BASE_URL}patrullas.php?foto=${foto.id}"
    val ruta = foto.rutaAnterior ?: return null
    if (!Regex("^uploads/patrullas/[a-zA-Z0-9_.-]+$").matches(ruta)) return null
    return java.net.URI(BuildConfig.API_BASE_URL).resolve("../web/$ruta").toString()
}

interface ApiService {
    @POST("login.php")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @Multipart
    @POST("reportes_app.php")
    suspend fun enviarReporte(
        @Part("numero_registro") numeroRegistro: RequestBody,
        @Part("usuario_id") usuarioId: RequestBody,
        @Part("origen") origen: RequestBody,
        @Part("data_json") dataJson: RequestBody,
        @Part("observaciones") observaciones: RequestBody,
        @Part("tipo_reporte") tipoReporte: RequestBody,
        @Part fotos: List<MultipartBody.Part>
    ): ApiResponse

    @Multipart
    @POST("patrullas.php")
    suspend fun enviarInspeccion(
        @Part("usuario_nombre") usuario: RequestBody,
        @Part("usuario_id") usuarioId: RequestBody,
        @Part("tipo_inspeccion") tipo: RequestBody,
        @Part("placa") placa: RequestBody,
        @Part("disco") disco: RequestBody,
        @Part("kilometraje") kilometraje: RequestBody,
        @Part("observaciones") obs: RequestBody,
        @Part fotos: List<MultipartBody.Part>
    ): ApiResponse

    @GET("patrullas.php")
    suspend fun listarInspecciones(): PatrullasResponse

    @GET("historial_app.php")
    suspend fun listarRutas(@Query("usuario") usuarioId: Int): RutasResponse
}

object RetrofitClient {
    val instance: ApiService by lazy {
        // Render puede tardar en despertar después de un período de inactividad.
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
