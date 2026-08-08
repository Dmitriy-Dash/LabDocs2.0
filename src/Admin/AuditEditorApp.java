package Admin;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Класс AuditEditorApp представляет собой независимое графическое приложение (JFrame)
 * для административного управления и редактирования журналов аудита СМК.
 * Обеспечивает одновременную расшифровку, редактирование, шифрование (.enc)
 * и синхронизацию с табличным форматом Excel (.csv).
 */
public class AuditEditorApp extends JFrame {

    // Графические компоненты интерфейса
    private JTextArea logTextArea;       // Текстовая область для просмотра и редактирования строк логов
    private JLabel statusLabel;          // Строка состояния приложения внизу окна
    private File currentLogFile;         // Ссылка на в данный момент открытый файл логов (.enc)
    private String originalContent = ""; // Буфер для хранения исходного текста (нужен для функции сброса изменений)

    // Стандартное имя файла журнала Excel для синхронизации в той же папке
    private static final String DEFAULT_CSV_PATH = "audit_journal.csv";

    /**
     * Конструктор приложения: задает размеры, название, отключает операцию закрытия по умолчанию
     * и инициализирует графический интерфейс.
     */
    public AuditEditorApp() {
        setTitle("Административная утилита: Редактор журнала аудита СМК (ENC + CSV)");
        setSize(1000, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Закрытие приложения останавливает процесс
        setLocationRelativeTo(null);                    // Центрируем окно на экране

        initUI();
    }

    /**
     * Инициализация и компоновка элементов пользовательского интерфейса (UI).
     */
    private void initUI() {
        // --- Верхняя панель управления (ToolBar) ---
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false); // Делаем панель инструментов неперетаскиваемой

        // Создаем кнопку смены пароля администратора
        JButton btnChangePass = new JButton("Сменить пароль");
        btnChangePass.addActionListener(e -> {
            ChangePasswordDialog dialog = new ChangePasswordDialog(this);
            dialog.setVisible(true); // Открываем диалоговое окно смены пароля
        });

        // Создаем остальные управляющие кнопки
        JButton btnOpenFile = new JButton("Открыть лог (.enc)");
        JButton btnSaveEncrypted = new JButton("Сохранить изменения (ENC + CSV)");
        JButton btnReload = new JButton("Сбросить изменения");

        // Визуальное оформление кнопок
        btnOpenFile.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSaveEncrypted.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSaveEncrypted.setForeground(new Color(0, 102, 204)); // Выделяем кнопку сохранения синим цветом

        // Добавляем элементы управления на тулбар
        toolBar.add(btnOpenFile);
        toolBar.add(btnReload);
        toolBar.addSeparator(); // Разделительная линия
        toolBar.add(btnSaveEncrypted);
        toolBar.add(btnChangePass);

        // --- Центральная текстовая область редактора ---
        logTextArea = new JTextArea();
        logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 13)); // Моноширинный шрифт для ровного выравнивания строк логов
        logTextArea.setTabSize(4);
        JScrollPane scrollPane = new JScrollPane(logTextArea); // Добавляем прокрутку для больших логов

        // --- Нижняя панель статуса ---
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Выберите зашифрованный файл audit_journal.enc для работы.");
        statusPanel.add(statusLabel);

        // --- Привязка обработчиков событий к кнопкам ---
        btnOpenFile.addActionListener(e -> openAndDecryptFile());
        btnSaveEncrypted.addActionListener(e -> saveAndEncryptFile());
        btnReload.addActionListener(e -> resetText());

        // --- Сборка главного окна ---
        add(toolBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * Открытие диалогового выбора файла, чтение зашифрованного содержимого
     * и его последующая расшифровка с помощью LogCryptoService.
     */
    private void openAndDecryptFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите файл зашифрованного лога");
        chooser.setSelectedFile(new File("audit_journal.enc")); // Предлагаем файл по умолчанию
        // Ограничиваем выбор файлов только расширениями .enc и .dat
        chooser.setFileFilter(new FileNameExtensionFilter("Файлы зашифрованных логов (*.enc, *.dat)", "enc", "dat"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentLogFile = chooser.getSelectedFile();
            try {
                // Читаем зашифрованную строку из файла
                String rawEncryptedContent = Files.readString(currentLogFile.toPath(), StandardCharsets.UTF_8);

                // Выполняем расшифровку через сервис криптографии
                originalContent = LogCryptoService.decrypt(rawEncryptedContent);

                // Выводим расшифрованный текст в текстовое поле и возвращаем каретку в начало
                logTextArea.setText(originalContent);
                logTextArea.setCaretPosition(0);

                statusLabel.setText("Файл расшифрован: " + currentLogFile.getAbsolutePath());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Ошибка при расшифровке файла:\n" + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Запрос подтверждения, шифрование измененного текста обратно в .enc
     * и синхронизация данных с CSV-файлом для Excel.
     */
    private void saveAndEncryptFile() {
        if (currentLogFile == null) {
            JOptionPane.showMessageDialog(this, "Сначала откройте файл лога!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Запрашиваем подтверждение у администратора
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Вы уверены, что хотите сохранить изменения?\nБудут одновременно обновлены файлы audit_journal.enc и audit_journal.csv!",
                "Подтверждение сохранения",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String editedText = logTextArea.getText();

                // 1. Зашифровываем отредактированный текст и сохраняем в основной .enc файл
                String encryptedText = LogCryptoService.encrypt(editedText);
                Files.writeString(currentLogFile.toPath(), encryptedText, StandardCharsets.UTF_8);

                // 2. Парсим текст и обновляем параллельный Excel CSV файл в той же директории
                File csvFile = new File(currentLogFile.getParent(), DEFAULT_CSV_PATH);
                syncWithCsvLog(csvFile, editedText);

                originalContent = editedText; // Обновляем оригинал после успешного сохранения

                JOptionPane.showMessageDialog(this,
                        "Логи успешно зашифрованы и синхронизированы с Excel (CSV)!",
                        "Успешно сохранено", JOptionPane.INFORMATION_MESSAGE);
                statusLabel.setText("Изменения внесены в .enc и .csv (" + java.time.LocalTime.now() + ")");

            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Ошибка при сохранении файлов:\n" + ex.getMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Конвертирует отредактированные текстовые строки лога обратно в табличный формат CSV
     * с добавлением UTF-8 BOM для корректного отображения кириллицы в Microsoft Excel.
     */
    private void syncWithCsvLog(File csvFile, String plainTextLogs) {
        try (FileOutputStream fos = new FileOutputStream(csvFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            // Записываем UTF-8 BOM (Byte Order Mark), чтобы Excel понимал кодировку без "кракозябр"
            fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            // Записываем шапку таблицы CSV
            writer.write("Дата и время;Пользователь/Роль;Действие;ID Документа;Детали/Примечание\n");

            // Разбиваем текст на отдельные строки
            String[] lines = plainTextLogs.split("\r?\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue; // Пропускаем пустые строки

                // Превращаем текстовую строку лога в строку с разделителями-точкой с запятой (;)
                String csvLine = parseLogLineToCsv(line);
                if (csvLine != null) {
                    writer.write(csvLine + "\n");
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обновлении CSV-файла: " + e.getMessage());
        }
    }

    /**
     * Вспомогательный метод парсинга строки лога на составные части
     * для формирования колонок табличного CSV-файла.
     */
    private String parseLogLineToCsv(String logLine) {
        try {
            String timestamp = "";
            String user = "";
            String action = "";
            String doc = "";
            String details = "";

            // Извлекаем временную метку из квадратных скобок [...] в начале строки
            if (logLine.startsWith("[")) {
                int endBracket = logLine.indexOf("]");
                if (endBracket != -1) {
                    timestamp = logLine.substring(1, endBracket);
                    logLine = logLine.substring(endBracket + 1);
                }
            }

            // Разбиваем оставшуюся часть строки по вертикальной черте (|)
            String[] parts = logLine.split("\\|");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("USER:")) user = part.substring(5).trim();
                else if (part.startsWith("ACTION:")) action = part.substring(7).trim();
                else if (part.startsWith("DOC:")) doc = part.substring(4).trim();
                else if (part.startsWith("DETAILS:")) details = part.substring(8).trim();
            }

            // Собираем обратно в CSV-строку, заменяя точки с запятой в деталях на запятые во избежание сдвига колонок
            return String.format("%s;%s;%s;%s;%s", timestamp, user, action, doc, details.replace(";", ","));
        } catch (Exception e) {
            // Если структура нарушена (админ написал произвольный текст), записываем всю строку в колонку деталей
            return String.format(";ADMIN_EDIT;;;%s", logLine.replace(";", ","));
        }
    }

    /**
     * Сбрасывает текущие изменения в редакторе к моменту последней загрузки/сохранения файла.
     */
    private void resetText() {
        if (originalContent != null) {
            logTextArea.setText(originalContent);
            statusLabel.setText("Изменения сброшены.");
        }
    }

    /**
     * Главный метод программы (точка входа).
     * Сначала вызывает модальное окно аутентификации администратора (`AdminSecurityManager.authenticate()`).
     * Если пароль введен неверно или окно закрыто — приложение завершает работу.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            // Проверка прав администратора перед запуском редактора
            if (!AdminSecurityManager.authenticate()) {
                System.exit(0); // Завершаем процесс, если авторизация провалена
                return;
            }

            // Устанавливаем системный стиль оформления интерфейса (Look and Feel)
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            // Создаем и делаем видимым главное окно утилиты
            new AuditEditorApp().setVisible(true);
        });
    }
}