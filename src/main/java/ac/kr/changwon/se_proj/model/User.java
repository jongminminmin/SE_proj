package ac.kr.changwon.se_proj.model;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;

@Entity
@Table(name = "user")
@Data
/* indicate for user.
* username : nickname
* id : user id for login
* password : for login Authenticate
* email : just e-mail*/
public class User implements Persistable<String>, Serializable {

    private static final long serialVersionUID = 1L;


    @Id
    @Column(length = 25
    , updatable = false,
    nullable = false)
    private String id;

    @Column(nullable = false)
    private String username;

    @Column(nullable = true, unique = true)
    private String password;

    private String email;

    private String role;

    @Transient
    private boolean isNew = false;

    public User(String id, String username, String password, String email) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.isNew = true;
    }

    public User() {}

    public User(String userId, String username, String pw, String email, String role) {
        this.id = userId;
        this.username = username;
        this.password = pw;
        this.email = email;
        this.role = role;
        this.isNew = true;
    }


    @Override
    @Transient
    public boolean isNew() {
        return this.isNew;
    }

    @Override
    public String getId() {
        return this.id;
    }


    /** DB에서 로딩할 땐 new 플래그를 꺼줘야 함 */
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }
}
