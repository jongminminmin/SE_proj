package ac.kr.changwon.se_proj.controller;

import ac.kr.changwon.se_proj.model.Notification;
import ac.kr.changwon.se_proj.model.User;
import ac.kr.changwon.se_proj.repository.NotificationRepository;
import ac.kr.changwon.se_proj.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<String>> getUnreadNotifications(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userRepository.findById(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Notification> notifications = notificationRepository.findByUserAndIsReadOrderByCreatedAtDesc(user, false);
        
        List<String> messages = notifications.stream()
                                             .map(Notification::getMessage)
                                             .collect(Collectors.toList());
        return ResponseEntity.ok(messages);
    }
} 