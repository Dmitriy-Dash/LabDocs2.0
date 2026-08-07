import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;

public class PasswordProtectionManager {
    private static final String CONFIG_FILE = "admin_config.cfg";
    // При первом старте по умолчанию используется SHA-256 хэш от строки "123"
    private static String adminPasswordHash = loadPasswordHash();

    /**
     * Считывает хэш пароля из файла конфигурации.
     * Если файл хранил открытый пароль (из старой версии) — автоматически мигрирует его в хэш.
     */
    private static String loadPasswordHash() {
        try {
            File configFile = new File(CONFIG_FILE);
            if (configFile.exists()) {
                String savedContent = Files.readString(configFile.toPath(), StandardCharsets.UTF_8).trim();
                if (!savedContent.isEmpty()) {
                    // Если строка длинее 64 символов или не похожа на HEX-хэш (длина SHA-256 ровно 64 символа)
                    if (savedContent.length() != 64) {
                        // Автоматическая миграция со старого формата открытого текста на SHA-256
                        String newHash = hashSHA256(savedContent);
                        savePasswordHash(newHash);
                        return newHash;
                    }
                    return savedContent;
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка чтения файла конфигурации пароля: " + e.getMessage());
        }
        // Хэш от дефолтного пароля "123"
        return hashSHA256("123");
    }

    /**
     * Сохраняет зашифрованный (хэшированный) пароль в файл конфигурации на диске.
     */
    private static void savePasswordHash(String hash) {
        try {
            Files.writeString(new File(CONFIG_FILE).toPath(), hash.trim(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.err.println("Ошибка сохранения зашифрованного пароля на диск: " + e.getMessage());
        }
    }

    /**
     * Генерация неповторимого одностороннего хэша SHA-256
     */
    public static String hashSHA256(String input) {
        if (input == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(input.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return "";
        }
    }

    // --- Существующий метод запроса доступа ---
    public static boolean requestAdminAccess(Component parent) {
        JPasswordField pf = new JPasswordField(15);
        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(new JLabel("Пароль:"), BorderLayout.WEST);
        panel.add(pf, BorderLayout.CENTER);
        panel.setPreferredSize(new Dimension(380, 40));

        JOptionPane optionPane = new JOptionPane(
                panel,
                JOptionPane.QUESTION_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION
        );

        JDialog dialog = optionPane.createDialog(parent, "Введите пароль администратора");
        dialog.setMinimumSize(new Dimension(400, 150));
        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        dialog.setVisible(true);

        Object selectedValue = optionPane.getValue();
        if (selectedValue != null && (int) selectedValue == JOptionPane.OK_OPTION) {
            String enteredPassword = new String(pf.getPassword());
            if (checkPassword(enteredPassword)) {
                return true;
            } else {
                JOptionPane.showMessageDialog(parent, "Неверный пароль!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    // --- Смена пароля с сохранением хэша на диск ---
    public static void changePassword(Component parent) {
        // Step 1: Проверка текущего пароля
        if (!requestAdminAccess(parent)) {
            return;
        }

        // Step 2: Запрос нового пароля и его подтверждения
        JPasswordField newPasswordField = new JPasswordField(15);
        JPasswordField confirmPasswordField = new JPasswordField(15);

        JPanel panel = new JPanel(new GridLayout(2, 2, 5, 5));
        panel.add(new JLabel("Новый пароль:"));
        panel.add(newPasswordField);
        panel.add(new JLabel("Подтвердите пароль:"));
        panel.add(confirmPasswordField);
        panel.setPreferredSize(new Dimension(380, 50));

        JOptionPane optionPane = new JOptionPane(
                panel,
                JOptionPane.PLAIN_MESSAGE,
                JOptionPane.OK_CANCEL_OPTION
        );

        JDialog dialog = optionPane.createDialog(parent, "Смена пароля администратора");
        dialog.setMinimumSize(new Dimension(420, 160));
        dialog.pack();
        dialog.setLocationRelativeTo(parent);

        dialog.setVisible(true);

        Object selectedValue = optionPane.getValue();
        if (selectedValue != null && (int) selectedValue == JOptionPane.OK_OPTION) {
            String newPassword = new String(newPasswordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (newPassword.trim().isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Пароль не может быть пустым!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newPassword.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(parent, "Пароли не совпадают!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Рассчитываем хэш от нового пароля и сохраняем на диск
            adminPasswordHash = hashSHA256(newPassword);
            savePasswordHash(adminPasswordHash);

            JOptionPane.showMessageDialog(parent, "Пароль успешно изменен и зашифрован на диске!", "Успех", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // Проверка пароля путем сравнения его SHA-256 хэша
    public static boolean checkPassword(String inputPassword) {
        if (inputPassword == null || adminPasswordHash == null) {
            return false;
        }
        return adminPasswordHash.equalsIgnoreCase(hashSHA256(inputPassword));
    }

    // Для обратной совместимости вызовов
    public static String getAdminPassword() {
        return adminPasswordHash;
    }
}