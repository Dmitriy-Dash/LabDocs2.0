import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class Document implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String title;
    private String date;
    private String status;
    private int smkLevel;
    private String origin;
    private String version;
    private String storageOriginal;
    private String storageCopies;
    private int copyCount;
    private String actualizationDate;
    private boolean isDeleted = false;

    public Document(String id, String title, String date, String status, int smkLevel,
                    String origin, String version, String storageOriginal, String storageCopies,
                    int copyCount, String actualizationDate) {
        this.id = id;
        this.title = title;
        this.date = date;
        this.status = status;
        this.smkLevel = smkLevel;
        this.origin = origin;
        this.storageOriginal = storageOriginal;
        this.storageCopies = storageCopies;
        this.copyCount = copyCount;

        // Контроль логики версии по уровню СМК
        if (smkLevel >= 1 && smkLevel <= 3) {
            this.version = version;
        } else {
            this.version = "Не требуется";
        }

        // Контроль логики даты актуализации
        if (actualizationDate != null && !actualizationDate.trim().isEmpty()) {
            this.actualizationDate = actualizationDate;
        } else {
            this.actualizationDate = "Не требуется";
        }
    }

    // Геттеры
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getDate() { return date; }
    public String getStatus() { return status; }
    public int getSmkLevel() { return smkLevel; }
    public String getOrigin() { return origin; }
    public String getVersion() { return version; }
    public String getStorageOriginal() { return storageOriginal; }
    public String getStorageCopies() { return storageCopies; }
    public int getCopyCount() { return copyCount; }
    public String getActualizationDate() { return actualizationDate; }
    public boolean isDeleted() { return isDeleted; }

    // Сеттеры
    public void setId(String id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDate(String date) { this.date = date; }
    public void setStatus(String status) { this.status = status; }
    public void setOrigin(String origin) { this.origin = origin; }
    public void setStorageOriginal(String storageOriginal) { this.storageOriginal = storageOriginal; }
    public void setStorageCopies(String storageCopies) { this.storageCopies = storageCopies; }
    public void setCopyCount(int copyCount) { this.copyCount = copyCount; }
    public void setDeleted(boolean deleted) {isDeleted = deleted;}

    public String getActualizationStatus() {
        if (actualizationDate == null || "Не требуется".equalsIgnoreCase(actualizationDate)) {
            return "OK";
        }

        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
            LocalDate actDate = LocalDate.parse(actualizationDate, formatter);

            // Логика: следующая актуализация = дата + 365 дней
            LocalDate nextActDate = actDate.plusDays(365);
            LocalDate today = LocalDate.now();

            long daysUntil = ChronoUnit.DAYS.between(today, nextActDate);

            if (daysUntil < 0) {
                return "EXPIRED"; // Красный
            } else if (daysUntil <= 30) {
                return "WARNING"; // Желтый
            }
        } catch (Exception e) {
            return "ERROR";
        }
        return "OK";
    }

    public void setActualizationDate(String actualizationDate) {
        if (actualizationDate != null && !actualizationDate.trim().isEmpty()) {
            this.actualizationDate = actualizationDate;
        } else {
            this.actualizationDate = "Не требуется";
        }
    }

    public void setSmkLevel(int smkLevel) {
        this.smkLevel = smkLevel;
        if (smkLevel > 3) {
            this.version = "Не требуется";
        }
    }

    public void setVersion(String version) {
        if (this.smkLevel >= 1 && this.smkLevel <= 3) {
            this.version = version;
        } else {
            this.version = "Не требуется";
        }
    }

    public String getSmkLevelDescription() {
        switch (smkLevel) {
            case 1: return "1 уровень (РК и Политика)";
            case 2: return "2 уровень (Процедуры, СОП)";
            case 3: return "3 уровень (Внутренние акты, графики)";
            case 4: return "4 уровень (Внешние ГОСТы, законы)";
            case 5: return "5 уровень (Свидетельства, записи)";
            default: return "Неизвестный уровень";
        }
    }

    /**
     * Формирует подробный отчет об изменениях между текущей и новой версией документа
     */
    public String getDiff(Document newDoc) {
        if (newDoc == null) return "Новые данные отсутствуют";

        List<String> changes = new ArrayList<>();

        if (!Objects.equals(this.id, newDoc.getId())) {
            changes.add("ID: '" + this.id + "' -> '" + newDoc.getId() + "'");
        }
        if (!Objects.equals(this.title, newDoc.getTitle())) {
            changes.add("Название: '" + this.title + "' -> '" + newDoc.getTitle() + "'");
        }
        if (this.smkLevel != newDoc.getSmkLevel()) {
            changes.add("Уровень: " + this.smkLevel + " -> " + newDoc.getSmkLevel());
        }
        if (!Objects.equals(this.version, newDoc.getVersion())) {
            changes.add("Версия: '" + this.version + "' -> '" + newDoc.getVersion() + "'");
        }
        if (!Objects.equals(this.origin, newDoc.getOrigin())) {
            changes.add("Происхождение: '" + this.origin + "' -> '" + newDoc.getOrigin() + "'");
        }
        if (!Objects.equals(this.date, newDoc.getDate())) {
            changes.add("Дата рег.: '" + this.date + "' -> '" + newDoc.getDate() + "'");
        }
        if (!Objects.equals(this.actualizationDate, newDoc.getActualizationDate())) {
            changes.add("Дата актуал.: '" + this.actualizationDate + "' -> '" + newDoc.getActualizationDate() + "'");
        }
        if (!Objects.equals(this.storageOriginal, newDoc.getStorageOriginal())) {
            changes.add("Хранение оригинала: '" + this.storageOriginal + "' -> '" + newDoc.getStorageOriginal() + "'");
        }
        if (!Objects.equals(this.storageCopies, newDoc.getStorageCopies())) {
            changes.add("Хранение копий: '" + this.storageCopies + "' -> '" + newDoc.getStorageCopies() + "'");
        }
        if (this.copyCount != newDoc.getCopyCount()) {
            changes.add("Кол-во копий: " + this.copyCount + " -> " + newDoc.getCopyCount());
        }
        if (!Objects.equals(this.status, newDoc.getStatus())) {
            changes.add("Статус: '" + this.status + "' -> '" + newDoc.getStatus() + "'");
        }

        return changes.isEmpty() ? "Изменений нет" : String.join("; ", changes);
    }
}

