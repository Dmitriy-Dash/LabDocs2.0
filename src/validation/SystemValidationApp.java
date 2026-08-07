package validation;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

public class SystemValidationApp extends JFrame {

    private JTextField txtValidatorName;
    private JPasswordField txtCorrectPassword;
    private JTextField txtWrongPassword;
    private JTextArea logArea;
    private JButton btnStart;

    public SystemValidationApp() {
        setTitle("Модуль Валидации ПО (ГОСТ ISO/IEC 17025-2019 п. 7.11)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(850, 640);
        setLocationRelativeTo(null);

        JPanel contentPane = new JPanel(new BorderLayout(15, 15));
        contentPane.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(contentPane);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder(" Параметры испытаний "));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // ФИО
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        inputPanel.add(new JLabel("ФИО Валидатора / Инженера:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.7;
        txtValidatorName = new JTextField("Иванов И.И.");
        inputPanel.add(txtValidatorName, gbc);

        // Эталонный пароль (Изначально ПУСТОЕ ПОЛЕ)
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        inputPanel.add(new JLabel("Эталонный пароль администратора:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.7;
        txtCorrectPassword = new JPasswordField("");
        inputPanel.add(txtCorrectPassword, gbc);

        // Тестовый неверный пароль
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        inputPanel.add(new JLabel("Тестовый неверный пароль:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.7;
        txtWrongPassword = new JTextField("WRONG_PASS_999");
        inputPanel.add(txtWrongPassword, gbc);

        // Кнопка
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        btnStart = new JButton("▶ Запустить комплексную валидацию ПО (ГОСТ 17025)");
        btnStart.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnStart.setBackground(new Color(220, 235, 252));
        btnStart.setFocusPainted(false);
        inputPanel.add(btnStart, gbc);

        contentPane.add(inputPanel, BorderLayout.NORTH);

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(" Протокол испытаний "));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        btnStart.addActionListener(e -> runValidation());
    }

    private void runValidation() {
        String validator = txtValidatorName.getText().trim();
        String correctPass = new String(txtCorrectPassword.getPassword()).trim();
        String wrongPass = txtWrongPassword.getText().trim();

        if (validator.isEmpty() || correctPass.isEmpty() || wrongPass.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Пожалуйста, введите эталонный пароль администратора и заполните все поля!",
                    "Пароль не указан",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        btnStart.setEnabled(false);
        logArea.setText("");
        appendLog("ИНИЦИАЛИЗАЦИЯ ВАЛИДАЦИИ ПО (ГОСТ ISO/IEC 17025-2019)...");
        appendLog("--------------------------------------------------------------------------------");

        new Thread(() -> {
            try {
                // Верификатор, проверяющий пароль из PasswordProtectionManager
                java.util.function.Function<String, Boolean> realPasswordVerifier = pass -> {
                    try {
                        Class<?> ppmClass = Class.forName("PasswordProtectionManager");
                        java.lang.reflect.Method method = ppmClass.getMethod("checkPassword", String.class);
                        return (Boolean) method.invoke(null, pass);
                    } catch (Exception e) {
                        System.err.println("Ошибка вызова PasswordProtectionManager: " + e.getMessage());
                        return false;
                    }
                };

                // Внутри метода runValidation() в SystemValidationApp.java:

// Выполнение тестов с передачей 'validator' (ФИО из текстового поля)
                List<TestSuite17025.TestResult> results = TestSuite17025.runAllTests(
                        correctPass,
                        wrongPass,
                        realPasswordVerifier,
                        this.getOwner(),
                        validator // <--- Передаем ФИО из поля txtValidatorName
                );

                for (TestSuite17025.TestResult res : results) {
                    String status = res.passed() ? "[ УСПЕШНО ]" : "[ ОШИБКА  ]";
                    appendLog(String.format("%-10s | %-12s | %s", status, res.gostClause(), res.testName()));
                    appendLog("           └─ " + res.details());
                }

                // 1. Генерируем HTML-отчет со всей системной информацией для аудиторов
                String htmlReport = ValidationReportGenerator.generateHtmlReport(validator, results, this.getOwner());

// 2. Сохраняем отчет в HTML-файл с временной меткой
                String reportFileName = String.format("ACT_VALIDATION_GOST_17025_%s.html",
                        new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()));
                File reportFile = ValidationReportGenerator.saveReportToFile(htmlReport, reportFileName);

                appendLog("--------------------------------------------------------------------------------");
                appendLog("ВАЛИДАЦИЯ ЗАВЕРШЕНА.");
                appendLog("Запись сохранена в зашифрованный журнал аудита и Excel-реестр.");
                appendLog("Файл отчета: " + reportFile.getAbsolutePath());

                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Валидация завершена!\nАкт создан: " + reportFile.getName(),
                        "Готово", JOptionPane.INFORMATION_MESSAGE));
            } catch (Exception ex) {
                appendLog("\n[КРИТИЧЕСКАЯ ОШИБКА]: " + ex.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> btnStart.setEnabled(true));
            }
        }).start();
    }

    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new SystemValidationApp().setVisible(true));
    }
}