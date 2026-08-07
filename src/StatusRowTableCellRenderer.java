import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class StatusRowTableCellRenderer extends JTextArea implements TableCellRenderer {
    private static final Color COLOR_ACTIVE   = new Color(220, 247, 220); // Зеленый
    private static final Color COLOR_IN_WORK  = new Color(220, 240, 255); // Голубой
    private static final Color COLOR_ARCHIVE  = new Color(230, 230, 230); // Серый
    private static final Color COLOR_CANCELED = new Color(255, 220, 220); // Красный

    // Цвета для ячейки актуализации
    private static final Color COLOR_WARNING  = new Color(255, 255, 180); // Желтый (до 30 дней)
    private static final Color COLOR_EXPIRED  = new Color(255, 200, 200); // Красный (просрочено)

    private String statusColumnName = "Статус";
    private List<Document> documentList;

    public StatusRowTableCellRenderer(String statusColumnName, List<Document> documentList) {
        this.statusColumnName = statusColumnName;
        this.documentList = documentList;
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    public StatusRowTableCellRenderer(String statusColumnName) {
        this(statusColumnName, null);
    }

    public StatusRowTableCellRenderer() {
        this("Статус", null);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        setText(value == null ? "" : value.toString());
        setFont(table.getFont());

        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
            return this;
        }

        setForeground(Color.BLACK);

        int modelRow = table.convertRowIndexToModel(row);
        int modelColumn = table.convertColumnIndexToModel(column);

        // Получаем имя текущей колонки, чтобы понять, дата ли это актуализации
        String columnName = table.getModel().getColumnName(modelColumn);

        // Если это колонка даты актуализации (в таблице она называется "Дата актуал." или "Дата актуализации")
        if (columnName != null && (columnName.toLowerCase().contains("актуал"))) {
            Color actColor = getActualizationCellColor(table, modelRow);
            setBackground(actColor);
        } else {
            // Для остальных ячеек применяем подсветку по статусу всей строки
            Color statusColor = getStatusColor(table, modelRow);
            setBackground(statusColor);
        }

        return this;
    }

    /**
     * Цвет только для ячейки актуализации
     */
    private Color getActualizationCellColor(JTable table, int modelRow) {
        if (documentList == null || documentList.isEmpty()) {
            return Color.WHITE;
        }

        Object idValue = table.getModel().getValueAt(modelRow, 0);
        if (idValue == null) return Color.WHITE;

        String targetId = idValue.toString().trim();
        Document doc = null;
        for (Document d : documentList) {
            if (d != null && targetId.equals(d.getId())) {
                doc = d;
                break;
            }
        }

        if (doc == null) return Color.WHITE;

        String actDateStr = doc.getActualizationDate();
        if (actDateStr == null || "Не требуется".equalsIgnoreCase(actDateStr.trim())) {
            return Color.WHITE;
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            LocalDate actDate = LocalDate.parse(actDateStr.trim(), formatter);
            LocalDate nextActDate = actDate.plusDays(365);
            LocalDate today = LocalDate.now();

            long daysUntil = ChronoUnit.DAYS.between(today, nextActDate);

            if (daysUntil < 0) {
                return COLOR_EXPIRED; // Просрочено -> Красный
            } else if (daysUntil <= 30) {
                return COLOR_WARNING; // Осталось <= 30 дней -> Желтый
            }
        } catch (Exception ignored) {
        }

        return Color.WHITE;
    }

    /**
     * Цвет строки по статусу документа (для остальных ячеек)
     */
    private Color getStatusColor(JTable table, int modelRow) {
        int statusModelColumn = -1;

        for (int i = 0; i < table.getModel().getColumnCount(); i++) {
            if (statusColumnName.equalsIgnoreCase(table.getModel().getColumnName(i))) {
                statusModelColumn = i;
                break;
            }
        }

        if (statusModelColumn == -1 && table.getModel().getColumnCount() > 9) {
            statusModelColumn = 9;
        }

        if (statusModelColumn != -1) {
            Object statusValue = table.getModel().getValueAt(modelRow, statusModelColumn);
            if (statusValue != null) {
                String status = statusValue.toString().trim().toLowerCase();
                switch (status) {
                    case "действует":   return COLOR_ACTIVE;
                    case "в работе":    return COLOR_IN_WORK;
                    case "в архиве":    return COLOR_ARCHIVE;
                    case "отменен":
                    case "отменён":     return COLOR_CANCELED;
                }
            }
        }
        return Color.WHITE;
    }
}