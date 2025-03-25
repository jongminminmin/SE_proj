package ac.kr.changwon.se_proj.Service.impl;


import ac.kr.changwon.se_proj.Repository.UserRepository;
import ac.kr.changwon.se_proj.UserRepository.User;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Optional;

@Service
public class LoginService {

    private static final String SECRET_KEY ="1234";

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean authenticate(String id, String rawPassword) {
        Optional<User> searchUser = userRepository.findByUsername(id);
        return searchUser.filter(user -> passwordEncoder.matches(rawPassword, user.getPassword())).isPresent();
    }

    public String generateToken(String id) {
        long expireTime = System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 7; //7일의 유효기간

        return JWT.create()
                .withIssuer("your_issuer")
                .withSubject(id)
                .withExpiresAt(new Date(System.currentTimeMillis() + expireTime))
                .sign(Algorithm.HMAC256(SECRET_KEY));
    }
}
