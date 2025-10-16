package UngDungChat_TCP;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.sound.sampled.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class Client {
    private JFrame frame;              // Cửa sổ giao diện chính của client
    private JPanel chatArea;
    private JScrollPane chatScrollPane;        // Khu vực hiển thị tin nhắn và log
    private String selectedClient = null;     // Client được chọn để chat
    private java.util.List<MessageData> allMessages = new java.util.ArrayList<>(); // Lưu tất cả tin nhắn
    private java.util.Map<String, Integer> unreadCounts = new java.util.HashMap<>(); // Số tin nhắn chưa đọc cho mỗi client
    private JTextField inputField;     // Ô nhập tin nhắn từ client
    private JButton sendButton;        // Nút gửi tin nhắn
    private JLabel statusLabel;        // Label hiển thị trạng thái kết nối
    private Socket socket;             // Socket kết nối với server
    private PrintWriter out;           // Dòng xuất để gửi tin nhắn
    private BufferedReader in;         // Dòng nhập để nhận tin nhắn
    private String clientName;         // Tên của client
    private boolean historyCleared = false; // Flag để tránh hiển thị thông báo lặp lại
    private JList<RecipientItem> contactsList; // Danh sách liên hệ
    private DefaultListModel<RecipientItem> contactsModel; // Model cho danh sách liên hệ
    private JLabel userNameLabel; // Label hiển thị tên client được chọn
    private JLabel userStatusLabel; // Label hiển thị trạng thái client
    private JLabel avatarLabel; // Label hiển thị avatar
    private JPanel selectedClientPanel; // Panel hiển thị thông tin client được chọn
    
    // Voice chat variables
    private JButton voiceButton; // Nút gửi voice
    private AtomicBoolean isRecording = new AtomicBoolean(false);
    private TargetDataLine microphone;
    private AudioFormat audioFormat;
    private ByteArrayOutputStream audioBuffer;
    
    // Avatar variables
    private JButton avatarButton; // Nút thay đổi avatar
    private String currentAvatar = "😀"; // Avatar mặc định

    // Constructor khởi tạo client, tạo GUI và kết nối đến server
    public Client() {
        initializeGUI();                // Gọi phương thức để khởi tạo giao diện
        loadChatHistory();               // Tải lịch sử chat từ file
        connectToServer();              // Gọi phương thức để kết nối đến server
    }

    // Khởi tạo giao diện Swing cho client
    private void initializeGUI() {
        // Thiết lập Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            // Nếu không thể thiết lập Look and Feel, sử dụng default
            System.err.println("Could not set Look and Feel: " + e.getMessage());
        }

        frame = new JFrame("Chat App"); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        frame.setSize(900, 600);
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(new Color(248, 249, 250)); // Light theme

        // Tạo layout chính với hai cột
        frame.setLayout(new BorderLayout());
        frame.getRootPane().setBorder(new EmptyBorder(0, 0, 0, 0));

        // Panel chính với hai cột
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(new Color(248, 249, 250));
        
        // Panel bên trái - danh sách liên hệ
        JPanel contactsPanel = createContactsPanel();
        mainPanel.add(contactsPanel, BorderLayout.WEST);
        
        // Panel bên phải - chat
        JPanel chatPanel = createChatPanel();
        mainPanel.add(chatPanel, BorderLayout.CENTER);

        frame.add(mainPanel, BorderLayout.CENTER);

        // Thiết lập sự kiện
        setupEventListeners();

        frame.setVisible(true);          // Hiển thị giao diện
    }

    private JPanel createContactsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(250, 0));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, new Color(220, 220, 220)));
        
        // Header với tên ứng dụng
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(new EmptyBorder(6, 12, 0, 12)); // Xóa khoảng cách dưới
        
        JLabel titleLabel = new JLabel("Chat App");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(new Color(50, 50, 50));
        headerPanel.add(titleLabel, BorderLayout.WEST);
        
        // Status indicator
        statusLabel = new JLabel("Connecting...");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        statusLabel.setForeground(new Color(100, 100, 100));
        headerPanel.add(statusLabel, BorderLayout.EAST);
        
        panel.add(headerPanel, BorderLayout.NORTH);
        
        // Online clients list ngay dưới Chat Application
        JPanel onlineClientsPanel = createOnlineClientsPanel();
        panel.add(onlineClientsPanel, BorderLayout.CENTER);
        
        // Contacts list
        JPanel contactsListPanel = new JPanel(new BorderLayout());
        contactsListPanel.setBackground(Color.WHITE);
        contactsListPanel.setBorder(null);
        
        // Tạo danh sách liên hệ
        contactsModel = new DefaultListModel<>();
        contactsList = new JList<>(contactsModel);
        contactsList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        contactsList.setBackground(Color.WHITE);
        contactsList.setForeground(new Color(50, 50, 50));
        contactsList.setSelectionBackground(new Color(0, 120, 215));
        contactsList.setCellRenderer(new ContactListCellRenderer());
        
        // Thêm sự kiện click để chọn client
        contactsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                RecipientItem selectedItem = contactsList.getSelectedValue();
                if (selectedItem != null) {
                    updateSelectedContact(selectedItem);
                }
            }
        });
        
        JScrollPane contactsScrollPane = new JScrollPane(contactsList);
        contactsScrollPane.setBorder(null);
        contactsScrollPane.getViewport().setBackground(Color.WHITE);
        contactsScrollPane.getVerticalScrollBar().setBackground(Color.WHITE);
        contactsScrollPane.getVerticalScrollBar().setForeground(new Color(150, 150, 150));
        
        contactsListPanel.add(contactsScrollPane, BorderLayout.CENTER);
        panel.add(contactsListPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createOnlineClientsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(null);
        
        // Header "Online Clients"
        JLabel onlineLabel = new JLabel("Danh sách Clients");
        onlineLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        onlineLabel.setForeground(new Color(50, 50, 50));
        onlineLabel.setBorder(new EmptyBorder(0, 0, 0, 0)); // Xóa khoảng cách trên
        
        // Panel chứa danh sách client online
        JPanel clientsListPanel = new JPanel();
        clientsListPanel.setLayout(new BoxLayout(clientsListPanel, BoxLayout.Y_AXIS));
        clientsListPanel.setBackground(Color.WHITE);
        clientsListPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        clientsListPanel.setBorder(null);
        
        // Thông tin client được chọn (ẩn ban đầu)
        selectedClientPanel = new JPanel(new BorderLayout(5, 0));
        selectedClientPanel.setBackground(Color.WHITE);
        selectedClientPanel.setBorder(new EmptyBorder(5, 0, 5, 0));
        selectedClientPanel.setVisible(false); // Ẩn vì sẽ chỉ hiển thị ở chat header
        
        // Avatar của client được chọn
        avatarLabel = new JLabel("?");
        avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setPreferredSize(new Dimension(35, 35));
        avatarLabel.setBackground(new Color(100, 100, 100));
        avatarLabel.setOpaque(true);
        avatarLabel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        
        // Thông tin chi tiết
        JPanel userDetailsPanel = new JPanel(new BorderLayout());
        userDetailsPanel.setBackground(Color.WHITE);
        
        userNameLabel = new JLabel("Select a contact");
        userNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        userNameLabel.setForeground(new Color(50, 50, 50));
        
        userStatusLabel = new JLabel("");
        userStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        userStatusLabel.setForeground(new Color(0, 150, 0));
        
        userDetailsPanel.add(userNameLabel, BorderLayout.NORTH);
        userDetailsPanel.add(userStatusLabel, BorderLayout.SOUTH);
        
        selectedClientPanel.add(avatarLabel, BorderLayout.WEST);
        selectedClientPanel.add(userDetailsPanel, BorderLayout.CENTER);
        
        // Sử dụng GridBagLayout để kiểm soát chính xác khoảng cách
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new GridBagLayout());
        contentPanel.setBackground(Color.WHITE);
        contentPanel.setBorder(null); // Loại bỏ margin mặc định
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, 0, 0, 0); // Không có khoảng cách
        
        contentPanel.add(onlineLabel, gbc);
        
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 0, 0); // Loại bỏ hoàn toàn khoảng cách
        contentPanel.add(clientsListPanel, gbc);
        
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0); // Không có khoảng cách
        contentPanel.add(selectedClientPanel, gbc);
        
        panel.add(contentPanel, BorderLayout.CENTER);
        
        return panel;
    }


    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(248, 249, 250));
        
        // Chat header
        JPanel chatHeader = new JPanel(new BorderLayout());
        chatHeader.setBackground(Color.WHITE);
        chatHeader.setBorder(new EmptyBorder(15, 15, 15, 15));
        chatHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(220, 220, 220)));
        
        // Avatar và thông tin người chat
        JPanel userInfoPanel = new JPanel(new BorderLayout(10, 0));
        userInfoPanel.setBackground(Color.WHITE);
        
        // Avatar placeholder
        avatarLabel = new JLabel("?");
        avatarLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        avatarLabel.setForeground(Color.WHITE);
        avatarLabel.setHorizontalAlignment(SwingConstants.CENTER);
        avatarLabel.setPreferredSize(new Dimension(40, 40));
        avatarLabel.setBackground(new Color(100, 100, 100));
        avatarLabel.setOpaque(true);
        avatarLabel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        
        JPanel userDetailsPanel = new JPanel(new BorderLayout());
        userDetailsPanel.setBackground(Color.WHITE);
        
        userNameLabel = new JLabel("Select a contact");
        userNameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        userNameLabel.setForeground(new Color(50, 50, 50));
        
        userStatusLabel = new JLabel("");
        userStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        userStatusLabel.setForeground(new Color(0, 150, 0));
        
        userDetailsPanel.add(userNameLabel, BorderLayout.NORTH);
        userDetailsPanel.add(userStatusLabel, BorderLayout.SOUTH);
        
        userInfoPanel.add(avatarLabel, BorderLayout.WEST);
        userInfoPanel.add(userDetailsPanel, BorderLayout.CENTER);
        
        chatHeader.add(userInfoPanel, BorderLayout.WEST);
        
        panel.add(chatHeader, BorderLayout.NORTH);
        
        // Chat area
        JPanel chatAreaWrapper = new JPanel(new BorderLayout());
        chatAreaWrapper.setBackground(Color.WHITE);
        chatAreaWrapper.setBorder(null);
        
        chatArea = new JPanel();
        chatArea.setLayout(new GridBagLayout()); // Thay đổi sang GridBagLayout để kiểm soát tốt hơn
        chatArea.setBackground(Color.WHITE);
        chatArea.setBorder(null);
        
        // Thêm welcome message
        JPanel welcomePanel = new JPanel();
        welcomePanel.setLayout(new BorderLayout());
        welcomePanel.setBackground(Color.WHITE);
        
        JLabel welcomeLabel = new JLabel("<html><div style='color: #666; text-align: center; padding: 20px;'>" +
                                        "Welcome to Chat Application!<br>Select a contact to start chatting." +
                                        "</div></html>");
        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        chatArea.add(welcomePanel, gbc);
        
        chatScrollPane = new JScrollPane(chatArea);
        chatScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        chatScrollPane.setBorder(null);
        chatScrollPane.getViewport().setBackground(Color.WHITE);
        
        // Custom scrollbar styling
        chatScrollPane.getVerticalScrollBar().setBackground(Color.WHITE);
        chatScrollPane.getVerticalScrollBar().setForeground(new Color(150, 150, 150));
        
        // Tăng tốc độ cuộn
        chatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
        chatScrollPane.getVerticalScrollBar().setBlockIncrement(64);
        
        chatAreaWrapper.add(chatScrollPane, BorderLayout.CENTER);
        panel.add(chatAreaWrapper, BorderLayout.CENTER);
        
        // Input panel
        JPanel inputPanel = createInputPanel();
        panel.add(inputPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        panel.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 0, new Color(220, 220, 220)));
        

        // Message input field
        inputField = new JTextField();
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inputField.setBackground(Color.WHITE);
        inputField.setForeground(new Color(50, 50, 50));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200), 1),
            BorderFactory.createEmptyBorder(12, 15, 12, 15)
        ));
        setPlaceholderText(inputField, "Type your message here...");
        
        // File button với icon file
        JButton fileButton = new JButton();
        fileButton.setBackground(Color.WHITE);
        fileButton.setForeground(Color.BLACK);
        fileButton.setFocusPainted(false);
        fileButton.setPreferredSize(new Dimension(45, 45));
        fileButton.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        fileButton.setIcon(new PaperclipIcon());
        
        // Hover effect for file button
        fileButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                fileButton.setBackground(new Color(240, 240, 240));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                fileButton.setBackground(Color.WHITE);
            }
        });
        
        // File button action
        fileButton.addActionListener(e -> sendFile());
        

        
        // Voice button
        voiceButton = new JButton();
        voiceButton.setBackground(Color.WHITE);
        voiceButton.setForeground(Color.BLACK);
        voiceButton.setFocusPainted(false);
        voiceButton.setPreferredSize(new Dimension(45, 45));
        voiceButton.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        voiceButton.setIcon(new VoiceIcon());
        
        // Hover effect for voice button
        voiceButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                voiceButton.setBackground(new Color(240, 240, 240));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                voiceButton.setBackground(Color.WHITE);
            }
        });
        
        // Voice button action
        voiceButton.addActionListener(e -> toggleVoiceRecording());
        
        // Avatar button
        avatarButton = new JButton(currentAvatar);
        avatarButton.setBackground(Color.WHITE);
        avatarButton.setForeground(Color.BLACK);
        avatarButton.setFocusPainted(false);
        avatarButton.setPreferredSize(new Dimension(45, 45));
        avatarButton.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        avatarButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
        avatarButton.setHorizontalAlignment(SwingConstants.CENTER);
        avatarButton.setVerticalAlignment(SwingConstants.CENTER);
        
        // Hover effect for avatar button
        avatarButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                avatarButton.setBackground(new Color(240, 240, 240));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                avatarButton.setBackground(Color.WHITE);
            }
        });
        
        // Avatar button action
        avatarButton.addActionListener(e -> showAvatarPicker());
        
        // Send button với hình tam giác màu đen
        sendButton = new JButton();
        sendButton.setBackground(Color.WHITE); // Nền trắng
        sendButton.setForeground(Color.BLACK);
        sendButton.setFocusPainted(false);
        sendButton.setPreferredSize(new Dimension(45, 45));
        sendButton.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        sendButton.setIcon(new BlackTriangleIcon());
        
        // Hover effect for send button
        sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                sendButton.setBackground(new Color(240, 240, 240));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                sendButton.setBackground(Color.WHITE);
            }
        });
        
        // Layout
        JPanel center = new JPanel(new BorderLayout(15, 0));
        center.setBackground(Color.WHITE);
        center.add(inputField, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.add(avatarButton);
        buttonPanel.add(voiceButton);
        buttonPanel.add(fileButton);
        buttonPanel.add(sendButton);

        panel.add(center, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.EAST);
        
        return panel;
    }

    private void setupEventListeners() {
        sendButton.addActionListener(new ActionListener() { 
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();           // Gửi tin nhắn đến tất cả client qua server
            }
        });

        inputField.addKeyListener(new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {}
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
            @Override
            public void keyReleased(KeyEvent e) {}
        });
    }

    // Kết nối đến server và xử lý luồng nhận tin nhắn
    private void connectToServer() {
        clientName = JOptionPane.showInputDialog(frame, "Enter your name:"); 
        if (clientName == null || clientName.trim().isEmpty()) { 
            clientName = "Anonymous_" + System.currentTimeMillis(); // Tạo tên duy nhất nếu trống
        }

        frame.setTitle("💬 " + clientName + " - Modern Chat");      

        try {
            socket = new Socket("localhost", 5000);  
            statusLabel.setText("Connected");
            statusLabel.setForeground(new Color(0, 200, 0));
                addSystemMessage("✅ Connected to server successfully!");

            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true); 
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8")); 

            out.println(clientName);                       

            new Thread(new Runnable() {              
                @Override
                public void run() {
                    String message;
                    try {
                        while ((message = in.readLine()) != null) { 
                            final String finalMessage = message;
                            System.out.println("Client received: " + finalMessage); // Debug log
                            
                            if (message.startsWith("[History]")) { 
                                // Bỏ qua lịch sử từ server vì Client đã tự load
                                // SwingUtilities.invokeLater(() -> {
                                //     addSystemMessage(finalMessage.substring(9)); 
                                // });
                            } else if (message.startsWith("clients_status:")) { // Cập nhật danh sách người nhận + trạng thái
                                String list = message.substring("clients_status:".length());
                                String[] entries = list.isEmpty() ? new String[0] : list.split(",");
                                SwingUtilities.invokeLater(() -> {
                                    contactsModel.clear();
                                    java.util.Set<String> seen = new java.util.HashSet<>();
                                    for (String e : entries) {
                                        int bar = e.indexOf('|');
                                        if (bar > -1) {
                                            String name = e.substring(0, bar).trim();
                                            String status = e.substring(bar + 1).trim();
                                            if (!name.isEmpty() && !name.equals(clientName) && !seen.contains(name)) {
                                                seen.add(name);
                                                boolean online = "online".equalsIgnoreCase(status);
                                                RecipientItem item = new RecipientItem(name, online);
                                                contactsModel.addElement(item);
                                                
                                            }
                                        }
                                    }
                                });
                            } else if (message.equals("[History Cleared]")) { 
                                if (!historyCleared) { // Chỉ hiển thị thông báo một lần
                                    historyCleared = true;
                                    clearChatHistoryFile(); // Xóa file lịch sử
                                    allMessages.clear(); // Xóa tin nhắn trong memory
                                    SwingUtilities.invokeLater(() -> {
                                        // Clear chat area and show welcome message
                                        chatArea.removeAll();
                                        JPanel welcomePanel = new JPanel();
                                        welcomePanel.setLayout(new BorderLayout());
                                        welcomePanel.setBackground(Color.WHITE);
                                        
                                        JLabel welcomeLabel = new JLabel("<html><div style='color: #666; text-align: center; padding: 20px;'>" +
                                                                        "Welcome to Chat Application!<br>Select a contact to start chatting." +
                                                                        "</div></html>");
                                        welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
                                        welcomePanel.add(welcomeLabel, BorderLayout.CENTER);
                                        chatArea.add(welcomePanel);
                                        chatArea.revalidate();
                                        chatArea.repaint();
                                    });
                                }
                            } else if (message.startsWith("Error:")) { 
                                SwingUtilities.invokeLater(() -> {
                                    addSystemMessage("❌ " + finalMessage);
                                });
                            } else if (message.startsWith("Server:")) { // Chỉ hiển thị tin nhắn từ server
                                SwingUtilities.invokeLater(() -> {
                                    addSystemMessage("🖥️ " + finalMessage);
                                });
                            } else if (message.startsWith("from:")) { // Tin nhắn trực tiếp từ client khác
                                // Định dạng: from:<sender>:<content>
                                int first = message.indexOf(':');
                                int second = message.indexOf(':', first + 1);
                                if (second > -1) {
                                    String sender = message.substring(first + 1, second);
                                    String content = message.substring(second + 1);
                                    SwingUtilities.invokeLater(() -> {
                                        // Lưu tin nhắn vào danh sách với timestamp đến phút
                                        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
                                        String timestamp = timeFormat.format(new java.util.Date());
                                        MessageData msgData = new MessageData(sender, clientName, content, timestamp, false);
                                        allMessages.add(msgData);
                                        saveMessageToFile(msgData); // Lưu vào file
                                        
                                        // Hiển thị tin nhắn mới nếu đang chat với người gửi
                                        if (selectedClient != null && selectedClient.equals(sender)) {
                                            displayMessagesForClient(selectedClient);
                                        } else {
                                            // Tăng số thông báo chưa đọc nếu không đang chat với người gửi
                                            incrementUnreadCount(sender);
                                        }
                                    });
                                }
                            } else if (message.startsWith("file:")) { // Nhận file từ client khác
                                // Định dạng: file:<sender>:<fileName>:<base64Content>
                                int first = message.indexOf(':');
                                int second = message.indexOf(':', first + 1);
                                int third = message.indexOf(':', second + 1);
                                if (third > -1) {
                                    String sender = message.substring(first + 1, second);
                                    String fileName = message.substring(second + 1, third);
                                    String base64Content = message.substring(third + 1);
                                    SwingUtilities.invokeLater(() -> {
                                        receiveFile(sender, fileName, base64Content);
                                    });
                                }
                            } else if (message.startsWith("voice:")) { // Nhận voice từ client khác
                                // Định dạng: voice:<sender>:<fileName>:<base64AudioContent>
                                int first = message.indexOf(':');
                                int second = message.indexOf(':', first + 1);
                                int third = message.indexOf(':', second + 1);
                                if (third > -1) {
                                    String sender = message.substring(first + 1, second);
                                    String fileName = message.substring(second + 1, third);
                                    String base64AudioContent = message.substring(third + 1);
                                    SwingUtilities.invokeLater(() -> {
                                        receiveVoice(sender, fileName, base64AudioContent);
                                    });
                                }
                            } else {
                                // Hiển thị tất cả tin nhắn khác (để debug)
                                SwingUtilities.invokeLater(() -> {
                                    addSystemMessage("⚠️ Unknown message: " + finalMessage);
                                });
                            }
                        }
                    } catch (IOException e) {             
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Disconnected from server");
                            statusLabel.setForeground(new Color(200, 0, 0));
                            addSystemMessage("❌ Server disconnected."); 
                        });
                    }
                }
            }).start();

        } catch (IOException e) {                
            statusLabel.setText("Disconnected");
            statusLabel.setForeground(new Color(200, 0, 0));
            addSystemMessage("❌ Error: " + e.getMessage()); 
        }
    }

    // Gửi tin nhắn đến server (chỉ lưu vào lịch sử chat riêng)
    private void sendMessage() {
        String message = inputField.getText().trim(); 
        RecipientItem selected = contactsList.getSelectedValue();
        String recipient = selected == null ? null : selected.name;
        
        // Kiểm tra placeholder text
        if (message.isEmpty() || message.equals("Type your message here...")) {
            return; // Không gửi tin nhắn rỗng hoặc placeholder
        }
        
        if (recipient == null || recipient.trim().isEmpty()) {
            // Hiển thị thông báo yêu cầu chọn client
            SwingUtilities.invokeLater(() -> {
                JPanel errorPanel = new JPanel();
                errorPanel.setLayout(new BorderLayout());
                errorPanel.setBackground(Color.WHITE);
                
                JLabel errorLabel = new JLabel("<html><div style='color: #ffc107; text-align: center; padding: 10px;'>⚠️ Please select a contact to send message to.</div></html>");
                errorLabel.setHorizontalAlignment(SwingConstants.CENTER);
                errorPanel.add(errorLabel, BorderLayout.CENTER);
                chatArea.add(errorPanel);
                chatArea.revalidate();
                chatArea.repaint();
            });
            return;
        }
        
            String formatted = "to:" + recipient + ":" + message;
            out.println(formatted);              // Gửi tin nhắn trực tiếp
        
        // Lưu tin nhắn vào danh sách với timestamp đến phút
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
        String timestamp = timeFormat.format(new java.util.Date());
        MessageData msgData = new MessageData(clientName, recipient, message, timestamp, true);
        allMessages.add(msgData);
        saveMessageToFile(msgData); // Lưu vào file
        
        // Hiển thị tin nhắn mới trực tiếp thay vì reload toàn bộ
        JPanel bubble = createMessageBubble(clientName, message, timestamp, true);
        chatArea.add(bubble);
        chatArea.revalidate();
        chatArea.repaint();
        scrollToBottom();
        
        // Clear input field - không đặt lại placeholder
        inputField.setText("");
        inputField.setForeground(new Color(50, 50, 50));
        historyCleared = false; // Reset flag khi gửi tin nhắn mới
    }


    // Item người nhận (tên + trạng thái)
    private static class RecipientItem {
        final String name;
        final boolean online;
        RecipientItem(String name, boolean online) {
            this.name = name;
            this.online = online;
        }
        @Override
        public String toString() {
            return name + (online ? " (Online)" : " (Offline)");
        }
    }

    // Class để lưu trữ thông tin tin nhắn
    private static class MessageData {
        String sender;
        String recipient;
        String content;
        String timestamp;
        boolean isSent; // true nếu là tin nhắn gửi đi, false nếu là tin nhắn nhận về
        
        MessageData(String sender, String recipient, String content, String timestamp, boolean isSent) {
            this.sender = sender;
            this.recipient = recipient;
            this.content = content;
            this.timestamp = timestamp;
            this.isSent = isSent;
        }
    }

    // Custom cell renderer cho danh sách liên hệ
    private class ContactListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, 
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value instanceof RecipientItem) {
                RecipientItem item = (RecipientItem) value;
                
                // Tạo avatar tròn với chữ cái đầu
                String firstLetter = item.name.isEmpty() ? "?" : 
                    String.valueOf(Character.toUpperCase(item.name.charAt(0)));
                
                // Màu avatar dựa trên chữ cái đầu
                Color avatarColor = getAvatarColor(firstLetter.charAt(0));
                
                // Lấy số tin nhắn chưa đọc
                int unreadCount = unreadCounts.getOrDefault(item.name, 0);
                setIcon(new AvatarWithNotificationIcon(avatarColor, firstLetter, 45, unreadCount));
                setIconTextGap(8); // Giảm khoảng cách giữa icon và text
                
                // Hiển thị tên và trạng thái
                String statusText = item.online ? "Online" : "Offline";
                setText(item.name + " (" + statusText + ")");
                setFont(new Font("Segoe UI", Font.PLAIN, 14));
                
                // Màu text dựa trên trạng thái
                if (item.online) {
                    setForeground(new Color(0, 150, 0)); // Xanh lá cho online
                } else {
                    setForeground(new Color(150, 150, 150)); // Xám cho offline
                }
                
                // Background khi được chọn
                if (isSelected) {
                    setBackground(new Color(0, 120, 215));
                    setForeground(Color.WHITE);
                } else {
                    setBackground(Color.WHITE);
                }
            }
            
            return this;
        }
        
        private Color getAvatarColor(char firstChar) {
            Color[] colors = {
                new Color(255, 99, 132), // Đỏ hồng
                new Color(54, 162, 235), // Xanh dương
                new Color(255, 205, 86), // Vàng
                new Color(75, 192, 192), // Xanh lá
                new Color(153, 102, 255), // Tím
                new Color(255, 159, 64), // Cam
                new Color(199, 199, 199), // Xám
                new Color(83, 102, 255), // Xanh đậm
                new Color(255, 99, 255), // Hồng
                new Color(99, 255, 132)  // Xanh lá nhạt
            };
            return colors[Math.abs(firstChar) % colors.length];
        }
    }

    // Icon hình tam giác màu đen cho nút send
    private static class BlackTriangleIcon implements Icon {
        private final int size = 16;
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            g2d.setColor(Color.BLACK);
            
            // Vẽ tam giác hướng về phải
            int[] xPoints = {x + 4, x + 4, x + 12};
            int[] yPoints = {y + 4, y + 12, y + 8};
            g2d.fillPolygon(xPoints, yPoints, 3);
            
            g2d.dispose();
        }
        
        @Override
        public int getIconWidth() { return size; }
        
        @Override
        public int getIconHeight() { return size; }
    }

    // Icon emoji có màu cho nút emoji

    // Icon paperclip cho nút gửi file
    private static class PaperclipIcon implements Icon {
        private final int size = 16;
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Vẽ paperclip
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2.0f));
            
            // Vẽ hình paperclip
            g2d.drawArc(x + 4, y + 3, 6, 4, 0, 180); // Phần trên
            g2d.drawArc(x + 6, y + 7, 6, 4, 180, 180); // Phần dưới
            g2d.drawLine(x + 10, y + 5, x + 10, y + 9); // Đường thẳng giữa
            
            g2d.dispose();
        }
        
        @Override
        public int getIconWidth() { return size; }
        
        @Override
        public int getIconHeight() { return size; }
    }

    // Icon microphone cho nút voice
    private static class VoiceIcon implements Icon {
        private final int size = 16;
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Vẽ microphone
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2.0f));
            
            // Vẽ thân microphone
            g2d.drawLine(x + 8, y + 2, x + 8, y + 10);
            
            // Vẽ đầu microphone
            g2d.drawOval(x + 6, y + 2, 4, 3);
            
            // Vẽ chân microphone
            g2d.drawLine(x + 6, y + 10, x + 10, y + 10);
            g2d.drawLine(x + 5, y + 12, x + 11, y + 12);
            
            g2d.dispose();
        }
        
        @Override
        public int getIconWidth() { return size; }
        
        @Override
        public int getIconHeight() { return size; }
    }

    // Icon avatar với thông báo số tin nhắn chưa đọc
    private static class AvatarWithNotificationIcon implements Icon {
        private final Color backgroundColor;
        private final String text;
        private final int size;
        private final int unreadCount;
        
        public AvatarWithNotificationIcon(Color backgroundColor, String text, int size, int unreadCount) {
            this.backgroundColor = backgroundColor;
            this.text = text;
            this.size = size;
            this.unreadCount = unreadCount;
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2d = (Graphics2D) g.create();
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            
            // Vẽ hình tròn avatar
            g2d.setColor(backgroundColor);
            g2d.fillOval(x, y, size, size);
            
            // Vẽ chữ cái
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Segoe UI", Font.BOLD, size / 2));
            FontMetrics fm = g2d.getFontMetrics();
            int textX = x + (size - fm.stringWidth(text)) / 2;
            int textY = y + (size + fm.getAscent()) / 2 - 2;
            g2d.drawString(text, textX, textY);
            
            // Vẽ thông báo số tin nhắn chưa đọc
            if (unreadCount > 0) {
                // Vẽ hình tròn đỏ cho thông báo
                int badgeSize = Math.min(size / 3, 12);
                int badgeX = x + size - badgeSize;
                int badgeY = y;
                
                g2d.setColor(Color.RED);
                g2d.fillOval(badgeX, badgeY, badgeSize, badgeSize);
                
                // Vẽ số
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Segoe UI", Font.BOLD, badgeSize / 2));
                String countText = unreadCount > 99 ? "99+" : String.valueOf(unreadCount);
                FontMetrics badgeFm = g2d.getFontMetrics();
                int badgeTextX = badgeX + (badgeSize - badgeFm.stringWidth(countText)) / 2;
                int badgeTextY = badgeY + (badgeSize + badgeFm.getAscent()) / 2 - 1;
                g2d.drawString(countText, badgeTextX, badgeTextY);
            }
            
            g2d.dispose();
        }
        
        @Override
        public int getIconWidth() { return size; }
        
        @Override
        public int getIconHeight() { return size; }
    }



    // Custom method to set placeholder text for JTextField
    private void setPlaceholderText(JTextField textField, String placeholder) {
        textField.setText(placeholder);
        textField.setForeground(new Color(150, 150, 150));
        
        textField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(new Color(50, 50, 50));
                }
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                // Không tự động set lại placeholder để tránh hiển thị khi gửi tin nhắn
                // Placeholder chỉ hiển thị khi field thực sự trống và không có nội dung
            }
        });
        
        // Thêm listener để clear placeholder khi người dùng bắt đầu gõ
        textField.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                if (textField.getText().equals(placeholder)) {
                    textField.setText("");
                    textField.setForeground(new Color(50, 50, 50));
                }
            }
        });
    }


   // Method to create message bubble JPanel
