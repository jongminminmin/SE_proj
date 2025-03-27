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

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;


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
        chat.setRoomId(Integer.parseInt(String.valueOf(message.getRoomId()))); // roomId 저장

        chatMessageRepository.save(chat);


        // 클라이언트로 전송될 데이터 구성
        message.setTimestamp(chat.getTimestamp());

        messagingTemplate.convertAndSend("/topic/private/" + message.getReceiverId(), message);

        return message;

    }

}
