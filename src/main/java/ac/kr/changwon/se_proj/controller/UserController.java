package ac.kr.changwon.se_proj.controller;


import ac.kr.changwon.se_proj.dto.UserDto;
import ac.kr.changwon.se_proj.model.CustomUserDetails;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.service.Interface.UserService;
import lombok.RequiredArgsConstructor;
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

    //현재 로그인한 사용자 정보 반환
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        User u = userDetails.getUser();
        UserDto dto = new UserDto(u.getId(), u.getUsername(), u.getEmail());
        return ResponseEntity.ok(dto);
    }

    //전체 사용자 목록 반환
    @GetMapping
    public ResponseEntity<List<UserDto>> listAllUsers() {
        List<UserDto> users = userService.findAll()
                .stream()
                .map(u -> new UserDto(u.getId(), u.getUsername(), null))
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    //단일 사용자 조회
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable String id) {
        User u = userService.findById(id);
        return ResponseEntity.ok(new UserDto(u.getId(), u.getUsername(), u.getEmail()));
    }


    //사용자 생성
    @PostMapping
    public ResponseEntity<UserDto> createUser(@RequestBody UserDto dto) {
        User savedUser=userService.saveUser(dto.toEntity());

        return ResponseEntity
                .created(URI.create("/api/users/" + savedUser.getId()))
                .body(new UserDto(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail()));

    }

    //사용자 수정
    @PutMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(@PathVariable String id,
                                              @RequestBody UserDto dto) {
        dto.setId(id);
        User updatedUser=userService.updateUser(dto.toEntity());
        return ResponseEntity.ok(new UserDto(updatedUser.getId(),
                updatedUser.getUsername(),
                updatedUser.getEmail()));
    }


    //사용자 삭제
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable String id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
