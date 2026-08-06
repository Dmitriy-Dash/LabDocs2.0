import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;

public class AuditLogger {
    private static AuditLogger instance;

    // Пути к файлам логов
    private static final String EXCEL_LOG_FILE = "audit_journal.csv";     // Таблица для Excel
    private static final String ENCRYPTED_LOG_FILE = "audit_journal.enc"; // Зашифрованный лог

    // Ключ шифрования (в продакшене лучше брать из пароля или файла конфигурации)
    private static final String SECRET_KEY_PASS = "SMK_Audit_Secret_Key_2026";
    private static SecretKeySpec secretKey;

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    private AuditLogger() {
        initKey();
        initExcelFile();
    }

    public static synchronized AuditLogger getInstance() {
        if (instance == null) {
            instance = new AuditLogger();
        }
        return instance;
    }

    /**
     * Инициализация AES ключа шифрования на основе пароля
     */
    private void initKey() {
        try {
            byte[] key = SECRET_KEY_PASS.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            key = sha.digest(key);
            key = Arrays.copyOf(key, 16); // Используем первые 128 бит
            secretKey = new SecretKeySpec(key, "AES");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Создает CSV файл для Excel с заголовками, если он еще не существует
     */
    private void initExcelFile() {
        File file = new File(EXCEL_LOG_FILE);
        if (!file.exists()) {
            try (FileOutputStream fos = new FileOutputStream(file);
                 FileWriter writer = new FileWriter(file, StandardCharsets.UTF_8, true)) {

                // Записываем UTF-8 BOM, чтобы Excel корректно отображал кириллицу
                fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

                // Заголовки таблицы Excel
                writer.write("Дата и время;Пользователь/Роль;Действие;ID Документа;Детали/Примечание\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Основной метод логирования действий
     *
     * @param user    Имя пользователя или роль (например, "Администратор", "Оператор")
     * @param action  Тип действия ("ДОБАВЛЕНИЕ", "ИЗМЕНЕНИЕ", "УДАЛЕНИЕ", "ВХОД")
     * @param docId   Идентификатор документа ("УР1-01", "Н/Д")
     * @param details Подробности (например, "Изменено название с X на Y")
     */
    public synchronized void log(String user, String action, String docId, String details) {
        String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        // 1. Запись в открытый журнал Excel (CSV)
        writeToExcelLog(timestamp, user, action, docId, details);

        // 2. Запись в зашифрованный журнал
        writeToEncryptedLog(timestamp, user, action, docId, details);
    }

    /**
     * Запись строки в файл Excel (CSV)
     */
    private void writeToExcelLog(String timestamp, String user, String action, String docId, String details) {
        // Экранируем возможные точки с запятой и кавычки для Excel
        String safeDetails = details != null ? details.replace(";", ",").replace("\n", " ") : "";
        String line = String.format("%s;%s;%s;%s;%s\n", timestamp, user, action, docId, safeDetails);

        try (FileWriter writer = new FileWriter(EXCEL_LOG_FILE, StandardCharsets.UTF_8, true)) {
            writer.write(line);
        } catch (IOException e) {
            System.err.println("Ошибка записи в открытый лог: " + e.getMessage());
        }
    }

    /**
     * Шифрование строки по алгоритму AES и запись в защищенный файл
     */
    private void writeToEncryptedLog(String timestamp, String user, String action, String docId, String details) {
        String rawRecord = String.format("[%s] | USER: %s | ACTION: %s | DOC: %s | DETAILS: %s",
                timestamp, user, action, docId, details);

        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            // Заменено Cipher.ENCRYPT -> Cipher.ENCRYPT_MODE
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(rawRecord.getBytes(StandardCharsets.UTF_8));

            String encryptedLine = Base64.getEncoder().encodeToString(encryptedBytes) + "\n";

            try (FileWriter writer = new FileWriter(ENCRYPTED_LOG_FILE, StandardCharsets.UTF_8, true)) {
                writer.write(encryptedLine);
            }
        } catch (Exception e) {
            System.err.println("Ошибка записи в зашифрованный лог: " + e.getMessage());
        }
    }

    /**
     * Вспомогательный метод для расшифровки и чтения защищенного лога (для администратора)
     */
    public String readEncryptedLog() {
        StringBuilder decryptedContent = new StringBuilder();
        File file = new File(ENCRYPTED_LOG_FILE);

        if (!file.exists()) {
            return "Зашифрованный журнал пуст.";
        }

        try (java.util.Scanner scanner = new java.util.Scanner(file, StandardCharsets.UTF_8)) {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            // Заменено Cipher.DECRYPT -> Cipher.DECRYPT_MODE
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            while (scanner.hasNextLine()) {
                String encryptedLine = scanner.nextLine().trim();
                if (!encryptedLine.isEmpty()) {
                    byte[] decodedBytes = Base64.getDecoder().decode(encryptedLine);
                    byte[] decryptedBytes = cipher.doFinal(decodedBytes);
                    decryptedContent.append(new String(decryptedBytes, StandardCharsets.UTF_8)).append("\n");
                }
            }
        } catch (Exception e) {
            return "Ошибка расшифровки лога: Неверный ключ или поврежденный файл.";
        }

        return decryptedContent.toString();
    }
}