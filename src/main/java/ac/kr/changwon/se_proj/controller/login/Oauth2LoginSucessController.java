package ac.kr.changwon.se_proj.controller.login;

import org.springframework.stereotype.Controller;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class Oauth2LoginSucessController {

    @GetMapping("/oauth2/success")
    public String oauth2Success(OAuth2AuthenticationToken authentication) {
        OAuth2User user = authentication.getPrincipal();
        System.out.println("✅ 로그인한 소셜 사용자 정보: " + user.getAttributes());
        return "redirect:/"; // 로그인 성공 후 리디렉션 경로
    }
}
