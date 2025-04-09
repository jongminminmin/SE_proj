package ac.kr.changwon.se_proj.service.Interface;

import ac.kr.changwon.se_proj.model.ChatMessage;

public interface ChatMessageObserver {
    void onMessageReceived(ChatMessage message);
}
