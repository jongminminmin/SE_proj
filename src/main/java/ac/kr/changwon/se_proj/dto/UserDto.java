package ac.kr.changwon.se_proj.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserDto {
    @NotBlank private String id;
    @NotBlank private String username;
    @NotBlank private String password;
    @NotBlank private String email;

    public UserDto() {

    }
}
