import javax.swing.*;
import java.awt.*;
// --- NEW ---
import javax.imageio.ImageIO;
import java.io.File;

// This is the new login window that will appear on startup.
public class LoginFrame extends JFrame {

    // --- (Colors are unchanged) ---
    private static final Color COLOR_BG_DARK = new Color(44, 62, 80);
    private static final Color COLOR_BG_LIGHT = new Color(236, 240, 241);
    private static final Color COLOR_TEXT_LIGHT = new Color(255, 255, 255);
    private static final Color COLOR_ACCENT_BLUE = new Color(52, 152, 219);
    private static final Color COLOR_ACCENT_GREEN = new Color(46, 204, 113);

    // --- MODIFIED ---
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 16);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);

    private DatabaseHelper dbHelper;
    private JTextField usernameField;
    private JPasswordField passwordField;

    // --- NEW ---
    private Image backgroundImage;

    public LoginFrame(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;

        // --- NEW ---
        // Load the background image
        try {
            backgroundImage = ImageIO.read(new File("background.jpg"));
        } catch (Exception e) {
            System.out.println("Could not load background.jpg. Using solid color.");
            backgroundImage = null;
        }

        setTitle("News Summarizer - Login");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // --- MODIFIED ---
        // Main panel now draws the background image
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (backgroundImage != null) {
                    // Draw the image scaled to fit the panel
                    g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
                } else {
                    // Fallback to solid color if image fails to load
                    g.setColor(COLOR_BG_LIGHT);
                    g.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // --- Title ---
        JLabel titleLabel = new JLabel("Login or Register", SwingConstants.CENTER);
        titleLabel.setFont(FONT_TITLE); // --- MODIFIED ---
        titleLabel.setForeground(COLOR_TEXT_LIGHT); // --- MODIFIED --- (White text)
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // --- Fields Panel ---
        JPanel fieldsPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        fieldsPanel.setOpaque(false); // --- MODIFIED --- (Make transparent)

        JLabel userLabel = new JLabel("Username:");
        userLabel.setFont(FONT_BODY);
        userLabel.setForeground(COLOR_TEXT_LIGHT); // --- MODIFIED --- (White text)
        usernameField = new JTextField();
        usernameField.setFont(FONT_BODY);

        JLabel passLabel = new JLabel("Password:");
        passLabel.setFont(FONT_BODY);
        passLabel.setForeground(COLOR_TEXT_LIGHT); // --- MODIFIED --- (White text)
        passwordField = new JPasswordField();
        passwordField.setFont(FONT_BODY);

        fieldsPanel.add(userLabel);
        fieldsPanel.add(usernameField);
        fieldsPanel.add(passLabel);
        fieldsPanel.add(passwordField);
        mainPanel.add(fieldsPanel, BorderLayout.CENTER);

        // --- Button Panel ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
        buttonPanel.setOpaque(false); // --- MODIFIED --- (Make transparent)

        JButton loginButton = new JButton("Login");
        loginButton.setFont(FONT_BUTTON);
        loginButton.setBackground(COLOR_ACCENT_BLUE);
        loginButton.setForeground(COLOR_TEXT_LIGHT);

        JButton registerButton = new JButton("Register");
        registerButton.setFont(FONT_BUTTON);
        registerButton.setBackground(COLOR_ACCENT_GREEN);
        registerButton.setForeground(COLOR_TEXT_LIGHT);

        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        // --- (Actions are unchanged) ---
        loginButton.addActionListener(e -> onLogin());
        registerButton.addActionListener(e -> onRegister());

        add(mainPanel);
    }

    private void onLogin() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username and password.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String storedHash = dbHelper.getPasswordHashForUser(username);
        if (storedHash == null) {
            JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (PasswordUtils.verifyPassword(password, storedHash)) {
            JOptionPane.showMessageDialog(this, "Login successful!", "Success", JOptionPane.INFORMATION_MESSAGE);
            NewsSummarizerApp mainApp = new NewsSummarizerApp(dbHelper);
            mainApp.setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Invalid username or password.", "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onRegister() {
        String username = usernameField.getText();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a username and password.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String passwordHash = PasswordUtils.generateSecurePassword(password);
        if (dbHelper.registerUser(username, passwordHash)) {
            JOptionPane.showMessageDialog(this, "Registration successful! You can now log in.", "Success", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Username is already taken.", "Registration Failed", JOptionPane.ERROR_MESSAGE);
        }
    }
}