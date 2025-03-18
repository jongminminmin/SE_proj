package ac.kr.changwon.se_proj.Model;


import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Data
@Entity
public class ChatMessage {
    @Id
    private String id;
    private String senderName;
    private String receiverName;
    private String content;
    private LocalDateTime timestamp;
}
