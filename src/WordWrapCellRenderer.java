import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/**
 * Рендерер для отображения многострочного текста в ячейках таблицы СМК.
 */
public class WordWrapCellRenderer extends JTextArea implements TableCellRenderer {

    public WordWrapCellRenderer() {
        setLineWrap(true);
        setWrapStyleWord(true);
        setOpaque(true);
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus, int row, int column) {
        setText(value != null ? value.toString() : "");

        if (isSelected) {
            setBackground(table.getSelectionBackground());
            setForeground(table.getSelectionForeground());
        } else {
            setBackground(table.getBackground());
            setForeground(table.getForeground());
        }

        setFont(table.getFont());

        int columnWidth = table.getColumnModel().getColumn(column).getWidth();
        if (columnWidth > 0) {
            setSize(new Dimension(columnWidth, 1000));
        }

        return this;
    }
}