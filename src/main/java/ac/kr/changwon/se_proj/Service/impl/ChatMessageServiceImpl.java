package ac.kr.changwon.se_proj.Service.impl;

import ac.kr.changwon.se_proj.Model.ChatMessage;
import ac.kr.changwon.se_proj.Repository.ChatMessageRepository;
import ac.kr.changwon.se_proj.Service.Interface.ChatMessageService;
import org.springframework.stereotype.Service;

import java.util.List;
@Service
public class ChatMessageServiceImpl implements ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageServiceImpl(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
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

}


