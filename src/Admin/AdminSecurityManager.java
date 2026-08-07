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

public class AdminSecurityManager {

    // Путь к файлу внутри папки Admin
    private static final String FOLDER_NAME = "Admin";
    private static final String FILE_NAME = "admin_config.cfg";

    // SHA-256 хэш по умолчанию для пароля "admin"
    private static final String DEFAULT_HASH = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918";

    private static String adminPasswordHash = DEFAULT_HASH;

    static {
        loadPassword();
    }

    /**
     * Определение и гарантия существования целевой папки Admin
     */
    private static Path getConfigFilePath() {
        // Проверяем существование папки src/Admin (для работы из IDE)
        File srcAdminDir = new File("src" + File.separator + FOLDER_NAME);
        if (srcAdminDir.exists() && srcAdminDir.isDirectory()) {
            return Paths.get("src", FOLDER_NAME, FILE_NAME);
        }

        // В ином случае создаем локальную папку Admin в рабочей директории
        File localAdminDir = new File(FOLDER_NAME);
        if (!localAdminDir.exists()) {
            localAdminDir.mkdirs();
        }
        return Paths.get(FOLDER_NAME, FILE_NAME);
    }

    /**
     * Загрузка зашифрованного хэша пароля из папки Admin
     */
    private static void loadPassword() {
        try {
            Path filePath = getConfigFilePath();
            File file = filePath.toFile();

            if (file.exists()) {
                String savedData = Files.readString(filePath, StandardCharsets.UTF_8).trim();
                if (!savedData.isEmpty()) {
                    if (savedData.matches("^[a-fA-F0-9]{64}$")) {
                        adminPasswordHash = savedData.toLowerCase();
                    } else {
                        // Авто-миграция открытого пароля в SHA-256 хэш
                        changePassword(savedData);
                    }
                }
            } else {
                saveHashToFile(DEFAULT_HASH);
            }
        } catch (Exception ignored) {}
    }

    /**
     * Проверка вводимого пароля
     */
    public static boolean verifyPassword(String input) {
        if (input == null) return false;
        String inputHash = hashPassword(input);
        return adminPasswordHash.equalsIgnoreCase(inputHash);
    }

    /**
     * Смена пароля
     */
    public static void changePassword(String newPassword) {
        if (newPassword == null || newPassword.isEmpty()) return;

        String newHash = hashPassword(newPassword);
        adminPasswordHash = newHash;
        saveHashToFile(newHash);
    }

    /**
     * Сохранение хэша в файл внутри папки Admin
     */
    private static void saveHashToFile(String hash) {
        try {
            Path filePath = getConfigFilePath();
            Files.writeString(
                    filePath,
                    hash,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING
            );
        } catch (Exception e) {
            System.err.println("Ошибка сохранения файла конфигурации в папку Admin: " + e.getMessage());
        }
    }

    /**
     * Генерация SHA-256
     */
    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder(2 * encodedHash.length);
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Ошибка: алгоритм SHA-256 недоступен", e);
        }
    }

    public static boolean authenticate() {
        JPanel panel = new JPanel(new GridLayout(2, 1, 5, 5));
        JLabel label = new JLabel("Введите пароль администратора:");
        JPasswordField passwordField = new JPasswordField(15);
        panel.add(label);
        panel.add(passwordField);

        int option = JOptionPane.showConfirmDialog(
                null,
                panel,
                "Авторизация Admin Audit Editor",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (option == JOptionPane.OK_OPTION) {
            String inputPassword = new String(passwordField.getPassword());
            if (verifyPassword(inputPassword)) {
                return true;
            } else {
                JOptionPane.showMessageDialog(
                        null,
                        "Неверный пароль доступа!",
                        "Ошибка авторизации",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
        return false;
    }
}