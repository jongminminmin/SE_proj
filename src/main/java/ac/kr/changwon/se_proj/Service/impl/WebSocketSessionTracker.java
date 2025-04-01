package ac.kr.changwon.se_proj.Service.impl;


import lombok.Getter;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Component
public class WebSocketSessionTracker {

    private final Set<String> connectedUsers = ConcurrentHashMap.newKeySet();

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if(accessor.getUser() != null){
            connectedUsers.add(accessor.getUser().getName());
        }
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if(accessor.getUser() != null){
            connectedUsers.remove(accessor.getUser().getName());
        }

    }

}
