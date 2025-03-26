package ac.kr.changwon.se_proj.Service.Interface;

import ac.kr.changwon.se_proj.Model.ChatMessage;

import java.util.List;

public interface ChatMessageService {
    List<ChatMessage> findAll();
    ChatMessage findById(Integer id);
    ChatMessage save(ChatMessage obj);
    void deleteById(Integer id);
}
