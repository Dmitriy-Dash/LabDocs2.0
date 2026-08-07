import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static final String FILE_NAME = "smk_documents.dat";

    /**
     * Сохранение списка документов в бинарный файл
     */
    public static synchronized void saveDocuments(List<Document> documents) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(FILE_NAME))) {
            oos.writeObject(documents);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Загрузка списка документов из файла
     */
    @SuppressWarnings("unchecked")
    public static synchronized List<Document> loadDocuments() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (List<Document>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}