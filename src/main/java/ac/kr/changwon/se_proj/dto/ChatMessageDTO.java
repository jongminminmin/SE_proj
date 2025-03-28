package ac.kr.changwon.se_proj.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChatMessageDTO {
    private String senderId;
    private int roomId;
    private String content;
    private String receiverId;
    private String username;
    private String message;
    private LocalDateTime timestamp;
}
