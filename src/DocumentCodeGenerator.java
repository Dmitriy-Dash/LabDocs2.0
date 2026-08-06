import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Сервис для генерации уникальных шифров документов СМК.
 * Формат шифра: УР_X-Y (где X — уровень СМК, Y — порядковый номер).
 */
public class DocumentCodeGenerator {

    /**
     * Вариант 1: Берет максимальный существующий номер + 1.
     * Пример: при наличии УР_1-1 и УР_1-2, после удаления УР_1-1 новый документ получит УР_1-3.
     */
    public static String generateNextMaxCode(int level, List<Document> documentList) {
        int maxIndex = 0;
        String prefix = "УР" + level + "-";

        if (documentList != null) {
            for (Document doc : documentList) {
                if (doc.getSmkLevel() == level) {
                    String id = doc.getId();
                    if (id != null && id.startsWith(prefix)) {
                        try {
                            // Извлекаем порядковое число после "УР_X-"
                            int index = Integer.parseInt(id.substring(prefix.length()).trim());
                            if (index > maxIndex) {
                                maxIndex = index;
                            }
                        } catch (NumberFormatException ignored) {
                            // Игнорируем ID с нетиповым форматом
                        }
                    }
                }
            }
        }

        return prefix + String.format("%02d", maxIndex + 1);
    }

    /**
     * Вариант 2: Заполняет пропуски.
     * Пример: при наличии УР_1-1 и УР_1-2, после удаления УР_1-1 новый документ получит УР_1-1.
     */
    public static String generateFirstAvailableCode(int level, List<Document> documentList) {
        Set<Integer> existingIndexes = new HashSet<>();
        String prefix = "УР" + level + "-";

        if (documentList != null) {
            for (Document doc : documentList) {
                if (doc.getSmkLevel() == level) {
                    String id = doc.getId();
                    if (id != null && id.startsWith(prefix)) {
                        try {
                            int index = Integer.parseInt(id.substring(prefix.length()).trim());
                            existingIndexes.add(index);
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }

        int nextIndex = 1;
        while (existingIndexes.contains(nextIndex)) {
            nextIndex++;
        }

        // Используем nextIndex (с ведением нуля %02d или без: prefix + nextIndex)
        return prefix + String.format("%02d", nextIndex);
    }
}