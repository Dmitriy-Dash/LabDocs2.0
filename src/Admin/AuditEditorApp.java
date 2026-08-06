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
 * Изолированная утилита для администрирования логов аудита.
 * Синхронно обновляет как зашифрованный файл (.enc), так и открытый Excel-файл (.csv).
 */
public class AuditEditorApp extends JFrame {

    private JTextArea logTextArea;
    private JLabel statusLabel;
    private File currentLogFile;
    private String originalContent = "";

    // Стандартные имена файлов журнала (если они находятся в папке проекта)
    private static final String DEFAULT_CSV_PATH = "audit_journal.csv";

    public AuditEditorApp() {
        setTitle("Административная утилита: Редактор журнала аудита СМК (ENC + CSV)");
        setSize(1000, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
    }

    private void initUI() {
        // --- Верхняя панель управления ---
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        // Добавляем новую кнопку на тулбар:
        JButton btnChangePass = new JButton("Сменить пароль");
        // Добавляем слушатель события:
        btnChangePass.addActionListener(e -> {
            ChangePasswordDialog dialog = new ChangePasswordDialog(this);
            dialog.setVisible(true);
        });

        JButton btnOpenFile = new JButton("Открыть лог (.enc)");
        JButton btnSaveEncrypted = new JButton("Сохранить изменения (ENC + CSV)");
        JButton btnReload = new JButton("Сбросить изменения");

        btnOpenFile.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSaveEncrypted.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnSaveEncrypted.setForeground(new Color(0, 102, 204));

        toolBar.add(btnOpenFile);
        toolBar.add(btnReload);
        toolBar.addSeparator();
        toolBar.add(btnSaveEncrypted);
        toolBar.add(btnChangePass); // Новая кнопка

        // --- Текстовая область редактора ---
        logTextArea = new JTextArea();
        logTextArea.setFont(new Font("Monospaced", Font.PLAIN, 13));
        logTextArea.setTabSize(4);
        JScrollPane scrollPane = new JScrollPane(logTextArea);

        // --- Ниже панель статуса ---
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        statusLabel = new JLabel("Выберите зашифрованный файл audit_journal.enc для работы.");
        statusPanel.add(statusLabel);

        // Обработчики событий
        btnOpenFile.addActionListener(e -> openAndDecryptFile());
        btnSaveEncrypted.addActionListener(e -> saveAndEncryptFile());
        btnReload.addActionListener(e -> resetText());

        add(toolBar, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusPanel, BorderLayout.SOUTH);
    }

    private void openAndDecryptFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Выберите файл зашифрованного лога");
        chooser.setSelectedFile(new File("audit_journal.enc"));
        chooser.setFileFilter(new FileNameExtensionFilter("Файлы зашифрованных логов (*.enc, *.dat)", "enc", "dat"));

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentLogFile = chooser.getSelectedFile();
            try {
                String rawEncryptedContent = Files.readString(currentLogFile.toPath(), StandardCharsets.UTF_8);
                // Расшифровка через LogCryptoService
                originalContent = LogCryptoService.decrypt(rawEncryptedContent);
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

    private void saveAndEncryptFile() {
        if (currentLogFile == null) {
            JOptionPane.showMessageDialog(this, "Сначала откройте файл лога!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Вы уверены, что хотите сохранить изменения?\nБудут одновременно обновлены файлы audit_journal.enc и audit_journal.csv!",
                "Подтверждение сохранения",
                JOptionPane.YES_NO_OPTION
        );

        if (confirm == JOptionPane.YES_OPTION) {
            try {
                String editedText = logTextArea.getText();

                // 1. Сохраняем и зашифровываем основной .enc файл
                String encryptedText = LogCryptoService.encrypt(editedText);
                Files.writeString(currentLogFile.toPath(), encryptedText, StandardCharsets.UTF_8);

                // 2. Парсим отредактированный текст и обновляем Excel CSV файл
                File csvFile = new File(currentLogFile.getParent(), DEFAULT_CSV_PATH);
                syncWithCsvLog(csvFile, editedText);

                originalContent = editedText;

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
     * Конвертирует отредактированные строки текстового лога в формат CSV для Excel с UTF-8 BOM
     */
    private void syncWithCsvLog(File csvFile, String plainTextLogs) {
        try (FileOutputStream fos = new FileOutputStream(csvFile);
             OutputStreamWriter writer = new OutputStreamWriter(fos, StandardCharsets.UTF_8)) {

            // Записываем UTF-8 BOM для корректного открытия кириллицы в Excel
            fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            // Заголовок CSV
            writer.write("Дата и время;Пользователь/Роль;Действие;ID Документа;Детали/Примечание\n");

            String[] lines = plainTextLogs.split("\r?\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;

                // Разбор строки вида: [dd.MM.yyyy HH:mm:ss] | USER: Admin | ACTION: ИЗМЕНЕНИЕ | DOC: УР1-01 | DETAILS: ...
                String csvLine = parseLogLineToCsv(line);
                if (csvLine != null) {
                    writer.write(csvLine + "\n");
                }
            }
        } catch (Exception e) {
            System.err.println("Ошибка при обновлении CSV-файла: " + e.getMessage());
        }
    }

    private String parseLogLineToCsv(String logLine) {
        try {
            String timestamp = "";
            String user = "";
            String action = "";
            String doc = "";
            String details = "";

            // Извлекаем Timestamp [...]
            if (logLine.startsWith("[")) {
                int endBracket = logLine.indexOf("]");
                if (endBracket != -1) {
                    timestamp = logLine.substring(1, endBracket);
                    logLine = logLine.substring(endBracket + 1);
                }
            }

            // Разбиваем оставшиеся части по разделителю "|"
            String[] parts = logLine.split("\\|");
            for (String part : parts) {
                part = part.trim();
                if (part.startsWith("USER:")) user = part.substring(5).trim();
                else if (part.startsWith("ACTION:")) action = part.substring(7).trim();
                else if (part.startsWith("DOC:")) doc = part.substring(4).trim();
                else if (part.startsWith("DETAILS:")) details = part.substring(8).trim();
            }

            return String.format("%s;%s;%s;%s;%s", timestamp, user, action, doc, details.replace(";", ","));
        } catch (Exception e) {
            // Если администратор ввел произвольную строку не по формату, помещаем её в детали
            return String.format(";ADMIN_EDIT;;;%s", logLine.replace(";", ","));
        }
    }

    private void resetText() {
        if (originalContent != null) {
            logTextArea.setText(originalContent);
            statusLabel.setText("Изменения сброшены.");
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            if (!AdminSecurityManager.authenticate()) {
                System.exit(0);
                return;
            }

            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}

            new AuditEditorApp().setVisible(true);
        });
    }
}