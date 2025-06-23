package ac.kr.changwon.se_proj.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @Column(length = 255
    , updatable = false,
    nullable = false)
    private String id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false, unique = true)
    private String password;

    private String email;

    private String role;

    @Transient
    private boolean isNew = false;

    @ManyToMany(mappedBy = "members")
    private Set<Project> projects = new HashSet<>();

    /** DB에서 로딩할 땐 new 플래그를 꺼줘야 함 */
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    // User와 UserChatRoom 간의 One-to-Many 관계 설정
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserChatRoom> userChatRooms = new HashSet<>();

    @Column(name = "profile_img_url")
    private String profile_img_url;
}
