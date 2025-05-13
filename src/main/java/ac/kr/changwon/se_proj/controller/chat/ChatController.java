package ac.kr.changwon.se_proj.controller.chat;


import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.service.Interface.ChatMessageService;
import lombok.Getter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

@Getter
@Controller
public class ChatController {
    //데모용 채팅 컨트롤러

    private final ChatMessageService chatMessageService;

    public ChatController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public ChatMessage sendMessage(ChatMessage message) {
        message.setTimestamp(LocalDateTime.now());
        return message;
    }

}
