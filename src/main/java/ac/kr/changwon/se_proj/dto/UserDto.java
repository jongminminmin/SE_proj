package ac.kr.changwon.se_proj.dto;

import lombok.Data;

@Data
public class UserDto {
    private String id;
    private String username;
    private String password;
    private String email;
}
