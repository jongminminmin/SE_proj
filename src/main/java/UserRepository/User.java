package UserRepository;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/* indicate for user.
* username : nickname
* id : user id for login
* password : for login Authenticate
* email : just e-mail*/
public class User {
    private String name;
    private String id;
    private String username;
    private String password;
    private String email;

    /*Generator
    * add to Maria DB
    * 이건 그냥 로그인 할 때 필요한 정보만 생성자로 사용*/
    public User(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }


    /* 비밀번호의 경우 프론트 단에서 보안을 위해 별표 또는 가릴 수 있는 표기로 사용자 입력 값 처리. 들어오는 값은 실제 값(데이터베이스에 저장할 값)*/






}
