package ac.kr.changwon.se_proj.controller.chat;

import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.service.Interface.ChatMessageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/messages") // API 경로를 더 명확하게 변경 (예: /api/messages)
public class ChatMessageController {
    private final ChatMessageService chatMessageService;

    public ChatMessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @GetMapping
    public List<ChatMessage> getAll() {
        return chatMessageService.findAll();
    }

    @GetMapping("/{id}")
    public ChatMessage getById(@PathVariable String id) { // Integer -> String으로 변경
        return chatMessageService.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found with id: " + id));
    }

    @PostMapping
    public ChatMessage create(@RequestBody ChatMessage chatMessage) {
        // ID가 있는 메시지는 업데이트로 간주될 수 있으므로, 새로운 메시지 생성 시 ID를 비워두는 것이 일반적입니다.
        // 또는 ChatMessageService.save()가 insert/update를 모두 처리하도록 합니다.
        // 여기서는 save()가 UPSERT (Update OR Insert)를 처리한다고 가정합니다.
        return chatMessageService.save(chatMessage);
    }

    @PutMapping("/{id}") // 특정 ID에 대한 PUT (업데이트)
    public ChatMessage update(@PathVariable String id, @RequestBody ChatMessage chatMessage) {
        // 요청 본문의 ID와 PathVariable의 ID가 다를 경우를 처리하거나, PathVariable의 ID를 강제할 수 있습니다.
        // 여기서는 PathVariable의 ID를 사용하여 업데이트를 시도합니다.
        if (!chatMessageService.findById(id).isPresent()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Message not found for update with id: " + id);
        }
        // 메시지 객체의 ID를 PathVariable의 ID로 강제 (안전성 확보)
        chatMessage.setMessageId(id);
        return chatMessageService.save(chatMessage);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 No Content 반환
    public void delete(@PathVariable String id) { // Integer -> String으로 변경
        chatMessageService.deleteById(id);
    }

}
