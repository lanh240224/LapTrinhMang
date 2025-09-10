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
    private JFrame frame;              
    private JTextArea chatArea;        
    private JTextField inputField;    
    private JButton sendButton;        
    private JButton clearHistoryButton; 
    private JComboBox<String> clientSelector; 
    private ServerSocket serverSocket; 
    private List<ClientHandler> clients; 
    // Constructor khởi tạo server, tạo GUI và bắt đầu lắng nghe
    public Server() {
        clients = new ArrayList<>();    
        initializeGUI();                
        startServer();                 
        }

    // Khởi tạo giao diện Swing cho server
    public void initializeGUI() {
        frame = new JFrame("Server Chat"); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        frame.setSize(400, 350);        

        chatArea = new JTextArea();      
        chatArea.setEditable(false);     
        JScrollPane scrollPane = new JScrollPane(chatArea);
        frame.add(scrollPane, BorderLayout.CENTER); 

        inputField = new JTextField();   
        sendButton = new JButton("Send"); 
        clearHistoryButton = new JButton("Clear History"); 
        clientSelector = new JComboBox<>(); 

        JPanel panel = new JPanel();     
        panel.setLayout(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER); 
        panel.add(sendButton, BorderLayout.EAST);  
        frame.add(panel, BorderLayout.SOUTH);      

        JPanel topPanel = new JPanel(); 
        topPanel.add(new JLabel("Select Client:")); 
        topPanel.add(clientSelector);   
        topPanel.add(clearHistoryButton); 
        frame.add(topPanel, BorderLayout.NORTH);   

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
            serverSocket = new ServerSocket(5000); 
            chatArea.append("Server started. Waiting for clients...\n");

            while (true) {                   
                Socket clientSocket = serverSocket.accept(); 
                chatArea.append("Client connected: " + clientSocket.getInetAddress().getHostAddress() + "\n");
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);  
                clientHandler.start();        
            }
        } catch (IOException e) {        
            chatArea.append("Error: " + e.getMessage() + "\n");
        }
    }

    // Gửi tin nhắn đến client được chọn
    public void sendMessage() {
        String message = inputField.getText(); 
        if (!message.isEmpty() && clientSelector.getSelectedItem() != null) {
            String selectedClient = (String) clientSelector.getSelectedItem(); 
            for (ClientHandler client : clients) {
                if (client.getClientName().equals(selectedClient)) {
                    client.sendMessage("Server: " + message); 
                    chatArea.append("Server: " + message + " (to " + selectedClient + ")\n");  
                    saveMessageToFile(selectedClient, "Server: " + message); 
                    break;
                }
            }
            inputField.setText("");          
        }
    }

    // Xóa lịch sử chat của client được chọn
    public void clearChatHistory() {
        if (clientSelector.getSelectedItem() != null) {  
            String selectedClient = (String) clientSelector.getSelectedItem();  
            String fileName = selectedClient + "_chat_history.txt";  
            try (PrintWriter writer = new PrintWriter(new FileWriter(fileName))) {  
                writer.print("");            // Xóa nội dung
                chatArea.append("Chat history cleared for " + selectedClient + ".\n"); 
                for (ClientHandler client : clients) {  
                    if (client.getClientName().equals(selectedClient)) {
                        client.sendMessage("[History Cleared]");  
                        break;
                    }
                }
            } catch (IOException e) {          
                chatArea.append("Error clearing history: " + e.getMessage() + "\n");
            }
        }
    }

    // Cập nhật danh sách client trong JComboBox trên giao diện
    public void updateClientSelector() {
        SwingUtilities.invokeLater(() -> {  
            clientSelector.removeAllItems();  
            for (ClientHandler client : clients) {  
                String name = client.getClientName();
                if (name != null) {         
                    clientSelector.addItem(name);  
                }
            }
            if (clients.isEmpty() || clientSelector.getItemCount() == 0) {  
                clientSelector.addItem("No clients connected");  
            }
        });
    }

    // Lưu tin nhắn vào file lịch sử của client
    public void saveMessageToFile(String clientName, String message) {
        String fileName = clientName + "_chat_history.txt"; 
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {  
            writer.println(message);      
        } catch (IOException e) {        
            chatArea.append("Error saving message: " + e.getMessage() + "\n");
        }
    }

    // Lớp nội bộ xử lý từng client như một thread
    public class ClientHandler extends Thread {
        private Socket socket;            
        private PrintWriter out;         
        private BufferedReader in;        
        private String clientName;       

        // Constructor khởi tạo với socket
        public ClientHandler(Socket socket) {
            this.socket = socket;        
        }

        // Chạy thread xử lý client
        @Override
        public void run() {
            try {
                out = new PrintWriter(socket.getOutputStream(), true);  
                in = new BufferedReader(new InputStreamReader(socket.getInputStream())); // Tạo dòng nhập

                // Nhận tên từ client
                clientName = in.readLine();
                chatArea.append("Client name: " + clientName + "\n");  
                // Cập nhật danh sách client trên GUI
                Server.this.updateClientSelector();

                // Load và gửi lịch sử chat
                loadChatHistory();
                sendChatHistoryToClient();

                String message;
                while ((message = in.readLine()) != null) {  
                    chatArea.append(clientName + ": " + message + "\n");  
                    saveMessageToFile(clientName, clientName + ": " + message); 
                }
            } catch (IOException e) {         
                chatArea.append(clientName + " disconnected.\n");
            } finally {                      
                clients.remove(this);        
                Server.this.updateClientSelector();  
                try {
                    socket.close();           
                } catch (IOException e) {     
                    chatArea.append("Error closing socket: " + e.getMessage() + "\n");
                }
            }
        }

        // Gửi tin nhắn đến client
        public void sendMessage(String message) {
            out.println(message);        
        }

        // Lấy tên của client
        public String getClientName() {
            return clientName;            
        }

        // Load lịch sử chat từ file
        private void loadChatHistory() {
            String fileName = clientName + "_chat_history.txt";
            try (BufferedReader fileReader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = fileReader.readLine()) != null) {
                    chatArea.append(line + "\n");  
                }
            } catch (IOException e) {      
                // Không làm gì
            }
        }

        // Gửi lịch sử chat đến client
        private void sendChatHistoryToClient() {
            String fileName = clientName + "_chat_history.txt";
            try (BufferedReader fileReader = new BufferedReader(new FileReader(fileName))) {
                String line;
                while ((line = fileReader.readLine()) != null) {
                    out.println("[History] " + line);  
                }
            } catch (IOException e) {      
                // Không làm gì
            }
        }
    }

    // Phương thức main để khởi động server
    public static void main(String[] args) {
        new Server();                 
    }
}