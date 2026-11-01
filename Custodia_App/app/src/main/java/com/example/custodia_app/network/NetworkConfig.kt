package com.example.custodia_app.network

import com.example.custodia_app.BuildConfig
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

data class LoginRequest(val cedula: String, val password: String)
data class LoginUsuario(val nombres: String)
data class LoginResponse(val status: String, val mensaje: String, val usuario: LoginUsuario? = null)
data class ApiResponse(val status: String, val mensaje: String)
data class AccionRequest(val accion: String, val observacion: String = "")
data class EstadoResponse(val status: String, val estado_ruta: String? = null, val mensaje: String? = null)

interface ApiService {
    @POST("login.php")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @Multipart
    @POST("reportes_app.php")
    suspend fun enviarReporte(
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
        @Part("placa") placa: RequestBody,
        @Part("disco") disco: RequestBody,
        @Part("kilometraje") kilometraje: RequestBody,
        @Part("observaciones") obs: RequestBody,
        @Part fotos: List<MultipartBody.Part>
    ): ApiResponse

    @POST("app_acciones.php")
    suspend fun enviarAccion(@Body request: AccionRequest): EstadoResponse

    @GET("verificar_estado.php")
    suspend fun verificarEstado(@Query("ruta") ruta: String): EstadoResponse
}

object RetrofitClient {
    val instance: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
