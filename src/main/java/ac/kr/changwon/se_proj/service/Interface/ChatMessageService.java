package ac.kr.changwon.se_proj.service.Interface;

import ac.kr.changwon.se_proj.model.ChatMessage;

import java.util.List;

public interface ChatMessageService {
    List<ChatMessage> findAll();
    ChatMessage findById(Integer id);
    ChatMessage save(ChatMessage obj);
    void deleteById(Integer id);
    void process(ChatMessage chatMessage);
}
