package ac.kr.changwon.se_proj.repository;


import ac.kr.changwon.se_proj.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {
}
