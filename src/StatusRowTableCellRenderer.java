import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

class StatusRowTableCellRenderer extends JTextArea implements TableCellRenderer {
    private static final Color COLOR_ACTIVE   = new Color(220, 247, 220); // Зеленый
    private static final Color COLOR_IN_WORK  = new Color(220, 240, 255); // Голубой
    private static final Color COLOR_ARCHIVE  = new Color(230, 230, 230); // Серый
    private static final Color COLOR_CANCELED = new Color(255, 220, 220); // Красный

    private String statusColumnName = "Статус";

    public StatusRowTableCellRenderer(String statusColumnName) {
        this.statusColumnName = statusColumnName;
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    public StatusRowTableCellRenderer() {
        this("Статус");
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        // Устанавливаем текст
        setText(value == null ? "" : value.toString());
        setFont(table.getFont());

        // Выделение строки мышью
        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
            return this;
        }

        setForeground(Color.BLACK);

        // Динамический поиск модели и статуса
        int modelRow = table.convertRowIndexToModel(row);
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

        Object statusValue = null;
        if (statusModelColumn != -1) {
            statusValue = table.getModel().getValueAt(modelRow, statusModelColumn);
        }

        // Подкрашивание фона
        if (statusValue != null) {
            String status = statusValue.toString().trim().toLowerCase();

            switch (status) {
                case "действует":
                    setBackground(COLOR_ACTIVE);
                    break;
                case "в работе":
                    setBackground(COLOR_IN_WORK);
                    break;
                case "в архиве":
                    setBackground(COLOR_ARCHIVE);
                    break;
                case "отменен":
                case "отменён":
                    setBackground(COLOR_CANCELED);
                    break;
                default:
                    setBackground(Color.WHITE);
                    break;
            }
        } else {
            setBackground(Color.WHITE);
        }

        return this;
    }
}