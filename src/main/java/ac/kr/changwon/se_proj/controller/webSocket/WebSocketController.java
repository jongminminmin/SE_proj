package ac.kr.changwon.se_proj.controller.webSocket;


import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.ChatMessageRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import ac.kr.changwon.se_proj.dto.ChatMessageDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;

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
    public ChatMessageDTO sendMessage(@Payload ChatMessageDTO message, Principal principal) {
        // principal이 null인 테스트 환경을 대비해, DTO에 담긴 username을 fallback으로 사용
        String username = (principal != null) ? principal.getName() : message.getUsername();
        User sender = userRepository.findByLoginId(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        ChatMessage chat = new ChatMessage();
        chat.setSender(sender);
        chat.setContent(message.getContent());
        chat.setUsername(sender.getUsername());
        chat.setTimestamp(LocalDateTime.now());
        chat.setReceiverId(message.getReceiverId());
        chat.setRoomId(message.getRoomId());

        chatMessageRepository.save(chat);

        message.setSenderId(sender.getId());
        message.setUsername(sender.getUsername());
        message.setTimestamp(chat.getTimestamp());

        messagingTemplate.convertAndSend("/topic/private/" + message.getReceiverId(), message);

        return message;
    }



}
