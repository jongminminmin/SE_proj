package ac.kr.changwon.se_proj.Model;

import lombok.Data;

//로그인 요청 모델 클래스

@Data
public class LoginRequest {
    private String id;
    private String password;
}
