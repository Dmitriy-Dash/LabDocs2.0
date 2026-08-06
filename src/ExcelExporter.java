import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableModel;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Класс-сервис для экспорта данных из JTable в файл Excel (.xlsx).
 */
public class ExcelExporter {

    public static void exportTableToExcel(JTable table, String tabTitle, JFrame parentFrame) {
        if (table == null || table.getRowCount() == 0) {
            JOptionPane.showMessageDialog(parentFrame,
                    "В текущей вкладке нет данных для экспорта!",
                    "Предупреждение",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Диалог выбора места и имени файла для сохранения
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить данные в Excel");
        fileChooser.setSelectedFile(new File("Документы_СМК_" + sanitizeFileName(tabTitle) + ".xlsx"));
        fileChooser.setFileFilter(new FileNameExtensionFilter("Файлы Excel (*.xlsx)", "xlsx"));

        int userSelection = fileChooser.showSaveDialog(parentFrame);
        if (userSelection != JFileChooser.APPROVE_OPTION) {
            return; // Пользователь отменил сохранение
        }

        File fileToSave = fileChooser.getSelectedFile();
        if (!fileToSave.getName().toLowerCase().endsWith(".xlsx")) {
            fileToSave = new File(fileToSave.getAbsolutePath() + ".xlsx");
        }

        // Создаем Excel книга
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Документы СМК");

            // Стиль для заголовка (темно-синий фон, белый жирный текст)
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Стиль для ячеек с тонкими границами
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);
            dataStyle.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());

            TableModel model = table.getModel();

            // 1. Записываем названия колонок
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < model.getColumnCount(); col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(model.getColumnName(col));
                cell.setCellStyle(headerStyle);
            }

            // 2. Записываем строки данных (с учетом текущей сортировки/фильтрации таблицы)
            for (int row = 0; row < table.getRowCount(); row++) {
                Row excelRow = sheet.createRow(row + 1);
                for (int col = 0; col < table.getColumnCount(); col++) {
                    Cell cell = excelRow.createCell(col);
                    Object value = table.getValueAt(row, col);

                    if (value != null) {
                        cell.setCellValue(value.toString());
                    } else {
                        cell.setCellValue("");
                    }
                    cell.setCellStyle(dataStyle);
                }
            }

            // 3. Автоматическая подгонка ширины колонок
            for (int col = 0; col < model.getColumnCount(); col++) {
                sheet.autoSizeColumn(col);
                // Добавляем немного отступа, чтобы текст не сжимался
                int currentWidth = sheet.getColumnWidth(col);
                sheet.setColumnWidth(col, currentWidth + 1000);
            }

            // Записываем в файл
            try (FileOutputStream out = new FileOutputStream(fileToSave)) {
                workbook.write(out);
            }

            JOptionPane.showMessageDialog(parentFrame,
                    "Данные успешно сохранены в файл:\n" + fileToSave.getAbsolutePath(),
                    "Успех",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (IOException e) {
            JOptionPane.showMessageDialog(parentFrame,
                    "Ошибка при сохранении файла Excel:\n" + e.getMessage(),
                    "Ошибка",
                    JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private static String sanitizeFileName(String name) {
        return name.replaceAll("[^a-zA-Z0-9а-яА-Я._-]", "_");
    }
}