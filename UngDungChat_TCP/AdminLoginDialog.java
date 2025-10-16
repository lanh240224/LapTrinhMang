package UngDungChat_TCP;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class AdminLoginDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton registerButton;
    private JButton cancelButton;
    private AdminManager adminManager;
    private boolean loginSuccessful = false;
    private String loggedInUsername;
    
    public AdminLoginDialog(JFrame parent) {
        super(parent, "Admin Login", true);
        this.adminManager = new AdminManager();
        initializeGUI();
        setupEventListeners();
    }
    
    private void initializeGUI() {
        setSize(450, 350);
        setLocationRelativeTo(getParent());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        
        // Thiết lập Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            System.err.println("Could not set Look and Feel: " + e.getMessage());
        }
        
        // Background khoa học - màu xám nhạt
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(245, 245, 245));
        mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));
        
        // Header khoa học
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(245, 245, 245));
        headerPanel.setBorder(BorderFactory.createMatteBorder(0, 0, 2, 0, new Color(200, 200, 200)));
        
        JLabel titleLabel = new JLabel("Server Administrator Login");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(50, 50, 50));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        JLabel subtitleLabel = new JLabel("TCP Chat Server Management System");
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setForeground(new Color(100, 100, 100));
        subtitleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        
        headerPanel.add(titleLabel, BorderLayout.NORTH);
        headerPanel.add(subtitleLabel, BorderLayout.SOUTH);
        
        // Form panel khoa học
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(new Color(245, 245, 245));
        formPanel.setBorder(new EmptyBorder(20, 0, 20, 0));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Username field
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameLabel.setForeground(new Color(70, 70, 70));
        formPanel.add(usernameLabel, gbc);
        
        gbc.gridy = 1;
        usernameField = new JTextField(20);
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameField.setPreferredSize(new Dimension(280, 35));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        usernameField.setBackground(Color.WHITE);
        usernameField.setForeground(new Color(50, 50, 50));
        formPanel.add(usernameField, gbc);
        
        // Password field
        gbc.gridy = 2;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordLabel.setForeground(new Color(70, 70, 70));
        formPanel.add(passwordLabel, gbc);
        
        gbc.gridy = 3;
        passwordField = new JPasswordField(20);
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordField.setPreferredSize(new Dimension(280, 35));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        passwordField.setBackground(Color.WHITE);
        passwordField.setForeground(new Color(50, 50, 50));
        formPanel.add(passwordField, gbc);
        
        // Button panel khoa học
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(new Color(245, 245, 245));
        buttonPanel.setBorder(new EmptyBorder(10, 0, 0, 0));
        
        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        loginButton.setBackground(new Color(70, 130, 180));
        loginButton.setForeground(Color.BLACK);
        loginButton.setFocusPainted(false);
        loginButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 100, 150), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        loginButton.setPreferredSize(new Dimension(90, 35));
        
        registerButton = new JButton("Register");
        registerButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        registerButton.setBackground(new Color(105, 105, 105));
        registerButton.setForeground(Color.BLACK);
        registerButton.setFocusPainted(false);
        registerButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(85, 85, 85), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        registerButton.setPreferredSize(new Dimension(100, 35));
        
        cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cancelButton.setBackground(new Color(160, 160, 160));
        cancelButton.setForeground(Color.BLACK);
        cancelButton.setFocusPainted(false);
        cancelButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(140, 140, 140), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        cancelButton.setPreferredSize(new Dimension(90, 35));
        
        buttonPanel.add(loginButton);
        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);
        
        // Add hover effects khoa học
        addHoverEffect(loginButton, new Color(50, 100, 150));
        addHoverEffect(registerButton, new Color(85, 85, 85));
        addHoverEffect(cancelButton, new Color(140, 140, 140));
        
        mainPanel.add(headerPanel, BorderLayout.NORTH);
        mainPanel.add(formPanel, BorderLayout.CENTER);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(mainPanel);
        
        // Focus on username field
        SwingUtilities.invokeLater(() -> usernameField.requestFocus());
    }
    
    private void addHoverEffect(JButton button, Color hoverColor) {
        Color originalColor = button.getBackground();
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(hoverColor);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(originalColor);
            }
        });
    }
    
    private void setupEventListeners() {
        loginButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                performLogin();
            }
        });
        
        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showRegisterDialog();
            }
        });
        
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
        
        // Enter key listeners
        KeyListener enterKeyListener = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    performLogin();
                }
            }
            
            @Override
            public void keyReleased(KeyEvent e) {}
        };
        
        usernameField.addKeyListener(enterKeyListener);
        passwordField.addKeyListener(enterKeyListener);
    }
    
    private void performLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        
        if (username.isEmpty() || password.isEmpty()) {
            showError("Please enter both username and password.");
            return;
        }
        
        if (adminManager.authenticate(username, password)) {
            loginSuccessful = true;
            loggedInUsername = username;
            dispose();
        } else {
            showError("Invalid username or password.");
            passwordField.setText("");
        }
    }
    
    private void showRegisterDialog() {
        JDialog registerDialog = new JDialog(this, "Register New Admin", true);
        registerDialog.setSize(400, 300);
        registerDialog.setLocationRelativeTo(this);
        registerDialog.setResizable(false);
        
        // Background khoa học
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(new Color(245, 245, 245));
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        
        // Title
        gbc.gridx = 0; gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JLabel titleLabel = new JLabel("Create New Admin Account");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        titleLabel.setForeground(new Color(50, 50, 50));
        titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(titleLabel, gbc);
        
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        
        // Username
        gbc.gridy = 1;
        JLabel usernameLabel = new JLabel("Username:");
        usernameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        usernameLabel.setForeground(new Color(70, 70, 70));
        panel.add(usernameLabel, gbc);
        
        gbc.gridy = 2;
        JTextField regUsernameField = new JTextField(18);
        regUsernameField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        regUsernameField.setPreferredSize(new Dimension(250, 35));
        regUsernameField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        regUsernameField.setBackground(Color.WHITE);
        regUsernameField.setForeground(new Color(50, 50, 50));
        panel.add(regUsernameField, gbc);
        
        // Password
        gbc.gridy = 3;
        JLabel passwordLabel = new JLabel("Password:");
        passwordLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        passwordLabel.setForeground(new Color(70, 70, 70));
        panel.add(passwordLabel, gbc);
        
        gbc.gridy = 4;
        JPasswordField regPasswordField = new JPasswordField(18);
        regPasswordField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        regPasswordField.setPreferredSize(new Dimension(250, 35));
        regPasswordField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        regPasswordField.setBackground(Color.WHITE);
        regPasswordField.setForeground(new Color(50, 50, 50));
        panel.add(regPasswordField, gbc);
        
        // Buttons
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        buttonPanel.setBackground(new Color(245, 245, 245));
        
        JButton createButton = new JButton("Create Account");
        createButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        createButton.setBackground(new Color(70, 130, 180));
        createButton.setForeground(Color.BLACK);
        createButton.setFocusPainted(false);
        createButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(50, 100, 150), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        createButton.setPreferredSize(new Dimension(130, 35));
        
        JButton cancelRegButton = new JButton("Cancel");
        cancelRegButton.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cancelRegButton.setBackground(new Color(160, 160, 160));
        cancelRegButton.setForeground(Color.BLACK);
        cancelRegButton.setFocusPainted(false);
        cancelRegButton.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(140, 140, 140), 1),
            BorderFactory.createEmptyBorder(8, 15, 8, 15)
        ));
        cancelRegButton.setPreferredSize(new Dimension(90, 35));
        
        // Hover effects khoa học
        createButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                createButton.setBackground(new Color(50, 100, 150));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                createButton.setBackground(new Color(70, 130, 180));
            }
        });
        
        cancelRegButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                cancelRegButton.setBackground(new Color(140, 140, 140));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                cancelRegButton.setBackground(new Color(160, 160, 160));
            }
        });
        
        createButton.addActionListener(e -> {
            String username = regUsernameField.getText().trim();
            String password = new String(regPasswordField.getPassword());
            
            if (username.isEmpty() || password.isEmpty()) {
                showError("Please enter both username and password.");
                return;
            }
            
            if (adminManager.registerAdmin(username, password)) {
                JOptionPane.showMessageDialog(registerDialog, 
                    "Account created successfully!", "Success", 
                    JOptionPane.INFORMATION_MESSAGE);
                registerDialog.dispose();
            } else {
                showError("Username already exists or invalid input.");
            }
        });
        
        cancelRegButton.addActionListener(e -> registerDialog.dispose());
        
        buttonPanel.add(createButton);
        buttonPanel.add(cancelRegButton);
        panel.add(buttonPanel, gbc);
        
        registerDialog.add(panel);
        registerDialog.setVisible(true);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
    
    public boolean isLoginSuccessful() {
        return loginSuccessful;
    }
    
    public String getLoggedInUsername() {
        return loggedInUsername;
    }
}
