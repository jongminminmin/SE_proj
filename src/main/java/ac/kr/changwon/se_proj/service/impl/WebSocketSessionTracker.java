package ac.kr.changwon.se_proj.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.scheduling.annotation.Scheduled; // Scheduled 어노테이션 임포트 추가

import java.security.Principal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class WebSocketSessionTracker implements ApplicationListener<ApplicationEvent> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketSessionTracker.class);

    private final ConcurrentHashMap<String, String> activeUserSessions = new ConcurrentHashMap<>();
    private final SimpMessageSendingOperations messagingTemplate;

    public WebSocketSessionTracker(SimpMessageSendingOperations messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof SessionConnectEvent) {
            SessionConnectEvent connectEvent = (SessionConnectEvent) event;
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(connectEvent.getMessage());
            Principal principal = headerAccessor.getUser();

            if (principal != null) {
                String username = principal.getName();
                String sessionId = headerAccessor.getSessionId();
                activeUserSessions.put(sessionId, username);
                logger.info("User connected via WebSocket: {}. Total active users: {}", username, activeUserSessions.size());
                // 연결 시 브로드캐스트
                broadcastConnectedUsers();
            } else {
                logger.warn("WebSocket CONNECT event without Principal. SessionId: {}", headerAccessor.getSessionId());
            }
        } else if (event instanceof SessionDisconnectEvent) {
            SessionDisconnectEvent disconnectEvent = (SessionDisconnectEvent) event;
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(disconnectEvent.getMessage());
            String sessionId = headerAccessor.getSessionId();
            String disconnectedUsername = activeUserSessions.remove(sessionId);

            if (disconnectedUsername != null) {
                logger.info("User disconnected from WebSocket: {}. Total active users: {}", disconnectedUsername, activeUserSessions.size());
                // 연결 해제 시 브로드캐스트
                broadcastConnectedUsers();
            } else {
                logger.warn("WebSocket DISCONNECT event for unknown session or Principal. SessionId: {}", sessionId);
            }
        }
    }

    /**
     * 현재 접속 중인 사용자들의 목록을 /topic/connectedUsers 토픽으로 브로드캐스트합니다.
     * 이 목록은 클라이언트의 Chat.js에서 'online' 상태를 업데이트하는 데 사용됩니다.
     */
    private void broadcastConnectedUsers() {
        Set<String> uniqueConnectedUsernames = activeUserSessions.values().stream()
                .collect(Collectors.toSet());

        logger.debug("Broadcasting connected users: {}", uniqueConnectedUsernames);
        messagingTemplate.convertAndSend("/topic/connectedUsers", uniqueConnectedUsernames);
    }

    /**
     * 일정 시간 간격으로 현재 접속 중인 사용자 목록을 모든 클라이언트에게 브로드캐스트합니다.
     * 클라이언트의 구독 타이밍 문제 보완 및 주기적인 상태 동기화를 위해 사용됩니다.
     * 예를 들어, 10초(10000ms)마다 브로드캐스트합니다.
     */
    @Scheduled(fixedRate = 5000) // 10초마다 실행 (밀리초 단위)
    public void broadcastConnectedUsersPeriodically() {
        if (!activeUserSessions.isEmpty()) { // 접속자가 있을 때만 브로드캐스트
            logger.debug("Periodic broadcast of connected users triggered.");
            broadcastConnectedUsers();
        }
    }

    public Set<String> getConnectedUsers() {
        return activeUserSessions.values().stream().collect(Collectors.toSet());
    }
}
