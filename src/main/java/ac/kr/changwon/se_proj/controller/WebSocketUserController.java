package ac.kr.changwon.se_proj.controller;


import ac.kr.changwon.se_proj.service.impl.WebSocketSessionTracker;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequiredArgsConstructor
public class WebSocketUserController {

    private final WebSocketSessionTracker webSocketSessionTracker;


    @GetMapping("/api/users/connected")
    public Set<String> getConnectedUsers() {
        return webSocketSessionTracker.getConnectedUsers();
    }

}