private JPanel createMessageBubble(String sender, String content, String timestamp, boolean isSent) {
    JPanel bubblePanel = new JPanel(new BorderLayout());
    bubblePanel.setOpaque(false);
    bubblePanel.setBorder(new EmptyBorder(0, 0, 0, 0)); // Không có margin ngoài

    // Bubble container (custom paint để bo tròn)
    JPanel bubbleContainer = new JPanel(new BorderLayout()) {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Màu nền theo loại tin nhắn
            if (isSent) {
                g2.setColor(new Color(0, 120, 215)); // Xanh dương cho tin nhắn gửi
            } else {
                g2.setColor(new Color(240, 240, 240)); // Xám nhạt cho tin nhắn nhận
            }

            // Vẽ hình bo tròn (corner radius = 20)
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);

            super.paintComponent(g);
        }
    };

    bubbleContainer.setOpaque(false);
    bubbleContainer.setBorder(new EmptyBorder(8, 12, 8, 12)); // Tăng padding để bubble lớn hơn

    // Nội dung tin nhắn
    JLabel messageLabel = new JLabel(content);
    messageLabel.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16)); // Tăng font size từ 14 lên 16
    messageLabel.setOpaque(false);

    if (isSent) {
        messageLabel.setForeground(Color.WHITE);
    } else {
        messageLabel.setForeground(Color.BLACK);
    }

    // Thời gian gửi
    JLabel timeLabel = new JLabel(timestamp);
    timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12)); // Tăng font size từ 10 lên 12
    timeLabel.setOpaque(false);
    if (isSent) {
        timeLabel.setForeground(Color.WHITE);
    } else {
        timeLabel.setForeground(new Color(100, 100, 100));
    }

    // Panel chứa nội dung và thời gian
    JPanel contentPanel = new JPanel(new BorderLayout(0, 0)); // Không có khoảng cách dọc giữa message và time
    contentPanel.setOpaque(false);
    contentPanel.add(messageLabel, BorderLayout.CENTER);
    contentPanel.add(timeLabel, BorderLayout.SOUTH);

    bubbleContainer.add(contentPanel, BorderLayout.CENTER);

    // Căn lề trái/phải tùy tin nhắn gửi hay nhận
    JPanel mainContainer = new JPanel(new FlowLayout(isSent ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 0));
    mainContainer.setOpaque(false);
    mainContainer.add(bubbleContainer);

    bubblePanel.add(mainContainer, BorderLayout.CENTER);

    return bubblePanel;
}

    // Method to display messages for selected client
    private void displayMessagesForClient(String clientName) {
        chatArea.removeAll();
        
        if (clientName == null) {
            // Hiển thị welcome message
            JPanel welcomePanel = new JPanel();
            welcomePanel.setLayout(new BorderLayout());
            welcomePanel.setBackground(Color.WHITE);
            
            JLabel welcomeLabel = new JLabel("<html><div style='color: #666; text-align: center; padding: 10px;'>" +
                                            "Welcome to Chat Application!<br>Select a contact to start chatting." +
                                            "</div></html>");
            welcomeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            welcomePanel.add(welcomeLabel, BorderLayout.CENTER);
            chatArea.add(welcomePanel);
        } else {
            // Hiển thị tin nhắn với client được chọn
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1.0;
            gbc.insets = new Insets(0, 0, 2, 0); // Khoảng cách dưới 2px
            
            for (MessageData msg : allMessages) {
                if ((msg.isSent && msg.recipient.equals(clientName)) || 
                    (!msg.isSent && msg.sender.equals(clientName))) {
                    JPanel bubble = createMessageBubble(msg.sender, msg.content, msg.timestamp, msg.isSent);
                    chatArea.add(bubble, gbc);
                    gbc.gridy++; // Tăng dòng tiếp theo
                }
            }
        }
        
        chatArea.revalidate();
        chatArea.repaint();
        scrollToBottom();
    }
    // Method to add system message to chat
    private void addSystemMessage(String message) {
        JPanel systemPanel = new JPanel();
        systemPanel.setLayout(new BorderLayout());
        systemPanel.setBackground(Color.WHITE);
        
        JLabel systemLabel = new JLabel("<html><div style='color: #666; text-align: center; padding: 10px;'>" + message + "</div></html>");
        systemLabel.setHorizontalAlignment(SwingConstants.CENTER);
        systemPanel.add(systemLabel, BorderLayout.CENTER);
        
        chatArea.add(systemPanel);
        chatArea.revalidate();
        chatArea.repaint();
        scrollToBottom();
    }

    // Method to scroll to bottom of chat area
    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar vertical = chatScrollPane.getVerticalScrollBar();
            vertical.setValue(vertical.getMaximum());
        });
    }

    // Method to update selected contact information
    private void updateSelectedContact(RecipientItem item) {
        SwingUtilities.invokeLater(() -> {
            userNameLabel.setText(item.name);
            userStatusLabel.setText(item.online ? "Online" : "Offline");
            userStatusLabel.setForeground(item.online ? new Color(0, 150, 0) : new Color(150, 150, 150));
            
            // Update avatar
            String firstLetter = item.name.isEmpty() ? "?" : 
                String.valueOf(Character.toUpperCase(item.name.charAt(0)));
            avatarLabel.setText(firstLetter);
            
            // Update avatar color
            Color avatarColor = getAvatarColor(firstLetter.charAt(0));
            avatarLabel.setBackground(avatarColor);
            
            // Cập nhật selected client và hiển thị tin nhắn
            selectedClient = item.name;
            displayMessagesForClient(selectedClient);
            
            // Xóa thông báo chưa đọc khi chọn client
            clearUnreadCount(selectedClient);
        });
    }

    // Method to get avatar color based on first character
    private Color getAvatarColor(char firstChar) {
        Color[] colors = {
            new Color(255, 99, 132), // Đỏ hồng
            new Color(54, 162, 235), // Xanh dương
            new Color(255, 205, 86), // Vàng
            new Color(75, 192, 192), // Xanh lá
            new Color(153, 102, 255), // Tím
            new Color(255, 159, 64), // Cam
            new Color(199, 199, 199), // Xám
            new Color(83, 102, 255), // Xanh đậm
            new Color(255, 99, 255), // Hồng
            new Color(99, 255, 132)  // Xanh lá nhạt
        };
        return colors[Math.abs(firstChar) % colors.length];
    }



    // Method to update unread count for a client
    private void updateUnreadCount(String clientName, int count) {
        unreadCounts.put(clientName, count);
        // Cập nhật lại danh sách liên hệ để hiển thị thông báo mới
        contactsList.repaint();
    }

    // Method to increment unread count for a client
    private void incrementUnreadCount(String clientName) {
        int currentCount = unreadCounts.getOrDefault(clientName, 0);
        updateUnreadCount(clientName, currentCount + 1);
    }

    // Method to clear unread count for a client
    private void clearUnreadCount(String clientName) {
        updateUnreadCount(clientName, 0);
    }

    // Method to send file
    private void sendFile() {
        RecipientItem selected = contactsList.getSelectedValue();
        String recipient = selected == null ? null : selected.name;
        
        if (recipient == null || recipient.trim().isEmpty()) {
            SwingUtilities.invokeLater(() -> {
                addSystemMessage("⚠️ Please select a contact to send file to.");
            });
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Select file to send");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        
        // Thêm các nút điều hướng
        fileChooser.setControlButtonsAreShown(true);
        fileChooser.setAcceptAllFileFilterUsed(true);
        
        // Thiết lập thư mục mặc định
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String fileName = selectedFile.getName();
            long fileSize = selectedFile.length();
            
            // Kiểm tra kích thước file (giới hạn 10MB)
            if (fileSize > 10 * 1024 * 1024) {
                SwingUtilities.invokeLater(() -> {
                    addSystemMessage("❌ File size too large! Maximum 10MB allowed.");
                });
                return;
            }
            
            try {
                // Đọc file thành byte array
                byte[] fileBytes = java.nio.file.Files.readAllBytes(selectedFile.toPath());
                String base64File = java.util.Base64.getEncoder().encodeToString(fileBytes);
                
                // Gửi file qua server
                String fileMessage = "file:" + recipient + ":" + fileName + ":" + base64File;
                out.println(fileMessage);
                
                // Hiển thị file đã gửi trong chat - phân biệt ảnh và file thường
                if (isImageFile(fileName)) {
                    displaySentImageInChat(fileName, fileBytes, recipient);
                } else {
                    displaySentFileInChat(fileName, fileSize, recipient);
                }
                
                // Không hiển thị thông báo sent file nữa
                
            } catch (IOException e) {
                SwingUtilities.invokeLater(() -> {
                    addSystemMessage("❌ Error reading file: " + e.getMessage());
                });
            }
        } else if (result == JFileChooser.CANCEL_OPTION) {
            // Người dùng hủy hoặc quay lại
            SwingUtilities.invokeLater(() -> {
                addSystemMessage("📁 File selection cancelled");
            });
        }
    }
    
    // Method to format file size
    
    // Method to show emoji picker
    
    // Method to receive file
    private void receiveFile(String sender, String fileName, String base64Content) {
        try {
            // Decode base64 content
            byte[] fileBytes = java.util.Base64.getDecoder().decode(base64Content);
            
            // Kiểm tra xem có phải file ảnh không
            boolean isImage = isImageFile(fileName);
            
            if (isImage) {
                // Hiển thị ảnh trong chat
                displayImageInChat(sender, fileName, fileBytes);
            } else {
                // Hiển thị preview dialog
                FilePreviewDialog previewDialog = new FilePreviewDialog(frame, sender, fileName, fileBytes);
                previewDialog.setVisible(true);
                
                // File thường - hiển thị thông báo và cho phép tải về
                displayFileInChat(sender, fileName, fileBytes);
            }
            
        } catch (Exception e) {
            addSystemMessage("❌ Error receiving file: " + e.getMessage());
        }
    }
    
    // Hiển thị file đã gửi trong chat
    private void displaySentFileInChat(String fileName, long fileSize, String recipient) {
        // Tạo timestamp
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
        String timestamp = timeFormat.format(new java.util.Date());
        
        // Lưu tin nhắn file vào danh sách
        String fileContent = "📎 " + fileName + " (" + formatFileSize(fileSize) + ")";
        MessageData msgData = new MessageData(clientName, recipient, fileContent, timestamp, true);
        allMessages.add(msgData);
        saveMessageToFile(msgData); // Lưu vào file
        
        // Tạo panel cho file đã gửi với bubble style
        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.setOpaque(false);
        filePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Tạo bubble container cho file
        JPanel bubbleContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Màu xanh dương cho file đã gửi
                g2.setColor(new Color(0, 120, 215));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                super.paintComponent(g);
            }
        };
        
        bubbleContainer.setOpaque(false);
        bubbleContainer.setBorder(new EmptyBorder(8, 12, 8, 12));
        
        JLabel fileLabel = new JLabel("📎 " + fileName + " (" + formatFileSize(fileSize) + ")");
        fileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        fileLabel.setForeground(Color.WHITE);
        fileLabel.setOpaque(false);
        
        // Thêm timestamp
        JLabel timeLabel = new JLabel(timestamp);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLabel.setForeground(new Color(200, 200, 200));
        timeLabel.setOpaque(false);
        
        // Panel chứa file và timestamp
        JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
        contentPanel.setOpaque(false);
        contentPanel.add(fileLabel, BorderLayout.CENTER);
        contentPanel.add(timeLabel, BorderLayout.SOUTH);
        
        bubbleContainer.add(contentPanel, BorderLayout.CENTER);
        
        // Căn phải cho file đã gửi
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(bubbleContainer);
        
        filePanel.add(rightPanel, BorderLayout.CENTER);
        
        // Thêm vào chat area
        chatArea.add(filePanel);
        chatArea.revalidate();
        chatArea.repaint();
        scrollToBottom();
    }
    
    // Hiển thị ảnh đã gửi trong chat
    private void displaySentImageInChat(String fileName, byte[] fileBytes, String recipient) {
        try {
            // Tạo timestamp
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
            String timestamp = timeFormat.format(new java.util.Date());
            
            // Lưu tin nhắn ảnh vào danh sách
            String imageContent = "🖼️ " + fileName + " (" + formatFileSize(fileBytes.length) + ")";
            MessageData msgData = new MessageData(clientName, recipient, imageContent, timestamp, true);
            allMessages.add(msgData);
            saveMessageToFile(msgData); // Lưu vào file
            
            // Tạo ImageIcon từ byte array
            ImageIcon imageIcon = new ImageIcon(fileBytes);
            Image image = imageIcon.getImage();
            
            // Resize ảnh nếu quá lớn (max 200x200)
            int maxWidth = 200;
            int maxHeight = 200;
            int originalWidth = image.getWidth(null);
            int originalHeight = image.getHeight(null);
            
            int newWidth = originalWidth;
            int newHeight = originalHeight;
            
            if (originalWidth > maxWidth || originalHeight > maxHeight) {
                double scaleX = (double) maxWidth / originalWidth;
                double scaleY = (double) maxHeight / originalHeight;
                double scale = Math.min(scaleX, scaleY);
                
                newWidth = (int) (originalWidth * scale);
                newHeight = (int) (originalHeight * scale);
            }
            
            Image resizedImage = image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            ImageIcon resizedIcon = new ImageIcon(resizedImage);
            
            // Tạo panel cho ảnh đã gửi với bubble style
            JPanel imagePanel = new JPanel(new BorderLayout());
            imagePanel.setOpaque(false);
            imagePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            
            // Tạo bubble container cho ảnh
            JPanel bubbleContainer = new JPanel(new BorderLayout()) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Màu xanh dương cho ảnh đã gửi
                    g2.setColor(new Color(0, 120, 215));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    
                    super.paintComponent(g);
                }
            };
            
            bubbleContainer.setOpaque(false);
            bubbleContainer.setBorder(new EmptyBorder(8, 12, 8, 12));
            
            // Tạo label cho ảnh
            JLabel imageLabel = new JLabel(resizedIcon);
            imageLabel.setOpaque(false);
            
            // Thêm timestamp
            JLabel timeLabel = new JLabel(timestamp);
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            timeLabel.setForeground(new Color(200, 200, 200));
            timeLabel.setOpaque(false);
            
            // Panel chứa ảnh và timestamp
            JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
            contentPanel.setOpaque(false);
            contentPanel.add(imageLabel, BorderLayout.CENTER);
            contentPanel.add(timeLabel, BorderLayout.SOUTH);
            
            bubbleContainer.add(contentPanel, BorderLayout.CENTER);
            
            // Panel bên phải để căn ảnh sang phải
            JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            rightPanel.setOpaque(false);
            rightPanel.add(bubbleContainer);
            
            imagePanel.add(rightPanel, BorderLayout.CENTER);
            
            // Thêm vào chat area
            chatArea.add(imagePanel);
            chatArea.revalidate();
            chatArea.repaint();
            scrollToBottom();
            
        } catch (Exception e) {
            addSystemMessage("❌ Error displaying sent image: " + e.getMessage());
        }
    }
    
    // Hiển thị ảnh trong chat
    private void displayImageInChat(String sender, String fileName, byte[] fileBytes) {
        try {
            // Tạo timestamp
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
            String timestamp = timeFormat.format(new java.util.Date());
            
            // Lưu tin nhắn ảnh vào danh sách
            String imageContent = "🖼️ " + fileName + " (" + formatFileSize(fileBytes.length) + ")";
            MessageData msgData = new MessageData(sender, clientName, imageContent, timestamp, false);
            allMessages.add(msgData);
            saveMessageToFile(msgData); // Lưu vào file
            
            // Tạo ImageIcon từ byte array
            ImageIcon imageIcon = new ImageIcon(fileBytes);
            Image image = imageIcon.getImage();
            
            // Resize ảnh nếu quá lớn (max width: 300px)
            int maxWidth = 300;
            int originalWidth = image.getWidth(null);
            int originalHeight = image.getHeight(null);
            
            if (originalWidth > maxWidth) {
                int newHeight = (originalHeight * maxWidth) / originalWidth;
                image = image.getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH);
                imageIcon = new ImageIcon(image);
            }
            
            // Tạo panel cho ảnh
            JPanel imagePanel = new JPanel(new BorderLayout());
            imagePanel.setOpaque(false);
            imagePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
            
            JLabel imageLabel = new JLabel(imageIcon);
            imageLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
            
            // Thêm click listener để tải về
            imageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseClicked(java.awt.event.MouseEvent evt) {
                    downloadFile(sender, fileName, fileBytes);
                }
            });
            
            // Thêm tooltip
            imageLabel.setToolTipText("Click to download: " + fileName);
            
            imagePanel.add(imageLabel, BorderLayout.CENTER);
            
            // Thêm label tên file và timestamp
            JPanel infoPanel = new JPanel(new BorderLayout());
            infoPanel.setOpaque(false);
            
            JLabel fileNameLabel = new JLabel(fileName);
            fileNameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            fileNameLabel.setForeground(new Color(100, 100, 100));
            fileNameLabel.setHorizontalAlignment(SwingConstants.CENTER);
            
            JLabel timeLabel = new JLabel(timestamp);
            timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 9));
            timeLabel.setForeground(new Color(150, 150, 150));
            timeLabel.setHorizontalAlignment(SwingConstants.CENTER);
            
            infoPanel.add(fileNameLabel, BorderLayout.CENTER);
            infoPanel.add(timeLabel, BorderLayout.SOUTH);
            imagePanel.add(infoPanel, BorderLayout.SOUTH);
            
            // Thêm vào chat area
            chatArea.add(imagePanel);
            chatArea.revalidate();
            chatArea.repaint();
            scrollToBottom();
            
        } catch (Exception e) {
            addSystemMessage("❌ Error displaying image: " + e.getMessage());
        }
    }
    
    // Hiển thị file thường trong chat
    private void displayFileInChat(String sender, String fileName, byte[] fileBytes) {
        // Tạo timestamp
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
        String timestamp = timeFormat.format(new java.util.Date());
        
        // Lưu tin nhắn file vào danh sách
        String fileContent = "📎 " + fileName + " (" + formatFileSize(fileBytes.length) + ")";
        MessageData msgData = new MessageData(sender, clientName, fileContent, timestamp, false);
        allMessages.add(msgData);
        saveMessageToFile(msgData); // Lưu vào file
        
        // Tạo panel cho file
        JPanel filePanel = new JPanel(new BorderLayout());
        filePanel.setOpaque(false);
        filePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JLabel fileLabel = new JLabel("📎 " + fileName + " (" + formatFileSize(fileBytes.length) + ")");
        fileLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        fileLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        fileLabel.setForeground(new Color(0, 120, 215));
        
        // Thêm click listener để tải về
        fileLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                downloadFile(sender, fileName, fileBytes);
            }
        });
        
        fileLabel.setToolTipText("Click to download: " + fileName);
        
        // Thêm timestamp
        JLabel timeLabel = new JLabel(timestamp);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(150, 150, 150));
        
        // Panel chứa file và timestamp
        JPanel contentPanel = new JPanel(new BorderLayout(0, 2));
        contentPanel.setOpaque(false);
        contentPanel.add(fileLabel, BorderLayout.CENTER);
        contentPanel.add(timeLabel, BorderLayout.SOUTH);
        
        filePanel.add(contentPanel, BorderLayout.CENTER);
        
        // Thêm vào chat area
        chatArea.add(filePanel);
        chatArea.revalidate();
        chatArea.repaint();
        scrollToBottom();
    }
    
    // Nhận voice message
    private void receiveVoice(String sender, String fileName, String base64AudioContent) {
        try {
            // Decode base64 audio content
            byte[] audioBytes = java.util.Base64.getDecoder().decode(base64AudioContent);
            
            // Tạo timestamp
            java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
            String timestamp = timeFormat.format(new java.util.Date());
            
            // Lưu tin nhắn voice vào danh sách
            String voiceContent = "🎤 Voice message";
            MessageData msgData = new MessageData(sender, clientName, voiceContent, timestamp, false);
            allMessages.add(msgData);
            saveMessageToFile(msgData);
            
            // Hiển thị voice message trong chat
            displayReceivedVoiceInChat(sender, fileName, audioBytes, timestamp);
            
        } catch (Exception e) {
            addSystemMessage("❌ Error receiving voice message: " + e.getMessage());
        }
    }
    
    // Hiển thị voice message đã nhận
    private void displayReceivedVoiceInChat(String sender, String fileName, byte[] audioBytes, String timestamp) {
        // Tạo panel cho voice message đã nhận
        JPanel voicePanel = new JPanel(new BorderLayout());
        voicePanel.setOpaque(false);
        voicePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        JLabel voiceLabel = new JLabel("🎤 Voice message");
        voiceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        voiceLabel.setCursor(new Cursor(Cursor.HAND_CURSOR));
        voiceLabel.setForeground(new Color(0, 120, 215));
        
        // Thêm click listener để phát voice
        voiceLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                playVoiceMessage(audioBytes);
            }
        });
        
        voiceLabel.setToolTipText("Click to play voice message");
        
        // Thêm timestamp
        JLabel timeLabel = new JLabel(timestamp);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLabel.setForeground(new Color(150, 150, 150));
        
        // Panel chứa voice và timestamp
        JPanel contentPanel = new JPanel(new BorderLayout(0, 2));
        contentPanel.setOpaque(false);
        contentPanel.add(voiceLabel, BorderLayout.CENTER);
        contentPanel.add(timeLabel, BorderLayout.SOUTH);
        
        voicePanel.add(contentPanel, BorderLayout.CENTER);
        
        // Thêm vào chat area
        chatArea.add(voicePanel);
        chatArea.revalidate();
        chatArea.repaint();
        scrollToBottom();
    }
    
    // Phát voice message
    private void playVoiceMessage(byte[] audioBytes) {
        try {
            // Tạo AudioFormat cho playback
            AudioFormat playbackFormat = new AudioFormat(16000, 16, 1, true, false);
            
            // Tạo AudioInputStream từ byte array
            ByteArrayInputStream bais = new ByteArrayInputStream(audioBytes);
            AudioInputStream audioInputStream = new AudioInputStream(bais, playbackFormat, audioBytes.length / playbackFormat.getFrameSize());
            
            // Phát audio
            Clip clip = AudioSystem.getClip();
            clip.open(audioInputStream);
            clip.start();
            
            addSystemMessage("🔊 Playing voice message...");
            
        } catch (Exception e) {
            addSystemMessage("❌ Error playing voice message: " + e.getMessage());
        }
    }

    // Tải file về máy
    private void downloadFile(String sender, String fileName, byte[] fileBytes) {
        try {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Save file as...");
            fileChooser.setSelectedFile(new File(fileName));
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home"), "Downloads"));
            
            int result = fileChooser.showSaveDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                java.nio.file.Files.write(selectedFile.toPath(), fileBytes);
                
                addSystemMessage("✅ File saved to: " + selectedFile.getAbsolutePath());
                
                // Hỏi có muốn mở file không
            int option = JOptionPane.showConfirmDialog(frame, 
                    "File saved successfully!\n\nDo you want to open the file?", 
                    "File Saved", 
                JOptionPane.YES_NO_OPTION);
            
            if (option == JOptionPane.YES_OPTION) {
                try {
                        Desktop.getDesktop().open(selectedFile);
                } catch (IOException e) {
                    addSystemMessage("⚠️ Could not open file: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            addSystemMessage("❌ Error saving file: " + e.getMessage());
        }
    }

    // Lưu tin nhắn vào file lịch sử
    private void saveMessageToFile(MessageData msg) {
        try {
            String fileName = clientName + "_chat_history.txt";
            try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(new FileOutputStream(fileName, true), "UTF-8"))) {
                String formattedMessage = String.format("[%s] %s -> %s: %s", 
                    msg.timestamp, msg.sender, msg.recipient, msg.content);
                writer.println(formattedMessage);
            }
        } catch (IOException e) {
            System.err.println("Error saving message to file: " + e.getMessage());
        }
    }

    // Tải lịch sử chat từ file khi khởi động
    private void loadChatHistory() {
        try {
            String fileName = clientName + "_chat_history.txt";
            File file = new File(fileName);
            if (!file.exists()) {
                return; // File không tồn tại, không cần tải
            }

            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Parse format: [timestamp] sender -> recipient: content
                    if (line.startsWith("[") && line.contains("] ")) {
                        int endBracket = line.indexOf("] ");
                        if (endBracket > 0) {
                            String timestamp = line.substring(1, endBracket);
                            String rest = line.substring(endBracket + 2);
                            
                            if (rest.contains(" -> ")) {
                                String[] parts = rest.split(" -> ", 2);
                                if (parts.length == 2 && parts[1].contains(": ")) {
                                    String sender = parts[0];
                                    String[] recipientContent = parts[1].split(": ", 2);
                                    if (recipientContent.length == 2) {
                                        String recipient = recipientContent[0];
                                        String content = recipientContent[1];
                                        
                                        boolean isSent = sender.equals(clientName);
                                        MessageData msgData = new MessageData(sender, recipient, content, timestamp, isSent);
                                        allMessages.add(msgData);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading chat history: " + e.getMessage());
        }
    }

    // Xóa file lịch sử chat
    private void clearChatHistoryFile() {
        try {
            String fileName = clientName + "_chat_history.txt";
            File file = new File(fileName);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            System.err.println("Error clearing chat history file: " + e.getMessage());
        }
    }


    // Voice chat methods
    private void toggleVoiceRecording() {
        if (isRecording.get()) {
            stopVoiceRecording();
        } else {
            startVoiceRecording();
        }
    }
    
    private void startVoiceRecording() {
        try {
            // Thiết lập audio format
            audioFormat = new AudioFormat(16000, 16, 1, true, false);
            
            // Mở microphone
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, audioFormat);
            if (!AudioSystem.isLineSupported(info)) {
                addSystemMessage("❌ Microphone not supported or not available");
                return;
            }
            
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(audioFormat);
            
            // Khởi tạo buffer
            audioBuffer = new ByteArrayOutputStream();
            
            // Bắt đầu recording
            microphone.start();
            isRecording.set(true);
            
            // Cập nhật UI
            SwingUtilities.invokeLater(() -> {
                voiceButton.setBackground(new Color(255, 100, 100)); // Màu đỏ khi đang ghi
                voiceButton.setToolTipText("Click to stop recording");
                addSystemMessage("🎤 Recording voice message... (Speak now)");
            });
            
            // Thread để đọc audio data
            new Thread(() -> {
                byte[] buffer = new byte[1024];
                while (isRecording.get() && microphone.isActive()) {
                    try {
                        int bytesRead = microphone.read(buffer, 0, buffer.length);
                        if (bytesRead > 0) {
                            audioBuffer.write(buffer, 0, bytesRead);
                        }
                    } catch (Exception ex) {
                        break;
                    }
                }
            }).start();
            
        } catch (Exception e) {
            addSystemMessage("❌ Error starting voice recording: " + e.getMessage());
            isRecording.set(false);
        }
    }
    
    private void stopVoiceRecording() {
        try {
            isRecording.set(false);
            
            // Đợi một chút để thread recording kết thúc
            Thread.sleep(100);
            
            if (microphone != null) {
                microphone.stop();
                microphone.close();
            }
            
            // Lấy audio data
            byte[] audioData = audioBuffer != null ? audioBuffer.toByteArray() : new byte[0];
            
            // Cập nhật UI
            SwingUtilities.invokeLater(() -> {
                voiceButton.setBackground(Color.WHITE);
                voiceButton.setToolTipText("Click to record voice");
                
                // Kiểm tra audio data có đủ lớn không (ít nhất 2KB)
                if (audioData.length > 2048) {
                    sendVoiceMessage(audioData);
                    addSystemMessage("✅ Voice message sent! (" + formatFileSize(audioData.length) + ")");
                } else {
                    addSystemMessage("⚠️ Recording too short (" + formatFileSize(audioData.length) + "). Please record longer.");
                }
            });
            
        } catch (Exception e) {
            addSystemMessage("❌ Error stopping voice recording: " + e.getMessage());
        }
    }
    
    private void sendVoiceMessage(byte[] audioData) {
        RecipientItem selected = contactsList.getSelectedValue();
        String recipient = selected == null ? null : selected.name;
        
        if (recipient == null || recipient.trim().isEmpty()) {
            addSystemMessage("⚠️ Please select a contact to send voice message to.");
            return;
        }
        
        try {
            // Encode audio data to base64
            String base64Audio = java.util.Base64.getEncoder().encodeToString(audioData);
            
            // Gửi voice message qua server
            String voiceMessage = "voice:" + recipient + ":voice_message.wav:" + base64Audio;
            out.println(voiceMessage);
            
            // Hiển thị voice message đã gửi trong chat
            displaySentVoiceInChat(recipient);
            
        } catch (Exception e) {
            addSystemMessage("❌ Error sending voice message: " + e.getMessage());
        }
    }
    
    private void displaySentVoiceInChat(String recipient) {
        // Tạo timestamp
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");
        String timestamp = timeFormat.format(new java.util.Date());
        
        // Lưu tin nhắn voice vào danh sách
        String voiceContent = "🎤 Voice message";
        MessageData msgData = new MessageData(clientName, recipient, voiceContent, timestamp, true);
        allMessages.add(msgData);
        saveMessageToFile(msgData);
        
        // Tạo panel cho voice message đã gửi
        JPanel voicePanel = new JPanel(new BorderLayout());
        voicePanel.setOpaque(false);
        voicePanel.setBorder(new EmptyBorder(5, 5, 5, 5));
        
        // Tạo bubble container cho voice
        JPanel bubbleContainer = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                
                // Màu xanh dương cho voice đã gửi
                g2.setColor(new Color(0, 120, 215));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                
                super.paintComponent(g);
            }
        };
        
        bubbleContainer.setOpaque(false);
        bubbleContainer.setBorder(new EmptyBorder(8, 12, 8, 12));
        
        JLabel voiceLabel = new JLabel("🎤 Voice message");
        voiceLabel.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        voiceLabel.setForeground(Color.WHITE);
        voiceLabel.setOpaque(false);
        
        // Thêm timestamp
        JLabel timeLabel = new JLabel(timestamp);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        timeLabel.setForeground(new Color(200, 200, 200));
        timeLabel.setOpaque(false);
        
        // Panel chứa voice và timestamp
        JPanel contentPanel = new JPanel(new BorderLayout(0, 5));
        contentPanel.setOpaque(false);
        contentPanel.add(voiceLabel, BorderLayout.CENTER);
        contentPanel.add(timeLabel, BorderLayout.SOUTH);
        
        bubbleContainer.add(contentPanel, BorderLayout.CENTER);
        
        // Căn phải cho voice đã gửi
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rightPanel.setOpaque(false);
        rightPanel.add(bubbleContainer);
        
        voicePanel.add(rightPanel, BorderLayout.CENTER);
        
        // Thêm vào chat area
        chatArea.add(voicePanel);
        chatArea.revalidate();
        chatArea.repaint();
        scrollToBottom();
    }
    
    // Avatar picker method
    private void showAvatarPicker() {
        String[] avatars = {
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
            "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
            "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
            "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬",
            "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗",
            "🤔", "🤭", "🤫", "🤥", "😶", "😐", "😑", "😬", "🙄", "😯",
            "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "🤐",
            "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕", "🤑", "🤠", "😈",
            "👿", "👹", "👺", "🤡", "💩", "👻", "💀", "☠️", "👽", "👾"
        };
        
        // Tạo dialog chọn avatar
        JDialog avatarDialog = new JDialog(frame, "Choose Avatar", true);
        avatarDialog.setSize(400, 300);
        avatarDialog.setLocationRelativeTo(frame);
        avatarDialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        
        JPanel avatarPanel = new JPanel(new GridLayout(0, 10, 5, 5));
        avatarPanel.setBackground(Color.WHITE);
        avatarPanel.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        for (String avatar : avatars) {
            JButton avatarBtn = new JButton(avatar);
            avatarBtn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));
            avatarBtn.setPreferredSize(new Dimension(30, 30));
            avatarBtn.setBackground(Color.WHITE);
            avatarBtn.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
            
            // Hover effect
            avatarBtn.addMouseListener(new java.awt.event.MouseAdapter() {
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    avatarBtn.setBackground(new Color(240, 240, 240));
                }
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    avatarBtn.setBackground(Color.WHITE);
                }
            });
            
            avatarBtn.addActionListener(e -> {
                currentAvatar = avatar;
                avatarButton.setText(avatar);
                avatarDialog.dispose();
                addSystemMessage("✅ Avatar updated to: " + avatar);
            });
            
            avatarPanel.add(avatarBtn);
        }
        
        JScrollPane scrollPane = new JScrollPane(avatarPanel);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(Color.WHITE);
        
        avatarDialog.add(scrollPane);
        avatarDialog.setVisible(true);
    }
    
    // Utility methods (từ ClientFeatures)
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
    
    private boolean isImageFile(String fileName) {
        String extension = fileName.toLowerCase();
        return extension.endsWith(".jpg") || extension.endsWith(".jpeg") || 
               extension.endsWith(".png") || extension.endsWith(".gif") || 
               extension.endsWith(".bmp") || extension.endsWith(".webp");
    }
    

    // Phương thức main để khởi động client
    public static void main(String[] args) {
        new Client();                        
    }
}	