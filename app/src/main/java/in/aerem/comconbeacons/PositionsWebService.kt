package `in`.aerem.comconbeacons

import `in`.aerem.comconbeacons.models.*
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

// See Swagger API documentation at http://85.143.222.113/api/documentation
interface PositionsWebService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>

    @POST("register")
    fun register(@Body request: RegisterRequest): Call<LoginResponse>

    @POST("positions")
    fun positions(@Header("Authorization") authorization: String, @Body request: PositionsRequest): Call<PositionsResponse>

    @GET("users")
    fun users(): Call<List<UsersResponse>>
}

