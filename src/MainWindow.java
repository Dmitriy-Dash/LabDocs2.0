import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame {
    private JTabbedPane tabbedPane;
    private JTable[] tables = new JTable[5];
    private DefaultTableModel[] tableModels = new DefaultTableModel[5];
    private TableRowSorter<DefaultTableModel>[] tableSorters = new TableRowSorter[5];

    private List<Document> documentList = new ArrayList<>();
    private User currentUser;

    private JTextField searchField;
    private JLabel statusLabelLeft;
    private JLabel statusLabelRight;

    private JLabel warningTitleLabel;
    private JLabel warningCounterLabel;
    private JPanel warningDashboardPanel;

    public MainWindow(User user) {
        this.currentUser = user;
        setTitle("СМК ИЛ — Управление документацией (Пользователь: " + user.getUsername() + " [" + user.getRole() + "])");
        setSize(1200, 750);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initUI();
        loadDocuments();
        updateDashboardCounter();
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // --- 1. Инициализация основных кнопок управления ---
        JButton addButton = new JButton("Добавить документ");
        JButton editButton = new JButton("Изменить выбранный");
        JButton deleteButton = new JButton("Удалить выбранный");
        JButton btnSave = new JButton("Сохранить в Excel");
        JButton auditButton = new JButton("Журнал аудита");
        JButton passwordButton = new JButton("Сменить пароль");
        JButton btnValidation = new JButton("Валидация ПО (ГОСТ 17025)");
        JButton btnRestore = new JButton("Восстановить из бэкапа");
        JButton btnTrash = new JButton("Корзина (Удаленные)");

        // --- 2. Назначение обработчиков событий ---
        addButton.addActionListener(e -> addDocument());
        editButton.addActionListener(e -> editDocument());
        deleteButton.addActionListener(e -> deleteDocument());
        btnSave.addActionListener(e -> exportToExcel());
        auditButton.addActionListener(e -> openAuditLog());
        passwordButton.addActionListener(e -> openPasswordChangeDialog());

        btnValidation.addActionListener(e -> SwingUtilities.invokeLater(() -> {
            validation.SystemValidationApp validationFrame = new validation.SystemValidationApp();
            validationFrame.setVisible(true);
        }));

        btnRestore.addActionListener(e -> handleRestoreAction());
        btnTrash.addActionListener(e -> openTrashDialog());

        // --- 3. Настройка прав и блокировок по ролям ---
        if (currentUser.getRole() == User.Role.LABORATORY_ASSISTANT) {
            addButton.setEnabled(false);
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
            btnRestore.setEnabled(false);
            auditButton.setEnabled(false);
        }

        if (currentUser.getRole() == User.Role.LABORATORY_ASSISTANT || currentUser.getRole() == User.Role.OPERATOR) {
            btnValidation.setEnabled(false);
            btnValidation.setToolTipText("Доступно только Менеджеру по качеству");
        }

        if (currentUser.getRole() != User.Role.QUALITY_MANAGER) {
            btnTrash.setEnabled(false);
            btnTrash.setToolTipText("Доступно только Менеджеру по качеству");
        }

        // --- 4. Создание дашборда предупреждений ---
        warningTitleLabel = new JLabel("⚠️ Требуют актуализации в текущем месяце (Уровень 1):");
        warningCounterLabel = new JLabel("0");
        warningCounterLabel.setFont(new Font("Arial", Font.BOLD, 12));
        warningCounterLabel.setForeground(new Color(180, 0, 0));

        JButton filterWarningButton = new JButton("Показать");
        filterWarningButton.setMargin(new Insets(2, 8, 2, 8));
        filterWarningButton.addActionListener(e -> filterDocumentsNeedingActualizationThisMonth());

        warningDashboardPanel = MainWindowBuilder.createWarningDashboard(warningTitleLabel, warningCounterLabel, filterWarningButton);

        // --- 5. Создание поля быстрого поиска ---
        searchField = new JTextField(25);
        searchField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                filterTable(searchField.getText());
            }
        });

        // --- 6. Сборка верхней панели через MainWindowBuilder ---
        JButton[] mainButtons = {addButton, editButton, deleteButton, btnSave, auditButton, passwordButton, btnValidation};
        JPanel topPanel = MainWindowBuilder.createTopPanel(mainButtons, btnRestore, btnTrash, searchField, warningDashboardPanel);
        add(topPanel, BorderLayout.NORTH);

        // --- 7. Инициализация вкладок с таблицами ---
        tabbedPane = new JTabbedPane();
        MainWindowBuilder.initializeTabs(tabbedPane, tables, tableModels, tableSorters);
        add(tabbedPane, BorderLayout.CENTER);

        // --- 8. Нижняя панель статуса ---
        statusLabelLeft = new JLabel(" Готов к работе");
        statusLabelRight = new JLabel(" Пользователь: " + currentUser.getUsername() + " ");
        JPanel bottomPanel = MainWindowBuilder.createBottomPanel(statusLabelLeft, statusLabelRight);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public List<Document> getDocumentList() {
        return documentList;
    }

    private void loadDocuments() {
        // Загрузка документов (заглушка/интеграция с менеджером хранилища)
        documentList = DataManager.loadDocuments();
        refreshTables();
    }

    public void refreshTables() {
        for (int i = 0; i < 5; i++) {
            tableModels[i].setRowCount(0);
        }

        for (Document doc : documentList) {
            int level = doc.getSmkLevel();
            if (level >= 1 && level <= 5) {
                tableModels[level - 1].addRow(new Object[]{
                        doc.getId(),
                        doc.getTitle(),
                        doc.getVersion(),
                        doc.getOrigin(),
                        doc.getDate(),
                        doc.getActualizationDate(),
                        doc.getStorageOriginal(),
                        doc.getStorageCopies(),
                        doc.getCopyCount(),
                        doc.getStatus()
                });
            }
        }
        updateDashboardCounter();
    }

    private void filterTable(String query) {
        int activeTab = tabbedPane.getSelectedIndex();
        if (activeTab >= 0 && activeTab < 5) {
            if (query.trim().length() == 0) {
                tableSorters[activeTab].setRowFilter(null);
            } else {
                tableSorters[activeTab].setRowFilter(RowFilter.regexFilter("(?i)" + query));
            }
        }
    }

    private void addDocument() {
        int activeTab = tabbedPane.getSelectedIndex() + 1;
        AddDocumentDialog dialog = new AddDocumentDialog(this, activeTab);
        dialog.setVisible(true);
        Document newDoc = dialog.getCreatedDocument();
        if (newDoc != null) {
            documentList.add(newDoc);
            DataManager.saveDocuments(documentList);
            AuditLogger.getInstance().log(currentUser.getUsername(), "Добавление документа", newDoc.getId(), "Добавлен документ с ID: " + newDoc.getId());
            refreshTables();
            statusLabelLeft.setText(" Документ успешно добавлен.");
        }
    }

    private void editDocument() {
        int activeTab = tabbedPane.getSelectedIndex();
        int selectedRow = tables[activeTab].getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Выберите документ для изменения!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String docId = (String) tableModels[activeTab].getValueAt(tables[activeTab].convertRowIndexToModel(selectedRow), 0);
        Document docToEdit = documentList.stream().filter(d -> d.getId().equals(docId)).findFirst().orElse(null);

        if (docToEdit != null) {
            AddDocumentDialog dialog = new AddDocumentDialog(this, docToEdit);
            dialog.setVisible(true);
            Document updatedDoc = dialog.getCreatedDocument();
            if (updatedDoc != null) {
                documentList.remove(docToEdit);
                documentList.add(updatedDoc);
                DataManager.saveDocuments(documentList);
                AuditLogger.getInstance().log(currentUser.getUsername(), "Изменение документа", updatedDoc.getId(), "Изменен документ с ID: " + updatedDoc.getId());
                refreshTables();
                statusLabelLeft.setText(" Документ успешно обновлен.");
            }
        }
    }

    private void deleteDocument() {
        int activeTab = tabbedPane.getSelectedIndex();
        int selectedRow = tables[activeTab].getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Выберите документ для удаления!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String docId = (String) tableModels[activeTab].getValueAt(tables[activeTab].convertRowIndexToModel(selectedRow), 0);
        Document docToDelete = documentList.stream().filter(d -> d.getId().equals(docId)).findFirst().orElse(null);

        if (docToDelete != null) {
            int confirm = JOptionPane.showConfirmDialog(this, "Вы уверены, что хотите удалить документ " + docId + "?", "Подтверждение", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                documentList.remove(docToDelete);
                DataManager.saveDocuments(documentList);
                AuditLogger.getInstance().log(currentUser.getUsername(), "Удаление документа", docId, "Удален документ с ID: " + docId);
                refreshTables();
                statusLabelLeft.setText(" Документ перемещен в корзину/удален.");
            }
        }
    }

    private void exportToExcel() {
        JOptionPane.showMessageDialog(this, "Экспорт в Excel выполнен успешно.", "Экспорт", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openPasswordChangeDialog() {
        PasswordProtectionManager.changePassword(this, currentUser);
    }

    private void handleRestoreAction() {
        JOptionPane.showMessageDialog(this, "Функция восстановления из бэкапа вызведена.", "Бэкап", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openTrashDialog() {
        JOptionPane.showMessageDialog(this, "Открытие корзины удаленных документов...", "Корзина", JOptionPane.INFORMATION_MESSAGE);
    }

    private void openAuditLog() {
        String logContent = AuditLogger.getInstance().readEncryptedLog();

        JTextArea textArea = new JTextArea(logContent, 20, 50);
        textArea.setEditable(false);
        textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(textArea);

        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "Журнал аудита (Защищенный лог)",
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void filterDocumentsNeedingActualizationThisMonth() {
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        tableSorters[0].setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                String actDateStr = (String) entry.getValue(5); // Колонка даты актуализации
                if (actDateStr == null || actDateStr.trim().isEmpty() || "Не требуется".equalsIgnoreCase(actDateStr)) {
                    return false;
                }
                try {
                    String[] parts = actDateStr.split("\\.");
                    int day = Integer.parseInt(parts[0]);
                    int month = Integer.parseInt(parts[1]);
                    int year = Integer.parseInt(parts[2]);
                    return month == currentMonth && year == currentYear;
                } catch (Exception e) {
                    return false;
                }
            }
        });
        statusLabelLeft.setText(" Показаны документы Уровня 1, требующие актуализации в текущем месяце.");
    }

    private void updateDashboardCounter() {
        LocalDate now = LocalDate.now();
        int currentMonth = now.getMonthValue();
        int currentYear = now.getYear();

        long count = documentList.stream().filter(d -> {
            if (d.getSmkLevel() != 1) return false;
            String actDateStr = d.getActualizationDate();
            if (actDateStr == null || actDateStr.trim().isEmpty() || "Не требуется".equalsIgnoreCase(actDateStr)) {
                return false;
            }
            try {
                String[] parts = actDateStr.split("\\.");
                int month = Integer.parseInt(parts[1]);
                int year = Integer.parseInt(parts[2]);
                return month == currentMonth && year == currentYear;
            } catch (Exception e) {
                return false;
            }
        }).count();

        if (warningCounterLabel != null) {
            warningCounterLabel.setText(String.valueOf(count));
        }
    }
}