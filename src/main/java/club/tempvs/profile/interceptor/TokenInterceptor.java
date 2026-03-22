package club.tempvs.profile.interceptor;

import club.tempvs.profile.exception.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;


@Component
public class TokenInterceptor implements HandlerInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String CHAR_ENCODING = "UTF-8";

    @Value("${authorization.token}")
    private String token;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authHeaderValue = request.getHeader(AUTHORIZATION_HEADER);
        byte[] tokenBytes = token.getBytes(CHAR_ENCODING);
        String tokenHash = DigestUtils.md5DigestAsHex(tokenBytes);

        if (authHeaderValue == null || !authHeaderValue.equals(tokenHash)) {
            throw new UnauthorizedException("Authentication failed. Wrong token is received.");
        }

        return true;
    }
}
