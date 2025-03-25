package ac.kr.changwon.se_proj.UserRepository;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "user")
@Data
/* indicate for user.
* username : nickname
* id : user id for login
* password : for login Authenticate
* email : just e-mail*/
public class User{

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private String id;

    private String username;

    private String password;

    private String email;

    /*비밀번호 검증 메서드*/

    /*public boolean checkPassword(String password) {
        return this.password.equals(password);
    }*/

    /*무결성을 위한 메서드*/

    /*public boolean isAvailable() {
        return true;
    }*/

    //위 메서드들은 여기서 필요없는 것 같아서 삭제
    //해당 기능은 UserService에서 구현




    /* 비밀번호의 경우 프론트 단에서 보안을 위해 별표 또는 가릴 수 있는
    표기로 사용자 입력 값 처리.
    들어오는 값은 실제 값(데이터베이스에 저장할 값)*/






}
