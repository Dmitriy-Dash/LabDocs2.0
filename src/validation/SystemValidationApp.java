package validation;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;

/**
 * Класс SystemValidationApp представляет собой графическое приложение (JFrame)
 * для проведения комплексной валидации программного обеспечения в соответствии с требованиями
 * стандарта ГОСТ ISO/IEC 17025-2019 (пункт 7.11 «Управление данными и информацией лаборатории»).
 */
public class SystemValidationApp extends JFrame {

    // Элементы управления пользовательского интерфейса
    private JTextField txtValidatorName;      // Поле ввода ФИО инженера, проводящего валидацию
    private JPasswordField txtCorrectPassword;// Поле ввода эталонного пароля администратора для тестов безопасности
    private JTextField txtWrongPassword;      // Поле ввода заведомо неверного пароля для проверки защиты от НСД
    private JTextArea logArea;                // Текстовая область для вывода хода выполнения тестов и протокола
    private JButton btnStart;                 // Кнопка запуска процесса валидации

    /**
     * Конструктор приложения: настраивает заголовок, размеры, поведение при закрытии
     * и инициализирует графические компоненты.
     */
    public SystemValidationApp() {
        setTitle("Модуль Валидации ПО (ГОСТ ISO/IEC 17025-2019 п. 7.11)");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE); // Закрытие только текущего окна утилиты валидации
        setSize(850, 640);
        setLocationRelativeTo(null); // Центрирование на экране

        // Главная панель с отступами
        JPanel contentPane = new JPanel(new BorderLayout(15, 15));
        contentPane.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(contentPane);

        // --- Панель ввода параметров испытаний ---
        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createTitledBorder(" Параметры испытаний "));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 8, 6, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Строка 1: ФИО Валидатора
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        inputPanel.add(new JLabel("ФИО Валидатора / Инженера:"), gbc);

        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.7;
        txtValidatorName = new JTextField("Иванов И.И.");
        inputPanel.add(txtValidatorName, gbc);

        // Строка 2: Эталонный пароль администратора
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        inputPanel.add(new JLabel("Эталонный пароль администратора:"), gbc);

        gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = 0.7;
        txtCorrectPassword = new JPasswordField("");
        inputPanel.add(txtCorrectPassword, gbc);

        // Строка 3: Тестовый неверный пароль
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        inputPanel.add(new JLabel("Тестовый неверный пароль:"), gbc);

        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.7;
        txtWrongPassword = new JTextField("WRONG_PASS_999");
        inputPanel.add(txtWrongPassword, gbc);

        // Строка 4: Кнопка запуска тестов
        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2;
        btnStart = new JButton("▶ Запустить комплексную валидацию ПО (ГОСТ 17025)");
        btnStart.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnStart.setBackground(new Color(220, 235, 252)); // Приятный синеватый оттенок кнопки
        btnStart.setFocusPainted(false);
        inputPanel.add(btnStart, gbc);

        contentPane.add(inputPanel, BorderLayout.NORTH);

        // --- Центральная область протокола испытаний (лог) ---
        logArea = new JTextArea();
        logArea.setEditable(false); // Запрещаем ручной ввод, только вывод результатов
        logArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        logArea.setMargin(new Insets(8, 8, 8, 8));

        JScrollPane scrollPane = new JScrollPane(logArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder(" Протокол испытаний "));
        contentPane.add(scrollPane, BorderLayout.CENTER);

        // Привязка действия к кнопке запуска
        btnStart.addActionListener(e -> runValidation());
    }

    /**
     * Запуск процесса валидации в отдельном потоке (Swing Worker / Thread),
     * чтобы не блокировать графический интерфейс во время выполнения проверок.
     */
    private void runValidation() {
        String validator = txtValidatorName.getText().trim();
        String correctPass = new String(txtCorrectPassword.getPassword()).trim();
        String wrongPass = txtWrongPassword.getText().trim();

        // Первичная валидация полей ввода формы
        if (validator.isEmpty() || correctPass.isEmpty() || wrongPass.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Пожалуйста, введите эталонный пароль администратора и заполните все поля!",
                    "Пароль не указан",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        btnStart.setEnabled(false); // Деактивируем кнопку на время тестов
        logArea.setText("");
        appendLog("ИНИЦИАЛИЗАЦИЯ ВАЛИДАЦИИ ПО (ГОСТ ISO/IEC 17025-2019)...");
        appendLog("--------------------------------------------------------------------------------");

        // Создаем отдельный поток для выполнения тестового набора
        new Thread(() -> {
            try {
                // Динамический верификатор пароля, обращающийся к PasswordProtectionManager через Reflection
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

                // Запуск всего набора тестов по стандарту ГОСТ 17025 из класса TestSuite17025
                List<TestSuite17025.TestResult> results = TestSuite17025.runAllTests(
                        correctPass,
                        wrongPass,
                        realPasswordVerifier,
                        this.getOwner(),
                        validator
                );

                // Вывод результатов каждого теста в лог-область интерфейса
                for (TestSuite17025.TestResult res : results) {
                    String status = res.passed() ? "[ УСПЕШНО ]" : "[ ОШИБКА  ]";
                    appendLog(String.format("%-10s | %-12s | %s", status, res.gostClause(), res.testName()));
                    appendLog("           └─ " + res.details());
                }

                // 1. Генерация HTML-отчета со всеми результатами валидации
                String htmlReport = ValidationReportGenerator.generateHtmlReport(validator, results, this.getOwner());

                // 2. Сохранение отчета на диск в файл с временной меткой в имени
                String reportFileName = String.format("ACT_VALIDATION_GOST_17025_%s.html",
                        new java.text.SimpleDateFormat("yyyyMMdd_HHmmss").format(new java.util.Date()));
                File reportFile = ValidationReportGenerator.saveReportToFile(htmlReport, reportFileName);

                appendLog("--------------------------------------------------------------------------------");
                appendLog("ВАЛИДАЦИЯ ЗАВЕРШЕНА.");
                appendLog("Запись сохранена в зашифрованный журнал аудита и Excel-реестр.");
                appendLog("Файл отчета: " + reportFile.getAbsolutePath());

                // Уведомление пользователя об успешном завершении в EDT (Event Dispatch Thread)
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Валидация завершена!\nАкт создан: " + reportFile.getName(),
                        "Готово", JOptionPane.INFORMATION_MESSAGE));
            } catch (Exception ex) {
                appendLog("\n[КРИТИЧЕСКАЯ ОШИБКА]: " + ex.getMessage());
            } finally {
                // Возвращаем кнопку в активное состояние в любом случае
                SwingUtilities.invokeLater(() -> btnStart.setEnabled(true));
            }
        }).start();
    }

    /**
     * Потокобезопасное добавление строк в текстовый лог с автоматической прокруткой вниз.
     */
    private void appendLog(String message) {
        SwingUtilities.invokeLater(() -> {
            logArea.append(message + "\n");
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    /**
     * Главный метод программы (точка входа утилиты валидации).
     */
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new SystemValidationApp().setVisible(true));
    }
}