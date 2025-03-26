package ac.kr.changwon.se_proj.Service.Interface;

import ac.kr.changwon.se_proj.Model.User;
import ac.kr.changwon.se_proj.Repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(request);

        Map<String, Object> attributes = oauth2User.getAttributes();
        String registrationId = request.getClientRegistration().getRegistrationId(); // google or kakao
        String userId = null;
        String username = null;
        String email = null;

        if ("google".equals(registrationId)) {
            userId = (String) attributes.get("sub"); // Google 고유 ID
            username = (String) attributes.get("name");
            email = (String) attributes.get("email");

        }
        else if ("kakao".equals(registrationId)) {
            userId = String.valueOf(attributes.get("id"));
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

            username = (String) profile.get("nickname");
            email = (String) kakaoAccount.get("email");
        }

        if (userId != null && !userRepository.existsById(userId)) {
            User newUser = new User(userId, username, "SOCIAL_USER", email);
            userRepository.save(newUser);
        }

        return oauth2User;
    }

}
