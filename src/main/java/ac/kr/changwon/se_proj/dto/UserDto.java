package ac.kr.changwon.se_proj.dto;

import ac.kr.changwon.se_proj.model.User;
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

    public UserDto(String id, String username, String password) {
        this.id = id;
        this.username = username;
        this.password = password;
    }

    public UserDto(String id, String username, String password, String email) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
    }


    public User toEntity(){
        User u=new User();
        u.setId(this.id);
        u.setUsername(this.username);
        u.setPassword(this.password);
        u.setEmail(this.email);

        return u;
    }


    public static UserDto fromEntity(User u){
        return new UserDto(u.getId(),
                u.getUsername(),
                null,
                u.getEmail());
    }
}
