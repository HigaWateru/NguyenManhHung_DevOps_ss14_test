package vn.edu.ptit.campushub.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.edu.ptit.campushub.category.Category;
import vn.edu.ptit.campushub.category.CategoryRepository;
import vn.edu.ptit.campushub.post.Post;
import vn.edu.ptit.campushub.post.PostRepository;
import vn.edu.ptit.campushub.user.Role;
import vn.edu.ptit.campushub.user.User;
import vn.edu.ptit.campushub.user.UserRepository;

@Component
public class DemoDataSeeder implements ApplicationRunner {
    private final UserRepository users;
    private final CategoryRepository categories;
    private final PostRepository posts;
    private final PasswordEncoder passwordEncoder;

    public DemoDataSeeder(UserRepository users, CategoryRepository categories, PostRepository posts,
                          PasswordEncoder passwordEncoder) {
        this.users = users;
        this.categories = categories;
        this.posts = posts;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Category events = createCategoryIfMissing("Sự kiện", "su-kien");
        Category lost = createCategoryIfMissing("Đồ thất lạc", "do-that-lac");
        createCategoryIfMissing("Hỏi đáp học tập", "hoi-dap");
        createCategoryIfMissing("Trao đổi đồ dùng", "trao-doi");
        createCategoryIfMissing("Phản ánh", "phan-anh");
        createCategoryIfMissing("Tìm nhóm", "tim-nhom");

        User admin = createUserIfMissing("admin", "Quản trị viên", "admin123", Role.ADMIN);
        User student = createUserIfMissing("student", "Nguyễn Minh Anh", "student123", Role.USER);

        if (posts.count() == 0) {
            posts.save(new Post("Ngày hội câu lạc bộ sinh viên",
                    "Các câu lạc bộ sẽ gặp gỡ sinh viên mới tại sân A2 vào chiều thứ Sáu.", events, admin));
            posts.save(new Post("Tìm ví màu đen gần thư viện",
                    "Mình làm rơi một chiếc ví màu đen vào sáng nay. Bạn nào nhặt được vui lòng bình luận giúp mình.",
                    lost, student));
        }
    }

    private Category createCategoryIfMissing(String name, String slug) {
        return categories.findBySlug(slug)
                .orElseGet(() -> categories.save(new Category(name, slug)));
    }

    private User createUserIfMissing(String username, String displayName, String password, Role role) {
        return users.findByUsername(username)
                .orElseGet(() -> users.save(new User(username, displayName,
                        passwordEncoder.encode(password), role)));
    }
}
