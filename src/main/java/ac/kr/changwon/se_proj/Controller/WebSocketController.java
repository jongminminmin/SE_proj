package ac.kr.changwon.se_proj.Controller;


import ac.kr.changwon.se_proj.Model.ChatMessage;
import ac.kr.changwon.se_proj.Model.User;
import ac.kr.changwon.se_proj.Repository.ChatMessageRepository;
import ac.kr.changwon.se_proj.Repository.UserRepository;
import ac.kr.changwon.se_proj.dto.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    /**
     * WebSocket 메시지 수신 처리
     */
    @MessageMapping("/chat.sendMessage")
    public ChatMessageDTO sendMessage(@Payload ChatMessageDTO message) {
        User sender = userRepository.findById(message.getSenderId()).orElse(null);
        if (sender == null) {
            System.out.println("Sender not found: " + message.getSenderId());
            return null;
        }

        ChatMessage chat = new ChatMessage();
        chat.setSender(sender);
        chat.setContent(message.getContent());
        chat.setUsername(sender.getUsername());
        chat.setTimestamp(LocalDateTime.now());
        chat.setReceiverId(message.getReceiverId());
        chat.setRoomId(message.getRoomId());

        chatMessageRepository.save(chat);

        // 메시지 DTO에 타임스탬프 세팅해서 수신자에게 전송
        message.setTimestamp(chat.getTimestamp());
        messagingTemplate.convertAndSend("/topic/private/" + message.getReceiverId(), message);

        return message;
    }



}
