import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String[] passwords = {"User@123456", "Root@123456"};
        for (String pwd : passwords) {
            System.out.println(pwd + " -> " + encoder.encode(pwd));
        }
    }
}
