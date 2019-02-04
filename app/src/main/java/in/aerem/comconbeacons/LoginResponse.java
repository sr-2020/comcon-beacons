package in.aerem.comconbeacons;

public class LoginResponse {
    public int id;
    public String api_key;

    public LoginResponse(int id, String api_key) {
        this.id = id;
        this.api_key = api_key;
    }
}
