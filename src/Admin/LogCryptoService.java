package Admin;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * Класс LogCryptoService отвечает за криптографическое шифрование и расшифровку данных журналов аудита.
 * Полностью совместим по алгоритму и секретному ключу с основным классом логгирования (AuditLogger)
 * (использует AES/ECB/PKCS5Padding и генерацию ключа через SHA-256).
 */
public class LogCryptoService {

    // Секретная кодовая фраза для генерации ключа шифрования (должна строго совпадать с ключом в AuditLogger)
    private static final String SECRET_KEY_PASS = "SMK_Audit_Secret_Key_2026";
    private static SecretKeySpec secretKey;

    // Статический блок инициализации: генерирует ключ при первой загрузке класса в память
    static {
        initKey();
    }

    /**
     * Инициализация 128-битного AES ключа на основе хэширования секретной фразы алгоритмом SHA-256.
     */
    private static void initKey() {
        try {
            byte[] key = SECRET_KEY_PASS.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // Обрезаем до 16 байт (128 бит), необходимых для AES
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Расшифровка всего содержимого зашифрованного файла лога (построчно).
     * Каждая строка файла расшифровывается из формата Base64 обратно в понятный текст.
     */
    public static String decrypt(String encryptedContent) throws Exception {
        if (encryptedContent == null || encryptedContent.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] lines = encryptedContent.split("\r?\n"); // Разбиваем файл на строки

        // Инициализируем криптографический шифр в режиме расшифровки (DECRYPT_MODE)
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty()) {
                try {
                    // Декодируем строку из Base64 в байтовый массив и расшифровываем с помощью AES
                    byte[] decodedBytes = Base64.getDecoder().decode(trimmedLine);
                    byte[] decryptedBytes = cipher.doFinal(decodedBytes);
                    // Добавляем расшифрованную строку в общий результат
                    result.append(new String(decryptedBytes, StandardCharsets.UTF_8)).append("\n");
                } catch (Exception e) {
                    // Если конкретная строка повреждена или не является валидным шифротекстом, помечаем её как ошибочную
                    result.append("[ОШИБКА СТРОКИ] ").append(trimmedLine).append("\n");
                }
            }
        }
        return result.toString();
    }

    /**
     * Зашифровка всего содержимого текста обратно в зашифрованный формат (построчно).
     * Каждая текстовая строка шифруется алгоритмом AES и упаковывается в строку Base64.
     */
    public static String encrypt(String plainText) throws Exception {
        if (plainText == null || plainText.trim().isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] lines = plainText.split("\r?\n"); // Разбиваем текст на отдельные строки

        // Инициализируем криптографический шифр в режиме шифрования (ENCRYPT_MODE)
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                // Шифруем байты строки и кодируем результат в безопасный ASCII-текст формата Base64
                byte[] encryptedBytes = cipher.doFinal(line.getBytes(StandardCharsets.UTF_8));
                String base64Encoded = Base64.getEncoder().encodeToString(encryptedBytes);
                result.append(base64Encoded).append("\n");
            }
        }
        return result.toString();
    }
}