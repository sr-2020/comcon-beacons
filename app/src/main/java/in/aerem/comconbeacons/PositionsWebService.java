package in.aerem.comconbeacons;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface PositionsWebService {
    @POST("login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("register")
    Call<LoginResponse> register(@Body LoginRequest request);

    @POST("positions")
    Call<PositionsResponse> positions(@Header("Authorization") String authorization, @Body PositionsRequest request);
}

