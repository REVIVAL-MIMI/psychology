package com.psychology.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class TelegramOtpService {

    private static final String TELEGRAM_API_BASE = "https://api.telegram.org";
    private static final String PHONE_TO_CHAT_PREFIX = "telegram:phone_chat:";
    private static final String CHAT_TO_PHONE_PREFIX = "telegram:chat_phone:";
    private static final String LAST_UPDATE_ID_KEY = "telegram:last_update_id";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestClient restClient = RestClient.create(TELEGRAM_API_BASE);

    @Value("${app.telegram.enabled:false}")
    private boolean telegramEnabled;

    @Value("${app.telegram.bot-token:}")
    private String botToken;

    @Value("${app.telegram.polling.enabled:true}")
    private boolean pollingEnabled;

    public boolean sendOtpToLinkedChat(String phone, String otp) {
        if (!isConfigured()) {
            return false;
        }

        String normalizedPhone = normalizePhone(phone);
        if (normalizedPhone == null) {
            log.warn("Unable to normalize phone for Telegram OTP delivery: {}", phone);
            return false;
        }

        String chatId = stringRedisTemplate.opsForValue().get(PHONE_TO_CHAT_PREFIX + normalizedPhone);
        if (chatId == null || chatId.isBlank()) {
            log.warn("No Telegram link found for phone {}", normalizedPhone);
            return false;
        }

        String text = """
                Код входа: %s
                Он действует 5 минут.
                Если это были не вы, просто проигнорируйте сообщение.
                """.formatted(otp).trim();

        return sendMessage(chatId, text);
    }

    @Scheduled(fixedDelayString = "${app.telegram.polling.fixed-delay-ms:5000}")
    public void pollUpdates() {
        if (!isConfigured() || !pollingEnabled) {
            return;
        }

        try {
            long offset = getLastUpdateId() + 1;
            JsonNode updates = getUpdates(offset);
            if (updates == null || !updates.isArray() || updates.isEmpty()) {
                return;
            }

            long maxUpdateId = offset - 1;
            for (JsonNode update : updates) {
                long updateId = update.path("update_id").asLong(0L);
                if (updateId > maxUpdateId) {
                    maxUpdateId = updateId;
                }
                processUpdate(update);
            }

            if (maxUpdateId >= offset - 1) {
                setLastUpdateId(maxUpdateId);
            }
        } catch (Exception e) {
            log.warn("Telegram polling failed: {}", e.getMessage());
        }
    }

    private void processUpdate(JsonNode update) {
        JsonNode message = update.path("message");
        if (message.isMissingNode()) {
            return;
        }

        String chatId = message.path("chat").path("id").asText("");
        String text = message.path("text").asText("");
        if (chatId.isBlank() || text.isBlank()) {
            return;
        }

        String command = text.trim();
        if (command.startsWith("/start")) {
            sendMessage(chatId, """
                    Привет! Чтобы получать коды входа, отправьте:
                    /link +79990000000

                    Чтобы отвязать номер:
                    /unlink
                    """.trim());
            return;
        }

        if (command.startsWith("/link")) {
            handleLinkCommand(chatId, command);
            return;
        }

        if (command.startsWith("/unlink")) {
            handleUnlinkCommand(chatId);
            return;
        }

        sendMessage(chatId, """
                Не понял команду.
                Используйте:
                /link +79990000000
                /unlink
                """.trim());
    }

    private void handleLinkCommand(String chatId, String command) {
        String[] parts = command.split("\\s+", 2);
        if (parts.length < 2 || parts[1].isBlank()) {
            sendMessage(chatId, "Укажите номер: /link +79990000000");
            return;
        }

        String normalizedPhone = normalizePhone(parts[1]);
        if (normalizedPhone == null) {
            sendMessage(chatId, "Неверный формат номера. Пример: /link +79990000000");
            return;
        }

        linkChatToPhone(chatId, normalizedPhone);
        sendMessage(chatId, "Готово. Номер " + normalizedPhone + " привязан. Теперь OTP будет приходить сюда.");
    }

    private void handleUnlinkCommand(String chatId) {
        String linkedPhone = stringRedisTemplate.opsForValue().get(CHAT_TO_PHONE_PREFIX + chatId);
        if (linkedPhone == null || linkedPhone.isBlank()) {
            sendMessage(chatId, "Сейчас к этому чату ничего не привязано.");
            return;
        }

        stringRedisTemplate.delete(CHAT_TO_PHONE_PREFIX + chatId);
        stringRedisTemplate.delete(PHONE_TO_CHAT_PREFIX + linkedPhone);
        sendMessage(chatId, "Номер " + linkedPhone + " отвязан.");
    }

    private void linkChatToPhone(String chatId, String phone) {
        String previousChat = stringRedisTemplate.opsForValue().get(PHONE_TO_CHAT_PREFIX + phone);
        if (previousChat != null && !previousChat.equals(chatId)) {
            stringRedisTemplate.delete(CHAT_TO_PHONE_PREFIX + previousChat);
        }

        String previousPhone = stringRedisTemplate.opsForValue().get(CHAT_TO_PHONE_PREFIX + chatId);
        if (previousPhone != null && !previousPhone.equals(phone)) {
            stringRedisTemplate.delete(PHONE_TO_CHAT_PREFIX + previousPhone);
        }

        stringRedisTemplate.opsForValue().set(PHONE_TO_CHAT_PREFIX + phone, chatId);
        stringRedisTemplate.opsForValue().set(CHAT_TO_PHONE_PREFIX + chatId, phone);
    }

    private JsonNode getUpdates(long offset) {
        try {
            String response = restClient.get()
                    .uri("/bot" + botToken + "/getUpdates?offset=" + offset + "&timeout=0")
                    .retrieve()
                    .body(String.class);
            return parseResult(response, "getUpdates");
        } catch (Exception e) {
            log.warn("Failed to fetch Telegram updates: {}", e.getMessage());
            return null;
        }
    }

    private boolean sendMessage(String chatId, String text) {
        try {
            String response = restClient.post()
                    .uri("/bot" + botToken + "/sendMessage")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "chat_id", chatId,
                            "text", text,
                            "disable_web_page_preview", true
                    ))
                    .retrieve()
                    .body(String.class);

            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            boolean ok = root.path("ok").asBoolean(false);
            if (!ok) {
                String description = root.path("description").asText("unknown error");
                log.warn("Telegram sendMessage failed: {}", description);
            }
            return ok;
        } catch (Exception e) {
            log.warn("Failed to send Telegram message: {}", e.getMessage());
            return false;
        }
    }

    private JsonNode parseResult(String response, String methodName) {
        try {
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            if (!root.path("ok").asBoolean(false)) {
                String description = root.path("description").asText("unknown error");
                log.warn("Telegram {} failed: {}", methodName, description);
                return null;
            }
            return root.path("result");
        } catch (Exception e) {
            log.warn("Failed to parse Telegram {} response: {}", methodName, e.getMessage());
            return null;
        }
    }

    private long getLastUpdateId() {
        String value = stringRedisTemplate.opsForValue().get(LAST_UPDATE_ID_KEY);
        if (value == null || value.isBlank()) {
            return 0L;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void setLastUpdateId(long updateId) {
        stringRedisTemplate.opsForValue().set(LAST_UPDATE_ID_KEY, String.valueOf(updateId));
    }

    private boolean isConfigured() {
        return telegramEnabled && botToken != null && !botToken.isBlank();
    }

    private String normalizePhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }

        String trimmed = rawPhone.trim();
        String digits = trimmed.replaceAll("\\D", "");
        if (digits.isBlank()) {
            return null;
        }

        if (trimmed.startsWith("+")) {
            if (digits.length() < 10 || digits.length() > 15) {
                return null;
            }
            return "+" + digits;
        }

        if (digits.length() == 11 && digits.startsWith("8")) {
            return "+7" + digits.substring(1);
        }

        if (digits.length() == 11 && digits.startsWith("7")) {
            return "+" + digits;
        }

        if (digits.length() == 10) {
            return "+7" + digits;
        }

        if (digits.length() >= 10 && digits.length() <= 15) {
            return "+" + digits;
        }

        return null;
    }
}
