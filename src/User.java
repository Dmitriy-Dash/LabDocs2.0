import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Role implements Serializable {
        QUALITY_MANAGER("Менеджер по качеству"),
        OPERATOR("Оператор"),
        LABORATORY_ASSISTANT("Лаборант");

        private final String displayName;

        Role(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private String username;
    private String password;
    private Role role;
    private String fullName;

    public User(String username, String password, Role role, String fullName) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
}