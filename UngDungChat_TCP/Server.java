package UngDungChat_TCP;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Server {
    private JFrame frame;              // Cửa sổ giao diện chính của server
    private JTextArea chatArea;        // Khu vực hiển thị log
    private JTextField inputField;     // Ô nhập tin nhắn từ server
    private JButton sendButton;        // Nút gửi tin nhắn
    private JButton clearHistoryButton; // Nút xóa lịch sử chat
    private JButton clearAllHistoriesButton; // Nút xóa tất cả lịch sử
    private JComboBox<String> clientSelector; // Danh sách thả xuống chọn client
    private JList<String> clientList;  // Danh sách hiển thị client
    private DefaultListModel<String> clientListModel; // Model cho danh sách client
    private JLabel statusLabel;        // Label hiển thị trạng thái server
    private ServerSocket serverSocket; // Socket server
    private List<ClientHandler> clients; // Danh sách các client
    private Map<String, ClientHandler> activeClients; // Lưu client theo tên
    private Map<String, List<String>> offlineMessages = new ConcurrentHashMap<>(); // Hàng đợi tin nhắn offline theo tên người nhận

    // Constructor khởi tạo server
    public Server() {
        clients = new ArrayList<>();    
        activeClients = new HashMap<>(); 
        initializeGUI();                
        startServer();                  
    }

    // Khởi tạo giao diện Swing cho server
    public void initializeGUI() {
        // Thiết lập Look and Feel
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            // Nếu không thể thiết lập Look and Feel, sử dụng default
            System.err.println("Could not set Look and Feel: " + e.getMessage());
        }

        frame = new JFrame("Server Chat - TCP"); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);

        // Tạo layout chính
        frame.setLayout(new BorderLayout(10, 10));
        frame.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        // Panel trạng thái server
        JPanel statusPanel = createStatusPanel();
        frame.add(statusPanel, BorderLayout.NORTH);

        // Panel chính với chat và danh sách client
        JPanel mainPanel = new JPanel(new BorderLayout(10, 0));
        
        // Panel chat
        JPanel chatPanel = createChatPanel();
        mainPanel.add(chatPanel, BorderLayout.CENTER);
        
        // Panel danh sách client
        JPanel clientPanel = createClientPanel();
        mainPanel.add(clientPanel, BorderLayout.EAST);
        
        frame.add(mainPanel, BorderLayout.CENTER);

        // Panel điều khiển
        JPanel controlPanel = createControlPanel();
        frame.add(controlPanel, BorderLayout.SOUTH);

        // Thiết lập sự kiện
        setupEventListeners();

        frame.setVisible(true);          
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(new TitledBorder("Server Status"));
        
        statusLabel = new JLabel("Server starting...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setForeground(new Color(0, 100, 0));
        
        panel.add(statusLabel);
        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Chat Log"));
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        chatArea.setBackground(new Color(248, 248, 248));
        chatArea.setForeground(new Color(50, 50, 50));
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createClientPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Connected Clients"));
        panel.setPreferredSize(new Dimension(200, 0));
        
        // Tạo danh sách client
        clientListModel = new DefaultListModel<>();
        clientList = new JList<>(clientListModel);
        clientList.setFont(new Font("Arial", Font.PLAIN, 11));
        clientList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        clientList.setCellRenderer(new ClientListCellRenderer());
        
        JScrollPane clientScrollPane = new JScrollPane(clientList);
        clientScrollPane.setBorder(null);
        
        // Panel chọn client để chat
        JPanel selectPanel = new JPanel(new BorderLayout(5, 0));
        selectPanel.setBorder(new EmptyBorder(5, 0, 5, 0));
        
        JLabel selectLabel = new JLabel("Chat with:");
        selectLabel.setFont(new Font("Arial", Font.BOLD, 11));
        selectPanel.add(selectLabel, BorderLayout.WEST);
        
        clientSelector = new JComboBox<>();
        clientSelector.setFont(new Font("Arial", Font.PLAIN, 11));
        selectPanel.add(clientSelector, BorderLayout.CENTER);
        
        panel.add(clientScrollPane, BorderLayout.CENTER);
        panel.add(selectPanel, BorderLayout.SOUTH);
        
        return panel;
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        
        // Tạo TitledBorder với màu sắc và font đẹp
        TitledBorder titledBorder = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(0,0,0), 2),
            "Message Control"
        );
        titledBorder.setTitleFont(new Font("Arial", Font.BOLD, 13));
        titledBorder.setTitleColor(new Color(0,0,0)); // Xanh đậm cho tiêu đề
        panel.setBorder(titledBorder);
        
        // Thiết lập nền cho panel chính
        panel.setBackground(new Color(248, 248, 255));
        
        // Panel nút điều khiển
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        buttonPanel.setBackground(new Color(248, 248, 255));
        
        clearHistoryButton = new JButton("Clear History");
        clearHistoryButton.setBackground(new Color(255, 140, 0)); // Cam đậm
        clearHistoryButton.setForeground(new Color(0,0,0)); //
        clearHistoryButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearHistoryButton.setFocusPainted(false);
        clearHistoryButton.setBorder(BorderFactory.createRaisedBevelBorder());
        clearHistoryButton.setPreferredSize(new Dimension(120, 30));
        
        // Hiệu ứng hover cho nút Clear History
        clearHistoryButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                clearHistoryButton.setBackground(new Color(255, 165, 0)); // Cam sáng hơn
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                clearHistoryButton.setBackground(new Color(255, 140, 0)); // Trở về màu gốc
            }
        });
        
        clearAllHistoriesButton = new JButton("Clear All");
        clearAllHistoriesButton.setBackground(new Color(220, 20, 60)); // Đỏ đậm
        clearAllHistoriesButton.setForeground(new Color(0,0,0)); // 
        clearAllHistoriesButton.setFont(new Font("Arial", Font.BOLD, 12));
        clearAllHistoriesButton.setFocusPainted(false);
        clearAllHistoriesButton.setBorder(BorderFactory.createRaisedBevelBorder());
        clearAllHistoriesButton.setPreferredSize(new Dimension(100, 30));
        
        // Hiệu ứng hover cho nút Clear All
        clearAllHistoriesButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                clearAllHistoriesButton.setBackground(new Color(255, 69, 0)); // Đỏ cam
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                clearAllHistoriesButton.setBackground(new Color(220, 20, 60)); // Trở về màu gốc
            }
        });
        
        buttonPanel.add(clearHistoryButton);
        buttonPanel.add(clearAllHistoriesButton);
        
        // Panel nhập tin nhắn
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBackground(new Color(248, 248, 255));
        
        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.PLAIN, 13));
        inputField.setBackground(new Color(255, 255, 255));
        inputField.setForeground(new Color(25, 25, 25)); // Đen đậm hơn cho dễ đọc
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(100, 149, 237), 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(65, 105, 255)); 
        sendButton.setForeground(new Color(0,0,0)); // 
        sendButton.setFont(new Font("Arial", Font.BOLD, 13));
        sendButton.setFocusPainted(false);
        sendButton.setBorder(BorderFactory.createRaisedBevelBorder());
        sendButton.setPreferredSize(new Dimension(80, 35));
        
        // Hiệu ứng hover cho nút Send
        sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                sendButton.setBackground(new Color(0, 128, 0)); // Xanh lá sáng hơn
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                sendButton.setBackground(new Color(34, 139, 34)); // Trở về màu gốc
            }
        });
        
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        
        panel.add(buttonPanel, BorderLayout.WEST);
        panel.add(inputPanel, BorderLayout.CENTER);
        
        return panel;
    }

    private void setupEventListeners() {
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();           
            }
        });

        clearHistoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearChatHistory();      
            }
        });

        clearAllHistoriesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearAllChatHistories();  
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

        // Sự kiện chọn client từ danh sách
        clientList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedClient = clientList.getSelectedValue();
                if (selectedClient != null) {
                    String clientName = extractClientName(selectedClient);
                    clientSelector.setSelectedItem(clientName + " (Online)");
                }
            }
        });
    }

    // Custom renderer cho danh sách client
    private class ClientListCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            
            if (value != null) {
                String clientInfo = value.toString();
                if (clientInfo.contains("(Online)")) {
                    setIcon(new ColorIcon(Color.GREEN, 8));
                    setForeground(new Color(0, 100, 0));
                } else if (clientInfo.contains("(Offline)")) {
                    setIcon(new ColorIcon(Color.RED, 8));
                    setForeground(new Color(150, 0, 0));
                }
            }
            
            if (isSelected) {
                setBackground(new Color(173, 216, 230));
            }
            
            return this;
        }
    }

    // Icon màu đơn giản
    private class ColorIcon implements Icon {
        private Color color;
        private int size;
        
        public ColorIcon(Color color, int size) {
            this.color = color;
            this.size = size;
        }
        
        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            g.setColor(color);
            g.fillOval(x, y, size, size);
        }
        
        @Override
        public int getIconWidth() {
            return size;
        }
        
        @Override
        public int getIconHeight() {
            return size;
        }
    }

    // Bắt đầu server và lắng nghe kết nối
    public void startServer() {
        try {
            serverSocket = new ServerSocket(5000); 
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Server running on port 5000");
                statusLabel.setForeground(new Color(0, 150, 0));
                chatArea.append("Server started on port 5000. Waiting for clients...\n");
            });

            while (true) {                   
                Socket clientSocket = serverSocket.accept(); 
                SwingUtilities.invokeLater(() -> {
                    chatArea.append("New client connected from: " + clientSocket.getInetAddress().getHostAddress() + "\n");
                });
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clientHandler.start();        
            }
        } catch (IOException e) {        
            SwingUtilities.invokeLater(() -> {
                statusLabel.setText("Server error: " + e.getMessage());
                statusLabel.setForeground(new Color(200, 0, 0));
                chatArea.append("Server error: " + e.getMessage() + "\n");
            });
        }
    }

    // Gửi tin nhắn đến client được chọn
    public void sendMessage() {
        String message = inputField.getText().trim(); 
        if (!message.isEmpty() && clientSelector.getSelectedItem() != null) {
            String selectedClient = extractClientName(clientSelector.getSelectedItem().toString()); 
            // gửi ngay nếu online, nếu offline thì queue để phát khi online
            
            // Tìm client trong danh sách activeClients trước
            ClientHandler targetClient = activeClients.get(selectedClient);
            if (targetClient != null && targetClient.isConnected()) {
                targetClient.sendMessage("Server: " + message);
                chatArea.append("Server: " + message + " (to " + selectedClient + ")\n");  
                saveMessageToFile(selectedClient, "Server: " + message);
            } else {
                // Client offline -> xếp hàng tin nhắn để gửi khi online
                queueOfflineMessage(selectedClient, "Server: " + message);
                saveMessageToFile(selectedClient, "Server: " + message);
                chatArea.append("Queued offline to " + selectedClient + ": " + message + "\n");
            }
            
            inputField.setText("");          
        }
    }

    // Xóa lịch sử chat của client được chọn
    public void clearChatHistory() {
        if (clientSelector.getSelectedItem() != null) {  
            String selectedClient = extractClientName(clientSelector.getSelectedItem().toString());  
            String fileName = selectedClient + "_chat_history.txt";  
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {  
                writer.print("");            
                chatArea.append("Chat history cleared for " + selectedClient + ".\n"); 
                
                // Gửi thông báo đến client được chọn
                ClientHandler targetClient = activeClients.get(selectedClient);
                if (targetClient != null && targetClient.isConnected()) {
                    targetClient.sendMessage("[History Cleared]");
                } else {
                    // Fallback: tìm trong danh sách clients
                    for (ClientHandler client : clients) {  
                        if (client.getClientName() != null && client.getClientName().equals(selectedClient) && client.isConnected()) {
                            client.sendMessage("[History Cleared]");  
                            break;
                        }
                    }
                }
            } catch (IOException e) {          
                chatArea.append("Error clearing history for " + selectedClient + ": " + e.getMessage() + "\n");
            }
        } else {
            chatArea.append("Please select a client to clear history.\n");
        }
    }

    // Xóa tất cả lịch sử chat
    public void clearAllChatHistories() {
        boolean success = true;
        int clearedCount = 0;
        int errorCount = 0;
        
        for (ClientHandler client : clients) {
            String clientName = client.getClientName();
            if (clientName != null) {
                String fileName = clientName + "_chat_history.txt";
                try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {
                    writer.print("");
                    clearedCount++;
                    if (client.isConnected()) {
                        client.sendMessage("[History Cleared]");
                    }
                } catch (IOException e) {
                    errorCount++;
                    success = false;
                }
            }
        }
        
        // Hiển thị thông báo 
        if (success && clearedCount > 0) {
            chatArea.append("All chat histories cleared successfully (" + clearedCount + " files).\n");
        } else if (errorCount > 0) {
            chatArea.append("Cleared " + clearedCount + " files, " + errorCount + " errors occurred.\n");
        } else {
            chatArea.append("No chat histories to clear.\n");
        }
    }

    // Cập nhật danh sách client
    public void updateClientSelector() {
        SwingUtilities.invokeLater(() -> {  
            // Cập nhật danh sách client
            clientListModel.clear();
            clientSelector.removeAllItems();
            
            for (ClientHandler client : activeClients.values()) {  
                String name = client.getClientName();
                if (name != null) {         
                    String status = client.isConnected() ? " (Online)" : " (Offline)";
                    String clientInfo = name + status;
                    
                    // Thêm vào danh sách hiển thị
                    clientListModel.addElement(clientInfo);
                    
                    // Thêm vào combobox
                    clientSelector.addItem(clientInfo);
                }
            }
            
            if (activeClients.isEmpty()) {  
                clientSelector.addItem("No clients");
                clientListModel.addElement("No clients connected");
            }
        });
    }

    // Gửi danh sách client với trạng thái online/offline, không trùng lặp
    private void broadcastClientList() {
        StringBuilder clientList = new StringBuilder("clients_status:");
        synchronized (clients) {
            // Duyệt theo activeClients để có đúng một handler cho mỗi tên
            for (Map.Entry<String, ClientHandler> entry : activeClients.entrySet()) {
                String name = entry.getKey();
                ClientHandler handler = entry.getValue();
                if (name != null && !name.trim().isEmpty()) {
                    clientList.append(name)
                              .append("|")
                              .append(handler != null && handler.isConnected() ? "online" : "offline")
                              .append(",");
                }
            }
            if (clientList.length() > "clients_status:".length()) {
                clientList.setLength(clientList.length() - 1);
            }
            for (ClientHandler client : clients) {
                if (client.isConnected()) {
                    client.sendMessage(clientList.toString());
                }
            }
        }
    }

    // Trích xuất tên client
    private String extractClientName(String item) {
        if (item != null && (item.contains(" (Online)") || item.contains(" (Offline)"))) {
            return item.substring(0, item.indexOf(" ("));
        }
        return item;
    }

    // Lưu tin nhắn vào file chat riêng của từng client
    public void saveMessageToFile(String clientName, String message) {
        if (clientName != null && !clientName.trim().isEmpty()) {
            String fileName = clientName + "_chat_history.txt"; 
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {  
                // Thêm timestamp vào tin nhắn
                String timestamp = java.time.LocalDateTime.now().toString();
                writer.println("[" + timestamp + "] " + message);      
                System.out.println("Saved to " + fileName + ": " + message); // Debug log
            } catch (IOException e) {        
                chatArea.append("Error saving message to " + fileName + ": " + e.getMessage() + "\n");
                System.err.println("Error saving message: " + e.getMessage());
            }
        }
    }

    // Lớp nội bộ xử lý từng client
    public class ClientHandler extends Thread {
        private Socket socket;            
        private PrintWriter out;         
        private BufferedReader in;        
        private String clientName;       
        private boolean isConnected;     // Theo dõi trạng thái kết nối

        public ClientHandler(Socket socket) {
            this.socket = socket;        
            this.isConnected = true;     
        }

        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);  
                in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 

                clientName = in.readLine();
                if (clientName == null) {
                    clientName = "Unknown_" + socket.getInetAddress().getHostAddress();
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append("Failed to get client name, using: " + clientName + "\n");
                    });
                }
                SwingUtilities.invokeLater(() -> {
                    chatArea.append("Client name: " + clientName + " connected\n");
                });

                synchronized (clients) {
                    clients.add(this);  
                    activeClients.put(clientName, this); 
                }
                updateClientSelector();
                broadcastClientList();

                loadChatHistory();
                sendChatHistoryToClient();

                // Gửi tin nhắn offline (nếu có) cho client vừa online
                deliverOfflineMessagesIfAny();

                String message;
                while ((message = in.readLine()) != null && isConnected) {  
                    final String finalMessage = message;
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append(clientName + ": " + finalMessage + "\n");  
                    });

                    // Định dạng tin nhắn: to:<recipient>:<content>
                    // (Tương thích ngược: nếu có thêm msgId vẫn xử lý)
                    if (message.startsWith("to:")) {
                        handleDirectMessage(message);
                    } else {
                        // Lưu vào lịch sử riêng của client nếu không đúng định dạng
                        saveMessageToFile(clientName, clientName + ": " + message); 
                    }
                }
            } catch (IOException e) {         
                SwingUtilities.invokeLater(() -> {
                    chatArea.append(clientName + " disconnected.\n");
                });
                isConnected = false;         
            } finally {                      
                synchronized (clients) {
                    // Không xóa khỏi clients
                }
                updateClientSelector();
                broadcastClientList();
                try {
                    socket.close();           
                } catch (IOException e) {     
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append("Error closing socket: " + e.getMessage() + "\n");
                    });
                }
            }
        }

        private void handleDirectMessage(String raw) {
            // Hỗ trợ hai dạng:
            // 1) to:RecipientName:Message content
            // 2) to:RecipientName:MessageId:Message content (bỏ qua msgId)
            try {
                int first = raw.indexOf(':');
                int second = raw.indexOf(':', first + 1);
                if (second == -1) return;
                String recipient = raw.substring(first + 1, second).trim();
                String content;
                int third = raw.indexOf(':', second + 1);
                if (third == -1) {
                    // không có msgId
                    content = raw.substring(second + 1).trim();
                } else {
                    // có msgId -> bỏ qua
                    content = raw.substring(third + 1).trim();
                }

                String formattedForRecipient = "from:" + clientName + ":" + content;
                String formattedForSenderLog = "You->" + recipient + ": " + content;

                // Lưu lịch sử của người gửi
                saveMessageToFile(clientName, formattedForSenderLog);

                // Gửi hoặc xếp hàng đợi
                ClientHandler target = activeClients.get(recipient);
                if (target != null && target.isConnected()) {
                    target.sendMessage(formattedForRecipient);
                } else {
                    queueOfflineMessage(recipient, formattedForRecipient);
                }
            } catch (Exception ex) {
                // Bỏ qua lỗi định dạng
            }
        }

        private void deliverOfflineMessagesIfAny() {
            List<String> queued = offlineMessages.remove(clientName);
            if (queued != null && !queued.isEmpty()) {
                for (String m : queued) {
                    sendMessage(m);
                }
            }
        }

        public void sendMessage(String message) {
            if (out != null && isConnected) {
                System.out.println("Server sending to " + clientName + ": " + message); // Debug log
                out.println(message);
                out.flush(); // Đảm bảo dữ liệu được gửi ngay lập tức
            } else {
                System.out.println("Cannot send message to " + clientName + " - not connected");
            }
        }

        public String getClientName() {
            return clientName;            
        }

        public boolean isConnected() {
            return isConnected;
        }

        private void loadChatHistory() {
            String fileName = clientName + "_chat_history.txt";
            try (BufferedReader fileReader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = fileReader.readLine()) != null) {
                    final String finalLine = line;
                    SwingUtilities.invokeLater(() -> {
                        chatArea.append(finalLine + "\n");  
                    });
                }
            } catch (IOException e) {      
                // Không làm gì nếu file không tồn tại
            }
        }

        private void sendChatHistoryToClient() {
            String fileName = clientName + "_chat_history.txt";
            try (BufferedReader fileReader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = fileReader.readLine()) != null) {
                    out.println("[History] " + line);  
                }
            } catch (IOException e) {      
                // Không làm gì nếu file không tồn tại
            }
        }

        // Phương thức broadcastMessage đã được loại bỏ vì không cần thiết
        // Tin nhắn từ client chỉ được lưu vào lịch sử chat riêng của client đó
    }

    private void queueOfflineMessage(String recipient, String message) {
        offlineMessages.computeIfAbsent(recipient, k -> new ArrayList<>()).add(message);
        SwingUtilities.invokeLater(() -> {
            chatArea.append("Queued for " + recipient + ": " + message + "\n");
        });
    }

    // Phương thức main để khởi động server
    public static void main(String[] args) {
        new Server();                 
    }
}