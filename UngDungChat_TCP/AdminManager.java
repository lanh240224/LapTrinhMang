package UngDungChat_TCP;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

public class AdminManager {
    private static final String ADMIN_FILE = "admin_accounts.dat";
    private Map<String, String> adminAccounts; // username -> password hash
    
    public AdminManager() {
        adminAccounts = new HashMap<>();
        loadAdminAccounts();
        
        // Tạo tài khoản admin mặc định nếu chưa có
        if (adminAccounts.isEmpty()) {
            createDefaultAdmin();
        }
    }
    
    private void loadAdminAccounts() {
        try {
            File file = new File(ADMIN_FILE);
            if (file.exists()) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> loadedAccounts = (Map<String, String>) ois.readObject();
                    adminAccounts = loadedAccounts;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading admin accounts: " + e.getMessage());
            adminAccounts = new HashMap<>();
        }
    }
    
    private void saveAdminAccounts() {
        try {
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(ADMIN_FILE))) {
                oos.writeObject(adminAccounts);
            }
        } catch (IOException e) {
            System.err.println("Error saving admin accounts: " + e.getMessage());
        }
    }
    
    private void createDefaultAdmin() {
        // Tạo tài khoản admin mặc định: admin/admin123
        String defaultUsername = "admin";
        String defaultPassword = "12344321";
        adminAccounts.put(defaultUsername, hashPassword(defaultPassword));
        saveAdminAccounts();
    }
    
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    public boolean authenticate(String username, String password) {
        String storedHash = adminAccounts.get(username);
        if (storedHash == null) {
            return false;
        }
        return storedHash.equals(hashPassword(password));
    }
    
    public boolean registerAdmin(String username, String password) {
        if (adminAccounts.containsKey(username)) {
            return false; // Username đã tồn tại
        }
        
        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            return false; // Username hoặc password rỗng
        }
        
        adminAccounts.put(username, hashPassword(password));
        saveAdminAccounts();
        return true;
    }
    
    public boolean changePassword(String username, String oldPassword, String newPassword) {
        if (!authenticate(username, oldPassword)) {
            return false; // Mật khẩu cũ không đúng
        }
        
        if (newPassword.trim().isEmpty()) {
            return false; // Mật khẩu mới rỗng
        }
        
        adminAccounts.put(username, hashPassword(newPassword));
        saveAdminAccounts();
        return true;
    }
    
    public boolean deleteAdmin(String username, String password) {
        if (!authenticate(username, password)) {
            return false; // Mật khẩu không đúng
        }
        
        if (adminAccounts.size() <= 1) {
            return false; // Không thể xóa tài khoản cuối cùng
        }
        
        adminAccounts.remove(username);
        saveAdminAccounts();
        return true;
    }
    
    public int getAdminCount() {
        return adminAccounts.size();
    }
}
