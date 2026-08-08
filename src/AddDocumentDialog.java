import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;

public class AddDocumentDialog extends JDialog {
    private JTextField idField;
    private JTextField versionField;

    private JTextArea titleArea;
    private JTextArea storageOriginalArea;
    private JTextArea storageCopiesArea;

    private JComboBox<Integer> smkLevelCombo;
    private JComboBox<String> originCombo;
    private JComboBox<String> statusComboBox;
    private JSpinner copyCountSpinner;

    private Document createdDocument = null;
    private boolean confirmed = false;
    private MainWindow mainWin;
    private boolean isEditMode = false;
    private int expectedLevel;

    private DatePickerPanel datePicker;
    private DatePickerPanel actualizationDatePicker;
    private JCheckBox actualizationCheckBox; // Чекбокс для контроля актуализации

    private Document docToEdit;

    /**
     * Конструктор для создания нового документа
     */
    public AddDocumentDialog(MainWindow parent, int targetLevel) {
        super(parent, "Добавление нового документа СМК", true);
        this.mainWin = parent;
        this.isEditMode = false;
        this.expectedLevel = targetLevel;
        this.docToEdit = null;

        initUI();

        if (targetLevel >= 1 && targetLevel <= 5) {
            smkLevelCombo.setSelectedItem(targetLevel);
        }
    }

    /**
     * Конструктор для редактирования существующего документа
     */
    /**
     * Конструктор для редактирования существующего документа
     */
    public AddDocumentDialog(MainWindow parent, Document docToEdit) {
        super(parent, docToEdit == null ? "Добавление документа СМК" : "Изменение документа СМК: " + docToEdit.getId(), true);
        this.mainWin = parent;
        this.docToEdit = docToEdit;
        this.isEditMode = (docToEdit != null);
        this.expectedLevel = docToEdit != null ? docToEdit.getSmkLevel() : 1;

        initUI();

        if (docToEdit != null) {
            populateFieldsForEditing(docToEdit);
        }
    }

