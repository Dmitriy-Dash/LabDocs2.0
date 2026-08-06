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
        table.setDefaultRenderer(Object.class, new WordWrapCellRenderer());
        table.setRowHeight(25);

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

        colModel.addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
            @Override public void columnMarginChanged(javax.swing.event.ChangeEvent e) {
                updateRowHeights(table);
            }
            @Override public void columnAdded(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnRemoved(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnMoved(javax.swing.event.TableColumnModelEvent e) {}
            @Override public void columnSelectionChanged(javax.swing.event.ListSelectionEvent e) {}
        });

        return table;
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
        if (table.getRowCount() == 0) return;

        for (int row = 0; row < table.getRowCount(); row++) {
            int maxRowHeight = 25;

            for (int col = 0; col < table.getColumnCount(); col++) {
                TableCellRenderer renderer = table.getCellRenderer(row, col);
                Component comp = table.prepareRenderer(renderer, row, col);

                int columnWidth = table.getColumnModel().getColumn(col).getWidth();

                if (columnWidth > 0) {
                    comp.setSize(new Dimension(columnWidth, 1000));
                    int preferredHeight = comp.getPreferredSize().height;

                    if (preferredHeight > maxRowHeight) {
                        maxRowHeight = preferredHeight;
                    }
                }
            }

            maxRowHeight = Math.min(maxRowHeight, 150);

            if (table.getRowHeight(row) != maxRowHeight) {
                table.setRowHeight(row, maxRowHeight);
            }
        }
    }
}