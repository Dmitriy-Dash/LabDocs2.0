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
    private JComboBox<String> statusCombo;
    private JSpinner copyCountSpinner;

    private Document createdDocument = null;
    private boolean confirmed = false;
    private MainWindow mainWin;
    private boolean isEditMode = false;
    private int expectedLevel;

    private DatePickerPanel datePicker;
    private DatePickerPanel actualizationDatePicker;
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
    public AddDocumentDialog(Frame parent, Document docToEdit) {
        super(parent, docToEdit == null ? "Добавление документа СМК" : "Изменение документа СМК: " + docToEdit.getId(), true);
        this.mainWin = (parent instanceof MainWindow) ? (MainWindow) parent : null;
        this.docToEdit = docToEdit;
        this.isEditMode = (docToEdit != null);
        this.expectedLevel = docToEdit != null ? docToEdit.getSmkLevel() : 1;

        initUI();

        if (docToEdit != null) {
            populateFieldsForEditing();
        }
    }

    private void initUI() {
        setSize(580, 630);
        setLocationRelativeTo(getParent());
        setResizable(false);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 4, 4, 4);

        idField = new JTextField();
        datePicker = new DatePickerPanel();
        actualizationDatePicker = new DatePickerPanel();

        versionField = new JTextField();
        titleArea = createTextArea();
        storageOriginalArea = createTextArea();
        storageCopiesArea = createTextArea();

        smkLevelCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5});
        originCombo = new JComboBox<>(new String[]{"Внутренний", "Внешний"});
        statusCombo = new JComboBox<>(new String[]{"Действует", "Заменен", "Отменен", "В работе", "Архив"});
        copyCountSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 1000, 1));

        idField.setBackground(new Color(245, 245, 245));

        // Обработчик выбора уровня СМК
        smkLevelCombo.addActionListener(e -> {
            if (smkLevelCombo.getSelectedItem() == null) return;
            int selectedLevel = (int) smkLevelCombo.getSelectedItem();

            // Генерируем новый ID ТОЛЬКО при создании нового документа!
            if (!isEditMode && mainWin != null) {
                int nextNumber = mainWin.getDocumentCountByLevel(selectedLevel) + 1;
                String generatedId = "УР" + selectedLevel + "-" + String.format("%02d", nextNumber);
                idField.setText(generatedId);
            }

            updateFieldsStateByLevel(selectedLevel);
        });

        // Разблокировка по двойному клику
        MouseAdapter doubleClickUnlockAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    if (smkLevelCombo.isEnabled() && idField.isEditable()) {
                        return;
                    }

                    if (PasswordProtectionManager.requestAdminAccess(AddDocumentDialog.this)) {
                        smkLevelCombo.setEnabled(true);
                        idField.setEditable(true);
                        idField.setBackground(Color.WHITE);
                    }
                }
            }
        };

        smkLevelCombo.addMouseListener(doubleClickUnlockAdapter);
        idField.addMouseListener(doubleClickUnlockAdapter);

        // Расстановка полей
        int row = 0;
        addFormRow(formPanel, gbc, "Уровень СМК:", smkLevelCombo, row++, false);
        addFormRow(formPanel, gbc, "ID / Шифр:", idField, row++, false);
        addFormRow(formPanel, gbc, "Название документа:", new JScrollPane(titleArea), row++, true);
        addFormRow(formPanel, gbc, "Версия (только 1-3 ур.):", versionField, row++, false);
        addFormRow(formPanel, gbc, "Происхождение:", originCombo, row++, false);
        addFormRow(formPanel, gbc, "Дата регистрации:", datePicker, row++, false);
        addFormRow(formPanel, gbc, "Дата актуализации (4-5 ур.):", actualizationDatePicker, row++, false);
        addFormRow(formPanel, gbc, "Хранение оригинала:", new JScrollPane(storageOriginalArea), row++, true);
        addFormRow(formPanel, gbc, "Хранение копий:", new JScrollPane(storageCopiesArea), row++, true);
        addFormRow(formPanel, gbc, "Кол-во копий:", copyCountSpinner, row++, false);
        addFormRow(formPanel, gbc, "Текущий статус:", statusCombo, row++, false);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        JButton saveButton = new JButton("Сохранить");
        JButton cancelButton = new JButton("Отмена");

        saveButton.addActionListener(e -> onSave());
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);

        add(formPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        if (!isEditMode) {
            smkLevelCombo.setSelectedIndex(0);
        }
    }

    private void populateFieldsForEditing() {
        smkLevelCombo.setSelectedItem(docToEdit.getSmkLevel());
        idField.setText(docToEdit.getId());
        idField.setEditable(false);
        smkLevelCombo.setEnabled(false); // Запрещаем случайно менять уровень при редактировании

        titleArea.setText(docToEdit.getTitle());
        versionField.setText(docToEdit.getVersion());
        originCombo.setSelectedItem(docToEdit.getOrigin());
        datePicker.setText(docToEdit.getDate());
        actualizationDatePicker.setText(docToEdit.getActualizationDate());
        storageOriginalArea.setText(docToEdit.getStorageOriginal());
        storageCopiesArea.setText(docToEdit.getStorageCopies());
        copyCountSpinner.setValue(docToEdit.getCopyCount());
        statusCombo.setSelectedItem(docToEdit.getStatus());

        updateFieldsStateByLevel(docToEdit.getSmkLevel());
    }

    private void onSave() {
        String title = titleArea.getText().trim();
        String regDate = datePicker.getText().trim();
        String actDate = actualizationDatePicker.getText().trim();
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

        if (selectedLevel >= 4) {
            if (actDate.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Для документов " + selectedLevel + "-го уровня дата актуализации обязательна!", "Предупреждение", JOptionPane.WARNING_MESSAGE);
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
                (String) statusCombo.getSelectedItem(),
                selectedLevel,
                (String) originCombo.getSelectedItem(),
                versionField.getText().trim(),
                storageOriginal,
                storageCopies,
                copyCount,
                actualizationDatePicker.getText()
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

        if (selectedLevel >= 4) {
            actualizationDatePicker.getTextField().setEnabled(true);
            actualizationDatePicker.getTextField().setBackground(Color.WHITE);
        } else {
            actualizationDatePicker.setText("");
            actualizationDatePicker.getTextField().setEnabled(false);
            actualizationDatePicker.getTextField().setBackground(new Color(240, 240, 240));
        }
    }

    private JTextArea createTextArea() {
        JTextArea area = new JTextArea(2, 20);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        return area;
    }

    private void addFormRow(JPanel panel, GridBagConstraints gbc, String labelText, Component component, int row, boolean isMultiLine) {
        gbc.gridy = row;
        gbc.gridx = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 1;
        gbc.weightx = 0.0;
        gbc.weighty = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        panel.add(new JLabel(labelText), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;

        if (isMultiLine) {
            gbc.weighty = 1.0;
            gbc.fill = GridBagConstraints.BOTH;
            component.setPreferredSize(new Dimension(280, 42));
        } else {
            gbc.weighty = 0.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            component.setPreferredSize(new Dimension(280, 24));
        }
        panel.add(component, gbc);
    }

    public Document getCreatedDocument() {
        return confirmed ? createdDocument : null;
    }
}