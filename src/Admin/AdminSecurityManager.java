package Admin;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Класс AdminSecurityManager отвечает за безопасность административной панели,
 * хранение, проверку, хэширование пароля администратора, а также за графический
 * диалог аутентификации.
 */
public class AdminSecurityManager {

    // Константы для определения путей к конфигурационному файлу администратора
    private static final String FOLDER_NAME = "Admin";
    private static final String FILE_NAME = "admin_config.cfg";

    // SHA-256 хэш по умолчанию для строки "admin" (используется при первом запуске)
    private static final String DEFAULT_HASH = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918";

    // Переменная для хранения текущего хэша пароля администратора в памяти
    private static String adminPasswordHash = DEFAULT_HASH;

    // Статический блок инициализации: автоматически вызывается при первом обращении к классу
    static {
        loadPassword(); // Сразу пытаемся загрузить сохраненный пароль из файла
    }

    /**
     * Определение и гарантия существования целевой папки Admin.
     * Возвращает корректный путь к файлу конфигурации (`admin_config.cfg`).
     */
    private static Path getConfigFilePath() {
        // Проверяем существование папки src/Admin (актуально при запуске проекта из среды разработки IDE)
        File srcAdminDir = new File("src" + File.separator + FOLDER_NAME);
        if (srcAdminDir.exists() && srcAdminDir.isDirectory()) {
            return Paths.get("src", FOLDER_NAME, FILE_NAME);
        }

        // Если папки в src нет, работаем с локальной папкой Admin в корневой рабочей директории приложения
        File localAdminDir = new File(FOLDER_NAME);
        if (!localAdminDir.exists()) {
            localAdminDir.mkdirs(); // Создаем директорию, если она отсутствует
        }
        return Paths.get(FOLDER_NAME, FILE_NAME);
    }

    /**
     * Загрузка зашифрованного хэша пароля из конфигурационного файла.
     * Если файл отсутствует, создает его и записывает хэш по умолчанию.
     * Поддерживает автомиграцию: если в файле случайно оказался открытый текст, он преобразуется в хэш.
     */
    private static void loadPassword() {
        try {
            Path filePath = getConfigFilePath();
            File file = filePath.toFile();

            if (file.exists()) {
                // Читаем содержимое файла и удаляем лишние пробелы/переносы строк
                String savedData = Files.readString(filePath, StandardCharsets.UTF_8).trim();
                if (!savedData.isEmpty()) {
                    // Проверяем, является ли прочитанная строка корректным 64-символьным SHA-256 хэшем в шестнадцатеричном формате
                    if (savedData.matches("^[a-fA-F0-9]{64}$")) {
                        adminPasswordHash = savedData.toLowerCase();
                    } else {
                        // Авто-миграция: если в файле был сохранен пароль в открытом виде, хэшируем и сохраняем его заново
                        changePassword(savedData);
                    }
                }
            } else {
                // Если файла конфигурации нет, сохраняем дефолтный хэш
                saveHashToFile(DEFAULT_HASH);
            }
        } catch (Exception ignored) {
            // Исключения при чтении игнорируются, приложение продолжит работать с хэшем по умолчанию
        }
    }

    /**
     * Проверка введенного пользователем пароля.
     * Сравнивает хэш введенной строки с актуальным хэшем администратора.
     */
    public static boolean verifyPassword(String input) {
        if (input == null) return false;
        String inputHash = hashPassword(input);
        return adminPasswordHash.equalsIgnoreCase(inputHash);
    }

    /**
     * Процедура изменения пароля администратора.
     * Вычисляет новый хэш, обновляет его в памяти и записывает на диск.
     */
    public static void changePassword(String newPassword) {
        if (newPassword == null || newPassword.isEmpty()) return;

        String newHash = hashPassword(newPassword);
        adminPasswordHash = newHash;
        saveHashToFile(newHash);
    }

    /**
     * Запись хэша строки пароля в конфигурационный файл `admin_config.cfg`.
     */
    private static void saveHashToFile(String hash) {
        try {
            Path filePath =getConfigFilePath();
            Files.writeString(
                    filePath,
                    hash,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, // Создать файл, если он не существует
                    StandardOpenOption.TRUNCATE_EXISTING // Перезаписать файл, если он уже существовал
            );
        } catch (Exception e) {
            System.err.println("Ошибка сохранения файла конфигурации в папку Admin: " + e.getMessage());
        }
    }

    /**
     * Криптографическое хэширование строки пароля с помощью алгоритма SHA-256.
     * Превращает байтовый массив хэша в читаемую шестнадцатеричную (hex) строку.
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0'); // Дополняем нулем до двух символов при необходимости
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка: алгоритм SHA-256 недоступен", e);
        }
    }

    /**
     * Вывод графического диалогового окна Swing для ввода пароля администратора.
     * Возвращает true при успешной аутентификации и false в случае отмены или неверного пароля.
     */
    public static boolean authenticate() {
        // Создаем панель для диалогового окна с сеткой 2x2 и отступами
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        JLabel label = new JLabel("Введите пароль администратора:");
        JPasswordField passwordField = new JPasswordField(15);
        panel.add(label);
        panel.add(passwordField);

        // Показываем модальное окно подтверждения
        int option = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Авторизация Admin Audit Editor",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        // Если пользователь нажал кнопку OK
        if (option == JOptionPane.OK_OPTION) {
            String inputPassword = new String(passwordField.getPassword());
            if (verifyPassword(inputPassword)) {
                return true; // Пароль верный
            } else {
                // Выводим сообщение об ошибке при неверном пароле
                JOptionPane.showMessageDialog(
                        null,
                        "Неверный пароль доступа!",
                        "Ошибка авторизации",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
        return false; // Авторизация не пройдена или отменена
    }
}