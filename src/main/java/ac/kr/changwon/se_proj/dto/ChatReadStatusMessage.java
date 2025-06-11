package ac.kr.changwon.se_proj.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChatReadStatusMessage {
    private String userId;
    private String roomId;
    private String messageId;
    private LocalDateTime timestamp;
    private String username;
}
