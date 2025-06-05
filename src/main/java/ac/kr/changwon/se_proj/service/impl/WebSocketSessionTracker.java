package ac.kr.changwon.se_proj.service.impl;


import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
@Component
public class WebSocketSessionTracker {

    private final Set<String> connectedUsers = ConcurrentHashMap.newKeySet();
    private final static Logger logger = LoggerFactory.getLogger(WebSocketSessionTracker.class);


    public void addConnectedUser(String username) {
        if(username != null){
            connectedUsers.add(username);
            logger.info("User added to connected users: {}. Current total: {}", username, connectedUsers.size() );        }
    }

    public void removeConnectedUser(String username) {
        if(username != null)
            connectedUsers.remove(username);
        logger.info("User removed from connected users: {}. Current total: {}", username, connectedUsers.size() );
    }

    //외부에서 set을 직접 수정하는 것을 방지하기 위해 복사본 반환
    public Set<String> getConnectedUsers() {
        Set<String> copy = ConcurrentHashMap.newKeySet(connectedUsers.size());
        copy.addAll(connectedUsers);

        return copy;
    }
}
