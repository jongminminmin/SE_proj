package ac.kr.changwon.se_proj.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import ac.kr.changwon.se_proj.Model.ChatMessage;
import ac.kr.changwon.se_proj.Service.Interface.ChatService;
import ac.kr.changwon.se_proj.Service.ChatMessageObserver;
import org.springframework.boot.actuate.autoconfigure.wavefront.WavefrontProperties;

public class ChatServiceImpl implements ChatService {
    private final List<ChatMessage> messages = new ArrayList<>();
    private final List<ChatMessageObserver> observers = new ArrayList<>();

    @Override
    public void sendMessage(String sender, String content) {
        if (sender == null || content == null || content.isBlank())
            return;

        ChatMessage message = new ChatMessage(sender, content, System.currentTimeMillis());
    }

    @Override
    public List<ChatMessage> fetchChatHistory() {
        return List.of();
    }

    @Override
    public List<ChatMessage> fetchChatHistoryByRoom(String roomId, ChatMessageObserver observer) {
        return List.of();
    }
}
