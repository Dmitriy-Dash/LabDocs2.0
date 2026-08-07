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
import java.util.Scanner;

public class AuditLogger {
    private static AuditLogger instance;

    // Пути к файлам логов
    private static final String EXCEL_LOG_FILE = "audit_journal.csv";     // Таблица для Excel
    private static final String ENCRYPTED_LOG_FILE = "audit_journal.enc"; // Зашифрованный лог

    // Ключ шифрования
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
            key = Arrays.copyOf(key, 16); // Используем 128-битный ключ
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
     * Основной метод логирования действий (5 стандартизированных колонок)
     *
     * @param user    Имя пользователя, ФИО или роль (например, "ввм", "Инженер Валидации (Автотест)")
     * @param action  Тип действия ("СОЗДАНИЕ", "ИЗМЕНЕНИЕ", "УДАЛЕНИЕ", "ВАЛИДАЦИЯ ПО")
     * @param docId   Идентификатор документа или объекта ("DOC-EXT-1477", "ГОСТ 17025")
     * @param details Подробности ("Документ успешно удален...", "Результат: ПРОЙДЕНА...")
     */
    public synchronized void log(String user, String action, String docId, String details) {
        String timeStamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);

        String safeUser = user != null ? user.trim() : "Неизвестный";
        String safeAction = action != null ? action.trim() : "ДЕЙСТВИЕ";
        String safeDocId = docId != null ? docId.trim() : "Н/Д";
        String safeDetails = details != null ? details.trim() : "";

        // 1. Запись в CSV/Excel файл
        writeToExcelLog(timeStamp, safeUser, safeAction, safeDocId, safeDetails);

        // 2. Запись в зашифрованный файл
        writeToEncryptedLog(timeStamp, safeUser, safeAction, safeDocId, safeDetails);
    }

    /**
     * Запись строки в файл Excel (CSV)
     */
    private void writeToExcelLog(String timestamp, String user, String action, String docId, String details) {
        String formattedDetails = details.replace(";", ",").replace("\r\n", " ").replace("\n", " ");
        String line = String.format("%s;%s;%s;%s;%s\n", timestamp, user, action, docId, formattedDetails);

        try (FileWriter writer = new FileWriter(EXCEL_LOG_FILE, StandardCharsets.UTF_8, true)) {
            writer.write(line);
        } catch (IOException e) {
            System.err.println("Ошибка записи в CSV лог: " + e.getMessage());
        }
    }

    /**
     * Шифрование строки по алгоритму AES и запись в защищенный файл
     */
    private void writeToEncryptedLog(String timestamp, String user, String action, String docId, String details) {
        String rawRecord = String.format("%s;%s;%s;%s;%s", timestamp, user, action, docId, details);

        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
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
     * Вспомогательный метод для расшифровки и чтения защищенного лога
     */
    public String readEncryptedLog() {
        StringBuilder decryptedContent = new StringBuilder();
        File file = new File(ENCRYPTED_LOG_FILE);

        if (!file.exists()) {
            return "Зашифрованный журнал пуст.";
        }

        try (Scanner scanner = new Scanner(file, StandardCharsets.UTF_8)) {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey);

            while (scanner.hasNextLine()) {
                String encryptedLine = scanner.nextLine().trim();
                if (!encryptedLine.isEmpty()) {
                    // Используем getDecoder() вместо getEncoder()
                    byte[] decodedBytes = Base64.getDecoder().decode(encryptedLine);
                    byte[] decryptedBytes = cipher.doFinal(decodedBytes);
                    decryptedContent.append(new String(decryptedBytes, StandardCharsets.UTF_8)).append("\n");
                }
            }
        } catch (Exception e) {
            return "Ошибка расшифровки лога: Неверный ключ или поврежденный файл (" + e.getMessage() + ")";
        }

        return decryptedContent.toString();
    }
}