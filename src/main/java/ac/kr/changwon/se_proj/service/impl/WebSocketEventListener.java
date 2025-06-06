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

//@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private static final Logger logger = LoggerFactory.getLogger(WebSocketEventListener.class);

    // SimpMessageSendingOperations는 이 리스너에서 직접 메시지를 보내지 않으므로 필요 없을 수 있습니다.
    private final SimpMessageSendingOperations messagingTemplate;
    // WebSocketSessionTracker는 더 이상 add/remove 메서드를 제공하지 않습니다.
    // 만약 이 리스너에서 sessionTracker의 getConnectedUsers() 같은 메서드를 호출해야 한다면 유지합니다.
    private final WebSocketSessionTracker sessionTracker;

    @EventListener
    public void handledWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String connectedUsername = null;

        if (headerAccessor.getUser() != null) {
            connectedUsername = headerAccessor.getUser().getName();
        }

        if (connectedUsername == null) {
            logger.warn("WebSocket connected, but username not found in Principal for session {}. This user will not be tracked.", headerAccessor.getSessionId());
            return;
        }

        // --- WebSocketSessionTracker가 이 이벤트를 직접 처리하므로 다음 줄은 제거됩니다. ---
        // sessionTracker.addConnectedUser(connectedUsername);
        logger.info("WebSocket connected, username={}", connectedUsername);

    }


    @EventListener
    public void handledWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String disconnectedUsername = null;

        if (headerAccessor.getUser() != null) {
            disconnectedUsername = headerAccessor.getUser().getName();
        }

        if (disconnectedUsername == null) {
            logger.warn("WebSocket disconnected, but username not found in Principal for session {}", headerAccessor.getSessionId());
            return;
        }

        // --- WebSocketSessionTracker가 이 이벤트를 직접 처리하므로 다음 줄은 제거됩니다. ---
        // sessionTracker.removeConnectedUser(disconnectedUsername);
        logger.info("WebSocket disconnected, username={} Total connected: {}", disconnectedUsername, sessionTracker.getConnectedUsers().size());

    }
}
