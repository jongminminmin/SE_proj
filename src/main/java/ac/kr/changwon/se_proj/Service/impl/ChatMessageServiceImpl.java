package ac.kr.changwon.se_proj.Service.impl;

import ac.kr.changwon.se_proj.Model.ChatMessage;
import ac.kr.changwon.se_proj.Model.User;
import ac.kr.changwon.se_proj.Repository.ChatMessageRepository;
import ac.kr.changwon.se_proj.Repository.UserRepository;
import ac.kr.changwon.se_proj.Service.Interface.ChatMessageService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;


@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;

    public ChatMessageServiceImpl(ChatMessageRepository chatMessageRepository, UserRepository userRepository) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
    }


    @Override
    public List<ChatMessage> findAll() {
        return chatMessageRepository.findAll();
    }

    @Override
    public ChatMessage findById(Integer id) {
        return chatMessageRepository.findById(id).orElse(null);
    }

    @Override
    public ChatMessage save(ChatMessage obj) {
        return chatMessageRepository.save(obj);
    }

    @Override
    public void deleteById(Integer id) {
        chatMessageRepository.deleteById(id);
    }

    @Override
    public void process(ChatMessage chatMessage) {
        User sender = userRepository.findById(String.valueOf(chatMessage.getSender())).orElse(null);
        if (sender == null) {
            return;
        }

        ChatMessage message = new ChatMessage(
                sender,
                chatMessage.getReceiverId(),
                chatMessage.getContent(),
                chatMessage.getTimestamp() != null ? chatMessage.getTimestamp() : LocalDateTime.now()
        );

        chatMessageRepository.save(message);
    }

}


