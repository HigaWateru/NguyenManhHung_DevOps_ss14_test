package vn.edu.ptit.campushub.reaction;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import vn.edu.ptit.campushub.post.Post;
import vn.edu.ptit.campushub.user.User;

@Entity
@Table(name = "reactions", uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"}))
public class Reaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    protected Reaction() {
    }

    public Reaction(Post post, User user) {
        this.post = post;
        this.user = user;
    }
}
