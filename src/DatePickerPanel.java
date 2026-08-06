import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DatePickerPanel extends JPanel {
    private final JTextField dateField;
    private final JButton calendarButton;

    public DatePickerPanel() {
        setLayout(new BorderLayout(2, 0));

        dateField = new JTextField();

        calendarButton = new JButton("...");
        calendarButton.setToolTipText("Выбрать дату из календаря");
        calendarButton.setMargin(new Insets(2, 6, 2, 6));
        calendarButton.setFocusable(false);
        calendarButton.setPreferredSize(new Dimension(30, 24));

        add(dateField, BorderLayout.CENTER);
        add(calendarButton, BorderLayout.EAST);

        calendarButton.addActionListener(e -> showCalendarDialog());
    }

    public String getText() {
        return dateField.getText().trim();
    }

    public void setText(String text) {
        dateField.setText(text != null ? text.trim() : "");
    }

    public JTextField getTextField() {
        return dateField;
    }

    /**
     * Проверка существования даты и запрет дат из будущего
     */
    public boolean isValidDate() {
        String dateStr = getText();
        if (dateStr.isEmpty()) {
            return true; // Пустая строка допустима для необязательных полей
        }

        String[] parts = dateStr.split("\\.");
        if (parts.length != 3) {
            return false;
        }

        try {
            int day = Integer.parseInt(parts[0].trim());
            int month = Integer.parseInt(parts[1].trim());
            int year = Integer.parseInt(parts[2].trim());

            if (year < 1900 || year > 2100) {
                return false;
            }

            // 1. Проверяем существование даты (выбросит исключение для 31.02, 31.04 и т.д.)
            LocalDate parsedDate = LocalDate.of(year, month, day);

            // 2. Проверяем, не находится ли дата в будущем
            if (parsedDate.isAfter(LocalDate.now())) {
                return false;
            }

            // Автоматически форматируем с ведущими нулями (1.5.2026 -> 01.05.2026)
            dateField.setText(String.format("%02d.%02d.%04d", day, month, year));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void showCalendarDialog() {
        Window parentWindow = SwingUtilities.getWindowAncestor(this);
        JDialog calendarDialog;

        if (parentWindow instanceof Frame) {
            calendarDialog = new JDialog((Frame) parentWindow, "Выберите дату", true);
        } else if (parentWindow instanceof Dialog) {
            calendarDialog = new JDialog((Dialog) parentWindow, "Выберите дату", true);
        } else {
            calendarDialog = new JDialog((Frame) null, "Выберите дату", true);
        }

        CalendarPanel calPanel = new CalendarPanel(getText(), selectedDateStr -> {
            dateField.setText(selectedDateStr);
            calendarDialog.dispose();
        });

        calendarDialog.setContentPane(calPanel);
        calendarDialog.pack();
        calendarDialog.setResizable(false);
        calendarDialog.setLocationRelativeTo(calendarButton);
        calendarDialog.setVisible(true);
    }

    private static class CalendarPanel extends JPanel {
        private YearMonth currentYearMonth;
        private final JLabel monthYearLabel;
        private final JPanel daysPanel;
        private final DateSelectionListener listener;
        private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

        public interface DateSelectionListener {
            void onDateSelected(String dateStr);
        }

        public CalendarPanel(String initialDateStr, DateSelectionListener listener) {
            this.listener = listener;
            setLayout(new BorderLayout(5, 5));
            setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

            if (initialDateStr != null && !initialDateStr.isEmpty()) {
                try {
                    String[] parts = initialDateStr.split("\\.");
                    int d = Integer.parseInt(parts[0].trim());
                    int m = Integer.parseInt(parts[1].trim());
                    int y = Integer.parseInt(parts[2].trim());
                    currentYearMonth = YearMonth.of(y, m);
                } catch (Exception e) {
                    currentYearMonth = YearMonth.now();
                }
            } else {
                currentYearMonth = YearMonth.now();
            }

            JPanel headerPanel = new JPanel(new BorderLayout());
            JButton prevBtn = new JButton("<");
            JButton nextBtn = new JButton(">");
            prevBtn.setFocusable(false);
            nextBtn.setFocusable(false);

            monthYearLabel = new JLabel("", SwingConstants.CENTER);
            monthYearLabel.setFont(new Font("SansSerif", Font.BOLD, 13));
            updateHeader();

            prevBtn.addActionListener(e -> {
                currentYearMonth = currentYearMonth.minusMonths(1);
                updateHeader();
                renderDays();
            });

            nextBtn.addActionListener(e -> {
                currentYearMonth = currentYearMonth.plusMonths(1);
                updateHeader();
                renderDays();
            });

            headerPanel.add(prevBtn, BorderLayout.WEST);
            headerPanel.add(monthYearLabel, BorderLayout.CENTER);
            headerPanel.add(nextBtn, BorderLayout.EAST);

            daysPanel = new JPanel(new GridLayout(0, 7, 2, 2));
            renderDays();

            JButton todayBtn = new JButton("Сегодня");
            todayBtn.setFocusable(false);
            todayBtn.addActionListener(e -> {
                if (listener != null) {
                    listener.onDateSelected(LocalDate.now().format(DISPLAY_FORMATTER));
                }
            });

            add(headerPanel, BorderLayout.NORTH);
            add(daysPanel, BorderLayout.CENTER);
            add(todayBtn, BorderLayout.SOUTH);
        }

        private void updateHeader() {
            String[] months = {
                    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
                    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
            };
            monthYearLabel.setText(months[currentYearMonth.getMonthValue() - 1] + " " + currentYearMonth.getYear());
        }

        private void renderDays() {
            daysPanel.removeAll();

            String[] headers = {"Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс"};
            for (String h : headers) {
                JLabel lbl = new JLabel(h, SwingConstants.CENTER);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
                if (h.equals("Сб") || h.equals("Вс")) {
                    lbl.setForeground(Color.RED);
                }
                daysPanel.add(lbl);
            }

            LocalDate firstOfMonth = currentYearMonth.atDay(1);
            int dayOfWeek = firstOfMonth.getDayOfWeek().getValue();
            int daysInMonth = currentYearMonth.lengthOfMonth();

            for (int i = 1; i < dayOfWeek; i++) {
                daysPanel.add(new JLabel(""));
            }

            LocalDate today = LocalDate.now();

            for (int day = 1; day <= daysInMonth; day++) {
                JButton dayBtn = new JButton(String.valueOf(day));
                dayBtn.setMargin(new Insets(3, 3, 3, 3));
                dayBtn.setFont(new Font("SansSerif", Font.PLAIN, 11));
                dayBtn.setFocusable(false);

                LocalDate btnDate = currentYearMonth.atDay(day);
                if (btnDate.equals(today)) {
                    dayBtn.setBackground(new Color(200, 230, 255));
                    dayBtn.setFont(new Font("SansSerif", Font.BOLD, 11));
                }

                // Запрет клика по будущим датам прямо в календаре
                if (btnDate.isAfter(today)) {
                    dayBtn.setEnabled(false);
                } else {
                    dayBtn.addActionListener(e -> {
                        if (listener != null) {
                            listener.onDateSelected(btnDate.format(DISPLAY_FORMATTER));
                        }
                    });
                }

                daysPanel.add(dayBtn);
            }

            daysPanel.revalidate();
            daysPanel.repaint();
        }
    }
}