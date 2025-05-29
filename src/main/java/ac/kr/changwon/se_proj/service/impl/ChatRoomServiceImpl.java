package ac.kr.changwon.se_proj.service.impl;

import ac.kr.changwon.se_proj.dto.ChatMessageDTO;
import ac.kr.changwon.se_proj.model.ChatMessage;
import ac.kr.changwon.se_proj.properties.ChatProperties;
import ac.kr.changwon.se_proj.repository.ChatMessageRepository;
import ac.kr.changwon.se_proj.service.Interface.ChatRoomService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;


@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private static final Logger log = LoggerFactory.getLogger(ChatRoomServiceImpl.class);
    private final ChatMessageRepository chatMessageRepository;
    private final ChatProperties chatProperties;


    @Override
    public List<ChatMessageDTO> getRoomByUserId(String userId) {
        log.info("사용자 ID: {}에 대한 채팅방 조회 시작",userId);

        //전체 채팅 메시지 조회
        List<ChatMessage> allMessages = chatMessageRepository.findAll();

        //사용자가 발신자이거나 수신자인 메시지 필터링
        List<ChatMessage> userMessages = allMessages.stream()
                .filter(message -> Objects.requireNonNull(message.getSender().getId()).equals(userId) ||
                        message.getReceiverId().equals(userId))
                .toList();
        log.info("사용자 관련 메시지 수: {}", userMessages.size());

        // 3. 채팅방 ID를 키로 사용하는 맵을 만들어 중복 없는 채팅방 목록을 생성합니다
        Map<Integer, ChatMessageDTO> uniqueRooms = new HashMap<>();

        for (ChatMessage message : userMessages) {
            int roomId = message.getRoomId();

            // 이미 맵에 해당 roomId가 있으면 최신 메시지인 경우만 업데이트
            if (uniqueRooms.containsKey(roomId)) {
                ChatMessageDTO existingDto = uniqueRooms.get(roomId);
                if (message.getTimestamp().isAfter(existingDto.getTimestamp())) {
                    uniqueRooms.put(roomId, convertToDto(message));
                }
            } else {
                // 맵에 roomId가 없으면 새로 추가
                uniqueRooms.put(roomId, convertToDto(message));
            }
        }

        log.info("사용자가 참여한 고유 채팅방 수: {}", uniqueRooms.size());

        // 4. 값 목록만 반환합니다
        return new ArrayList<>(uniqueRooms.values());

    }

    private ChatMessageDTO convertToDto(ChatMessage message) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setRoomId(message.getRoomId());
        dto.setSenderId(message.getSender().getId());
        dto.setUsername(message.getSender().getUsername());
        dto.setContent(message.getContent());
        dto.setTimestamp(message.getTimestamp());
        dto.setReceiverId(message.getReceiverId());
        return dto;
    }
}
