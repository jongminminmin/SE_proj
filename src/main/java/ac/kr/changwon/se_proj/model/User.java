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
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
/* indicate for user.
* username : nickname
* id : user id for login
* password : for login Authenticate
* email : just e-mail*/
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="project_id")
    private Project project;


    // 비밀번호 재설정 토큰 필드 추가
    @Column(name = "password_reset_token")
    private String passwordResetToken;

    // 비밀번호 재설정 토큰 만료 시간 필드 추가
    @Column(name = "password_reset_token_expiry")
    private LocalDateTime passwordResetTokenExpiry;



    /** DB에서 로딩할 땐 new 플래그를 꺼줘야 함 */
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    //public Object getUserId() {}

    // User와 UserChatRoom 간의 One-to-Many 관계 설정
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserChatRoom> userChatRooms = new HashSet<>();
}
