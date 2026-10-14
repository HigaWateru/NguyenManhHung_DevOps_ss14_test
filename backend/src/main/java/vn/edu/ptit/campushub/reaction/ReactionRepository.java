package vn.edu.ptit.campushub.reaction;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReactionRepository extends JpaRepository<Reaction, Long> {
    Optional<Reaction> findByPostIdAndUserId(Long postId, Long userId);
    long countByPostId(Long postId);
    void deleteByPostId(Long postId);
}
