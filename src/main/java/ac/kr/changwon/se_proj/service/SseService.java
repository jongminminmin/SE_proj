package ac.kr.changwon.se_proj.service;

import ac.kr.changwon.se_proj.dto.TaskDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SseService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 클라이언트가 SSE 구독을 요청할 때 호출됩니다.
     * @param userId 사용자 ID
     * @return SseEmitter 객체
     */
    public SseEmitter addEmitter(String userId) {
        // 매우 긴 타임아웃을 설정하여 연결이 유실되지 않도록 합니다.
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);

        // emitter를 관리 목록에 추가합니다.
        this.emitters.put(userId, emitter);

        // 연결이 완료되거나, 타임아웃되거나, 에러 발생 시 목록에서 제거합니다.
        emitter.onCompletion(() -> this.emitters.remove(userId, emitter));
        emitter.onTimeout(() -> this.emitters.remove(userId, emitter));
        emitter.onError((e) -> this.emitters.remove(userId, emitter));

        // 연결 성공을 알리는 초기 이벤트를 보냅니다.
        sendToUser(userId, "connect", "SSE connected successfully.");

        return emitter;
    }

    /**
     * 특정 사용자에게 이벤트를 전송합니다.
     * @param userId 사용자 ID
     * @param eventName 이벤트 이름
     * @param data 전송할 데이터
     */
    public void sendToUser(String userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter != null) {
            try {
                String jsonData = objectMapper.writeValueAsString(data);
                emitter.send(SseEmitter.event().name(eventName).data(jsonData));
            } catch (IOException e) {
                emitters.remove(userId);
            }
        }
    }

    /**
     * 업무 업데이트를 브로드캐스트합니다.
     * @param task 업무 정보
     */
    public void broadcastTaskUpdate(TaskDTO task) {
        emitters.keySet().forEach(userId -> sendToUser(userId, "tasks-updated", task));
    }
}
