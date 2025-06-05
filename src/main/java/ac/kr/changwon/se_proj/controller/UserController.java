package ac.kr.changwon.se_proj.controller;


import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.model.CustomUserDetails;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.service.Interface.ChatRoomService;
import ac.kr.changwon.se_proj.service.Interface.UserService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    private final ChatRoomService chatRoomService;
    // Logger 인스턴스 생성
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    //현재 로그인한 사용자 정보 반환
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        // SecurityContextHolder를 통해 직접 Authentication 객체를 가져올 수도 있습니다.
        // Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        logger.info("/api/users/me 호출됨");

        if (authentication == null) {
            logger.warn("Authentication 객체가 null입니다. 사용자가 인증되지 않았을 수 있습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        logger.info("Authentication 객체 존재: {}", authentication);
        logger.info("인증 여부: {}", authentication.isAuthenticated());
        logger.info("Principal 객체: {}", authentication.getPrincipal());

        if (!authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal().toString())) {
            logger.warn("사용자가 인증되지 않았거나 익명 사용자입니다. Principal: {}", authentication.getPrincipal());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object principal = authentication.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            logger.error("Principal 객체가 CustomUserDetails 타입이 아닙니다. 실제 타입: {}", principal.getClass().getName());
            // 이 경우 클라이언트에게 내부 서버 오류를 알리거나, 적절한 오류 응답을 보내야 합니다.
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null); // 혹은 다른 적절한 응답
        }

        CustomUserDetails userDetails = (CustomUserDetails) principal;
        User u = userDetails.getUser();
        if (u == null) {
            logger.error("CustomUserDetails에 User 객체가 null입니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }

        logger.info("성공적으로 사용자 정보를 조회했습니다: {}", u.getUsername());
        UserDto dto = new UserDto(u.getId(), u.getUsername(), u.getEmail());
        return ResponseEntity.ok(dto);
    }

    //전체 사용자 목록 반환
    @GetMapping("/api/chat/")
    public ResponseEntity<List<UserDto>> listAllUsers() {
        logger.info("/api/users (전체 목록) 호출됨");
        List<UserDto> users = userService.findAll()
                .stream()
                .map(u -> new UserDto(u.getId(), u.getUsername(), null)) // 이메일은 목록에서 제외 (개인정보)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    //단일 사용자 조회
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable String id) {
        logger.info("/api/users/{} 호출됨", id);
        User u = userService.findById(id);
        // 사용자를 찾지 못한 경우 404 Not Found 반환 고려
        if (u == null) {
            logger.warn("{} ID의 사용자를 찾을 수 없습니다.", id);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new UserDto(u.getId(), u.getUsername(), u.getEmail()));
    }


    //사용자 생성 (회원가입은 보통 /api/auth/register에서 처리되지만, 관리자용 기능일 수 있음)
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto dto) {
        logger.info("/api/users (생성) 호출됨. 요청 DTO: {}", dto);
        // 비밀번호 암호화는 서비스 계층에서 처리되어야 합니다.
        User savedUser = userService.saveUser(dto.toEntity());
        logger.info("사용자 생성 완료: {}", savedUser.getUsername());
        return ResponseEntity
                .created(URI.create("/api/users/" + savedUser.getId()))
                .body(new UserDto(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail()));
    }

    //사용자 수정
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable String id,
                                              @RequestBody UserDto dto) {
        logger.info("/api/users/{} (수정) 호출됨. 요청 DTO: {}", id, dto);
        dto.setId(id); // 경로 변수 ID를 DTO에 설정
        User updatedUser = userService.updateUser(dto.toEntity());
        logger.info("사용자 수정 완료: {}", updatedUser.getUsername());
        return ResponseEntity.ok(new UserDto(updatedUser.getId(),
                updatedUser.getUsername(),
                updatedUser.getEmail()));
    }


    //사용자 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        logger.info("/api/users/{} (삭제) 호출됨", id);
        userService.deleteById(id);
        logger.info("{} ID의 사용자 삭제 완료", id);
        return ResponseEntity.noContent().build();
    }

}
