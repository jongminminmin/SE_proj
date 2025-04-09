package ac.kr.changwon.se_proj.controller;

import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.service.Interface.ChatMessageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
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
    public ChatMessage getById(@PathVariable Integer id) {
        return chatMessageService.findById(id);
    }

    @PostMapping
    public ChatMessage create(@RequestBody ChatMessage chatMessage) {
        return chatMessageService.save(chatMessage);
    }

    @PutMapping
    public ChatMessage update(@RequestBody ChatMessage chatMessage) {
        return chatMessageService.save(chatMessage);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        chatMessageService.deleteById(id);
    }

}
