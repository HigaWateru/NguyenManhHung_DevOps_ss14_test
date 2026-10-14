package vn.edu.ptit.campushub.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import vn.edu.ptit.campushub.comment.Comment;

public final class PostDtos {
    private PostDtos() {
    }

    public record PostRequest(
            @NotBlank(message = "Tiêu đề không được để trống")
            @Size(max = 160, message = "Tiêu đề tối đa 160 ký tự") String title,
            @NotBlank(message = "Nội dung không được để trống")
            @Size(max = 10000, message = "Nội dung tối đa 10.000 ký tự") String content,
            @NotNull(message = "Vui lòng chọn chuyên mục") Long categoryId) {
    }

    public record StatusRequest(@NotNull(message = "Trạng thái không được để trống") PostStatus status) {
    }

    public record CommentRequest(
            @NotBlank(message = "Bình luận không được để trống")
            @Size(max = 1000, message = "Bình luận tối đa 1.000 ký tự") String content) {
    }

    public record AuthorView(Long id, String username, String displayName) {
    }

    public record CategoryView(Long id, String name, String slug) {
    }

    public record CommentView(Long id, String content, AuthorView author, Instant createdAt) {
        public static CommentView from(Comment comment) {
            return new CommentView(comment.getId(), comment.getContent(),
                    new AuthorView(comment.getAuthor().getId(), comment.getAuthor().getUsername(),
                            comment.getAuthor().getDisplayName()), comment.getCreatedAt());
        }
    }

    public record PostView(Long id, String title, String content, PostStatus status,
                           CategoryView category, AuthorView author, Instant createdAt, Instant updatedAt,
                           long reactionCount, long commentCount, boolean reactedByCurrentUser,
                           List<CommentView> comments) {
    }
}
