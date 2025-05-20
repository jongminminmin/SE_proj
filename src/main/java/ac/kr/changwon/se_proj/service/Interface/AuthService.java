package ac.kr.changwon.se_proj.service.Interface;

import ac.kr.changwon.se_proj.dto.FindIdRequestDto;
import ac.kr.changwon.se_proj.dto.FindPasswordRequestDto;

public interface AuthService {
    boolean login(String userId, String password);
    boolean register(String userId, String username, String password, String email);

    // 아이디 찾기 메서드 추가
    String findUserIdByEmail(FindIdRequestDto findIdRequestDto);

    // 비밀번호 재설정 전 사용자 확인 메서드 추가
    String generatePasswordResetToken(FindPasswordRequestDto findPasswordRequestDto);
}
