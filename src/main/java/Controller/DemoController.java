package Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/* 예제 컨트롤러
* 추가 구현 사항 - 기본 페이지 : index.html
* 사용자 추가 -> 로그인 페이지 (구현)
* 데이터베이스 연결
* 데이터베이스 로직은 구현할 기능부터 추가 후 버전 업
* */
@RestController
public class DemoController {
    @GetMapping("/")
    public String index() {
        return "Hello World";
    }

    @PostMapping("/disconnect")
    public String disconnect() {
        return "Disconnected";
    }
}
