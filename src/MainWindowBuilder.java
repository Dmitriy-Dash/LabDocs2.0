import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class MainWindowBuilder {

    public static JPanel createTopPanel(JButton[] buttons, JTextField searchField, JPanel warningDashboardPanel, User currentUser) {
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));

        // Панель для основных действий
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        for (JButton btn : buttons) {
            buttonsPanel.add(btn);
        }

        // Панель для бэкапа и поиска
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        JButton btnRestore = buttons[buttons.length - 1]; // Последняя кнопка - восстановление (или можно передать отдельно)

        // Пересортируем порядок для нижней панели, если нужно
        searchPanel.add(buttons[3]); // Предположим, здесь кнопка бэкапа
        searchPanel.add(new JLabel("Быстрый поиск:"));
        searchPanel.add(searchField);

        topPanel.add(buttonsPanel);
        topPanel.add(warningDashboardPanel);

        return topPanel;
    }

    public static JPanel createWarningDashboard(JLabel warningTitleLabel, JLabel warningCounterLabel, JButton filterWarningButton) {
        JPanel warningDashboardPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        warningDashboardPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 5, 5, 5),
                BorderFactory.createLineBorder(new Color(220, 180, 100), 1, true)
        ));
        warningDashboardPanel.setBackground(new Color(255, 255, 220));

        warningDashboardPanel.add(warningTitleLabel);
        warningDashboardPanel.add(warningCounterLabel);
        warningDashboardPanel.add(filterWarningButton);

        return warningDashboardPanel;
    }

    public static void initializeTabs(JTabbedPane tabbedPane, JTable[] tables, DefaultTableModel[] tableModels, TableRowSorter<DefaultTableModel>[] tableSorters) {
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

            JScrollPane scrollPane = new JScrollPane(tables[i]);
            JPanel tabContentPanel = new JPanel(new BorderLayout(5, 5));
            JLabel descLabel = new JLabel("<html><i style='color:gray;'>" + tabDescriptions[i] + "</i></html>");
            descLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            descLabel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            tabContentPanel.add(descLabel, BorderLayout.NORTH);
            tabContentPanel.add(scrollPane, BorderLayout.CENTER);
            tabbedPane.addTab(tabShortTitles[i], tabContentPanel);
        }
    }

    public static JPanel createBottomPanel(JLabel statusLabelLeft, JLabel statusLabelRight) {
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEtchedBorder());
        statusLabelLeft.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        statusLabelRight.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        bottomPanel.add(statusLabelLeft, BorderLayout.WEST);
        bottomPanel.add(statusLabelRight, BorderLayout.EAST);
        return bottomPanel;
    }
}