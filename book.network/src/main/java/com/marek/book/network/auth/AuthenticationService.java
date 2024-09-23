package com.marek.book.network.auth;

import com.marek.book.network.email.EmailService;
import com.marek.book.network.email.EmailTemplateName;
import com.marek.book.network.role.Role;
import com.marek.book.network.role.RoleRepository;
import com.marek.book.network.security.JwtService;
import com.marek.book.network.user.Token;
import com.marek.book.network.user.TokenRepository;
import com.marek.book.network.user.User;
import com.marek.book.network.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Value("${application.mailing.frontend.activation-url}")
    private String activationUrl;

    public void register(RegistrationRequest request) throws MessagingException {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Role USER not found"));
        User user = User.builder().firstName(request.getFirstname())
                .lastName(request.getLastname())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .accountLocked(false)
                .enabled(false)
                .roles(List.of(userRole))
                .build();
        userRepository.save(user);
        sendValidationEmail(user);
    }

    private void sendValidationEmail(User user) throws MessagingException {
        var newToken = generateAndSaveActivationToken(user);
        emailService.sendEmail(
                user.getEmail(),
                user.getFullName(),
                EmailTemplateName.ACTIVATE_ACCOUNT,
                activationUrl,
                newToken,
                "Account activation"
        );
    }

    private String generateAndSaveActivationToken(User user) {
        String generatedCode = generateActivationCode(6);
        var token = Token.builder()
                .token(generatedCode)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .user(user)
                .build();
        tokenRepository.save(token);
        return generatedCode;
    }

    private String generateActivationCode(int length) {
        String chars = "0123456789";
        StringBuilder code = new StringBuilder();
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < length; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest authenticationRequest) {
        var auth = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
                authenticationRequest.getEmail(),
                authenticationRequest.getPassword()
        ));
        var claims = new HashMap<String, Object>();
        var user = (User) auth.getPrincipal();
        claims.put("fullName", user.getFullName());
        var jwtToken = jwtService.generateToken(claims, user);
        return AuthenticationResponse.builder().token(jwtToken).build();

    }

    //@Transactional
    public void activateAccount(String token) throws MessagingException {
        Token savedToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("Token not found"));
        if (savedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            sendValidationEmail(savedToken.getUser());
            throw new RuntimeException("Activation token has expired. A new token has been sent to your email.");
        }
        var user = userRepository.findById(savedToken.getUser().getId())
                .orElseThrow(() -> new IllegalStateException("User not found"));
        user.setEnabled(true);
        userRepository.save(user);
        savedToken.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(savedToken);
    }
}
