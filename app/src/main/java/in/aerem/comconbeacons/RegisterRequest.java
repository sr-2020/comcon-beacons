package in.aerem.comconbeacons;

public class RegisterRequest {
    public String email;
    public String name;
    public String password;

    public RegisterRequest(String email, String name, String password) {
        this.email = email;
        this.name = name;
        this.password = password;
    }
}
