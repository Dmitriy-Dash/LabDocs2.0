package Admin;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;

public class AdminSecurityManager {

    private static final String PASS_FILE = "admin_config.cfg";
    private static String adminPassword = "admin"; // Пароль по умолчанию

    static {
        loadPassword();
    }

    /**
     * Загрузка пароля из локального конфига, если он существует
     */
    private static void loadPassword() {
        try {
            File file = new File(PASS_FILE);
            if (file.exists()) {
                String savedPass = Files.readString(file.toPath()).trim();
                if (!savedPass.isEmpty()) {
                    adminPassword = savedPass;
                }
            }
        } catch (Exception ignored) {}
    }

    public static boolean verifyPassword(String input) {
        return adminPassword.equals(input);
    }

    public static void changePassword(String newPassword) {
        adminPassword = newPassword;
        try {
            Files.writeString(new File(PASS_FILE).toPath(), newPassword, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            System.err.println("Ошибка сохранения нового пароля: " + e.getMessage());
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