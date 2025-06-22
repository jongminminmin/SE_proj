package ac.kr.changwon.se_proj.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class SseService {

    // 여러 스레드에서 동시에 접근해도 안전한 리스트를 사용하여 Emitter를 관리합니다.
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    /**
     * 클라이언트가 SSE 구독을 요청할 때 호출됩니다.
     * @return SseEmitter 객체
     */
    public SseEmitter addEmitter() {
        // 매우 긴 타임아웃을 설정하여 연결이 유실되지 않도록 합니다.
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // emitter를 관리 목록에 추가합니다.
        this.emitters.add(emitter);

        // 연결이 완료되거나, 타임아웃되거나, 에러 발생 시 목록에서 제거합니다.
        emitter.onCompletion(() -> this.emitters.remove(emitter));
        emitter.onTimeout(() -> this.emitters.remove(emitter));
        emitter.onError((e) -> this.emitters.remove(emitter));

        // 연결 성공을 알리는 초기 이벤트를 보냅니다.
        try {
            emitter.send(SseEmitter.event().name("connect").data("SSE 연결 성공"));
        } catch (IOException e) {
            // 초기 이벤트 전송 실패 시 목록에서 즉시 제거합니다.
            this.emitters.remove(emitter);
        }

        return emitter;
    }

    /**
     * 연결된 모든 클라이언트에게 이벤트를 전송합니다.
     * @param eventName 이벤트 이름
     * @param data 전송할 데이터
     */
    public void sendToAll(String eventName, Object data) {
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        // 모든 emitter에 이벤트를 보냅니다.
        this.emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                // 전송 실패 시, 해당 emitter를 '제거할 목록'에 추가합니다.
                deadEmitters.add(emitter);
            }
        });

        // 실패한 emitter들을 전체 목록에서 제거합니다.
        this.emitters.removeAll(deadEmitters);
    }
}
