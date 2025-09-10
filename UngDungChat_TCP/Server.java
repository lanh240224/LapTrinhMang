package UngDungChat_TCP;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;

public class Server {
    private JFrame frame;              // Cửa sổ giao diện chính của server
    private JTextArea chatArea;        // Khu vực hiển thị log (kết nối, tin nhắn, lỗi)
    private JTextField inputField;     // Ô nhập tin nhắn từ server
    private JButton sendButton;        // Nút gửi tin nhắn
    private JButton clearHistoryButton; // Nút xóa lịch sử chat
    private JComboBox<String> clientSelector; // Danh sách thả xuống chọn client
    private ServerSocket serverSocket; // Socket server lắng nghe kết nối
    private List<ClientHandler> clients; // Danh sách các client đang kết nối

    // Constructor khởi tạo server, tạo GUI và bắt đầu lắng nghe
    public Server() {
        clients = new ArrayList<>();    // Khởi tạo danh sách client
        initializeGUI();                // Thiết lập giao diện
        startServer();                  // Bắt đầu server
    }

    // Khởi tạo giao diện Swing cho server
    public void initializeGUI() {
        frame = new JFrame("Server Chat"); // Tạo cửa sổ với tiêu đề
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // Đóng ứng dụng khi thoát
        frame.setSize(400, 350);         // Đặt kích thước cửa sổ

        chatArea = new JTextArea();      // Tạo khu vực hiển thị log
        chatArea.setEditable(false);     // Không cho phép chỉnh sửa
        JScrollPane scrollPane = new JScrollPane(chatArea); // Thêm thanh cuộn
        frame.add(scrollPane, BorderLayout.CENTER); // Đặt vào trung tâm

        inputField = new JTextField();   // Ô nhập tin nhắn
        sendButton = new JButton("Send"); // Nút gửi
        clearHistoryButton = new JButton("Clear History"); // Nút xóa lịch sử
        clientSelector = new JComboBox<>(); // Danh sách chọn client

        JPanel panel = new JPanel();     // Tạo panel cho ô nhập và nút
        panel.setLayout(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER); // Ô nhập ở giữa
        panel.add(sendButton, BorderLayout.EAST);   // Nút gửi bên phải
        frame.add(panel, BorderLayout.SOUTH);       // Đặt panel dưới cùng

        JPanel topPanel = new JPanel();  // Tạo panel cho phần trên
        topPanel.add(new JLabel("Select Client:")); // Nhãn chọn client
        topPanel.add(clientSelector);    // Thêm danh sách client
        topPanel.add(clearHistoryButton); // Thêm nút xóa lịch sử
        frame.add(topPanel, BorderLayout.NORTH);    // Đặt panel trên cùng

        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();           // Gọi phương thức gửi tin nhắn khi nhấn
            }
        });

        clearHistoryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearChatHistory();      // Gọi phương thức xóa lịch sử khi nhấn
            }
        });

        frame.setVisible(true);          // Hiển thị giao diện
    }

    // Bắt đầu server và lắng nghe kết nối từ client
    public void startServer() {
        try {
            serverSocket = new ServerSocket(5000); // Tạo server socket trên port 5000
            chatArea.append("Server started. Waiting for clients...\n"); // Thông báo khởi động

            while (true) {                   // Vòng lặp vô hạn lắng nghe
                Socket clientSocket = serverSocket.accept(); // Chấp nhận kết nối
                chatArea.append("Client connected: " + clientSocket.getInetAddress().getHostAddress() + "\n");
                ClientHandler clientHandler = new ClientHandler(clientSocket); // Tạo handler cho client
                clients.add(clientHandler);   // Thêm vào danh sách
                clientHandler.start();        // Khởi động thread xử lý
            }
        } catch (IOException e) {         // Xử lý lỗi kết nối
            chatArea.append("Error: " + e.getMessage() + "\n");
        }
    }

    // Gửi tin nhắn đến client được chọn
    public void sendMessage() {
        String message = inputField.getText(); // Lấy tin nhắn từ ô nhập
        if (!message.isEmpty() && clientSelector.getSelectedItem() != null) { // Kiểm tra đầu vào
            String selectedClient = (String) clientSelector.getSelectedItem(); // Lấy tên client
            for (ClientHandler client : clients) { // Tìm client trong danh sách
                if (client.getClientName().equals(selectedClient)) {
                    client.sendMessage("Server: " + message); // Gửi tin nhắn
                    chatArea.append("Server: " + message + " (to " + selectedClient + ")\n"); // Hiển thị log
                    saveMessageToFile(selectedClient, "Server: " + message); // Lưu file
                    break;
                }
            }
            inputField.setText("");          // Xóa ô nhập
        }
    }

    // Xóa lịch sử chat của client được chọn
    public void clearChatHistory() {
        if (clientSelector.getSelectedItem() != null) { // Kiểm tra có client được chọn
            String selectedClient = (String) clientSelector.getSelectedItem(); // Lấy tên client
            String fileName = selectedClient + "_chat_history.txt"; // Tên file lịch sử
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) { // Mở file để ghi
                writer.print("");            // Xóa nội dung
                chatArea.append("Chat history cleared for " + selectedClient + ".\n"); // Thông báo
                for (ClientHandler client : clients) { // Tìm client
                    if (client.getClientName().equals(selectedClient)) {
                        client.sendMessage("[History Cleared]"); // Gửi thông báo xóa
                        break;
                    }
                }
            } catch (IOException e) {         // Xử lý lỗi ghi file
                chatArea.append("Error clearing history: " + e.getMessage() + "\n");
            }
        }
    }

    // Cập nhật danh sách client trong JComboBox trên giao diện
    public void updateClientSelector() {
        SwingUtilities.invokeLater(() -> { // Đảm bảo cập nhật trên thread GUI
            clientSelector.removeAllItems(); // Xóa tất cả item cũ
            for (ClientHandler client : clients) { // Duyệt danh sách client
                String name = client.getClientName();
                if (name != null) {          // Kiểm tra tên không null
                    clientSelector.addItem(name); // Thêm tên vào selector
                }
            }
            if (clients.isEmpty() || clientSelector.getItemCount() == 0) { // Nếu không có client
                clientSelector.addItem("No clients connected"); // Hiển thị thông báo
            }
        });
    }

    // Lưu tin nhắn vào file lịch sử của client
    public void saveMessageToFile(String clientName, String message) {
        String fileName = clientName + "_chat_history.txt"; // Tên file dựa trên tên client
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) { // Mở file ở chế độ append
            writer.println(message);      // Ghi tin nhắn
        } catch (IOException e) {        // Xử lý lỗi ghi file
            chatArea.append("Error saving message: " + e.getMessage() + "\n");
        }
    }

    // Lớp nội bộ xử lý từng client như một thread
    public class ClientHandler extends Thread {
        private Socket socket;           // Socket kết nối với client
        private PrintWriter out;         // Dòng xuất để gửi tin nhắn
        private BufferedReader in;       // Dòng nhập để nhận tin nhắn
        private String clientName;       // Tên của client

        // Constructor khởi tạo với socket
        public ClientHandler(Socket socket) {
            this.socket = socket;         // Gán socket
        }

        // Chạy thread xử lý client
        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true); // Tạo dòng xuất
                in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Tạo dòng nhập

                // Nhận tên từ client
                clientName = in.readLine();
                chatArea.append("Client name: " + clientName + "\n"); // Hiển thị tên

                // Cập nhật danh sách client trên GUI
                Server.this.updateClientSelector();

                // Load và gửi lịch sử chat
                loadChatHistory();
                sendChatHistoryToClient();

                String message;
                while ((message = in.readLine()) != null) { // Nhận tin nhắn
                    chatArea.append(clientName + ": " + message + "\n"); // Hiển thị
                    saveMessageToFile(clientName, clientName + ": " + message); // Lưu
                }
            } catch (IOException e) {         // Xử lý lỗi kết nối
                chatArea.append(clientName + " disconnected.\n");
            } finally {                      // Khi client ngắt kết nối
                clients.remove(this);        // Xóa khỏi danh sách
                Server.this.updateClientSelector(); // Cập nhật selector
                try {
                    socket.close();          // Đóng socket
                } catch (IOException e) {    // Xử lý lỗi đóng socket
                    chatArea.append("Error closing socket: " + e.getMessage() + "\n");
                }
            }
        }

        // Gửi tin nhắn đến client
        public void sendMessage(String message) {
            out.println(message);        // Gửi tin nhắn qua socket
        }

        // Lấy tên của client
        public String getClientName() {
            return clientName;           // Trả về tên client
        }

        // Load lịch sử chat từ file
        private void loadChatHistory() {
            String fileName = clientName + "_chat_history.txt";
            try (BufferedReader fileReader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = fileReader.readLine()) != null) {
                    chatArea.append(line + "\n"); // Hiển thị lịch sử
                }
            } catch (IOException e) {     // Bỏ qua nếu file không tồn tại
                // Không làm gì
            }
        }

        // Gửi lịch sử chat đến client
        private void sendChatHistoryToClient() {
            String fileName = clientName + "_chat_history.txt";
            try (BufferedReader fileReader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = fileReader.readLine()) != null) {
                    out.println("[History] " + line); // Gửi với prefix
                }
            } catch (IOException e) {     // Bỏ qua nếu không gửi được
                // Không làm gì
            }
        }
    }

    // Phương thức main để khởi động server
    public static void main(String[] args) {
        new Server();                // Tạo và chạy instance server
    }
}