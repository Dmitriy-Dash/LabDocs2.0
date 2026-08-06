import javax.swing.*;
import java.awt.*;

public class PasswordProtectionManager {
    // Внимание: измените дефолтный пароль или механизм его хранения при необходимости
    private static String adminPassword = "123";

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
            if (adminPassword.equals(enteredPassword)) {
                return true;
            } else {
                JOptionPane.showMessageDialog(parent, "Неверный пароль!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
        return false;
    }

    // --- НОВЫЙ МЕТОД: Смена пароля ---
    public static void changePassword(Component parent) {
        // Step 1: Проверка текущего пароля
        if (!requestAdminAccess(parent)) {
            return; // Если текущий пароль введен неверно или отменен, выходим
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

            // Успешное изменение
            adminPassword = newPassword;
            JOptionPane.showMessageDialog(parent, "Пароль успешно изменен!", "Успех", JOptionPane.INFORMATION_MESSAGE);
        }
    }
}