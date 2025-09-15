
<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
   XÂY DỰNG ỨNG DỤNG CHAT CLIENT-SERVER SỬ DỤNG GIAO THỨC TCP
</h2>
<div align="center">
    <p align="center">
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>


## 📖 1. Giới thiệu hệ thống
Ứng dụng chat Client-Server sử dụng giao thức TCP cho phép nhiều người dùng giao tiếp thời gian thực qua mạng. Server đóng vai trò trung tâm, quản lý kết nối và chuyển tiếp tin nhắn, trong khi client cung cấp giao diện người dùng để gửi và nhận tin nhắn. Dữ liệu được lưu trữ dưới dạng file văn bản thay vì cơ sở dữ liệu, giúp đơn giản hóa triển khai.

Các chức năng chính: 
1. Kết nối và xác thực cơ bản: Client kết nối đến server qua địa chỉ IP và port (mặc định: 1234). Server hỗ trợ nhiều client đồng thời thông qua đa luồng.
2. Gửi và nhận tin nhắn: Người dùng gửi tin nhắn từ client, server nhận và phát tán (broadcast) đến tất cả client khác, hỗ trợ chat nhóm.
3. Lưu trữ lịch sử chat: Server lưu tin nhắn vào file chat_history.txt với định dạng [Timestamp] - [Tên người dùng]: [Nội dung]. Client mới có thể tải lịch sử từ file.
4. Quản lý người dùng: Server theo dõi danh sách client online, cập nhật khi có kết nối/ngắt kết nối. Client hiển thị danh sách này (tùy chọn).
5. Giao diện người dùng: Sử dụng Java Swing với cửa sổ chat gồm khu vực hiển thị tin nhắn, ô nhập văn bản và nút gửi.
6. Xử lý lỗi: Xử lý các trường hợp như mất kết nối hoặc lỗi ghi file.
Hệ thống sử dụng TCP để đảm bảo truyền tin nhắn đáng tin cậy, không hỗ trợ mã hóa hoặc bảo mật nâng cao trong phiên bản cơ bản.

## 🔧 2. Công nghệ sử dụng
Dưới đây là mô tả chi tiết về các công nghệ được sử dụng để xây dựng ứng dụng chat Client-Server sử dụng TCP với Java Swing, dựa trên yêu cầu của bạn:

#### Java Core và Multithreading:
Sử dụng ExecutorService (thuộc gói java.util.concurrent) để quản lý một pool các luồng (thread) trên server, cho phép xử lý đồng thời nhiều kết nối client mà không cần tạo thủ công từng Thread. Điều này giúp cải thiện hiệu suất và quản lý tài nguyên hiệu quả hơn so với sử dụng Thread trực tiếp. Ví dụ: Executors.newFixedThreadPool() được dùng để giới hạn số luồng tối đa, mỗi luồng xử lý một client.

#### Java Swing:
Xây dựng giao diện đồ họa (GUI) cho client sử dụng các thành phần của gói javax.swing.*:

    JFrame: Cửa sổ chính của ứng dụng client.
    JTextArea: Hiển thị lịch sử tin nhắn, đặt trong JScrollPane để hỗ trợ cuộn khi số lượng tin nhắn dài.
    JTextField: Ô nhập liệu để người dùng gõ tin nhắn.
    JButton: Nút "Gửi" để gửi tin nhắn khi nhấn hoặc khi nhấn Enter.
    JScrollPane: Bao quanh JTextArea để cung cấp thanh cuộn, cải thiện trải nghiệm người dùng.

Swing cung cấp giao diện thân thiện, dễ tùy chỉnh mà không cần thư viện bên ngoài.

#### Java Sockets:
Sử dụng gói java.net.* để triển khai kết nối mạng theo giao thức TCP:

    ServerSocket: Được server sử dụng để lắng nghe các kết nối đến trên một cổng cụ thể (ví dụ: port 1234). Phương thức accept() trả về Socket cho mỗi client kết nối.
    Socket: Được client sử dụng để kết nối đến server thông qua địa chỉ IP và port.
    DataInputStream và DataOutputStream: Xử lý việc đọc/ghi dữ liệu dạng nhị phân giữa client và server, đảm bảo truyền tin nhắn chính xác, tuần tự và không mất mát. 

Đây là lựa chọn phù hợp khi cần truyền dữ liệu đơn giản như chuỗi văn bản.

#### File I/O:

Sử dụng các lớp trong gói java.io.* để lưu trữ và truy xuất lịch sử chat:

    FileWriter hoặc BufferedWriter: Ghi tin nhắn vào file văn bản (ví dụ: chat_history.txt) theo chế độ append để không ghi đè dữ liệu cũ. Mỗi tin nhắn được lưu với định dạng như [Timestamp] - [Tên người dùng]: [Nội dung].
    BufferedReader: Đọc lịch sử tin nhắn từ file để hiển thị khi client mới kết nối hoặc khi người dùng yêu cầu tải lịch sử.
Sử dụng từ khóa synchronized hoặc Lock (từ java.util.concurrent.locks) để đảm bảo an toàn luồng (thread-safe) khi nhiều client gửi tin nhắn đồng thời, tránh xung đột ghi file.

#### Hỗ trợ:

    java.util.Date hoặc java.time.LocalDateTime: Tạo timestamp cho mỗi tin nhắn để ghi vào file và hiển thị trên giao diện, giúp người dùng theo dõi thời gian gửi.
    ArrayList: Quản lý danh sách các client đang kết nối trên server (lưu trữ PrintWriter hoặc DataOutputStream của từng client) để broadcast tin nhắn. Có thể mở rộng để lưu danh sách tên người dùng và trạng thái online/offline.
