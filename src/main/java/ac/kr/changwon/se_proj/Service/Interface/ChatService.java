package ac.kr.changwon.se_proj.Service.Interface;

import ac.kr.changwon.se_proj.Model.ChatMessage;

import java.util.List;

public interface ChatService {
    void sendMessage(String sender, String content);
    List<ChatMessage> fetchChatHistory();
    List<ChatMessage> fetchChatHistoryByRoom(String roomId,ChatMessageObserver observer);
}
