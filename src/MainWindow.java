import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

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

    public MainWindow() {
        setTitle("Учет документов СМК Испытательной Лаборатории (x86/x64)");
        setSize(1250, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- ВЕРХНЯЯ ПАНЕЛЬ С КНОПКАМИ И ПОИСКОМ ---
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // Панель для основных действий (верхняя строчка)
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton addButton = new JButton("Добавить документ");
        JButton editButton = new JButton("Изменить выбранный");
        JButton deleteButton = new JButton("Удалить выбранный");
        JButton btnSave = new JButton("Сохранить в Excel");
        JButton auditButton = new JButton("Журнал аудита");
        JButton passwordButton = new JButton("Сменить пароль");
        JButton btnValidation = new JButton("Валидация ПО (ГОСТ 17025)");

        btnValidation.addActionListener(e -> {
            SwingUtilities.invokeLater(() -> {
                validation.SystemValidationApp validationFrame = new validation.SystemValidationApp();
                validationFrame.setVisible(true);
            });
        });

        buttonsPanel.add(addButton);
        buttonsPanel.add(editButton);
        buttonsPanel.add(deleteButton);
        buttonsPanel.add(btnSave);
        buttonsPanel.add(auditButton);
        buttonsPanel.add(passwordButton);
        buttonsPanel.add(btnValidation);

        // Панель для бэкапа и поиска (нижняя строчка)
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));

        JButton btnRestore = new JButton("Восстановить из бэкапа");
        JLabel searchLabel = new JLabel("Быстрый поиск:");
        searchField = new JTextField(25);

        // Порядок добавления: сначала кнопка восстановления, затем поиск
        searchPanel.add(btnRestore);
        searchPanel.add(searchLabel);
        searchPanel.add(searchField);

        topPanel.add(buttonsPanel);
        topPanel.add(searchPanel);
        // --- ВИДЖЕТ ПРЕДУПРЕЖДЕНИЙ (ДАШБОРД) ---
        warningDashboardPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        warningDashboardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 5, 5, 5),
                BorderFactory.createLineBorder(new Color(220, 180, 100), 1, true)
        ));
        warningDashboardPanel.setBackground(new Color(255, 255, 220)); // Светло-желтый оттенок для привлечения внимания
        warningTitleLabel = new JLabel("⚠️ Требуют актуализации в текущем месяце (Уровень 1):");
        warningCounterLabel = new JLabel("0");
        warningCounterLabel.setFont(new Font("Arial", Font.BOLD, 12));
        warningCounterLabel.setForeground(new Color(180, 0, 0));

        JButton filterWarningButton = new JButton("Показать");
        filterWarningButton.setMargin(new Insets(2, 8, 2, 8));

// Клик по кнопке или счетчику для быстрой фильтрации
        filterWarningButton.addActionListener(e -> filterDocumentsNeedingActualizationThisMonth());

        warningDashboardPanel.add(warningTitleLabel);
        warningDashboardPanel.add(warningCounterLabel);
        warningDashboardPanel.add(filterWarningButton);

