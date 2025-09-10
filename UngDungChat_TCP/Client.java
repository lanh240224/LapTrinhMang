package UngDungChat_TCP;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import javax.swing.*;

public class Client {
    private JFrame frame;              
    private JTextArea chatArea;        
    private JTextField inputField;     
    private JButton sendButton;        
    private Socket socket;             
    private PrintWriter out;           
    private BufferedReader in;        

    // Constructor khởi tạo client, tạo GUI và kết nối đến server
    public Client() {
        initializeGUI();                
        connectToServer();             
    }

    // Khởi tạo giao diện Swing cho client
    private void initializeGUI() {
        frame = new JFrame("Client Chat"); 
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        frame.setSize(400, 300);        

        chatArea = new JTextArea();      
        chatArea.setEditable(false);     
        JScrollPane scrollPane = new JScrollPane(chatArea); 
        frame.add(scrollPane, BorderLayout.CENTER); 
        inputField = new JTextField();   
        sendButton = new JButton("Send"); 

        JPanel panel = new JPanel();     
        panel.setLayout(new BorderLayout());
        panel.add(inputField, BorderLayout.CENTER); 
        panel.add(sendButton, BorderLayout.EAST);   
        frame.add(panel, BorderLayout.SOUTH);      
        sendButton.addActionListener(new ActionListener() { 
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();           
            }
        });

        frame.setVisible(true);          
    }

    // Kết nối đến server và xử lý luồng nhận tin nhắn
    private void connectToServer() {
        String name = JOptionPane.showInputDialog(frame, "Enter your name:"); 
        if (name == null || name.trim().isEmpty()) { 
            name = "Anonymous";                      
        }

        frame.setTitle(name + " Chat");      

        try {
            socket = new Socket("localhost", 5000);  
            chatArea.append("Connected to server.\n"); 

            out = new PrintWriter(socket.getOutputStream(), true); 
            in = new BufferedReader(new InputStreamReader(socket.getInputStream())); 

            out.println(name);                       

            new Thread(new Runnable() {              
                @Override
                public void run() {
                    String message;
                    try {
                        while ((message = in.readLine()) != null) { 
                            if (message.startsWith("[History]")) { 
                                chatArea.append(message.substring(9) + "\n"); 
                            } else {
                                chatArea.append("Server: " + message + "\n"); 
                            }
                        }
                    } catch (IOException e) {             
                        chatArea.append("Server disconnected.\n"); 
                    }
                }
            }).start();

        } catch (IOException e) {                
            chatArea.append("Error: " + e.getMessage() + "\n"); 
        }
    }

    // Gửi tin nhắn đến server
    private void sendMessage() {
        String message = inputField.getText(); 
        if (!message.isEmpty()) {              
            out.println(message);              
            chatArea.append("Client: " + message + "\n");
            inputField.setText("");            
        }
    }

    // Phương thức main để khởi động client
    public static void main(String[] args) {
        new Client();                        
    }
}