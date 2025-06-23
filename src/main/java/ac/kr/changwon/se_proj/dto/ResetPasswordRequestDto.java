package ac.kr.changwon.se_proj.dto;

import lombok.Data;

@Data
public class ResetPasswordRequestDto {
    private String userId;
    private String email;
    private String newPassword;
} 