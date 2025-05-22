package ac.kr.changwon.se_proj.controller.webSocket;

import ac.kr.changwon.se_proj.dto.ChatMessageDTO;
import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.properties.ChatProperties;
import ac.kr.changwon.se_proj.repository.ChatMessageRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class WebSocketController {

    private final ChatProperties chatProperties;

    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final ChatProperties chatProps;


    /**
     * WebSocket 메시지 수신 처리
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageDTO message, Principal principal) {
         int privateRoomMaxId = chatProps.getPrivateMaxRoomId();


        // principal이 null인 테스트 환경을 대비해, DTO에 담긴 username을 fallback으로 사용
        String username = (principal != null) ? principal.getName() : message.getUsername();
        User sender = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username));

        //DB 저장
        ChatMessage chat = ChatMessage.builder()
                .sender(sender)
                .content(message.getContent())
                .username(sender.getUsername())
                .timestamp(message.getTimestamp())
                .roomId(message.getRoomId())
                .build();
        chatMessageRepository.save(chat);

        //분기 설정. roomId필드 기준으로 Private, Group 구분
        String destination;
        if(message.getRoomId() <= privateRoomMaxId){
            destination = "/topic/private/" + message.getRoomId();
        }
        else{
            destination = "/topic/group/" + message.getRoomId();
        }

        //메시지 전송
        message.setSenderId(sender.getId());
        message.setTimestamp(chat.getTimestamp());
        messagingTemplate.convertAndSend(destination, message);

    }



}
