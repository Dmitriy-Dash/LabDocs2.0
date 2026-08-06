package Admin;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Сервис шифрования и расшифровки логов аудита,
 * полностью совместимый с алгоритмом AuditLogger (AES/ECB/PKCS5Padding).
 */
public class LogCryptoService {

    private static final String SECRET_KEY_PASS = "SMK_Audit_Secret_Key_2026";
    private static SecretKeySpec secretKey;

    static {
        initKey();
    }

    /**
     * Инициализация 128-битного AES ключа на основе SHA-256 хэша
     */
    private static void initKey() {
        try {
            byte[] key = SECRET_KEY_PASS.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // 128 бит
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Расшифровка всего содержимого зашифрованного файла лога (построчно)
     */
    public static String decrypt(String encryptedContent) throws Exception {
        if (encryptedContent == null || encryptedContent.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] lines = encryptedContent.split("\r?\n");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                try {
                    byte[] decodedBytes = Base64.getDecoder().decode(trimmedLine);
                    byte[] decryptedBytes = cipher.doFinal(decodedBytes);
                    result.append(new String(decryptedBytes, StandardCharsets.UTF_8)).append("\n");
                } catch (Exception e) {
                    // Если строка повреждена или это обычный текст
                    result.append("[ОШИБКА СТРОКИ] ").append(trimmedLine).append("\n");
                }
            }
        }
        return result.toString();
    }

    /**
     * Зашифровка всего содержимого текста обратно в зашифрованный формат (построчно)
     */
    public static String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] lines = plainText.split("\r?\n");

        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                byte[] encryptedBytes = cipher.doFinal(line.getBytes(StandardCharsets.UTF_8));
                String base64Encoded = Base64.getEncoder().encodeToString(encryptedBytes);
                result.append(base64Encoded).append("\n");
            }
        }
        return result.toString();
    }
}