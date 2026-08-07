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

    public MainWindow() {
        setTitle("Учет документов СМК Испытательной Лаборатории (x86/x64)");
        setSize(1250, 680);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- ВЕРХНЯЯ ПАНЕЛЬ С КНОПКАМИ И ПОИСКОМ ---
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // Панель для кнопок действий (верхняя строчка)
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

        // Панель для поиска (нижняя строчка)
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JLabel searchLabel = new JLabel("Быстрый поиск:");
        searchField = new JTextField(25);

        searchPanel.add(searchLabel);
        searchPanel.add(searchField);

        topPanel.add(buttonsPanel);
        topPanel.add(searchPanel);

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
            updateStatus(tabbedPane.getSelectedIndex() + 1);
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

        passwordButton.addActionListener(e -> PasswordProtectionManager.changePassword(MainWindow.this));

        // --- ЗАГРУЗКА ИЗ ФАЙЛА ИЛИ ИНИЦИАЛИЗАЦИЯ ---
        loadDataFromFile();
        updateStatus(1);

        // Автосохранение данных при закрытии программы
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

    /**
     * Загрузка сохраненных документов из файла на диске
     */
    private void loadDataFromFile() {
        List<Document> savedDocs = DataManager.loadDocuments();
        if (savedDocs.isEmpty()) {
            addTestValues(); // Если файл пуст/отсутствует, добавляем первичное наполнение
        } else {
            for (Document doc : savedDocs) {
                addDocumentToUI(doc);
            }
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

    /**
     * Добавление документа с автосохранением в файл
     */
    public void addDocument(Document doc) {
        addDocumentToUI(doc);
        DataManager.saveDocuments(documentList); // Сохраняем изменение на диск
    }

    /**
     * Вспомогательный метод для вставки документа в таблицу Swing
     */
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

            DataManager.saveDocuments(documentList); // Сохраняем на диск после удаления

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

                DataManager.saveDocuments(documentList); // Сохраняем на диск после редактирования

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
        StatusRowTableCellRenderer renderer = new StatusRowTableCellRenderer("Статус");

        table.setDefaultRenderer(Object.class, renderer);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    public List<Document> getDocumentList() {
        return documentList;
    }
}