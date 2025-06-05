package ac.kr.changwon.se_proj.config;


import ac.kr.changwon.se_proj.service.impl.WebSocketSessionTracker;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
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
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String connectedUsername = headerAccessor.getUser().getName();

        if(connectedUsername == null){
            logger.warn("WebSocket connected, but username not found in Principal for session {}. This user will not be tracked.", headerAccessor.getSessionId());
            return;
        }

        logger.info("User Connected: {}. Total connected: {}", connectedUsername, sessionTracker.getConnectedUsers().size());


        //연결된 사용자 목록 업데이트를 모든 클라이언트에게 보냄.
        messagingTemplate.convertAndSend("/topic/connectedUsers", sessionTracker.getConnectedUsers());
    }


    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String disconnectedUsername = headerAccessor.getUser() != null ? headerAccessor.getUser().getName() : null;


        if(disconnectedUsername == null){
            logger.warn("WebSocket disconnected, but username not found in Principal for session {}", headerAccessor.getSessionId());

            return;
        }

        logger.info("User disconnected: {}. Total disconnected: {}", disconnectedUsername, sessionTracker.getConnectedUsers().size());

        messagingTemplate.convertAndSend("/topic/connectedUsers", sessionTracker.getConnectedUsers());

    }
}
