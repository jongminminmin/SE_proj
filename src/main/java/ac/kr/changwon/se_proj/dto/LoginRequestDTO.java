package ac.kr.changwon.se_proj.dto;


import lombok.Data;

@Data
public class LoginRequestDTO {
    private String userId;
    private String password;
}
