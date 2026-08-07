import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DataManager {
    private static final String FILE_NAME = "smk_documents.dat";
    private static final String BACKUP_DIR = "backups";

    /**
     * Сохранение списка документов с автоматическим бэкапом
     */
    public static synchronized void saveDocuments(List<Document> documents) {
        // Создаем резервную копию перед перезаписью основного файла
        createBackup();

        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(documents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Загрузка списка документов с автосозданием бэкапа перед работой
     */
    @SuppressWarnings("unchecked")
    public static synchronized List<Document> loadDocuments() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        // Делаем бэкап при старте программы перед чтением
        createBackup();

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<Document>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Создание резервной копии основного файла данных в папку backups/
     */
    public static synchronized void createBackup() {
        File sourceFile = new File(FILE_NAME);
        if (!sourceFile.exists() || sourceFile.length() == 0) {
            return; // Не делаем бэкап, если основного файла еще нет или он пуст
        }

        try {
            File backupFolder = new File(BACKUP_DIR);
            if (!backupFolder.exists()) {
                backupFolder.mkdirs(); // Создаем папку backups/, если ее нет
            }

            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmm_ss").format(new Date());
            String backupFileName = String.format("%s/backup_%s.dat", BACKUP_DIR, timeStamp);
            File backupFile = new File(backupFileName);

            // Копируем файл с заменой, если метка совпала
            Files.copy(sourceFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            System.err.println("Ошибка при создании резервной копии: " + e.getMessage());
        }
    }
}