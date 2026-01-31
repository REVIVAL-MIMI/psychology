package com.psychology.service;

import com.psychology.dto.AuthDTO.*;
import com.psychology.model.entity.Client;
import com.psychology.model.entity.Invite;
import com.psychology.model.entity.Psychologist;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
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
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;

    private static final String BLACKLIST_PREFIX = "blacklist:";
    private static final String REFRESH_PREFIX = "refresh:";

    public AuthResponse verifyOTPAndAuthenticate(VerifyOtpRequest request) {
        // Проверяем OTP
        if (!otpService.verifyOTP(request.getPhone(), request.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        // Ищем пользователя
        var user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // ВАЖНО: Проверяем верификацию для психологов
//        if (user instanceof Psychologist psychologist && !psychologist.isVerified()) {
//            throw new RuntimeException("Psychologist account is pending verification by administrator");
//        }

        log.info("User authenticated: {} with role {}", user.getPhone(), user.getRole());

        // Генерируем токены
        return generateAuthResponse(user);
    }

    public AuthResponse refreshToken(String refreshToken) {
        // Проверяем, не в черном списке ли токен
        if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + refreshToken))) {
            throw new RuntimeException("Token is blacklisted");
        }

        // Валидируем refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        String phone = jwtTokenProvider.extractUsername(refreshToken);
        var user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Проверяем, что этот refresh token еще валиден
        String storedRefreshToken = stringRedisTemplate.opsForValue().get(REFRESH_PREFIX + phone);
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
        return generateAuthResponse(user);
    }

    public AuthResponse registerPsychologist(PsychologistRegisterRequest request) {
        // Проверяем, не занят ли телефон
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone already registered");
        }

        // Создаем психолога
        Psychologist psychologist = new Psychologist();
        psychologist.setPhone(request.getPhone());
        psychologist.setFullName(request.getFullName());
        psychologist.setEmail(request.getEmail());
        psychologist.setEducation(request.getEducation());
        psychologist.setSpecialization(request.getSpecialization());
        psychologist.setDescription(request.getDescription());
        psychologist.setPhotoUrl(request.getPhotoUrl());
        psychologist.setRole(UserRole.ROLE_PSYCHOLOGIST);
        psychologist.setVerified(false); // Требуется верификация админом

        psychologistRepository.save(psychologist);

        return generateAuthResponse(psychologist);
    }

    public AuthResponse registerClient(ClientRegisterRequest request, String inviteToken) {
        // Проверяем инвайт
        Invite invite = inviteRepository.findByToken(inviteToken)
                .orElseThrow(() -> new RuntimeException("Invalid invite token"));

        if (invite.isUsed()) {
            throw new RuntimeException("Invite already used");
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invite expired");
        }

        // Проверяем, не занят ли телефон
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone already registered");
        }

        // Создаем клиента
        Client client = new Client();
        client.setPhone(request.getPhone());
        client.setFullName(request.getFullName());
        client.setAge(request.getAge());
        client.setPhotoUrl(request.getPhotoUrl());
        client.setPsychologist(invite.getPsychologist());
        client.setLinkedAt(LocalDateTime.now());
        client.setRole(UserRole.ROLE_CLIENT);

        clientRepository.save(client);

        // Помечаем инвайт как использованный
        invite.setUsed(true);
        invite.setUsedAt(LocalDateTime.now());
        inviteRepository.save(invite);

        return generateAuthResponse(client);
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

        if (refreshToken != null) {
            // Удаляем refresh token из валидных
            String phone = jwtTokenProvider.extractUsername(refreshToken);
            stringRedisTemplate.delete(REFRESH_PREFIX + phone);
        }
    }

    private AuthResponse generateAuthResponse(com.psychology.model.entity.User user) {
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
        response.setRefreshToken(refreshToken);
        response.setUserId(user.getId());
        response.setUserRole(user.getRole().name());
        response.setFullName(getFullName(user));
        response.setPhone(user.getPhone());

        return response;
    }

    private String getFullName(com.psychology.model.entity.User user) {
        if (user instanceof Psychologist) {
            return ((Psychologist) user).getFullName();
        } else if (user instanceof Client) {
            return ((Client) user).getFullName();
        }
        return "";
    }
}