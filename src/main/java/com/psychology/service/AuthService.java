package com.psychology.service;

import com.psychology.dto.AuthDTO.*;
import com.psychology.model.entity.Client;
import com.psychology.model.entity.Invite;
import com.psychology.model.entity.Psychologist;
import com.psychology.model.entity.User;
import com.psychology.model.entity.UserRole;
import com.psychology.repository.ClientRepository;
import com.psychology.repository.InviteRepository;
import com.psychology.repository.PsychologistRepository;
import com.psychology.repository.UserRepository;
import com.psychology.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PsychologistRepository psychologistRepository;
    private final ClientRepository clientRepository;
    private final InviteRepository inviteRepository;
    private final OTPService otpService;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${app.organization.name:ООО «Телеком без границ»}")
    private String defaultOrganizationName;

    @Value("${app.psychologists.require-verification:false}")
    private boolean psychologistVerificationRequired;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String REFRESH_PREFIX = "refresh:";

    public record AuthResult(AuthResponse response, String refreshToken) {}

    public AuthResult verifyOTPAndAuthenticate(VerifyOtpRequest request) {
        String loginId = normalizeLoginId(request.getPhone());

        // Проверяем OTP
        if (!otpService.verifyOTP(loginId, request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        // Ищем пользователя
        var user = userRepository.findByPhone(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ВАЖНО: Проверяем верификацию для психологов
        if (user instanceof Psychologist psychologist && !psychologist.isVerified()) {
            throw new RuntimeException("Psychologist account is pending verification by administrator");
        }

        log.info("User authenticated: {} with role {}", user.getPhone(), user.getRole());

        // Генерируем токены
        return generateAuthForUser(user);
    }

    public AuthResult refreshToken(String refreshToken) {
        // Проверяем, не в черном списке ли токен
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + refreshToken))) {
            throw new RuntimeException("Token is blacklisted");
        }

        // Валидируем refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String loginId = normalizeLoginId(jwtTokenProvider.extractUsername(refreshToken));
        var user = userRepository.findByPhone(loginId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Проверяем, что этот refresh token еще валиден
        String storedRefreshToken = stringRedisTemplate.opsForValue().get(REFRESH_PREFIX + loginId);
        if (!refreshToken.equals(storedRefreshToken)) {
            throw new RuntimeException("Refresh token mismatch");
        }

        // Добавляем старый refresh token в blacklist
        stringRedisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + refreshToken,
                "blacklisted",
                jwtTokenProvider.getRefreshTokenExpirationMs(), // Время жизни refresh token
                TimeUnit.MILLISECONDS
        );

        // Генерируем новую пару токенов
        return generateAuthForUser(user);
    }

    public AuthResult registerPsychologist(PsychologistRegisterRequest request) {
        String loginEmail = normalizeLoginId(request.getPhone());
        String profileEmail = normalizeLoginId(request.getEmail());

        // Проверяем, не занят ли email
        if (userRepository.existsByPhone(loginEmail)) {
            throw new RuntimeException("Email already registered");
        }

        // Создаем психолога
        Psychologist psychologist = new Psychologist();
        psychologist.setPhone(loginEmail);
        psychologist.setFullName(request.getFullName());
        psychologist.setEmail(profileEmail);
        psychologist.setOrganizationName(hasText(request.getOrganizationName()) ? request.getOrganizationName() : defaultOrganizationName);
        psychologist.setServiceFormat(request.getServiceFormat());
        psychologist.setEducation(request.getEducation());
        psychologist.setSpecialization(request.getSpecialization());
        psychologist.setDescription(request.getDescription());
        psychologist.setRole(UserRole.ROLE_PSYCHOLOGIST);
        psychologist.setVerified(!psychologistVerificationRequired);
        if (!psychologistVerificationRequired) {
            psychologist.setVerifiedAt(LocalDateTime.now());
        }

        psychologistRepository.save(psychologist);

        return generateAuthForUser(psychologist);
    }

    public AuthResult registerClient(ClientRegisterRequest request, String inviteToken) {
        // Проверяем инвайт
        Invite invite = inviteRepository.findByToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("Invalid invite token"));

        if (invite.isUsed()) {
            throw new RuntimeException("Invite already used");
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invite expired");
        }

        String loginEmail = normalizeLoginId(request.getPhone());

        // Проверяем, не занят ли email
        if (userRepository.existsByPhone(loginEmail)) {
            throw new RuntimeException("Email already registered");
        }

        // Создаем клиента
        Client client = new Client();
        client.setPhone(loginEmail);
        client.setFullName(request.getFullName());
        client.setAge(request.getAge());
        client.setCompanyName(hasText(request.getCompanyName()) ? request.getCompanyName() : defaultOrganizationName);
        client.setWorkEmail(hasText(request.getWorkEmail()) ? normalizeLoginId(request.getWorkEmail()) : loginEmail);
        client.setDepartment(request.getDepartment());
        client.setPosition(request.getPosition());
        client.setEmployeeCode(request.getEmployeeCode());
        client.setPsychologist(invite.getPsychologist());
        client.setLinkedAt(LocalDateTime.now());
        client.setRole(UserRole.ROLE_CLIENT);

        clientRepository.save(client);

        // Помечаем инвайт как использованный
        invite.setUsed(true);
        invite.setUsedAt(LocalDateTime.now());
        inviteRepository.save(invite);

        return generateAuthForUser(client);
    }


    public void logout(String accessToken, String refreshToken) {
        // Добавляем токены в черный список
        if (accessToken != null) {
            // Добавляем access token в черный список на 30 минут
            stringRedisTemplate.opsForValue().set(
                    BLACKLIST_PREFIX + accessToken,
                    "blacklisted",
                    30, // 30 минут
                    java.util.concurrent.TimeUnit.MINUTES
            );
        }

        String loginId = null;

        if (refreshToken != null) {
            loginId = normalizeLoginId(jwtTokenProvider.extractUsername(refreshToken));
        } else if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            loginId = normalizeLoginId(jwtTokenProvider.extractUsername(accessToken));
        }

        if (loginId != null && !loginId.isBlank()) {
            stringRedisTemplate.delete(REFRESH_PREFIX + loginId);
        }
    }

    public AuthResult generateAuthForUser(com.psychology.model.entity.User user) {
        // Создаем UserDetails для генерации токена
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getPhone())
                .password("") // Пароль не используется
                .roles(user.getRole().name().replace("ROLE_", ""))
                .build();

        String accessToken = jwtTokenProvider.generateAccessToken(userDetails);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userDetails);

        // Сохраняем refresh token в Redis
        stringRedisTemplate.opsForValue().set(
                REFRESH_PREFIX + user.getPhone(),
                refreshToken,
                7, // 7 дней
                java.util.concurrent.TimeUnit.DAYS
        );

        AuthResponse response = new AuthResponse();
        response.setAccessToken(accessToken);
        response.setUserId(user.getId());
        response.setUserRole(user.getRole().name());
        response.setFullName(getFullName(user));
        response.setPhone(user.getPhone());
        response.setVerified(user instanceof Psychologist psych && psych.isVerified());

        return new AuthResult(response, refreshToken);
    }

    public AuthResult changePhone(User user, String newPhone, String otp) {
        String newLoginId = normalizeLoginId(newPhone);

        if (!otpService.verifyOTP(newLoginId, otp)) {
            throw new RuntimeException("Invalid OTP");
        }

        if (userRepository.existsByPhone(newLoginId)) {
            throw new RuntimeException("Email already registered");
        }

        String oldPhone = user.getPhone();
        user.setPhone(newLoginId);
        userRepository.save(user);

        // Сбрасываем старый refresh token
        stringRedisTemplate.delete(REFRESH_PREFIX + oldPhone);

        return generateAuthForUser(user);
    }

    private String getFullName(com.psychology.model.entity.User user) {
        if (user instanceof Psychologist) {
            return ((Psychologist) user).getFullName();
        } else if (user instanceof Client) {
            return ((Client) user).getFullName();
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalizeLoginId(String value) {
        if (value == null) return "";
        return value.trim().toLowerCase();
    }
}
