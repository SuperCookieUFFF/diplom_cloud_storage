package spingcloud.diplom.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import spingcloud.diplom.dto.LoginRequest;
import spingcloud.diplom.service.UserService;

import java.util.HashMap;
import java.util.Map;

@RestController
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {

        logger.info("Login attempt for user: {}", loginRequest.getLogin());
        logger.debug("Login request received - username: {}, content type: {}",
                loginRequest.getLogin(), loginRequest.getClass().getSimpleName());

        try {
            long startTime = System.currentTimeMillis();

            String token = userService.authenticate(loginRequest.getLogin(), loginRequest.getPassword());

            long duration = System.currentTimeMillis() - startTime;

            Map<String, String> response = new HashMap<>();
            response.put("auth-token", token);


            logger.info("Successful login for user: {}, token generated: {}, duration: {}ms",
                    loginRequest.getLogin(),
                    maskToken(token), // Маскируем токен для безопасности
                    duration);
            logger.debug("Full token for user {}: {}", loginRequest.getLogin(), token);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {

            logger.warn("Failed login attempt for user: {}, reason: {}",
                    loginRequest.getLogin(), e.getMessage());
            logger.debug("Authentication failure details for user: {}", loginRequest.getLogin(), e);

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("auth-token") String token) {
        logger.info("Logout request received for token: {}", maskToken(token));
        logger.debug("Full logout token: {}", token);

        try {
            long startTime = System.currentTimeMillis();

            userService.logout(token);

            long duration = System.currentTimeMillis() - startTime;

            logger.info("Successful logout for token: {}, duration: {}ms",
                    maskToken(token), duration);

            return ResponseEntity.ok().build();

        } catch (RuntimeException e) {
            logger.error("Logout failed for token: {}, reason: {}",
                    maskToken(token), e.getMessage());
            logger.debug("Logout failure details:", e);


            return ResponseEntity.ok().build();
        }
    }


    private String maskToken(String token) {
        if (token == null || token.length() <= 12) {
            return "***";
        }
        return token.substring(0, 8) + "..." + token.substring(token.length() - 4);
    }
}