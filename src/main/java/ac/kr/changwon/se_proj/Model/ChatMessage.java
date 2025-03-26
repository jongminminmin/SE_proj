package ac.kr.changwon.se_proj.Model;


import jakarta.persistence.*;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "chat")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int roomId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String content;

    @Column(nullable = false, unique = true)
    private String username;

    @Temporal(TemporalType.TIMESTAMP)
    private Date timestamp;

//    @Column(unique = true) 이 테이블은 파일 데이터를 업로드 하거나
//    다운로드 시
//    데이터베이스에서 가져 올지 아니면 GET 메서드로 가져 올 지
//    정해야함.
//    private String fileData;
}