// Добавляем виджет на верхнюю панель (например, под панель кнопок)
        topPanel.add(warningDashboardPanel);

        // --- ВКЛАДКИ С ТАБЛИЦАМИ ---
        tabbedPane = new JTabbedPane();
        String[] tabShortTitles = {"Уровень 1", "Уровень 2", "Уровень 3", "Уровень 4", "Уровень 5"};
        String[] tabDescriptions = {
                "Состав уровня: Документы, описывающие СМК ИЛ (Руководство по качеству, Политика в области качества)",
                "Состав уровня: Руководящие документы, документированные процедуры, СОП, рабочие инструкции",
                "Состав уровня: Внутренние организационно-распорядительные документы, графики, таблицы",
                "Состав уровня: Правовые, нормативные и технические документы внешнего происхождения (ГОСТы)",
                "Состав уровня: Документы внешнего происхождения, содержащие свидетельства выполнения требований"
        };

        for (int i = 0; i < 5; i++) {
            tableModels[i] = DocumentTableManager.createTableModel();
            tableSorters[i] = new TableRowSorter<>(tableModels[i]);
            tables[i] = DocumentTableManager.createTable(tableModels[i], tableSorters[i]);

            applyRowColoring(tables[i]);

            tables[i].addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        openEditDialog();
                    }
                }
            });

            JScrollPane scrollPane = new JScrollPane(tables[i]);
            JPanel tabContentPanel = new JPanel(new BorderLayout(5, 5));
            JLabel descLabel = new JLabel("<html><i style='color:gray;'>" + tabDescriptions[i] + "</i></html>");
            descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            descLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            tabContentPanel.add(descLabel, BorderLayout.NORTH);
            tabContentPanel.add(scrollPane, BorderLayout.CENTER);
            tabbedPane.addTab(tabShortTitles[i], tabContentPanel);
        }

        filterManager = new DocumentFilterManager(searchField, tableSorters, tabbedPane);

        tabbedPane.addChangeListener(e -> {
            filterManager.applyFilter();
            int selectedTab = tabbedPane.getSelectedIndex();
            updateStatus(selectedTab + 1);
            updateWarningDashboardCount(); // <--- Обновляем счетчик при смене вкладки
        });

        // --- НИЖНЯЯ ПАНЕЛЬ СТАТУСА ---
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        statusLabelLeft = new JLabel("НА УРОВНЕ 1: Всего: 0 | Действует: 0 | В работе: 0 | В архиве: 0 | Отменен: 0");
        statusLabelRight = new JLabel("| ИТОГО В БАЗЕ СМК: 0 документов ");
        statusLabelLeft.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        statusLabelRight.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        bottomPanel.add(statusLabelLeft, BorderLayout.WEST);
        bottomPanel.add(statusLabelRight, BorderLayout.EAST);

        add(topPanel, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        // --- ОБРАБОТЧИКИ КНОПОК ---
        addButton.addActionListener(e -> onAddDocumentButtonClicked());
        editButton.addActionListener(e -> openEditDialog());
        deleteButton.addActionListener(e -> deleteSelectedDocument());
        btnSave.addActionListener(e -> exportCurrentTabToExcel());

        auditButton.addActionListener(e -> {
            if (PasswordProtectionManager.requestAdminAccess(this)) {
                String logData = AuditLogger.getInstance().readEncryptedLog();
                JTextArea textArea = new JTextArea(logData, 20, 50);
                textArea.setEditable(false);
                JOptionPane.showMessageDialog(this, new JScrollPane(textArea), "Зашифрованный журнал аудита", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        btnRestore.addActionListener(e -> {
            if (!PasswordProtectionManager.requestAdminAccess(this)) {
                return;
            }

            JFileChooser fileChooser = new JFileChooser(new File("backups"));
            fileChooser.setDialogTitle("Выберите файл резервной копии");
            int result = fileChooser.showOpenDialog(this);

            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedBackup = fileChooser.getSelectedFile();
                try {
                    Files.copy(
                            selectedBackup.toPath(),
                            new File("smk_documents.dat").toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                    );

                    documentList.clear();
                    for (DefaultTableModel model : tableModels) {
                        model.setRowCount(0);
                    }

                    loadDataFromFile();
                    updateStatus(tabbedPane.getSelectedIndex() + 1);

                    JOptionPane.showMessageDialog(this,
                            "База успешно восстановлена и данные обновлены!",
                            "Восстановление", JOptionPane.INFORMATION_MESSAGE);

                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this,
                            "Ошибка восстановления: " + ex.getMessage(),
                            "Ошибка", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        passwordButton.addActionListener(e -> PasswordProtectionManager.changePassword(MainWindow.this));

        // --- ЗАГРУЗКА ИЗ ФАЙЛА ИЛИ ИНИЦИАЛИЗАЦИЯ ---
        loadDataFromFile();
        updateStatus(1);

        // Автосохранение и бэкап при выходе
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

    private void loadDataFromFile() {
        List<Document> savedDocs = DataManager.loadDocuments();
        if (savedDocs.isEmpty()) {
            addTestValues();
        } else {
            for (Document doc : savedDocs) {
                addDocumentToUI(doc);
            }
        }
        // ОБЯЗАТЕЛЬНО обновляем счетчик дашборда после загрузки всех данных
        updateWarningDashboardCount();
    }

    private void updateWarningDashboardCount() {
        int currentTabIndex = tabbedPane.getSelectedIndex();
        int currentLevel = currentTabIndex + 1; // Уровни от 1 до 5

        // Обновляем текст заголовка виджета, показывая текущий уровень
        if (warningTitleLabel != null) {
            warningTitleLabel.setText("⚠️ Требуют актуализации в текущем месяце (Уровень " + currentLevel + "):");
        }

        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        int count = 0;

        for (Document doc : documentList) {
            // Считаем только для документов текущего уровня вкладки
            if (doc.getSmkLevel() != currentLevel) {
                continue;
            }

            String actDateStr = doc.getActualizationDate();
            if (actDateStr == null || "Не требуется".equalsIgnoreCase(actDateStr.trim())) {
                continue;
            }

            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                LocalDate actDate = LocalDate.parse(actDateStr.trim(), formatter);
                LocalDate nextActDate = actDate.plusDays(365);

                // Проверяем, попадает ли дата следующей актуализации на текущий месяц/год или уже прошла
                if ((nextActDate.getYear() == currentYear && nextActDate.getMonthValue() == currentMonth) || nextActDate.isBefore(LocalDate.now())) {
                    count++;
                }
            } catch (Exception ignored) {
            }
        }

        if (warningCounterLabel != null) {
            warningCounterLabel.setText(String.valueOf(count));
        }
        if (warningDashboardPanel != null) {
            warningDashboardPanel.setVisible(true);
        }
    }
    private void exportCurrentTabToExcel() {
        JTable currentTable = getCurrentTable();
        int selectedIndex = tabbedPane.getSelectedIndex();

        if (currentTable != null && selectedIndex != -1) {
            String tabTitle = tabbedPane.getTitleAt(selectedIndex);
            ExcelExporter.exportTableToExcel(currentTable, tabTitle, this);
        }
    }

    private void onAddDocumentButtonClicked() {
        int currentActiveLevel = tabbedPane.getSelectedIndex() + 1;

        AddDocumentDialog dialog = new AddDocumentDialog(this, currentActiveLevel);
        dialog.setVisible(true);

        Document newDoc = dialog.getCreatedDocument();
        if (newDoc != null) {
            addDocument(newDoc);

            AuditLogger.getInstance().log(
                    "Оператор",
                    "СОЗДАНИЕ",
                    newDoc.getId(),
                    "Добавлен документ: " + newDoc.getTitle()
            );

            tabbedPane.setSelectedIndex(newDoc.getSmkLevel() - 1);
            updateStatus(newDoc.getSmkLevel());
        }
    }

    public void addDocument(Document doc) {
        addDocumentToUI(doc);
        DataManager.saveDocuments(documentList);
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
        // Обновляем счетчик при добавлении
        updateWarningDashboardCount();
    }

    private void filterDocumentsNeedingActualizationThisMonth() {
        int currentTabIndex = tabbedPane.getSelectedIndex();
        TableRowSorter<DefaultTableModel> sorter = tableSorters[currentTabIndex];

        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();

        // Создаем кастомный фильтр для TableRowSorter текущей таблицы
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                int modelRow = entry.getIdentifier();
                Object idValue = entry.getModel().getValueAt(modelRow, 0); // ID документа
                if (idValue == null) return false;

                String targetId = idValue.toString().trim();
                Document doc = null;
                for (Document d : documentList) {
                    if (d != null && targetId.equals(d.getId())) {
                        doc = d;
                        break;
                    }
                }

                if (doc == null) return false;

                String actDateStr = doc.getActualizationDate();
                if (actDateStr == null || "Не требуется".equalsIgnoreCase(actDateStr.trim())) {
                    return false;
                }

                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
                    LocalDate actDate = LocalDate.parse(actDateStr.trim(), formatter);
                    LocalDate nextActDate = actDate.plusDays(365);

                    // Оставляем только те, у которых срок в этом месяце или просрочен
                    boolean isThisMonth = (nextActDate.getYear() == currentYear && nextActDate.getMonthValue() == currentMonth);
                    boolean isExpired = nextActDate.isBefore(LocalDate.now());

                    return isThisMonth || isExpired;
                } catch (Exception e) {
                    return false;
                }
            }
        });

        JOptionPane.showMessageDialog(this,
                "В таблице применен фильтр: показаны документы, требующие актуализации в текущем месяце.",
                "Фильтр виджета",
                JOptionPane.INFORMATION_MESSAGE);
    }

    public int getDocumentCountByLevel(int level) {
        int count = 0;
        for (Document doc : documentList) {
            if (doc.getSmkLevel() == level) count++;
        }
        return count;
    }

    private void deleteSelectedDocument() {
        int currentTabIndex = tabbedPane.getSelectedIndex();
        JTable currentTable = tables[currentTabIndex];
        DefaultTableModel currentModel = tableModels[currentTabIndex];

        int viewRow = currentTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите запись для удаления!", "Удаление", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!PasswordProtectionManager.requestAdminAccess(this)) {
            return;
        }

        int modelRow = currentTable.convertRowIndexToModel(viewRow);

        String docId = currentModel.getValueAt(modelRow, 0).toString();
        String docTitle = currentModel.getValueAt(modelRow, 1).toString();

        int choice = JOptionPane.showConfirmDialog(
                this,
                "Вы действительно хотите безвозвратно удалить документ?\n\nID: " + docId + "\nНазвание: " + docTitle,
                "Подтверждение удаления",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE
        );

        if (choice == JOptionPane.YES_OPTION) {
            documentList.removeIf(doc -> doc.getId().equals(docId));
            currentModel.removeRow(modelRow);

            DataManager.saveDocuments(documentList);
            // Обновляем счетчик при добавлении
            updateWarningDashboardCount();

            AuditLogger.getInstance().log(
                    "Администратор",
                    "УДАЛЕНИЕ",
                    docId,
                    "Удален документ: " + docTitle
            );

            updateStatus(currentTabIndex + 1);
            JOptionPane.showMessageDialog(this, "Документ \"" + docId + "\" успешно удален.", "Удаление", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void openEditDialog() {
        int currentTabIndex = tabbedPane.getSelectedIndex();
        JTable currentTable = tables[currentTabIndex];
        DefaultTableModel currentModel = tableModels[currentTabIndex];

        int viewRow = currentTable.getSelectedRow();
        if (viewRow == -1) {
            JOptionPane.showMessageDialog(this, "Пожалуйста, выберите строку!", "Редактирование", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = currentTable.convertRowIndexToModel(viewRow);

        String docId = currentModel.getValueAt(modelRow, 0).toString();
        Document docToEdit = null;
        for (Document doc : documentList) {
            if (doc.getId().equals(docId)) {
                docToEdit = doc;
                break;
            }
        }

        if (docToEdit != null) {
            int oldLevel = docToEdit.getSmkLevel();
            String originalDocId = docToEdit.getId();

            AddDocumentDialog editDialog = new AddDocumentDialog(this, docToEdit);
            editDialog.setVisible(true);

            Document updatedDoc = editDialog.getCreatedDocument();
            if (updatedDoc != null) {
                updatedDoc.setId(originalDocId);

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

                int newLevel = updatedDoc.getSmkLevel();

                if (oldLevel != newLevel) {
                    currentModel.removeRow(modelRow);

                    int newTabIndex = newLevel - 1;
                    tableModels[newTabIndex].addRow(new Object[]{
                            docToEdit.getId(), docToEdit.getTitle(), docToEdit.getVersion(), docToEdit.getOrigin(),
                            docToEdit.getDate(), docToEdit.getActualizationDate(), docToEdit.getStorageOriginal(),
                            docToEdit.getStorageCopies(), docToEdit.getCopyCount(), docToEdit.getStatus()
                    });

                    tabbedPane.setSelectedIndex(newTabIndex);
                    DocumentTableManager.updateRowHeights(tables[newTabIndex]);

                    tableModels[newTabIndex].fireTableDataChanged();
                    tables[newTabIndex].repaint();
                } else {
                    currentModel.setValueAt(docToEdit.getId(), modelRow, 0);
                    currentModel.setValueAt(docToEdit.getTitle(), modelRow, 1);
                    currentModel.setValueAt(docToEdit.getVersion(), modelRow, 2);
                    currentModel.setValueAt(docToEdit.getOrigin(), modelRow, 3);
                    currentModel.setValueAt(docToEdit.getDate(), modelRow, 4);
                    currentModel.setValueAt(docToEdit.getActualizationDate(), modelRow, 5);
                    currentModel.setValueAt(docToEdit.getStorageOriginal(), modelRow, 6);
                    currentModel.setValueAt(docToEdit.getStorageCopies(), modelRow, 7);
                    currentModel.setValueAt(docToEdit.getCopyCount(), modelRow, 8);
                    currentModel.setValueAt(docToEdit.getStatus(), modelRow, 9);

                    currentModel.fireTableRowsUpdated(modelRow, modelRow);
                    currentTable.clearSelection();

                    DocumentTableManager.updateRowHeights(currentTable);
                    currentTable.revalidate();
                    currentTable.repaint();
                }

                DataManager.saveDocuments(documentList);
                // Обновляем счетчик при добавлении
                updateWarningDashboardCount();

                AuditLogger.getInstance().log(
                        "Оператор",
                        "ИЗМЕНЕНИЕ",
                        originalDocId,
                        changeDetails
                );

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

                if (status.equalsIgnoreCase("Действует")) {
                    active++;
                } else if (status.equalsIgnoreCase("В работе")) {
                    inProgress++;
                } else if (status.equalsIgnoreCase("В архиве")) {
                    archive++;
                } else if (status.equalsIgnoreCase("Отменен") || status.equalsIgnoreCase("Отменён")) {
                    canceled++;
                }
            }
        }

        statusLabelLeft.setText(String.format("НА УРОВНЕ %d: Всего: %d | Действует: %d | В работе: %d | В архиве: %d | Отменен: %d",
                level, total, active, inProgress, archive, canceled));
        statusLabelRight.setText(String.format("| ИТОГО В БАЗЕ СМК: %d документов ", documentList.size()));
    }

    private void addTestValues() {
        addDocument(new Document("УР1-01", "Руководство по качеству испытательной лаборатории ООО 'Спектр'",
                "05.08.2026", "Действует", 1, "Внутренний", "v2.0", "Сейф №1", "Сервер СМК", 2, "01.06.2026"));
        addDocument(new Document("УР4-01", "ГОСТ ISO/IEC 17025-2019 Общие требования к компетентности испытательных лабораторий",
                "12.01.2024", "Действует", 4, "Внешний", "", "Архив", "Полка №2", 1, "15.03.2026"));
    }

    private JTable getCurrentTable() {
        int selectedIndex = tabbedPane.getSelectedIndex();
        if (selectedIndex != -1 && tables != null && selectedIndex < tables.length) {
            return tables[selectedIndex];
        }
        return null;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new MainWindow().setVisible(true));
    }

    private void applyRowColoring(JTable table) {
        // Передаем в конструктор имя колонки статуса и общий список документов для Warning System
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