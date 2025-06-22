package ac.kr.changwon.se_proj.controller;

import ac.kr.changwon.se_proj.service.SseService;
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
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribe() {
        return sseService.addEmitter();
    }
}
