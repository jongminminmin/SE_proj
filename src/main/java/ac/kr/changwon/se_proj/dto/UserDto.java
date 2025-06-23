package ac.kr.changwon.se_proj.dto;

import ac.kr.changwon.se_proj.model.User;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // null인 필드는 JSON으로 만들지 않음
public class UserDto {
    @NotBlank private String id;
    @NotBlank private String username;
    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @Pattern(regexp = "(?=.*[\\W_]).+", message = "비밀번호에는 특수문자가 최소 하나 이상 포함되어야 합니다.")
    private String password;
    @NotBlank private String email;

    public static UserDto fromEntity(User user){
        if (user == null) return null;
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                // 비밀번호는 DTO에 포함하지 않음
                .email(user.getEmail())
                .build();
    }
}
