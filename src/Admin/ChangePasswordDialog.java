package Admin;

import javax.swing.*;
import java.awt.*;

public class ChangePasswordDialog extends JDialog {

    private JPasswordField oldPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    public ChangePasswordDialog(Frame owner) {
        super(owner, "Смена пароля администратора", true);
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(400, 220);
        setLocationRelativeTo(getOwner());
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        panel.add(new JLabel("Текущий пароль:"));
        oldPasswordField = new JPasswordField();
        panel.add(oldPasswordField);

        panel.add(new JLabel("Новый пароль:"));
        newPasswordField = new JPasswordField();
        panel.add(newPasswordField);

        panel.add(new JLabel("Повторите новый пароль:"));
        confirmPasswordField = new JPasswordField();
        panel.add(confirmPasswordField);

        JButton btnSave = new JButton("Сохранить");
        JButton btnCancel = new JButton("Отмена");

        btnSave.addActionListener(e -> handleChangePassword());
        btnCancel.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void handleChangePassword() {
        String oldPass = new String(oldPasswordField.getPassword());
        String newPass = new String(newPasswordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());

        if (!AdminSecurityManager.verifyPassword(oldPass)) {
            JOptionPane.showMessageDialog(this, "Старый пароль введён неверно!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (newPass.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Новый пароль не может быть пустым!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Новые пароли не совпадают!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        AdminSecurityManager.changePassword(newPass);
        JOptionPane.showMessageDialog(this, "Пароль успешно изменён!", "Успех", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}