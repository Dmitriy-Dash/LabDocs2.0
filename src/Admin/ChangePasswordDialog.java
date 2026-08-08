package Admin;

import javax.swing.*;
import java.awt.*;

/**
 * Класс ChangePasswordDialog представляет собой модальное диалоговое окно
 * для смены пароля администратора утилиты управления аудитом.
 * Наследуется от JDialog и работает в модальном режиме (блокирует родительское окно).
 */
public class ChangePasswordDialog extends JDialog {

    // Поля ввода для старого пароля, нового пароля и подтверждения нового пароля
    private JPasswordField oldPasswordField;
    private JPasswordField newPasswordField;
    private JPasswordField confirmPasswordField;

    /**
     * Конструктор диалогового окна смены пароля.
     * Принимает родительское окно (`owner`), задает заголовок и устанавливает модальность (`true`).
     */
    public ChangePasswordDialog(Frame owner) {
        super(owner, "Смена пароля администратора", true);
        initUI(); // Инициализация графических элементов
    }

    /**
     * Создание и компоновка элементов интерфейса диалогового окна.
     */
    private void initUI() {
        setLayout(new BorderLayout(10, 10));
        setSize(400, 220);
        setLocationRelativeTo(getOwner()); // Центрируем окно относительно родительского (AuditEditorApp)
        setResizable(false);             // Запрещаем изменение размера окна

        // Основная панель с сеткой 3x2 для полей ввода паролей
        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); // Внутренние отступы

        panel.add(new JLabel("Текущий пароль:"));
        oldPasswordField = new JPasswordField();
        panel.add(oldPasswordField);

        panel.add(new JLabel("Новый пароль:"));
        newPasswordField = new JPasswordField();
        panel.add(newPasswordField);

        panel.add(new JLabel("Повторите новый пароль:"));
        confirmPasswordField = new JPasswordField();
        panel.add(confirmPasswordField);

        // Кнопки управления диалогом
        JButton btnSave = new JButton("Сохранить");
        JButton btnCancel = new JButton("Отмена");

        // Привязываем обработчики нажатий к кнопкам
        btnSave.addActionListener(e -> handleChangePassword());
        btnCancel.addActionListener(e -> dispose()); // Закрываем диалог без сохранения

        // Панель для кнопок выравнивается по правому краю
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(btnSave);
        buttonPanel.add(btnCancel);

        // Добавляем панели на диалоговое окно
        add(panel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Логика проверки введенных данных и смены пароля администратора.
     * Проверяет правильность старого пароля, непустоту нового пароля и совпадение паролей.
     */
    private void handleChangePassword() {
        // Извлекаем текстовые значения из защищенных полей ввода паролей
        String oldPass = new String(oldPasswordField.getPassword());
        String newPass = new String(newPasswordField.getPassword());
        String confirmPass = new String(confirmPasswordField.getPassword());

        // 1. Проверяем, правильно ли введен текущий (старый) пароль через AdminSecurityManager
        if (!AdminSecurityManager.verifyPassword(oldPass)) {
            JOptionPane.showMessageDialog(this, "Старый пароль введён неверно!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return; // Прерываем выполнение в случае ошибки
        }

        // 2. Проверяем, что новый пароль не пустой (или не состоит только из пробелов)
        if (newPass.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Новый пароль не может быть пустым!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 3. Проверяем совпадение нового пароля и подтверждения
        if (!newPass.equals(confirmPass)) {
            JOptionPane.showMessageDialog(this, "Новые пароли не совпадают!", "Ошибка", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Если все проверки пройдены успешно, сохраняем новый пароль в AdminSecurityManager
        AdminSecurityManager.changePassword(newPass);

        // Уведомляем пользователя об успехе и закрываем окно диалога
        JOptionPane.showMessageDialog(this, "Пароль успешно изменён!", "Успех", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }
}