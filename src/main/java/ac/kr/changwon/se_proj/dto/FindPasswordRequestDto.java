package ac.kr.changwon.se_proj.dto;


import lombok.Data;

@Data
public class FindPasswordRequestDto {
    private String userId;
    private String email;
}
