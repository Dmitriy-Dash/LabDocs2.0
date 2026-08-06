import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

/**
 * Класс-помощник для инициализации таблиц уровней СМК и авто-расчета высоты строк.
 */
public class DocumentTableManager {

    private static final String[] COLUMN_NAMES = {
            "ID / Шифр", "Название документа", "Версия", "Происхождение",
            "Дата регистрации", "Дата актуализации", "Хранение оригинала", "Хранение копий", "Кол-во копий", "Статус"
    };

    public static JTable createTable(DefaultTableModel model, TableRowSorter<DefaultTableModel> sorter) {
        JTable table = new JTable(model);
        table.setRowSorter(sorter);
        table.setRowHeight(25);

        // 1. Устанавливаем наш рендерер с подсветкой и переносом текста
        applyRowColoring(table);

        // 2. Настраиваем ширину колонок
        TableColumnModel colModel = table.getColumnModel();
        colModel.getColumn(0).setPreferredWidth(80);
        colModel.getColumn(1).setPreferredWidth(350);
        colModel.getColumn(2).setPreferredWidth(70);
        colModel.getColumn(3).setPreferredWidth(100);
        colModel.getColumn(4).setPreferredWidth(110);
        colModel.getColumn(5).setPreferredWidth(110);
        colModel.getColumn(6).setPreferredWidth(120);
        colModel.getColumn(7).setPreferredWidth(120);
        colModel.getColumn(8).setPreferredWidth(80);
        colModel.getColumn(9).setPreferredWidth(90);

        // 3. Слушатель изменения ширины столбцов мышкой
        colModel.addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                updateRowHeights(table);
            }
            @Override public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
        });

        // 4. Расчет высот после того, как Swing закончит layout
        SwingUtilities.invokeLater(() -> updateRowHeights(table));

        return table; // Единая точка выхода из метода
    }

    private static void applyRowColoring(JTable table) {
        StatusRowTableCellRenderer renderer = new StatusRowTableCellRenderer("Статус");

        table.setDefaultRenderer(Object.class, renderer);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Автоматический перерасчет высоты при изменении размера окна
        table.addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(java.awt.event.ComponentEvent e) {
                DocumentTableManager.updateRowHeights(table);
            }
        });
    }

    public static DefaultTableModel createTableModel() {
        return new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    public static void updateRowHeights(JTable table) {
        for (int row = 0; row < table.getRowCount(); row++) {
            int rowHeight = 25; // Минимальная высота строки

            for (int column = 0; column < table.getColumnCount(); column++) {
                TableCellRenderer renderer = table.getCellRenderer(row, column);
                Component comp = table.prepareRenderer(renderer, row, column);

                // Передаем текущую ширину колонки для правильного расчете высоты текста
                int columnWidth = table.getColumnModel().getColumn(column).getWidth();
                if (columnWidth > 0) {
                    comp.setSize(columnWidth, Integer.MAX_VALUE);
                }

                int preferredHeight = comp.getPreferredSize().height;
                rowHeight = Math.max(rowHeight, preferredHeight);
            }

            if (table.getRowHeight(row) != rowHeight) {
                table.setRowHeight(row, rowHeight);
            }
        }
    }
}