Không sử dụng thư viện bên ngoài, đảm bảo ứng dụng nhẹ và dễ triển khai trên mọi môi trường Java.

## 🚀 3. Hình ảnh các chức năng

<p align="center">
  <img src="docs/anhGiaoDien.jpg" alt="Ảnh 1" width="800"/>
</p>

<p align="center">
  <em>Hình 1: Ảnh giao diện chat giữa Client-Server  Hình 2: Ảnh 2 Client chat với Server</em>
</p>

<p align="center">
  <img src="docs/anhClientChatServer.jpg" alt="Ảnh 2" width="300"/>
</p>
<p align="center">
  <em> Hình 2: Ảnh 2 Client chat với Server</em>
</p>

<p align="center">
  <img src="docs/anhServertraloiClientLA.jpg" alt="Ảnh 3" width="500"/>
    <img src="docs/anhServertraloiClientHoa.jpg" alt="Ảnh 4" width="500"/>
</p>
<p align="center">
  <em> Hình 3: Ảnh Server trả lời Client Lanh - Hình 4: Ảnh Server trả lời Client Hoa</em>
</p>

<p align="center">
  <img src="docs/anhLichSuChatLuuTxt.jpg" alt="Ảnh 5" width="500"/>
    <img src="docs/anhServerxoaDL.jpg" alt="Ảnh 6" width="300"/>
</p>
<p align="center">
  <em> Hình 5: Ảnh lịch sử chat được lưu vào file txt - Hình 5: Ảnh Server xóa dữ liệu</em>
</p>

<p align="center">
  <img src="docs/anhServerngatKetNoiClient.jpg" alt="Ảnh 7" width="600"/>
</p>
<p align="center">
  <em> Hình 7: Ảnh Server ngắt kết nối với CLient</em>
</p>

## 📝 4. Hướng dẫn cài đặt và sử dụng

### 🔧 Yêu cầu hệ thống

- **Java Development Kit (JDK)**: Phiên bản 8 trở lên
- **Hệ điều hành**: Windows, macOS, hoặc Linux
- **Môi trường phát triển**: IDE (IntelliJ IDEA, Eclipse, VS Code) hoặc terminal/command prompt
- **Bộ nhớ**: Tối thiểu 512MB RAM
- **Dung lượng**: Khoảng 10MB cho mã nguồn và file thực thi

### 📦 Cài đặt và triển khai

#### Bước 1: Chuẩn bị môi trường
1. **Kiểm tra Java**: Mở terminal/command prompt và chạy:
   ```bash
   java -version
   javac -version
   ```
   Đảm bảo cả hai lệnh đều hiển thị phiên bản Java 8 trở lên.

2. **Tải mã nguồn**: Sao chép thư mục `UngDungChat_TCP` chứa các file:
   - `Server.java`
   - `Client.java`

#### Bước 2: Biên dịch mã nguồn
1. **Mở terminal** và điều hướng đến thư mục chứa mã nguồn
2. **Biên dịch các file Java**:
   ```bash
   javac UngDungChat_TCP/*.java
   ```
   Hoặc biên dịch từng file riêng lẻ:
   ```bash
   javac UngDungChat_TCP/Server.java
   javac UngDungChat_TCP/Client.java
   ```

3. **Kiểm tra kết quả**: Nếu biên dịch thành công, sẽ tạo ra các file `.class` tương ứng.

#### Bước 3: Chạy ứng dụng

**Khởi động Server:**
```bash
java UngDungChat_TCP.Server
```
- Server sẽ khởi động trên port mặc định (1234)
- Giao diện server sẽ hiển thị, sẵn sàng nhận kết nối từ client
- Server sẽ tạo file `chat_history.txt` để lưu lịch sử chat

**Khởi động Client:**
```bash
java UngDungChat_TCP.Client
```
- Mở terminal mới cho mỗi client
- Nhập tên người dùng khi được yêu cầu (ví dụ: "Lanh", "Hoa", "Minh")
- Client sẽ kết nối đến server và hiển thị giao diện chat

### 🚀 Sử dụng ứng dụng

1. **Kết nối**: Client tự động kết nối đến server sau khi nhập tên
2. **Gửi tin nhắn**: Gõ tin nhắn vào ô nhập và nhấn Enter hoặc nút "Gửi"
3. **Nhận tin nhắn**: Tin nhắn từ các client khác sẽ hiển thị trong khu vực chat
4. **Lịch sử chat**: Server tự động lưu tất cả tin nhắn vào file `chat_history.txt`
5. **Ngắt kết nối**: Đóng cửa sổ client hoặc nhấn Ctrl+C để ngắt kết nối

### ⚠️ Lưu ý quan trọng

- **Thứ tự khởi động**: Luôn khởi động Server trước khi chạy Client
- **Port**: Đảm bảo port 1234 không bị sử dụng bởi ứng dụng khác
- **Firewall**: Có thể cần cấu hình firewall để cho phép kết nối
- **Mạng**: Server và Client phải cùng mạng hoặc có thể truy cập lẫn nhau
- **File lịch sử**: File `chat_history.txt` sẽ được tạo tự động trong thư mục chứa Server

### 🔧 Khắc phục sổ lỗi thường gặp

- **"Port already in use"**: Thay đổi port trong mã nguồn hoặc đóng ứng dụng đang sử dụng port
- **"Connection refused"**: Kiểm tra Server đã khởi động chưa và địa chỉ IP có đúng không
- **"Class not found"**: Đảm bảo đã biên dịch thành công và đang chạy từ đúng thư mục

© 2025 AIoTLab, Faculty of Information Technology, DaiNam University. All rights reserved.

---
