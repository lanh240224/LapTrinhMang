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
import javax.swing.border.TitledBorder;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Client {
    private JFrame frame;              // Cửa sổ giao diện chính của client
    private JTextArea chatArea;        // Khu vực hiển thị tin nhắn và log
    private JTextField inputField;     // Ô nhập tin nhắn từ client
    private JButton sendButton;        // Nút gửi tin nhắn
    private JComboBox<RecipientItem> recipientSelector; // Chọn người nhận kèm trạng thái
    private JLabel statusLabel;        // Label hiển thị trạng thái kết nối
    private Socket socket;             // Socket kết nối với server
    private PrintWriter out;           // Dòng xuất để gửi tin nhắn
    private BufferedReader in;         // Dòng nhập để nhận tin nhắn
    private String clientName;         // Tên của client
    private boolean historyCleared = false; // Flag để tránh hiển thị thông báo lặp lại

    // Constructor khởi tạo client, tạo GUI và kết nối đến server
    public Client() {
        initializeGUI();                // Gọi phương thức để khởi tạo giao diện
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

        frame = new JFrame("Client Chat - TCP"); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        frame.setSize(500, 400);
        frame.setLocationRelativeTo(null);

        // Tạo layout chính
        frame.setLayout(new BorderLayout(10, 10));
        frame.getRootPane().setBorder(new EmptyBorder(10, 10, 10, 10));

        // Panel trạng thái kết nối
        JPanel statusPanel = createStatusPanel();
        frame.add(statusPanel, BorderLayout.NORTH);

        // Panel chat
        JPanel chatPanel = createChatPanel();
        frame.add(chatPanel, BorderLayout.CENTER);

        // Panel nhập tin nhắn
        JPanel inputPanel = createInputPanel();
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Thiết lập sự kiện
        setupEventListeners();

        frame.setVisible(true);          // Hiển thị giao diện
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBackground(new Color(240, 248, 255));
        panel.setBorder(new TitledBorder("Connection Status"));
        
        statusLabel = new JLabel("Connecting to server...");
        statusLabel.setFont(new Font("Arial", Font.BOLD, 12));
        statusLabel.setForeground(new Color(255, 140, 0));
        
        panel.add(statusLabel);
        return panel;
    }

    private JPanel createChatPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(new TitledBorder("Chat Messages"));
        
        chatArea = new JTextArea();
        chatArea.setEditable(false);
        chatArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        chatArea.setBackground(new Color(248, 248, 248));
        chatArea.setForeground(new Color(50, 50, 50));
        chatArea.setLineWrap(true);
        chatArea.setWrapStyleWord(true);
        
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);
        
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createInputPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 0));
        panel.setBorder(new TitledBorder("Send Message"));
        
        JPanel leftPanel = new JPanel(new BorderLayout(6, 0));
        JLabel toLabel = new JLabel("To:");
        recipientSelector = new JComboBox<>();
        recipientSelector.setRenderer(new RecipientRenderer(false));
        recipientSelector.setPrototypeDisplayValue(new RecipientItem("Tên ví dụ (Online)", true));
        recipientSelector.setPreferredSize(new Dimension(180, 28));
        leftPanel.add(toLabel, BorderLayout.WEST);
        leftPanel.add(recipientSelector, BorderLayout.CENTER);

        inputField = new JTextField();
        inputField.setFont(new Font("Arial", Font.PLAIN, 12));
        inputField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        
        sendButton = new JButton("Send");
        sendButton.setBackground(new Color(0, 123, 255));
        sendButton.setForeground(new Color(0,0,0));
        sendButton.setFont(new Font("Arial", Font.BOLD, 12));
        sendButton.setFocusPainted(false);
        sendButton.setPreferredSize(new Dimension(80, 35));
        
        JPanel center = new JPanel(new BorderLayout(6, 0));
        center.add(leftPanel, BorderLayout.WEST);
        center.add(inputField, BorderLayout.CENTER);

        panel.add(center, BorderLayout.CENTER);
        panel.add(sendButton, BorderLayout.EAST);
        
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

        frame.setTitle(clientName + " - Client Chat");      

        try {
            socket = new Socket("localhost", 5000);  
            statusLabel.setText("Connected to server");
            statusLabel.setForeground(new Color(0, 150, 0));
            chatArea.append("Connected to server.\n"); 

            out = new PrintWriter(socket.getOutputStream(), true); 
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 

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
                                SwingUtilities.invokeLater(() -> {
                                    chatArea.append(finalMessage.substring(9) + "\n"); 
                                });
                            } else if (message.startsWith("clients_status:")) { // Cập nhật danh sách người nhận + trạng thái
                                String list = message.substring("clients_status:".length());
                                String[] entries = list.isEmpty() ? new String[0] : list.split(",");
                                SwingUtilities.invokeLater(() -> {
                                    recipientSelector.removeAllItems();
                                    java.util.Set<String> seen = new java.util.HashSet<>();
                                    for (String e : entries) {
                                        int bar = e.indexOf('|');
                                        if (bar > -1) {
                                            String name = e.substring(0, bar).trim();
                                            String status = e.substring(bar + 1).trim();
                                            if (!name.isEmpty() && !name.equals(clientName) && !seen.contains(name)) {
                                                seen.add(name);
                                                boolean online = "online".equalsIgnoreCase(status);
                                                recipientSelector.addItem(new RecipientItem(name, online));
                                            }
                                        }
                                    }
                                });
                            } else if (message.equals("[History Cleared]")) { 
                                if (!historyCleared) { // Chỉ hiển thị thông báo một lần
                                    historyCleared = true;
                                    SwingUtilities.invokeLater(() -> {
                                        chatArea.setText(""); // Xóa toàn bộ nội dung chat
                                        chatArea.append("Chat history cleared by server.\n");
                                    });
                                }
                            } else if (message.startsWith("Error:")) { 
                                SwingUtilities.invokeLater(() -> {
                                    chatArea.append(finalMessage + "\n");
                                });
                            } else if (message.startsWith("Server:")) { // Chỉ hiển thị tin nhắn từ server
                                SwingUtilities.invokeLater(() -> {
                                    chatArea.append(finalMessage + "\n");
                                });
                            } else if (message.startsWith("from:")) { // Tin nhắn trực tiếp từ client khác
                                // Định dạng: from:<sender>:<content>
                                int first = message.indexOf(':');
                                int second = message.indexOf(':', first + 1);
                                if (second > -1) {
                                    String sender = message.substring(first + 1, second);
                                    String content = message.substring(second + 1);
                                    SwingUtilities.invokeLater(() -> {
                                        chatArea.append(sender + ": " + content + "\n");
                                    });
                                }
                            } else {
                                // Hiển thị tất cả tin nhắn khác (để debug)
                                SwingUtilities.invokeLater(() -> {
                                    chatArea.append("Unknown message: " + finalMessage + "\n");
                                });
                            }
                        }
                    } catch (IOException e) {             
                        SwingUtilities.invokeLater(() -> {
                            statusLabel.setText("Disconnected from server");
                            statusLabel.setForeground(new Color(200, 0, 0));
                            chatArea.append("Server disconnected.\n"); 
                        });
                    }
                }
            }).start();

        } catch (IOException e) {                
            statusLabel.setText("Connection failed");
            statusLabel.setForeground(new Color(200, 0, 0));
            chatArea.append("Error: " + e.getMessage() + "\n"); 
        }
    }

    // Gửi tin nhắn đến server (chỉ lưu vào lịch sử chat riêng)
    private void sendMessage() {
        String message = inputField.getText().trim(); 
        RecipientItem selected = (RecipientItem) recipientSelector.getSelectedItem();
        String recipient = selected == null ? null : selected.name;
        if (!message.isEmpty() && recipient != null && !recipient.trim().isEmpty()) {
            String formatted = "to:" + recipient + ":" + message;
            out.println(formatted);              // Gửi tin nhắn trực tiếp
            chatArea.append("You->" + recipient + ": " + message + "\n");
            inputField.setText("");
            historyCleared = false; // Reset flag khi gửi tin nhắn mới
        }
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

    // Renderer hiển thị trạng thái với chấm màu
    private static class RecipientRenderer extends DefaultListCellRenderer {
        private final boolean compact;
        RecipientRenderer(boolean compact) { this.compact = compact; }
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof RecipientItem) {
                RecipientItem ri = (RecipientItem) value;
                setText(compact ? ri.name : (ri.name + (ri.online ? " (Online)" : " (Offline)")));
                setIcon(new ColorIcon(ri.online ? Color.GREEN : Color.RED, 8));
            }
            return this;
        }
    }

    // Icon màu đơn giản
    private static class ColorIcon implements Icon {
        private final Color color;
        private final int size;
        ColorIcon(Color color, int size) { this.color = color; this.size = size; }
        @Override public void paintIcon(Component c, Graphics g, int x, int y) { g.setColor(color); g.fillOval(x, y, size, size); }
        @Override public int getIconWidth() { return size; }
        @Override public int getIconHeight() { return size; }
    }

    // Phương thức main để khởi động client
    public static void main(String[] args) {
        new Client();                        
    }
}	