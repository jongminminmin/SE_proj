package ac.kr.changwon.se_proj.controller.chat;

import ac.kr.changwon.se_proj.dto.ChatMessageDTO;
import ac.kr.changwon.se_proj.model.CustomUserDetails;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.service.Interface.ChatRoomService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/chats")
public class ChatRoomController {

    private final static Logger logger= LoggerFactory.getLogger(ChatRoomController.class);
    private final ChatRoomService chatRoomService;

    public ChatRoomController(ChatRoomService chatRoomService) {
        this.chatRoomService = chatRoomService;
    }


    //채팅방 조회
    @GetMapping("/my-rooms")
    public ResponseEntity <List<ChatMessageDTO>> getMyRooms(Authentication auth) {
        logger.info("/api/users/my-rooms 호출됨");

        if (auth == null || !auth.isAuthenticated()) {
            logger.warn("사용자가 인증되지 않았습니다.");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Object principal = auth.getPrincipal();
        if (!(principal instanceof CustomUserDetails)) {
            logger.error("Principal 객체가 CustomUserDetails 타입이 아닙니다. 실제 타입: {}", principal.getClass().getName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        CustomUserDetails userDetails = (CustomUserDetails) principal;
        User currentUser = userDetails.getUser();

        if (currentUser == null) {
            logger.error("CustomUserDetails에 User 객체가 null입니다.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        // 현재 사용자의 채팅방을 가져오는 서비스 메서드 호출
        List<ChatMessageDTO> rooms = chatRoomService.getRoomByUserId(currentUser.getId());

        logger.info("사용자 {}의 채팅방 {}개 조회 완료", currentUser.getUsername(), rooms.size());
        return ResponseEntity.ok(rooms);
    }
}
