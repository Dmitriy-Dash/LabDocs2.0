import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.util.regex.Pattern;

/**
 * Класс, отвечающий за логику фильтрации строк (быстрый поиск).
 */
public class DocumentFilterManager {

    private final JTextField searchField;
    private final TableRowSorter<DefaultTableModel>[] tableSorters;
    private final JTabbedPane tabbedPane;

    public DocumentFilterManager(JTextField searchField, TableRowSorter<DefaultTableModel>[] tableSorters, JTabbedPane tabbedPane) {
        this.searchField = searchField;
        this.tableSorters = tableSorters;
        this.tabbedPane = tabbedPane;

        initListeners();
    }

    private void initListeners() {
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
    }

    public void applyFilter() {
        int activeTabIndex = tabbedPane.getSelectedIndex();
        if (activeTabIndex < 0 || tableSorters == null || tableSorters[activeTabIndex] == null) {
            return;
        }

        TableRowSorter<DefaultTableModel> sorter = tableSorters[activeTabIndex];
        String text = searchField.getText().trim();

        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
    }
}