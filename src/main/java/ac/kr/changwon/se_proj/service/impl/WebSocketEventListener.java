package ac.kr.changwon.se_proj.service.impl;


import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketEventListener {


    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final SimpMessageSendingOperations messagingTemplate;
    private final WebSocketSessionTracker sessionTracker;

    @EventListener
    public void handledWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String connectedUsername = null;

        //널포인트익셉션 방지 및 사용자 이름 추출
        if(headerAccessor.getUser() != null){
            connectedUsername = headerAccessor.getUser().getName();
        }

        if(connectedUsername == null){
            logger.warn("WebSocket connected, but username not found in Principal for session {}. This user will not be traker", headerAccessor.getSessionId());
            return;
        }

        //세션 트래커에 사용자 추가
        sessionTracker.addConnectedUser(connectedUsername);

        logger.info("WebSocket connected, username={}", connectedUsername);

        //연결된 사용자 목록 업데이트를 모든 클라이언트에게 브로드캐스팅
        //사용자가 추가된 후 최신 목록 발행
        messagingTemplate.convertAndSend("/topic/connectedUsers", sessionTracker.getConnectedUsers());
    }


    @EventListener
    public void handledWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String disconnectedUsername = null;

        //널포인트익셉션 방지 및 사용자 이름 추출
        if(headerAccessor.getUser() != null){
            disconnectedUsername = headerAccessor.getUser().getName();
        }

        if( disconnectedUsername == null){
            logger.warn(" WebSocket disconnected, but username not found in Principal for session {}", headerAccessor.getSessionId());
            return;
        }

        sessionTracker.removeConnectedUser(disconnectedUsername);
        logger.info("WebSocket disconnected, username={} Total connected: {}", disconnectedUsername, sessionTracker.getConnectedUsers().size() );
        messagingTemplate.convertAndSend("/topic/connectedUsers", sessionTracker.getConnectedUsers());
    }
}