    private void initUI() {
        setSize(580, 680);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        // --- ИНИЦИАЛИЗА КОМПОНЕНТОВ ---
        idField = new JTextField();
        datePicker = new DatePickerPanel();
        actualizationDatePicker = new DatePickerPanel();

        // Чекбокс "Требуется ли актуализация?"
        actualizationCheckBox = new JCheckBox("Требуется ли актуализация?", true);

        versionField = new JTextField();
        titleArea = createTextArea();
        storageOriginalArea = createTextArea();
        storageCopiesArea = createTextArea();

        smkLevelCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        originCombo = new JComboBox<>(new String[]{"Внутренний", "Внешний"});

        String[] statuses = {"Действует", "В работе", "В архиве", "Отменен"};
        statusComboBox = new JComboBox<>(statuses);

        copyCountSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));

        // Настройка блокировки ID/Шифра
        idField.setEditable(false);
        idField.setBackground(new Color(245, 245, 245));
        idField.setToolTipText("Двойной клик для изменения (требуется пароль)");

        // Настройка блокировки уровня СМК (для режима редактирования)
        if (isEditMode) {
            smkLevelCombo.setEnabled(false);
            smkLevelCombo.setToolTipText("Двойной клик для изменения (требуется пароль)");
        }

        // --- ЛОГИКА ПЕРЕКЛЮЧЕНИЯ ЧЕКБОКСА АКТУАЛИЗАЦИИ ---
        actualizationCheckBox.addActionListener(e -> updateActualizationState());

        // --- ДВОЙНОЙ КЛИК ПО ПОЛЮ ID / ШИФР ---
        idField.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !idField.isEditable()) {
                    if (PasswordProtectionManager.requestAdminAccess(AddDocumentDialog.this)) {
                        idField.setEditable(true);
                        idField.setBackground(Color.WHITE);
                        idField.requestFocus();
                        JOptionPane.showMessageDialog(AddDocumentDialog.this, "Поле 'ID / Шифр' разблокировано для редактирования.", "Доступ разрешен", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });

        // --- ДВОЙНОЙ КЛИК ПО ВЫПАДАЮЩЕМУ СПИСКУ УРОВНЯ СМК ---
        smkLevelCombo.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && !smkLevelCombo.isEnabled()) {
                    if (PasswordProtectionManager.requestAdminAccess(AddDocumentDialog.this)) {
                        smkLevelCombo.setEnabled(true);
                        smkLevelCombo.requestFocus();
                        JOptionPane.showMessageDialog(AddDocumentDialog.this, "Выпадающий список 'Уровень СМК' разблокирован.", "Доступ разрешен", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });

        // --- ОБРАБОТЧИК ВЫБОРА УРОВНЯ СМК ---
        smkLevelCombo.addActionListener(e -> {
            if (smkLevelCombo.getSelectedItem() == null) return;
            int selectedLevel = (int) smkLevelCombo.getSelectedItem();

            if (!isEditMode && mainWin != null) {
                String generatedId = DocumentCodeGenerator.generateNextMaxCode(selectedLevel, mainWin.getDocumentList());
                idField.setText(generatedId);
            }

            updateFieldsStateByLevel(selectedLevel);
        });

        // --- РАЗМЕЩЕНИЕ ЭЛЕМЕНТОВ В GRIDBAGLAYOUT ---
        int row = 0;

        // Уровень СМК
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Уровень СМК:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formPanel.add(smkLevelCombo, gbc);

        // ID / Шифр
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(new JLabel("ID / Шифр:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formPanel.add(idField, gbc);

        // Название документа
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Название документа:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formPanel.add(new JScrollPane(titleArea), gbc);

        // Версия
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Версия / Редакция:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formPanel.add(versionField, gbc);

        // Происхождение
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Происхождение:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formPanel.add(originCombo, gbc);

        // Дата регистрации
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Дата регистрации:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formPanel.add(datePicker, gbc);

        // Флаг потребности в актуализации
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 2; gbc.weightx = 1.0;
        formPanel.add(actualizationCheckBox, gbc);
        gbc.gridwidth = 1; row++;

        // Дата актуализации
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Дата актуализации:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formPanel.add(actualizationDatePicker, gbc);

        // Хранение оригинала
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Место хранения оригинала:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formPanel.add(new JScrollPane(storageOriginalArea), gbc);

        // Хранение копий
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Место хранения копий:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formPanel.add(new JScrollPane(storageCopiesArea), gbc);

        // Кол-во копий
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Количество копий:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formPanel.add(copyCountSpinner, gbc);

        // Статус документа
        gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Статус документа:"), gbc);
        gbc.gridx = 1; gbc.gridy = row++; gbc.weightx = 0.7;
        formPanel.add(statusComboBox, gbc);

        // --- КНОПКИ УПРАВЛЕНИЯ ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton saveButton = new JButton("Сохранить");
        JButton cancelButton = new JButton("Отмена");

        saveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        // --- ДОБАВЛЕНИЕ ПАНЕЛЕЙ В ДИАЛОГ ---
        setLayout(new BorderLayout());
        add(new JScrollPane(formPanel), BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    /**
     * Обновление доступности и текста поля даты актуализации при клике на галочку
     */
    private void updateActualizationState() {
        boolean isRequired = actualizationCheckBox.isSelected();
        if (isRequired) {
            actualizationDatePicker.getTextField().setEnabled(true);
            actualizationDatePicker.getTextField().setBackground(Color.WHITE);
            if ("Не требуется".equalsIgnoreCase(actualizationDatePicker.getText().trim())) {
                actualizationDatePicker.setText("");
            }
        } else {
            actualizationDatePicker.setText("Не требуется");
            actualizationDatePicker.getTextField().setEnabled(false);
            actualizationDatePicker.getTextField().setBackground(new Color(240, 240, 240));
        }
    }

    private void populateFieldsForEditing(Document doc) {
        idField.setText(doc.getId());
        smkLevelCombo.setSelectedItem(doc.getSmkLevel());
        titleArea.setText(doc.getTitle());
        versionField.setText(doc.getVersion());
        originCombo.setSelectedItem(doc.getOrigin());

        datePicker.setText(doc.getDate());

        // Настройка состояния актуализации на основе сохраненных данных
        String actDate = doc.getActualizationDate();
        if (actDate == null || actDate.trim().isEmpty() || "Не требуется".equalsIgnoreCase(actDate.trim())) {
            actualizationCheckBox.setSelected(false);
            actualizationDatePicker.setText("Не требуется");
            actualizationDatePicker.getTextField().setEnabled(false);
            actualizationDatePicker.getTextField().setBackground(new Color(240, 240, 240));
        } else {
            actualizationCheckBox.setSelected(true);
            actualizationDatePicker.setText(actDate);
            actualizationDatePicker.getTextField().setEnabled(true);
            actualizationDatePicker.getTextField().setBackground(Color.WHITE);
        }

        storageOriginalArea.setText(doc.getStorageOriginal());
        storageCopiesArea.setText(doc.getStorageCopies());
        copyCountSpinner.setValue(doc.getCopyCount());

        if (statusComboBox != null) {
            statusComboBox.setSelectedItem(doc.getStatus());
        }
    }

    private void onSave() {
        String title = titleArea.getText().trim();
        String regDate = datePicker.getText().trim();
        String actDate;
        String storageOriginal = storageOriginalArea.getText().trim();
        String storageCopies = storageCopiesArea.getText().trim();
        int copyCount = (int) copyCountSpinner.getValue();
        int selectedLevel = (int) smkLevelCombo.getSelectedItem();

        if (!isEditMode && selectedLevel != expectedLevel) {
            JOptionPane.showMessageDialog(this,
                    "Выбранный Уровень СМК (" + selectedLevel + ") не совпадает с текущей активной вкладкой (" + expectedLevel + ")!\n" +
                            "Пожалуйста, выберите уровень " + expectedLevel + " или перейдите на соответствующую вкладку.",
                    "Ошибка совпадения уровней",
                    JOptionPane.ERROR_MESSAGE);
            smkLevelCombo.requestFocus();
            return;
        }

        if (title.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Заполните Название документа!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            titleArea.requestFocus();
            return;
        }

        if (regDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Дата регистрации обязательна для заполнения!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            datePicker.getTextField().requestFocus();
            return;
        }

        if (!datePicker.isValidDate()) {
            JOptionPane.showMessageDialog(this,
                    "Введена некорректная дата регистрации!\nДата не должна превышать сегодняшний день и должна быть в формате ДД.ММ.ГГГГ",
                    "Ошибка даты", JOptionPane.ERROR_MESSAGE);
            datePicker.getTextField().requestFocus();
            return;
        }

        // Проверка и обработка даты актуализации
        if (actualizationCheckBox.isSelected()) {
            actDate = actualizationDatePicker.getText().trim();

            if (actDate.isEmpty() || "Не требуется".equalsIgnoreCase(actDate)) {
                JOptionPane.showMessageDialog(this, "Укажите дату актуализации!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
                actualizationDatePicker.getTextField().requestFocus();
                return;
            }

            if (!actualizationDatePicker.isValidDate()) {
                JOptionPane.showMessageDialog(this,
                        "Введена некорректная дата актуализации!\nДата не должна превышать сегодняшний день и должна быть в формате ДД.ММ.ГГГГ",
                        "Ошибка даты", JOptionPane.ERROR_MESSAGE);
                actualizationDatePicker.getTextField().requestFocus();
                return;
            }

            try {
                String[] regP = regDate.split("\\.");
                String[] actP = actDate.split("\\.");
                LocalDate regParsed = LocalDate.of(Integer.parseInt(regP[2]), Integer.parseInt(regP[1]), Integer.parseInt(regP[0]));
                LocalDate actParsed = LocalDate.of(Integer.parseInt(actP[2]), Integer.parseInt(actP[1]), Integer.parseInt(actP[0]));

                if (actParsed.isBefore(regParsed)) {
                    JOptionPane.showMessageDialog(this, "Дата актуализации не может быть раньше даты регистрации!", "Ошибка хронологии", JOptionPane.ERROR_MESSAGE);
                    actualizationDatePicker.getTextField().requestFocus();
                    return;
                }
            } catch (Exception ignored) {}
        } else {
            actDate = "Не требуется";
        }

        if (storageOriginal.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Укажите место хранения оригинала!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            storageOriginalArea.requestFocus();
            return;
        }

        if (copyCount > 0 && storageCopies.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Указано количество копий (" + copyCount + " шт.), но не заполнено место хранения копий!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
            storageCopiesArea.requestFocus();
            return;
        }

        createdDocument = new Document(
                idField.getText().trim(),
                title,
                datePicker.getText(),
                (String) statusComboBox.getSelectedItem(),
                selectedLevel,
                (String) originCombo.getSelectedItem(),
                versionField.getText().trim(),
                storageOriginal,
                storageCopies,
                copyCount,
                actDate
        );
        confirmed = true;
        dispose();
    }

    private void updateFieldsStateByLevel(int selectedLevel) {
        if (selectedLevel > 3) {
            versionField.setText("Не требуется");
            versionField.setEnabled(false);
        } else {
            if ("Не требуется".equals(versionField.getText())) {
                versionField.setText("");
            }
            versionField.setEnabled(true);
        }

        // Автоматически настраиваем галочку при переключении уровня
        if (selectedLevel >= 4) {
            actualizationCheckBox.setSelected(true);
        }
        updateActualizationState();
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea(2, 20);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    public Document getCreatedDocument() {
        if (!confirmed) return null;
        return createdDocument;
    }
}