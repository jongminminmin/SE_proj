package ac.kr.changwon.se_proj.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest; // 추가: HttpServletRequest 임포트
import java.security.Principal;
import java.util.Map;
import java.util.Objects;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic","/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*");
//                .withSockJS()
//                .setInterceptors(new HttpHandshakeInterceptor());
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (StompCommand.CONNECT.equals(Objects.requireNonNull(accessor).getCommand())) {
                    Principal principal = accessor.getUser();
                    if (principal != null) {
                        logger.debug("STOMP CONNECT: Principal already set by HandshakeInterceptor: {}", principal.getName());
                    } else {
                        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails) {
                            String username = ((UserDetails) authentication.getPrincipal()).getUsername();
                            accessor.setUser(authentication);
                            logger.debug("STOMP CONNECT: Principal set from SecurityContext (fallback) for user: {}", username);
                        } else {
                            String usernameHeader = accessor.getFirstNativeHeader("username");
                            if (usernameHeader != null) {
                                accessor.setUser(() -> usernameHeader);
                                logger.debug("STOMP CONNECT: Principal set from 'username' header (fallback) for user: {}", usernameHeader);
                            } else {
                                logger.warn("STOMP CONNECT: No authenticated Principal in SecurityContext and 'username' header is null. Principal not set for session {}. This might lead to authorization issues.", accessor.getSessionId());
                            }
                        }
                    }
                }
                return message;
            }
        });
    }

    private class HttpHandshakeInterceptor implements HandshakeInterceptor {
        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                       WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
            if (request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;

                // === 수정 부분: getNativeRequest() 대신 getServletRequest() 사용 ===
                HttpServletRequest httpServletRequest = servletRequest.getServletRequest();
                Principal principal = httpServletRequest.getUserPrincipal();

                if (principal != null) {
                    attributes.put(StompHeaderAccessor.USER_HEADER, principal);
                    logger.debug("HandshakeInterceptor: Principal {} found in HTTP session and set to WebSocket attributes.", principal.getName());
                    return true;
                } else {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if(authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof UserDetails){
                        Principal securityPrincipal = authentication;
                        attributes.put(StompHeaderAccessor.USER_HEADER, securityPrincipal);
                        logger.debug("HandshakeInterceptor: Authenticated Principal {} found in SecurityContextHolder and set to WebSocket attributes.", securityPrincipal.getName());
                        return true;
                    } else {
                        logger.warn("HandshakeInterceptor: No authenticated Principal found for WebSocket handshake. SessionId: {}. This might result in a 400 Bad Request.", httpServletRequest.getSession(false) != null ? httpServletRequest.getSession(false).getId() : "N/A");
                        return true;
                    }
                }
            }
            logger.warn("HandshakeInterceptor: Request is not ServletServerHttpRequest. Skipping Principal transfer.");
            return true;
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
            if (exception != null) {
                logger.error("HandshakeInterceptor: WebSocket handshake failed: {}", exception.getMessage(), exception);
            } else {
                logger.debug("HandshakeInterceptor: WebSocket handshake completed.");
            }
        }
    }
}
