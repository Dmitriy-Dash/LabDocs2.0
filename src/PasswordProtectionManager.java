import javax.swing.*;
import java.awt.*;

public class PasswordProtectionManager {

    // Запрос прав администратора / менеджера по качеству
    public static boolean requestAdminAccess(Component parent) {
        JPasswordField pf = new JPasswordField();
        int action = JOptionPane.showConfirmDialog(
                parent,
                pf,
                "Введите пароль Менеджера по качеству:",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        );

        if (action == JOptionPane.OK_OPTION) {
            String password = new String(pf.getPassword());
            return UserManager.checkPassword(User.Role.QUALITY_MANAGER, password);
        }
        return false;
    }

    // ----------------------------------------------------
    // НОВЫЙ МЕТОД ДЛЯ ВАЛИДАТОРОВ (принимает только пароль)
    // ----------------------------------------------------
    public static boolean checkPassword(String rawPassword) {
        // По умолчанию при валидации проверяем пароль роли QUALITY_MANAGER
        return UserManager.checkPassword(User.Role.QUALITY_MANAGER, rawPassword);
    }

    // Перегрузка для совместимости (роль/имя пользователя + пароль)
    public static boolean checkPassword(String identifier, String rawPassword) {
        try {
            User.Role role = User.Role.valueOf(identifier);
            return UserManager.checkPassword(role, rawPassword);
        } catch (IllegalArgumentException e) {
            return UserManager.checkPasswordByUsername(identifier, rawPassword);
        }
    }

    // Функция смены пароля
    public static void changePassword(Component parent, User currentUser) {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        JPasswordField oldPassField = new JPasswordField();
        JPasswordField newPassField = new JPasswordField();
        JPasswordField confirmPassField = new JPasswordField();

        panel.add(new JLabel("Старый пароль:"));
        panel.add(oldPassField);
        panel.add(new JLabel("Новый пароль:"));
        panel.add(newPassField);
        panel.add(new JLabel("Подтвердите пароль:"));
        panel.add(confirmPassField);

        int option = JOptionPane.showConfirmDialog(parent, panel,
                "Смена пароля (" + currentUser.getRole().getDisplayName() + ")",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (option == JOptionPane.OK_OPTION) {
            String oldPass = new String(oldPassField.getPassword());
            String newPass = new String(newPassField.getPassword());
            String confirmPass = new String(confirmPassField.getPassword());

            if (!UserManager.checkPassword(currentUser.getRole(), oldPass)) {
                JOptionPane.showMessageDialog(parent, "Указан неверный старый пароль!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (newPass.isEmpty()) {
                JOptionPane.showMessageDialog(parent, "Новый пароль не может быть пустым!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!newPass.equals(confirmPass)) {
                JOptionPane.showMessageDialog(parent, "Новые пароли не совпадают!", "Ошибка", JOptionPane.ERROR_MESSAGE);
                return;
            }

            UserManager.updatePassword(currentUser.getRole(), newPass);
            JOptionPane.showMessageDialog(parent, "Пароль успешно изменен!", "Успех", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}