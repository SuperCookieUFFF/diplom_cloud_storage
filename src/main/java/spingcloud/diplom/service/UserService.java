package spingcloud.diplom.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import spingcloud.diplom.entity.AuthToken;
import spingcloud.diplom.entity.User;
import spingcloud.diplom.repository.AuthTokenRepository;
import spingcloud.diplom.repository.UserRepository;
import spingcloud.diplom.security.JwtTokenProvider;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Collections;

@Service
@Transactional
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenRepository authTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider tokenProvider;

    public String authenticate(String login, String password) {
        User user = userRepository.findByLogin(login)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.isEnabled()) {
            throw new RuntimeException("User is disabled");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        // Clean expired tokens
        authTokenRepository.deleteExpiredTokens(LocalDateTime.now());

        String token = tokenProvider.generateToken(login);

        AuthToken authToken = new AuthToken();
        authToken.setUser(user);
        authToken.setToken(token);
        authToken.setExpiresAt(LocalDateTime.now().plusDays(1));

        authTokenRepository.save(authToken);

        return token;
    }

    public void logout(String token) {
        authTokenRepository.deleteByToken(token);
    }

    public User getUserFromToken(String token) {
        String username = tokenProvider.getUsernameFromToken(token);
        return userRepository.findByLogin(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token) &&
                authTokenRepository.existsByToken(token);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByLogin(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        return new org.springframework.security.core.userdetails.User(
                user.getLogin(),
                user.getPassword(),
                Collections.emptyList()
        );
    }
}
