package ac.kr.changwon.se_proj.Service.Interface;

import ac.kr.changwon.se_proj.Model.ChatMessage;

public interface ChatMessageObserver {
    void onMessageReceived(ChatMessage message);
}
