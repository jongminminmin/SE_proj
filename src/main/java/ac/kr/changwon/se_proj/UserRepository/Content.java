package ac.kr.changwon.se_proj.UserRepository;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

/* 컨텐츠 인스턴스*/

@Entity
@Data
@Table(name = "content")
public class Content {
    /* id 인스턴스 참조 객체 : user table의 user_id 엔티티
    * */


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private String id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = true)
    private String content;

    /*컨텐츠 내용이 없으면 다시 작성하라 알리는 메서드*/
    public boolean isAvailable(String content) {
        if(content == null)return false;
        return content.equals(this.content);
    }


}
