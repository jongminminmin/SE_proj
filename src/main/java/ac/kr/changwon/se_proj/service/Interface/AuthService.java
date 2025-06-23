package ac.kr.changwon.se_proj.service.Interface;

import ac.kr.changwon.se_proj.dto.FindIdRequestDto;
import ac.kr.changwon.se_proj.dto.FindPasswordRequestDto;
import ac.kr.changwon.se_proj.dto.ResetPasswordRequestDto;
import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.model.User;

public interface AuthService {
    boolean login(String userId, String password);
    boolean register(String userId, String username, String password, String email);

    UserDto getUserById(String userId);

    // 아이디 찾기 메서드 추가
    String findUserIdByEmail(FindIdRequestDto findIdRequestDto);

    // 비밀번호 재설정 전 사용자 확인 메서드
    boolean verifyUserForPasswordReset(FindPasswordRequestDto findPasswordRequestDto);

    // 비밀번호 재설정 메서드
    boolean resetPassword(ResetPasswordRequestDto resetPasswordRequestDto);
}
