package ac.kr.changwon.se_proj.Repository;


import ac.kr.changwon.se_proj.Model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, String> {
}
