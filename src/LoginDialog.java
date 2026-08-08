import javax.swing.*;
import java.awt.*;
import java.util.List;

public class LoginDialog extends JDialog {
    private JComboBox<User.Role> roleComboBox;
    private JPasswordField passField;
    private User authenticatedUser = null;

    public LoginDialog(Frame parent) {
        super(parent, "Авторизация в СМК ИЛ", true);
        setSize(400, 210);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);

        JPanel panel = new JPanel(new GridLayout(3, 2, 10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        panel.add(new JLabel("Выберите роль:"));

        roleComboBox = new JComboBox<>(User.Role.values());
        roleComboBox.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof User.Role) {
                    setText(((User.Role) value).getDisplayName());
                }
                return this;
            }
        });
        panel.add(roleComboBox);

        panel.add(new JLabel("Пароль:"));
        passField = new JPasswordField();
        panel.add(passField);

        JButton loginButton = new JButton("Войти");
        JButton cancelButton = new JButton("Отмена");

        panel.add(loginButton);
        panel.add(cancelButton);

        loginButton.addActionListener(e -> onLogin());
        cancelButton.addActionListener(e -> {
            authenticatedUser = null;
            dispose();
        });

        add(panel);
    }

    private void onLogin() {
        User.Role selectedRole = (User.Role) roleComboBox.getSelectedItem();
        String password = new String(passField.getPassword());

        if (UserManager.checkPassword(selectedRole, password)) {
            List<User> users = UserManager.loadUsers();
            for (User u : users) {
                if (u.getRole() == selectedRole) {
                    authenticatedUser = u;
                    dispose();
                    return;
                }
            }
        }

        JOptionPane.showMessageDialog(this,
                "Неверный пароль для выбранной роли!",
                "Ошибка авторизации",
                JOptionPane.ERROR_MESSAGE);
    }

    public User getAuthenticatedUser() {
        return authenticatedUser;
    }
}