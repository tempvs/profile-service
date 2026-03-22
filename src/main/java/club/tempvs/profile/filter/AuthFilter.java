package club.tempvs.profile.filter;

import club.tempvs.profile.dto.TempvsPrincipal;
import club.tempvs.profile.token.AuthToken;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;
import tools.jackson.databind.json.JsonMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static java.util.stream.Collectors.toSet;

@RequiredArgsConstructor
public class AuthFilter extends GenericFilterBean {

    private static final String USER_INFO_HEADER = "User-Info";

    private final JsonMapper jsonMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        String userInfoHeaderValue = httpRequest.getHeader(USER_INFO_HEADER);

        if (!StringUtils.isEmpty(userInfoHeaderValue)) {
            httpResponse.setHeader(USER_INFO_HEADER, userInfoHeaderValue);

            TempvsPrincipal principal = jsonMapper.readValue(userInfoHeaderValue, TempvsPrincipal.class);
            Set<String> roles = principal.getRoles();
            Set<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(toSet());
            AuthToken authToken = new AuthToken(principal, authorities);
            SecurityContextHolder.getContext()
                    .setAuthentication(authToken);

            Optional.ofNullable(principal.getLang())
                    .map(Locale::new)
                    .ifPresent(LocaleContextHolder::setLocale);
        }

        chain.doFilter(httpRequest, httpResponse);
    }
}
