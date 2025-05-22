package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.dto.FindIdRequestDto;
import ac.kr.changwon.se_proj.dto.FindPasswordRequestDto;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.service.Interface.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // 회원가입 시 사용

    @Override
    public boolean login(String userId, String password) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            // BCryptPasswordEncoder는 암호화된 비밀번호와 평문 비밀번호를 비교합니다.
            // DB에 저장된 비밀번호가 BCrypt로 암호화되어 있어야 합니다.
            if (passwordEncoder.matches(password, user.getPassword())) {
                return true; // 로그인 성공
            } else {
                throw new org.springframework.security.authentication.BadCredentialsException("비밀번호 불일치");
            }
        }
        return false; // 사용자가 존재하지 않거나 비밀번호 불일치
    }

    @Override
    public boolean register(String id, String username, String password, String email) {
        if (userRepository.findById(id).isPresent() || userRepository.findByEmail(email).isPresent()) {
            return false; // 이미 존재하는 아이디 또는 이메일
        }
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password)); // 비밀번호 암호화
        user.setEmail(email);
        // 추가적인 사용자 정보 설정 (예: 역할 등)
        userRepository.save(user);
        return true;
    }


    @Override
    public String findUserIdByEmail(FindIdRequestDto findIdRequestDto) {
        Optional<User> userOptional = userRepository.findByEmail(findIdRequestDto.getEmail());
        // 실제 운영 시에는 아이디 전체를 반환하기보다, 아이디의 일부를 마스킹 처리하거나
        // "해당 이메일로 가입된 아이디가 존재합니다." 정도의 안내가 더 안전할 수 있습니다.
        return Objects.requireNonNull(userOptional.map(User::getId).orElse(null)).toString();
    }

    @Override
    public String generatePasswordResetToken(FindPasswordRequestDto findPasswordRequestDto) {
        Optional<User> userOptional = userRepository.findByIdAndEmail(
                findPasswordRequestDto.getUserId(),
                findPasswordRequestDto.getEmail()
        );

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            String token = UUID.randomUUID().toString();

             user.setPasswordResetToken(token);
             user.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1)); // 예: 1시간 후 만료
             userRepository.save(user);


            // User 엔티티에 해당 필드가 없다면, 이 부분은 주석 처리하거나 User 엔티티를 수정해야 합니다.
            // 현재는 토큰만 생성하여 반환하는 것으로 가정합니다. 실제로는 DB에 저장해야 합니다.
            // 이메일 발송 로직은 여기서 처리하지 않고, 컨트롤러나 별도 서비스에서 이 토큰을 사용합니다.
            System.out.println("Generated password reset token for user " + user.getId() + ": " + token); // 실제 운영에서는 로깅 프레임워크 사용
            return token; // 생성된 토큰 반환
        }
        return null; // 사용자가 존재하지 않으면 null 반환
    }
}
