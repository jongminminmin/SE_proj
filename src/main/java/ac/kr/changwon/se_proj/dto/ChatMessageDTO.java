package ac.kr.changwon.se_proj.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageDTO {
    private String messageId; // UUID 메시지 ID
    private String chatRoomId; // ChatRoom의 UUID ID
    private String senderId;   // 발신자 User의 UUID ID
    private String username;   // 발신자 사용자 이름
    private String content;    // 메시지 내용
    private LocalDateTime timestamp; // 메시지 전송 시간 (LocalDateTime)
}
