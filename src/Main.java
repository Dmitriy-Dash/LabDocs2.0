import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame dummyFrame = new JFrame();
            LoginDialog loginDialog = new LoginDialog(dummyFrame);
            loginDialog.setVisible(true);

            User user = loginDialog.getAuthenticatedUser();
            if (user != null) {
                dummyFrame.dispose();
                new MainWindow(user).setVisible(true);
            } else {
                System.exit(0);
            }
        });
    }
}