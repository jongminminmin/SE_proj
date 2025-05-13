package ac.kr.changwon.se_proj.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "chat")
public class ChatProperties {
    /* 1- 10까지는 개인 채팅
    테스팅 환경이기 때문에 10으로 설정. 이후에는 설정해야함.
    , 그 이후로는 그룹채팅으로 나뉨.*/
    private int privateMaxRoomId=10;
}
