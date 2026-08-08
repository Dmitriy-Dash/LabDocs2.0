import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MainWindow extends JFrame {
    private List<Document> documentList = new ArrayList<>();
    private JTable[] tables = new JTable[5];
    private DefaultTableModel[] tableModels = new DefaultTableModel[5];

    @SuppressWarnings("unchecked")
    private TableRowSorter<DefaultTableModel>[] tableSorters = new TableRowSorter[5];

    private JTabbedPane tabbedPane;
    private JLabel statusLabelLeft;
    private JLabel statusLabelRight;
    private JTextField searchField;
    private DocumentFilterManager filterManager;
    private JLabel warningCounterLabel;
    private JPanel warningDashboardPanel;
    private JLabel warningTitleLabel;
    private User currentUser;

    public MainWindow(User currentUser) {
        this.currentUser = currentUser;
        setTitle("Учет документов СМК Испытательной Лаборатории | Пользователь: " +
                currentUser.getFullName() + " (" + currentUser.getRole().getDisplayName() + ")");
        setSize(1250, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Кнопки управления
        JButton addButton = new JButton("Добавить документ");
        JButton editButton = new JButton("Изменить выбранный");
        JButton deleteButton = new JButton("Удалить выбранный");
        JButton btnSave = new JButton("Сохранить в Excel");
        JButton auditButton = new JButton("Журнал аудита");
        JButton passwordButton = new JButton("Сменить пароль");
        JButton btnValidation = new JButton("Валидация ПО (ГОСТ 17025)");

        // Блокируем валидацию для Лаборанта и Оператора (оставляем только для Менеджера)
        if (currentUser.getRole() == User.Role.LABORATORY_ASSISTANT || currentUser.getRole() == User.Role.OPERATOR) {
            btnValidation.setEnabled(false);
            btnValidation.setToolTipText("Доступно только Менеджеру по качеству");
        }

        btnValidation.addActionListener(e -> SwingUtilities.invokeLater(() -> {
            validation.SystemValidationApp validationFrame = new validation.SystemValidationApp();
            validationFrame.setVisible(true);
        }));
        JButton btnRestore = new JButton("Восстановить из бэкапа");

        btnValidation.addActionListener(e -> SwingUtilities.invokeLater(() -> {
            validation.SystemValidationApp validationFrame = new validation.SystemValidationApp();
            validationFrame.setVisible(true);
        }));

        // Блокировка по ролям
        if (currentUser.getRole() == User.Role.LABORATORY_ASSISTANT) {
            addButton.setEnabled(false);
            editButton.setEnabled(false);
            deleteButton.setEnabled(false);
            btnRestore.setEnabled(false);
            auditButton.setEnabled(false);
        }

        // Дашборд предупреждений
        warningTitleLabel = new JLabel("⚠️ Требуют актуализации в текущем месяце (Уровень 1):");
        warningCounterLabel = new JLabel("0");
        warningCounterLabel.setFont(new Font("Arial", Font.BOLD, 12));
        warningCounterLabel.setForeground(new Color(180, 0, 0));

        JButton filterWarningButton = new JButton("Показать");
        filterWarningButton.setMargin(new Insets(2, 8, 2, 8));
        filterWarningButton.addActionListener(e -> filterDocumentsNeedingActualizationThisMonth());

        warningDashboardPanel = MainWindowBuilder.createWarningDashboard(warningTitleLabel, warningCounterLabel, filterWarningButton);

        // Верхняя панель
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        buttonsPanel.add(addButton);
        buttonsPanel.add(editButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(btnSave);
        buttonsPanel.add(auditButton);
        buttonsPanel.add(passwordButton);
        buttonsPanel.add(btnValidation);
        buttonsPanel.add(btnRestore);

        searchField = new JTextField(25);
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        searchPanel.add(new JLabel("Быстрый поиск:"));
        searchPanel.add(searchField);

        topPanel.add(buttonsPanel);
        topPanel.add(searchPanel);
        topPanel.add(warningDashboardPanel);

        // Вкладки и таблицы
        tabbedPane = new JTabbedPane();
        MainWindowBuilder.initializeTabs(tabbedPane, tables, tableModels, tableSorters);

        for (int i = 0; i < 5; i++) {
            applyRowColoring(tables[i]);
            final int tabIdx = i;
            tables[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        openEditDialog();
                    }
                }
            });
        }

        filterManager = new DocumentFilterManager(searchField, tableSorters, tabbedPane);

        tabbedPane.addChangeListener(e -> {
            filterManager.applyFilter();
            int selectedTab = tabbedPane.getSelectedIndex();
            updateStatus(selectedTab + 1);
            updateWarningDashboardCount();
        });

        // Нижняя панель статуса
        statusLabelLeft = new JLabel("НА УРОВНЕ 1: Всего: 0 | Действует: 0 | В работе: 0 | В архиве: 0 | Отменен: 0");
        statusLabelRight = new JLabel("| ИТОГО В БАЗЕ СМК: 0 документов ");
        JPanel bottomPanel = MainWindowBuilder.createBottomPanel(statusLabelLeft, statusLabelRight);

        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // Слушатели кнопок
        addButton.addActionListener(e -> onAddDocumentButtonClicked());
        editButton.addActionListener(e -> openEditDialog());
        deleteButton.addActionListener(e -> deleteSelectedDocument());
        btnSave.addActionListener(e -> exportCurrentTabToExcel());

        auditButton.addActionListener(e -> handleAuditAction());
        btnRestore.addActionListener(e -> handleRestoreAction());
        passwordButton.addActionListener(e -> PasswordProtectionManager.changePassword(MainWindow.this, currentUser));

        loadDataFromFile();
        updateStatus(1);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                DataManager.saveDocuments(documentList);
            }
        });

        SwingUtilities.invokeLater(() -> {
            for (int i = 0; i < 5; i++) {
                DocumentTableManager.updateRowHeights(tables[i]);
            }
        });
    }

    private void handleAuditAction() {
        if (currentUser.getRole() == User.Role.LABORATORY_ASSISTANT) {
            JOptionPane.showMessageDialog(this, "У вас роль 'Лаборант'. Доступно только чтение.", "Доступ ограничен", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentUser.getRole() == User.Role.OPERATOR) {
            if (!PasswordProtectionManager.requestAdminAccess(this)) {
                JOptionPane.showMessageDialog(this, "Неверный пароль.", "Доступ запрещен", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }
        String logData = AuditLogger.getInstance().readEncryptedLog();
        JTextArea textArea = new JTextArea(logData, 20, 50);
        textArea.setEditable(false);
        JOptionPane.showMessageDialog(this, new JScrollPane(textArea), "Зашифрованный журнал аудита", JOptionPane.INFORMATION_MESSAGE);
    }

    private void handleRestoreAction() {
        if (currentUser.getRole() == User.Role.LABORATORY_ASSISTANT) {
            JOptionPane.showMessageDialog(this, "У вас роль 'Лаборант'.", "Доступ ограничен", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentUser.getRole() == User.Role.OPERATOR) {
            if (!PasswordProtectionManager.requestAdminAccess(this)) {
                JOptionPane.showMessageDialog(this, "Неверный пароль.", "Доступ запрещен", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        JFileChooser fileChooser = new JFileChooser(new File("backups"));
        fileChooser.setDialogTitle("Выберите файл резервной копии");
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                Files.copy(fileChooser.getSelectedFile().toPath(), new File("smk_documents.dat").toPath(), StandardCopyOption.REPLACE_EXISTING);
                documentList.clear();
                for (DefaultTableModel model : tableModels) model.setRowCount(0);
                loadDataFromFile();
                updateStatus(tabbedPane.getSelectedIndex() + 1);
                JOptionPane.showMessageDialog(this, "База успешно восстановлена!", "Восстановление", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Ошибка восстановления: " + ex.getMessage(), "Ошибка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadDataFromFile() {
        List<Document> savedDocs = DataManager.loadDocuments();
        if (savedDocs.isEmpty()) {
            addTestValues();
        } else {
            for (Document doc : savedDocs) {
                addDocumentToUI(doc);
            }
        }
        updateWarningDashboardCount();
    }

    private void updateWarningDashboardCount() {
        int currentTabIndex = tabbedPane.getSelectedIndex();
        int currentLevel = currentTabIndex + 1;

        if (warningTitleLabel != null) {
            warningTitleLabel.setText("⚠️ Требуют актуализации в текущем месяце (Уровень " + currentLevel + "):");
        }

        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        int count = 0;

        for (Document doc : documentList) {
            if (doc.getSmkLevel() != currentLevel) continue;
            String actDateStr = doc.getActualizationDate();
            if (actDateStr == null || "Не требуется".equalsIgnoreCase(actDateStr.trim())) continue;

            try {
                LocalDate actDate = LocalDate.parse(actDateStr.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
                LocalDate nextActDate = actDate.plusDays(365);
                if ((nextActDate.getYear() == currentYear && nextActDate.getMonthValue() == currentMonth) || nextActDate.isBefore(LocalDate.now())) {
                    count++;
                }
            } catch (Exception ignored) {}
        }

        if (warningCounterLabel != null) warningCounterLabel.setText(String.valueOf(count));
    }

    private void exportCurrentTabToExcel() {
        JTable currentTable = getCurrentTable();
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (currentTable != null && selectedIndex != -1) {
            ExcelExporter.exportTableToExcel(currentTable, tabbedPane.getTitleAt(selectedIndex), this);
        }
    }

    private void onAddDocumentButtonClicked() {
        if (currentUser.getRole() == User.Role.LABORATORY_ASSISTANT) {
            JOptionPane.showMessageDialog(this, "У вас роль 'Лаборант'.", "Доступ ограничен", JOptionPane.WARNING_MESSAGE);
            return;
        }

        AddDocumentDialog dialog = new AddDocumentDialog(this, tabbedPane.getSelectedIndex() + 1);
        dialog.setVisible(true);

        Document newDoc = dialog.getCreatedDocument();
        if (newDoc != null) {
            addDocument(newDoc);
            tabbedPane.setSelectedIndex(newDoc.getSmkLevel() - 1);
            updateStatus(newDoc.getSmkLevel());
        }
    }

    public void addDocument(Document doc) {
        addDocumentToUI(doc);
        DataManager.saveDocuments(documentList);
        AuditLogger.getInstance().log(currentUser.getFullName() + " [" + currentUser.getRole().getDisplayName() + "]", "СОЗДАНИЕ", doc.getId(), "Добавлен документ: " + doc.getTitle());
    }

    private void addDocumentToUI(Document doc) {
        documentList.add(doc);
        int tabIndex = doc.getSmkLevel() - 1;
        if (tabIndex >= 0 && tabIndex < 5) {
            tableModels[tabIndex].addRow(new Object[]{
                    doc.getId(), doc.getTitle(), doc.getVersion(), doc.getOrigin(),
                    doc.getDate(), doc.getActualizationDate(), doc.getStorageOriginal(),
                    doc.getStorageCopies(), doc.getCopyCount(), doc.getStatus()
            });
            SwingUtilities.invokeLater(() -> DocumentTableManager.updateRowHeights(tables[tabIndex]));
        }
        updateWarningDashboardCount();
    }

    private void filterDocumentsNeedingActualizationThisMonth() {
        int currentTabIndex = tabbedPane.getSelectedIndex();
        TableRowSorter<DefaultTableModel> sorter = tableSorters[currentTabIndex];
        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        sorter.setRowFilter(new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                int modelRow = entry.getIdentifier();
                Object idValue = entry.getModel().getValueAt(modelRow, 0);
                if (idValue == null) return false;

                Document doc = documentList.stream().filter(d -> idValue.toString().trim().equals(d.getId())).findFirst().orElse(null);
                if (doc == null) return false;

                String actDateStr = doc.getActualizationDate();
                if (actDateStr == null || "Не требуется".equalsIgnoreCase(actDateStr.trim())) return false;

                try {
                    LocalDate nextActDate = LocalDate.parse(actDateStr.trim(), DateTimeFormatter.ofPattern("dd.MM.yyyy")).plusDays(365);
                    return (nextActDate.getYear() == currentYear && nextActDate.getMonthValue() == currentMonth) || nextActDate.isBefore(LocalDate.now());
                } catch (Exception e) {
                    return false;
                }
            }
        });

        JOptionPane.showMessageDialog(this, "Фильтр применен: показаны документы, требующие актуализации.", "Фильтр", JOptionPane.INFORMATION_MESSAGE);
    }

    private void deleteSelectedDocument() {
        if (currentUser.getRole() == User.Role.LABORATORY_ASSISTANT) {
            JOptionPane.showMessageDialog(this, "У вас роль 'Лаборант'.", "Доступ ограничен", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentUser.getRole() == User.Role.OPERATOR && !PasswordProtectionManager.requestAdminAccess(this)) {
            JOptionPane.showMessageDialog(this, "Неверный пароль.", "Доступ запрещен", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int currentTabIndex = tabbedPane.getSelectedIndex();
        int viewRow = tables[currentTabIndex].getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Выберите запись для удаления!", "Удаление", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tables[currentTabIndex].convertRowIndexToModel(viewRow);
        String docId = tableModels[currentTabIndex].getValueAt(modelRow, 0).toString();
        String docTitle = tableModels[currentTabIndex].getValueAt(modelRow, 1).toString();

        if (JOptionPane.showConfirmDialog(this, "Удалить документ?\nID: " + docId, "Подтверждение", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            documentList.removeIf(doc -> doc.getId().equals(docId));
            tableModels[currentTabIndex].removeRow(modelRow);
            DataManager.saveDocuments(documentList);
            updateWarningDashboardCount();
            AuditLogger.getInstance().log(currentUser.getFullName() + " [" + currentUser.getRole().getDisplayName() + "]", "УДАЛЕНИЕ", docId, "Удален: " + docTitle);
            updateStatus(currentTabIndex + 1);
        }
    }

    private void openEditDialog() {
        if (currentUser.getRole() == User.Role.LABORATORY_ASSISTANT) {
            JOptionPane.showMessageDialog(this, "У вас роль 'Лаборант'.", "Доступ ограничен", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int currentTabIndex = tabbedPane.getSelectedIndex();
        int viewRow = tables[currentTabIndex].getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Выберите строку для редактирования!", "Редактирование", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = tables[currentTabIndex].convertRowIndexToModel(viewRow);
        String docId = tableModels[currentTabIndex].getValueAt(modelRow, 0).toString();
        Document docToEdit = documentList.stream().filter(d -> d.getId().equals(docId)).findFirst().orElse(null);

        if (docToEdit != null) {
            int oldLevel = docToEdit.getSmkLevel();
            AddDocumentDialog editDialog = new AddDocumentDialog(this, docToEdit);
            editDialog.setVisible(true);

            Document updatedDoc = editDialog.getCreatedDocument();
            if (updatedDoc != null) {
                if (currentUser.getRole() == User.Role.OPERATOR && (!docToEdit.getId().equals(updatedDoc.getId()) || oldLevel != updatedDoc.getSmkLevel())) {
                    if (!PasswordProtectionManager.requestAdminAccess(this)) {
                        JOptionPane.showMessageDialog(this, "Неверный пароль.", "Доступ запрещен", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }

                String changeDetails = docToEdit.getDiff(updatedDoc);
                docToEdit.setTitle(updatedDoc.getTitle());
                docToEdit.setSmkLevel(updatedDoc.getSmkLevel());
                docToEdit.setVersion(updatedDoc.getVersion());
                docToEdit.setOrigin(updatedDoc.getOrigin());
                docToEdit.setDate(updatedDoc.getDate());
                docToEdit.setActualizationDate(updatedDoc.getActualizationDate());
                docToEdit.setStorageOriginal(updatedDoc.getStorageOriginal());
                docToEdit.setStorageCopies(updatedDoc.getStorageCopies());
                docToEdit.setStatus(updatedDoc.getStatus());
                docToEdit.setCopyCount(updatedDoc.getCopyCount());

                if (oldLevel != updatedDoc.getSmkLevel()) {
                    tableModels[currentTabIndex].removeRow(modelRow);
                    addDocumentToUI(docToEdit);
                    tabbedPane.setSelectedIndex(updatedDoc.getSmkLevel() - 1);
                } else {
                    tableModels[currentTabIndex].fireTableRowsUpdated(modelRow, modelRow);
                }

                DataManager.saveDocuments(documentList);
                updateWarningDashboardCount();
                AuditLogger.getInstance().log(currentUser.getFullName() + " [" + currentUser.getRole().getDisplayName() + "]", "ИЗМЕНЕНИЕ", docId, changeDetails);
                updateStatus(tabbedPane.getSelectedIndex() + 1);
            }
        }
    }

    private void updateStatus(int level) {
        int total = 0, active = 0, inProgress = 0, archive = 0, canceled = 0;
        for (Document doc : documentList) {
            if (doc.getSmkLevel() == level) {
                total++;
                String status = doc.getStatus().trim();
                if (status.equalsIgnoreCase("Действует")) active++;
                else if (status.equalsIgnoreCase("В работе")) inProgress++;
                else if (status.equalsIgnoreCase("В архиве")) archive++;
                else if (status.toLowerCase().startsWith("отменен")) canceled++;
            }
        }
        statusLabelLeft.setText(String.format("НА УРОВНЕ %d: Всего: %d | Действует: %d | В работе: %d | В архиве: %d | Отменен: %d", level, total, active, inProgress, archive, canceled));
        statusLabelRight.setText(String.format("| ИТОГО В БАЗЕ СМК: %d документов ", documentList.size()));
    }

    private void addTestValues() {
        addDocument(new Document("УР1-01", "Руководство по качеству испытательной лаборатории ООО 'Спектр'", "05.08.2026", "Действует", 1, "Внутренний", "v2.0", "Сейф №1", "Сервер СМК", 2, "01.06.2026"));
        addDocument(new Document("УР4-01", "ГОСТ ISO/IEC 17025-2019 Общие требования к компетентности испытательных лабораторий", "12.01.2024", "Действует", 4, "Внешний", "", "Архив", "Полка №2", 1, "15.03.2026"));
    }

    private JTable getCurrentTable() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        return (selectedIndex != -1 && tables != null) ? tables[selectedIndex] : null;
    }

    private void applyRowColoring(JTable table) {
        StatusRowTableCellRenderer renderer = new StatusRowTableCellRenderer("Статус", documentList);
        table.setDefaultRenderer(Object.class, renderer);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    public List<Document> getDocumentList() {
        return documentList;
    }
}