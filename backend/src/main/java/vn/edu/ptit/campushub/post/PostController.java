package vn.edu.ptit.campushub.post;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import vn.edu.ptit.campushub.post.PostDtos.CommentRequest;
import vn.edu.ptit.campushub.post.PostDtos.CommentView;
import vn.edu.ptit.campushub.post.PostDtos.PostRequest;
import vn.edu.ptit.campushub.post.PostDtos.PostView;
import vn.edu.ptit.campushub.post.PostDtos.StatusRequest;
import vn.edu.ptit.campushub.post.PostService.PageView;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService service;

    public PostController(PostService service) {
        this.service = service;
    }

    @GetMapping
    PageView<PostView> search(@RequestParam(defaultValue = "") String keyword,
                              @RequestParam(required = false) Long categoryId,
                              @RequestParam(defaultValue = "0") int page,
                              @RequestParam(defaultValue = "12") int size,
                              Principal principal) {
        return service.search(keyword, categoryId, page, size, principal);
    }

    @GetMapping("/{id}")
    PostView findOne(@PathVariable Long id, Principal principal) {
        return service.findOne(id, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    PostView create(@Valid @RequestBody PostRequest request, Principal principal) {
        return service.create(request, principal);
    }

    @PutMapping("/{id}")
    PostView update(@PathVariable Long id, @Valid @RequestBody PostRequest request, Principal principal) {
        return service.update(id, request, principal);
    }

    @PatchMapping("/{id}/status")
    PostView changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest request,
                          Principal principal) {
        return service.changeStatus(id, request.status(), principal);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable Long id, Principal principal) {
        service.delete(id, principal);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    CommentView addComment(@PathVariable Long id, @Valid @RequestBody CommentRequest request,
                           Principal principal) {
        return service.addComment(id, request, principal);
    }

    @PostMapping("/{id}/reaction")
    Map<String, Boolean> toggleReaction(@PathVariable Long id, Principal principal) {
        return Map.of("reacted", service.toggleReaction(id, principal));
    }
}
