package in.aerem.comconbeacons;

import in.aerem.comconbeacons.models.*;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.util.List;

// See Swagger API documentation at http://85.143.222.113/api/documentation
public interface PositionsWebService {
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("register")
    Call<LoginResponse> register(@Body RegisterRequest request);

    @POST("positions")
    Call<PositionsResponse> positions(@Header("Authorization") String authorization, @Body PositionsRequest request);

    @GET("users")
    Call<List<UsersResponse>> users();
}

