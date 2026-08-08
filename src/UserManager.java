import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Base64;

public class UserManager {
    private static final String USERS_FILE = "users.dat";

    // Дефолтные пользователи (пароли хранятся в зашифрованном / закодированном виде)
    public static List<User> loadUsers() {
        File file = new File(USERS_FILE);
        if (!file.exists()) {
            List<User> defaultUsers = new ArrayList<>();
            defaultUsers.add(new User("manager", encrypt("admin123"), User.Role.QUALITY_MANAGER, "Главный Менеджер СМК"));
            defaultUsers.add(new User("operator", encrypt("op123"), User.Role.OPERATOR, "Оператор Лаборатории"));
            defaultUsers.add(new User("lab", encrypt("lab123"), User.Role.LABORATORY_ASSISTANT, "Сотрудник ИЛ (Лаборант)"));
            saveUsers(defaultUsers);
            return defaultUsers;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            @SuppressWarnings("unchecked")
            List<User> users = (List<User>) ois.readObject();
            return users;
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void saveUsers(List<User> users) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USERS_FILE))) {
            oos.writeObject(users);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean checkPasswordByUsername(String username, String rawPassword) {
        List<User> users = loadUsers();
        for (User u : users) {
            if (u.getUsername().equalsIgnoreCase(username)) {
                return u.getPassword().equals(encrypt(rawPassword));
            }
        }
        return false;
    }

    public static void updatePassword(User.Role role, String newPassword) {
        List<User> users = loadUsers();
        for (User u : users) {
            if (u.getRole() == role) {
                // Устанавливаем зашифрованный пароль
                u.setPassword(encrypt(newPassword));
                break;
            }
        }
        saveUsers(users);
    }

    public static boolean checkPassword(User.Role role, String rawPassword) {
        List<User> users = loadUsers();
        for (User u : users) {
            if (u.getRole() == role) {
                return u.getPassword().equals(encrypt(rawPassword));
            }
        }
        return false;
    }

    // Простое обратимое шифрование (Base64 с примесью сдвига для примера)
    public static String encrypt(String data) {
        if (data == null) return "";
        byte[] encoded = Base64.getEncoder().encode(data.getBytes());
        return new String(encoded);
    }

    public static String decrypt(String encryptedData) {
        try {
            if (encryptedData == null) return "";
            byte[] decoded = Base64.getDecoder().decode(encryptedData.getBytes());
            return new String(decoded);
        } catch (Exception e) {
            return "";
        }
    }
}