package ac.kr.changwon.se_proj.Model;

import jakarta.persistence.*;
import lombok.Data;

//로그인 요청 모델 클래스

@Data
public class LoginRequest {
    private String id;

    @Column(unique = true, nullable = false)
    private String password;
}
