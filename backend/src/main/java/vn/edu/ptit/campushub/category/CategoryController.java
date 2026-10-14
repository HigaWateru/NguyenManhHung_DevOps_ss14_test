package vn.edu.ptit.campushub.category;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryRepository categories;

    public CategoryController(CategoryRepository categories) {
        this.categories = categories;
    }

    @GetMapping
    List<CategoryView> findAll() {
        return categories.findAll().stream()
                .map(category -> new CategoryView(category.getId(), category.getName(), category.getSlug()))
                .toList();
    }

    public record CategoryView(Long id, String name, String slug) {
    }
}
