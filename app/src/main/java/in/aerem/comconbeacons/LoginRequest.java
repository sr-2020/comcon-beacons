package in.aerem.comconbeacons;

public class LoginRequest {
    public String email;
    public String password;

    public  LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
