package vn.edu.ptit.campushub.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.campushub.common.ApiException;
import vn.edu.ptit.campushub.user.Role;
import vn.edu.ptit.campushub.user.User;
import vn.edu.ptit.campushub.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    UserView register(@Valid @RequestBody RegisterRequest request) {
        String username = request.username().trim().toLowerCase();
        if (users.existsByUsername(username)) {
            throw new ApiException(HttpStatus.CONFLICT, "Tên đăng nhập đã được sử dụng");
        }
        User user = users.save(new User(username, request.displayName().trim(),
                passwordEncoder.encode(request.password()), Role.USER));
        return UserView.from(user);
    }

    @GetMapping("/me")
    UserView me(Principal principal) {
        return UserView.from(findUser(principal));
    }

    private User findUser(Principal principal) {
        return users.findByUsername(principal.getName())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Tài khoản không hợp lệ"));
    }

    public record RegisterRequest(
            @NotBlank(message = "Tên đăng nhập không được để trống")
            @Pattern(regexp = "[a-zA-Z0-9._-]+", message = "Tên đăng nhập chỉ gồm chữ không dấu, số, dấu chấm, gạch ngang hoặc gạch dưới")
            @Size(min = 3, max = 60, message = "Tên đăng nhập cần từ 3 đến 60 ký tự") String username,
            @NotBlank(message = "Tên hiển thị không được để trống")
            @Size(max = 100, message = "Tên hiển thị tối đa 100 ký tự") String displayName,
            @NotBlank(message = "Mật khẩu không được để trống")
            @Size(min = 6, max = 72, message = "Mật khẩu cần từ 6 đến 72 ký tự") String password) {
    }

    public record UserView(Long id, String username, String displayName, Role role) {
        static UserView from(User user) {
            return new UserView(user.getId(), user.getUsername(), user.getDisplayName(), user.getRole());
        }
    }
}
