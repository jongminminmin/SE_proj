package ac.kr.changwon.se_proj.controller;

import ac.kr.changwon.se_proj.service.SseService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/sse")
public class SseController {

    private final SseService sseService;

    public SseController(SseService sseService) {
        this.sseService = sseService;
    }

    /**
     * 클라이언트가 SSE 연결을 구독하기 위한 엔드포인트입니다.
     * @return SseEmitter 객체
     */
    @GetMapping("/subscribe")
    public SseEmitter subscribe(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            // 사용자가 인증되지 않은 경우, 구독을 거부하거나 예외 처리를 할 수 있습니다.
            // 여기서는 간단히 null을 반환하지만, 실제로는 예외를 던지는 것이 더 좋습니다.
            return null;
        }
        String userId = userDetails.getUsername(); // CustomUserDetails에서 반환하는 고유 ID
        return sseService.addEmitter(userId);
    }
}
