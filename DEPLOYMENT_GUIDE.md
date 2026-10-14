# Hướng Dẫn Toàn Diện: Triển Khai Fullstack Campus Hub (React + Spring Boot + MySQL) Với Nginx, Git & UFW Trên Linux

> **Thông tin môi trường thực tế:**
> - **Hệ điều hành:** Ubuntu 22.04 LTS
> - **Web Server / Reverse Proxy:** Nginx 1.18.0+
> - **Backend Runtime:** OpenJDK 21 (Spring Boot 3.5.7)
> - **Frontend Runtime:** Node.js 20 LTS (Vite + React)
> - **Cơ sở dữ liệu:** MySQL Server 8.0 (Database: `campus_hub`)
> - **Quản lý mã nguồn:** Git & GitHub
> - **Tường lửa:** UFW (OS Level) & Cloud Security Group (Cổng mở public: `22`, `80`, `443`; bảo vệ nội bộ: `8080`, `3306`)
> - **Thư mục dự án trên máy chủ:** `/var/www/campus-hub`

---

## PHẦN 1: QUY TRÌNH TRIỂN KHAI TỪ ĐẦU ĐẾN CUỐI

```mermaid
flowchart TD
    subgraph Local ["1. Máy Cá Nhân (Local)"]
        Dev["Code & Test dự án"] -->|"git push"| GitRepo["GitHub Repository"]
    end

    subgraph VPS_OS ["2. Máy Chủ VPS Linux (Ubuntu 22.04)"]
        User["User an toàn: deployer"] --> FW["Tường lửa UFW (Mở 22, 80, 443)"]
        FW --> Tools["Cài đặt: OpenJDK 21, Node.js 20, MySQL, Nginx, Git"]
        Tools --> DB[("MySQL Database: campus_hub (Port 3306 nội bộ)")]
        
        GitRepo -->|"git clone"| AppDir["/var/www/campus-hub"]
        
        AppDir --> BuildFE["Build Frontend: npm run build ➜ frontend/dist"]
        AppDir --> BuildBE["Build Backend: ./gradlew bootJar ➜ .jar"]
        
        BuildBE --> Systemd["Systemd Service: campus-hub.service (Port 8080)"]
        Systemd <-->|"JDBC Flyway & Data"| DB
        
        Nginx["Nginx Reverse Proxy (Port 80)"]
        Nginx -->|"Static files: /"| BuildFE
        Nginx -->|"Proxy: /api, /actuator"| Systemd
    end

    Client["3. Trình Duyệt Người Dùng (Client)"] -->|"HTTP/HTTPS :80/:443"| Nginx
```

---

### Bước 1: Chuẩn bị mã nguồn trên máy cá nhân & đẩy lên GitHub

Tại thư mục gốc dự án trên máy tính cá nhân:

| Lệnh | Ý nghĩa & Giải thích tham số |
| :--- | :--- |
| `git init` | Khởi tạo kho lưu trữ Git cục bộ nếu chưa có. |
| `git add .` | Đưa toàn bộ mã nguồn backend, frontend và file cấu hình vào Staging Area. |
| `git commit -m "feat: complete campus hub base fullstack"` | Đóng gói các thay đổi vào lịch sử Git với thông điệp mô tả. |
| `git branch -M main` | Đổi tên nhánh hiện tại thành `main`. |
| `git remote add origin <URL_REPO_GITHUB>` | Liên kết kho lưu trữ máy tính với repository trên GitHub. |
| `git push -u origin main` | Đẩy commit lên nhánh `main` của GitHub (`-u` đặt upstream cho các lần sau). |

---

### Bước 2: Thiết lập User an toàn & Bộ nhớ đệm (Swap) trên máy chủ Linux

Đăng nhập vào VPS ban đầu bằng tài khoản `root`:
```bash
ssh root@<IP_SERVER>
```

Tạo user quản trị `deployer` để tránh thao tác trực tiếp bằng `root` và tạo file Swap bộ nhớ ảo (đặc biệt quan trọng với VPS 1GB - 2GB RAM tránh bị tràn bộ nhớ khi build):

| Lệnh | Ý nghĩa & Giải thích tham số |
| :--- | :--- |
| `apt update && apt upgrade -y` | `apt update` làm mới danh sách package; `apt upgrade -y` cập nhật toàn bộ hệ điều hành lên bản mới nhất. |
| `adduser deployer` | Tạo tài khoản người dùng mới tên là `deployer` kèm thư mục `/home/deployer`. |
| `usermod -aG sudo deployer` | Thêm user `deployer` vào nhóm `sudo` (`-a` append, `-G` nhóm phụ) để có quyền quản trị. |
| `fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile` | Tạo và kích hoạt bộ nhớ ảo 2GB Swap giúp ngăn hiện tượng Gradle/Node bị hệ thống dừng tiến trình do thiếu RAM (Out of Memory). |
| `su - deployer` | Chuyển phiên làm việc sang tài khoản `deployer` (dấu `-` để nạp đủ biến môi trường). |

---

### Bước 3: Cài đặt và cấu hình tường lửa UFW (Firewall tầng hệ điều hành)

> ⚠️ **CỰC KỲ QUAN TRỌNG:** 
> - Luôn mở cổng SSH (`22/tcp`) **TRƯỚC KHI** bật UFW (`sudo ufw enable`), nếu không bạn sẽ bị khóa quyền truy cập VPS từ xa!
> - **KHÔNG** mở cổng `8080` (Spring Boot) và `3306` (MySQL) ra internet. Chỉ mở cổng `80` và `443` cho Nginx tiếp nhận và ủy quyền ngược nội bộ.

| Lệnh | Ý nghĩa & Giải thích tham số |
| :--- | :--- |
| `sudo apt install ufw -y` | Cài đặt gói tường lửa UFW (nếu hệ thống chưa có sẵn). |
| `sudo ufw allow 22/tcp` | Cho phép kết nối vào cổng 22 (SSH) qua giao thức TCP. |
| `sudo ufw allow 80/tcp` | Cho phép kết nối vào cổng 80 (HTTP) để người dùng truy cập web. |
| `sudo ufw allow 443/tcp` | Cho phép kết nối vào cổng 443 (HTTPS) khi cài chứng chỉ SSL sau này. |
| `sudo ufw enable` | Bật tường lửa UFW và tự động kích hoạt cùng hệ thống khi khởi động lại. |
| `sudo ufw status verbose` | Kiểm tra chi tiết trạng thái UFW và danh sách các quy tắc (Rule) đang có hiệu lực. |
| `sudo ufw reload` | Nạp lại các quy tắc tường lửa sau khi có chỉnh sửa. |

---

### Bước 4: Mở cổng trên Tường lửa Cloud / VPS (WiServices, AWS, v.v.)

Tường lửa hệ điều hành (UFW) chỉ là lớp bên trong. Các nhà cung cấp Cloud luôn có thêm một lớp tường lửa bên ngoài (Security Group):

1. Vào Dashboard máy ảo > **Tường lửa & Quy tắc** (hoặc **Security Groups**) > Chọn **Thêm Quy Tắc (Add Rule)**.
2. Thiết lập quy tắc cho cổng Web:
   - **Hướng:** `Đầu Vào (IN / Inbound)`
   - **Hành Động:** `ACCEPT / Allow`
   - **Giao Thức:** `TCP`
   - **NGUỒN (phía gửi):** IP: `All` (`0.0.0.0/0`) | Cổng: `Any`
   - **ĐÍCH (phía nhận):** IP: `All` | Cổng: **`80`** (và **`443`** nếu có SSL)
3. Gạt công tắc **Bật Quy Tắc** > Nhấn **Lưu Thay Đổi**.

---

### Bước 5: Cài đặt môi trường runtime (Java 21, Node.js 20, MySQL, Nginx, Git)

Chạy các lệnh sau dưới tài khoản `deployer`:

#### 1. Cài đặt OpenJDK 21, Git & Nginx:
```bash
sudo apt update
sudo apt install -y openjdk-21-jdk git nginx
java -version
```

#### 2. Cài đặt Node.js 20 LTS (qua kho chính thức NodeSource):
```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v && npm -v
```

#### 3. Cài đặt và kích hoạt MySQL Server 8.0:
```bash
sudo apt install -y mysql-server
sudo systemctl enable mysql
sudo systemctl start mysql
```

---

### Bước 6: Cấu hình Cơ sở dữ liệu MySQL cho Campus Hub

Truy cập MySQL console bằng quyền quản trị:
```bash
sudo mysql
```

Chạy các câu lệnh SQL sau để tạo database `campus_hub` và người dùng chuyên dụng:
```sql
CREATE DATABASE IF NOT EXISTS campus_hub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Tạo user campus_user với mật khẩu bảo mật (ví dụ: CampusHub@2026)
CREATE USER IF NOT EXISTS 'campus_user'@'localhost' IDENTIFIED BY 'CampusHub@2026';

-- Cấp toàn quyền thao tác trên database campus_hub
GRANT ALL PRIVILEGES ON campus_hub.* TO 'campus_user'@'localhost';
FLUSH PRIVILEGES;

EXIT;
```

---

### Bước 7: Đưa mã nguồn lên VPS và Đóng gói (Build)

Cấp quyền sở hữu thư mục `/var/www` cho `deployer` để clone mã nguồn không bị lỗi phân quyền:
```bash
sudo chown -R deployer:deployer /var/www
```

Clone mã nguồn từ GitHub vào thư mục `/var/www/campus-hub`:
```bash
git clone <URL_REPO_GITHUB> /var/www/campus-hub
cd /var/www/campus-hub
```

#### 1. Đóng gói Backend (Spring Boot):
```bash
cd /var/www/campus-hub/backend
chmod +x gradlew
./gradlew bootJar -x test
```
*File `.jar` sau khi build xong sẽ nằm tại: `/var/www/campus-hub/backend/build/libs/campus-hub-1.0.0.jar`.*

#### 2. Đóng gói Frontend (React Vite):
```bash
cd /var/www/campus-hub/frontend
npm install
npm run build
```
*Các file tĩnh sau khi build xong sẽ nằm tại: `/var/www/campus-hub/frontend/dist`.*

---

### Bước 8: Cấu hình Systemd Service chạy Backend tự động

Tạo file định nghĩa service cho Spring Boot để chạy nền và tự phục hồi khi gặp sự cố:
```bash
sudo nano /etc/systemd/system/campus-hub.service
```

Nội dung file cấu hình:
```ini
[Unit]
Description=Campus Hub Spring Boot Backend Monolith
After=syslog.target network.target mysql.service

[Service]
User=deployer
WorkingDirectory=/var/www/campus-hub/backend
ExecStart=/usr/bin/java -jar /var/www/campus-hub/backend/build/libs/campus-hub-1.0.0.jar
SuccessExitStatus=143

# Cấu hình biến môi trường khớp với application.yml và MySQL đã tạo
Environment="DB_URL=jdbc:mysql://localhost:3306/campus_hub?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true"
Environment="DB_USERNAME=campus_user"
Environment="DB_PASSWORD=CampusHub@2026"
Environment="SERVER_PORT=8080"
Environment="CORS_ALLOWED_ORIGINS=*"

Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

Kích hoạt và khởi chạy Backend Service:
| Lệnh | Ý nghĩa & Giải thích |
| :--- | :--- |
| `sudo systemctl daemon-reload` | Nạp lại danh sách các file cấu hình systemd vừa tạo/sửa đổi. |
| `sudo systemctl enable campus-hub` | Bật chế độ tự động chạy dịch vụ khi máy chủ khởi động lại. |
| `sudo systemctl start campus-hub` | Khởi chạy tiến trình Spring Boot ngay lập tức. |
| `sudo systemctl status campus-hub` | Kiểm tra trạng thái hoạt động của Backend (phải có màu xanh `active (running)`). |

Kiểm tra endpoint Healthcheck nội bộ:
```bash
curl -I http://127.0.0.1:8080/actuator/health
# Trả về HTTP/1.1 200 OK chứng tỏ backend đã kết nối thành công với MySQL và Flyway migration hoàn tất!
```

---

### Bước 9: Cấu hình Nginx làm Web Server & Reverse Proxy

Tạo file cấu hình virtual host cho Nginx:
```bash
sudo nano /etc/nginx/sites-available/campus-hub
```

Nội dung cấu hình Nginx:
```nginx
server {
    listen 80;
    listen [::]:80;

    # Điền IP Public của máy chủ hoặc domain (dùng _ để chấp nhận mọi IP)
    server_name _;

    # Thư mục gốc chứa các file tĩnh frontend React đã build
    root /var/www/campus-hub/frontend/dist;
    index index.html;

    # 1. Hỗ trợ Single Page Application (tránh lỗi 404 khi người dùng F5 trên trang con)
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 2. Reverse proxy chuyển tiếp toàn bộ request API sang Spring Boot (Port 8080)
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 3. Reverse proxy chuyển tiếp endpoint Actuator (Healthcheck & Info)
    location /actuator/ {
        proxy_pass http://127.0.0.1:8080/actuator/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    access_log /var/log/nginx/campus-hub.access.log;
    error_log /var/log/nginx/campus-hub.error.log;
}
```

Kích hoạt cấu hình Nginx:
| Lệnh | Ý nghĩa & Giải thích tham số |
| :--- | :--- |
| `sudo ln -s /etc/nginx/sites-available/campus-hub /etc/nginx/sites-enabled/` | Tạo liên kết biểu tượng (Symlink) để kích hoạt trang web. |
| `sudo rm -f /etc/nginx/sites-enabled/default` | Xóa site mặc định của Nginx để không bị xung đột cổng 80. |
| `sudo nginx -t` | Kiểm tra cú pháp của toàn bộ cấu hình Nginx (`-t` là test). |
| `sudo systemctl reload nginx` | Nạp lại cấu hình Nginx mà không làm gián đoạn các kết nối đang hoạt động. |

---

### Bước 10: Phân quyền Web Root và Truy cập Public

Đảm bảo tiến trình Nginx (`www-data`) có quyền đọc thư mục build:
```bash
sudo chmod -R 755 /var/www/campus-hub/frontend/dist
```

Mở trình duyệt trên máy tính cá nhân và truy cập:
- **Trang chủ giao diện:** `http://<IP_SERVER>/`
- **Kiểm tra Healthcheck qua Public:** `http://<IP_SERVER>/actuator/health`
- **Tài khoản kiểm thử mẫu:**
  - Sinh viên: `student` / `student123`
  - Quản trị viên: `admin` / `admin123`

---

## 💡 PHƯƠNG ÁN BỔ TRỢ: TRIỂN KHAI BẰNG DOCKER & DOCKER COMPOSE

Nếu triển khai theo quy chuẩn bài thi DevOps (Containerization):

### 1. `backend/Dockerfile`
```dockerfile
# Sử dụng JDK 21 phù hợp với Java toolchain của dự án
FROM eclipse-temurin:21-jdk-alpine

WORKDIR /app

# Sao chép file JAR đã build vào image
COPY build/libs/campus-hub-1.0.0.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
```

### 2. `docker-compose.yml` (tại thư mục gốc dự án)
```yaml
services:
  mysql-db:
    image: mysql:8.0
    container_name: campus-hub-mysql
    restart: unless-stopped
    environment:
      MYSQL_DATABASE: campus_hub
      MYSQL_ROOT_PASSWORD: rootpassword
      MYSQL_USER: campus_user
      MYSQL_PASSWORD: CampusHub@2026
    ports:
      - "127.0.0.1:3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      timeout: 5s
      retries: 10
      interval: 5s

  backend:
    build:
      context: ./backend
      dockerfile: Dockerfile
    container_name: campus-hub-backend
    restart: unless-stopped
    ports:
      - "127.0.0.1:8080:8080"
    environment:
      DB_URL: jdbc:mysql://mysql-db:3306/campus_hub?createDatabaseIfNotExist=true&useSSL=false
      DB_USERNAME: campus_user
      DB_PASSWORD: CampusHub@2026
      SERVER_PORT: 8080
      CORS_ALLOWED_ORIGINS: "*"
    depends_on:
      mysql-db:
        condition: service_healthy

volumes:
  mysql_data:
```

*Lệnh khởi chạy: `docker compose up -d --build`. Khi đó Nginx trên máy chủ vẫn giữ nguyên nhiệm vụ reverse proxy trỏ vào `http://127.0.0.1:8080`.*

---

## PHẦN 2: TỔNG HỢP CÁC LỖI THỰC TẾ & BÀI HỌC KINH NGHIỆM

### Lỗi 1: `502 Bad Gateway` khi gọi API hoặc tải bài viết
- **Hiện tượng:** Truy cập giao diện React bình thường, nhưng các tác vụ gọi API (`/api/posts`, `/api/auth/me`, `/actuator/health`) đều bị báo lỗi `502 Bad Gateway`.
- **Nguyên nhân:**
  1. Backend Spring Boot chưa khởi động xong hoặc bị crash do lỗi kết nối cơ sở dữ liệu.
  2. Nginx cấu hình sai cổng upstream (không đúng port 8080).
- **Cách xử lý & Kiểm tra:**
  1. Xem log chi tiết của service backend:
     ```bash
     sudo journalctl -u campus-hub -n 50 --no-pager
     ```
  2. Kiểm tra xem port 8080 có đang LISTEN:
     ```bash
     sudo ss -tulpn | grep 8080
     ```
  3. Thử gọi trực tiếp API nội bộ trên server:
     ```bash
     curl -i http://127.0.0.1:8080/actuator/health
     ```

---

### Lỗi 2: `404 Not Found` khi F5 (Reload) trang con trên trình duyệt
- **Hiện tượng:** Truy cập trang chủ `http://<IP_SERVER>/` thành công, nhưng khi vào một đường dẫn con hoặc bấm Reload lại trang (F5) thì bị màn hình `404 Not Found` của Nginx.
- **Nguyên nhân:** React là Single Page Application (SPA). Các URL con là client-side routing. Khi F5, trình duyệt gửi request trực tiếp lên Nginx tìm file tương ứng trong thư mục `dist/` nhưng không có file thực tế nào tên như vậy.
- **Cách xử lý:** Bổ sung directive `try_files` trong block `location /` của file Nginx:
  ```nginx
  location / {
      try_files $uri $uri/ /index.html;
  }
  ```
  Sau đó chạy: `sudo nginx -t && sudo systemctl reload nginx`.

---

### Lỗi 3: `Access denied for user 'campus_user'@'localhost'` hoặc lỗi Flyway Migration
- **Hiện tượng:** Log backend báo lỗi không thể kết nối tới cơ sở dữ liệu, Spring Boot tự động shutdown.
- **Nguyên nhân:**
  1. Sai mật khẩu hoặc thông tin user giữa file `/etc/systemd/system/campus-hub.service` và tài khoản thực tế trong MySQL.
  2. User chưa được cấp quyền trên database `campus_hub`.
  3. Dịch vụ MySQL chưa khởi động.
- **Cách xử lý:**
  1. Đăng nhập root MySQL để cấp lại quyền:
     ```sql
     GRANT ALL PRIVILEGES ON campus_hub.* TO 'campus_user'@'localhost';
     FLUSH PRIVILEGES;
     ```
  2. Kiểm tra đăng nhập thử nghiệm trên terminal máy chủ:
     ```bash
     mysql -u campus_user -p -e "SHOW DATABASES;"
     ```

---

### Lỗi 4: Quá trình Gradle Build bị `Killed` (Out Of Memory - OOM)
- **Hiện tượng:** Khi chạy `./gradlew bootJar`, terminal thông báo `Killed` và tiến trình bị hủy đột ngột.
- **Nguyên nhân:** Máy chủ VPS cấu hình thấp (1GB hoặc 2GB RAM). Trình biên dịch Java chiếm dụng RAM vượt ngưỡng cho phép nên nhân Linux tự động gửi tín hiệu SIGKILL để bảo vệ hệ thống.
- **Cách xử lý:** Kích hoạt Swap Memory 2GB như ở **Bước 2**:
  ```bash
  sudo fallocate -l 2G /swapfile
  sudo chmod 600 /swapfile
  sudo mkswap /swapfile
  sudo swapon /swapfile
  ```
  Sau đó chạy lại lệnh build với cờ tối ưu:
  ```bash
  ./gradlew bootJar -x test --no-daemon
  ```

---

### Lỗi 5: `Permission denied` khi chạy `./gradlew` hoặc `git clone`
- **Hiện tượng:** Chạy `./gradlew` báo `bash: ./gradlew: Permission denied`, hoặc clone code vào `/var/www` bị lỗi từ chối quyền.
- **Nguyên nhân:**
  - File `gradlew` mất thuộc tính thực thi khi sao chép.
  - Thư mục `/var/www` thuộc sở hữu của `root`.
- **Cách xử lý:**
  ```bash
  sudo chown -R deployer:deployer /var/www
  chmod +x /var/www/campus-hub/backend/gradlew
  ```

---

### Lỗi 6: `WARNING: REMOTE HOST IDENTIFICATION HAS CHANGED!`
- **Hiện tượng:** Không thể SSH vào máy chủ và nhận thông báo cảnh báo Host key verification failed.
- **Nguyên nhân:** Máy chủ VPS được cài lại hệ điều hành, fingerprint SSH mới không khớp với mã định danh cũ đã lưu trong file `known_hosts` trên máy tính cá nhân.
- **Cách xử lý:** Trên terminal máy cá nhân chạy:
  ```powershell
  ssh-keygen -R <IP_SERVER>
  ssh deployer@<IP_SERVER>
  ```
  Gõ `yes` rồi nhập mật khẩu để lưu fingerprint mới.

---

### Lỗi 7: `ERR_CONNECTION_TIMED_OUT` khi vào web trên trình duyệt
- **Hiện tượng:** Truy cập địa chỉ IP máy chủ trên trình duyệt quay vòng rồi báo hết thời gian chờ, nhưng kết nối SSH vẫn bình thường.
- **Nguyên nhân:** Tường lửa chưa mở cổng 80 (cổng HTTP):
  1. Chưa cho phép trong UFW.
  2. Chưa thêm Inbound Rule cho TCP port 80 trên Cloud Security Group của nhà cung cấp VPS.
- **Cách xử lý:**
  1. Kiểm tra UFW trên máy chủ: `sudo ufw allow 80/tcp && sudo ufw reload`.
  2. Kiểm tra lại bảng điều khiển Cloud VPS để đảm bảo cổng 80 đang mở cho `0.0.0.0/0`.

---

## PHẦN 3: CHEAT SHEET - BẢNG LỆNH HÀNG NGÀY & KIỂM TRA HỆ THỐNG

| Nhóm công việc | Câu lệnh thực thi |
| :--- | :--- |
| **Kiểm tra trạng thái Backend** | `sudo systemctl status campus-hub` |
| **Xem log thời gian thực của Backend** | `sudo journalctl -u campus-hub -f` |
| **Khởi động lại Backend** | `sudo systemctl restart campus-hub` |
| **Kiểm tra trạng thái Nginx** | `sudo systemctl status nginx` |
| **Kiểm tra cú pháp cấu hình Nginx** | `sudo nginx -t` |
| **Nạp lại (Reload) Nginx** | `sudo systemctl reload nginx` |
| **Xem log lỗi Nginx** | `sudo tail -n 50 /var/log/nginx/campus-hub.error.log` |
| **Kiểm tra trạng thái MySQL** | `sudo systemctl status mysql` |
| **Kiểm tra các cổng mạng đang mở** | `sudo ss -tulpn` |
| **Kiểm tra tường lửa UFW** | `sudo ufw status verbose` |
| **Kiểm tra Healthcheck nội bộ** | `curl -i http://127.0.0.1:8080/actuator/health` |
| **Quy trình cập nhật code mới (CI/CD thủ công)** | `cd /var/www/campus-hub && git pull origin main`<br>`cd backend && ./gradlew bootJar -x test && sudo systemctl restart campus-hub`<br>`cd ../frontend && npm install && npm run build` |

---

## PHẦN 4: HƯỚNG DẪN THIẾT LẬP VÀ CHẠY WORKFLOW CI/CD TRÊN GITHUB ACTIONS

Tự động hóa toàn bộ quy trình kiểm thử, đóng gói và triển khai mã nguồn lên máy chủ VPS mỗi khi lập trình viên đẩy mã nguồn mới lên nhánh `main`.

```mermaid
flowchart TD
    A["Developer push code lên nhánh main"] --> B["GitHub Repository Trigger Event: on push"]
    B --> C["GitHub Actions Runner (Ubuntu Latest)"]
    
    subgraph CI ["Giai đoạn 1: Build & Test (CI)"]
        C --> D["Set up JDK 21 & Build Spring Boot Jar"]
        C --> E["Set up Node.js 20 & Build React Vite Dist"]
    end
    
    subgraph CD ["Giai đoạn 2: Triển khai lên VPS (CD)"]
        D & E -->|"Build Thành Công"| F["SSH Action (sử dụng GitHub Secrets)"]
        F --> G["Kết nối VPS: Pull code / Image mới"]
        G --> H["Khởi động lại Service (Systemd hoặc Docker Compose)"]
        H --> I["Reload Nginx & Kiểm tra Healthcheck /actuator/health"]
    end
    
    D & E -->|"Build Thất Bại"| J["Dừng Pipeline - KHÔNG Triển Khai"]
```

---

### Bước 1: Chuẩn bị file Workflow trong kho mã nguồn

Trong thư mục dự án, tạo file `.github/workflows/deploy.yml` (file này đã được tạo sẵn trong dự án tại [.github/workflows/deploy.yml](file:///.github/workflows/deploy.yml)):

```yaml
name: Campus Hub CI/CD Pipeline

on:
  push:
    branches: [ "main" ]
  workflow_dispatch: # Cho phép kích hoạt thủ công bằng nút bấm trên giao diện GitHub

jobs:
  build-and-test:
    name: Build & Test Backend and Frontend
    runs-on: ubuntu-latest

    steps:
      - name: 1. Checkout mã nguồn từ GitHub
        uses: actions/checkout@v4

      - name: 2. Thiết lập môi trường OpenJDK 21
        uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: gradle

      - name: 3. Cấp quyền thực thi cho gradlew
        run: chmod +x backend/gradlew

      - name: 4. Build Spring Boot Jar
        run: |
          cd backend
          ./gradlew bootJar -x test

      - name: 5. Thiết lập môi trường Node.js 20
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: frontend/package-lock.json

      - name: 6. Build Frontend React với Vite
        run: |
          cd frontend
          npm ci
          npm run build

  deploy-to-vps:
    name: Deploy to VPS via SSH
    needs: [build-and-test] # Chỉ chạy khi giai đoạn build thành công
    runs-on: ubuntu-latest
    if: "${{ secrets.VPS_HOST != '' && (secrets.VPS_SSH_KEY != '' || secrets.VPS_PASSWORD != '') }}"

    steps:
      - name: Kết nối VPS qua SSH và cập nhật ứng dụng
        uses: appleboy/ssh-action@v1.0.3
        with:
          host: ${{ secrets.VPS_HOST }}
          username: ${{ secrets.VPS_USERNAME }}
          key: ${{ secrets.VPS_SSH_KEY }}
          password: ${{ secrets.VPS_PASSWORD }}
          port: ${{ secrets.VPS_PORT || 22 }}
          script: |
            echo "==== BẮT ĐẦU CẬP NHẬT ỨNG DỤNG TRÊN VPS ===="
            cd /var/www/campus-hub

            # 1. Kéo mã nguồn mới nhất
            git fetch origin main
            git reset --hard origin/main

            # 2. Cập nhật và Build Frontend
            echo "==== Building Frontend ===="
            cd /var/www/campus-hub/frontend
            npm install
            npm run build
            sudo chmod -R 755 /var/www/campus-hub/frontend/dist

            # 3. Triển khai Backend
            if [ -f "/etc/systemd/system/campus-hub.service" ]; then
                echo "==== Restarting Systemd Service ===="
                cd /var/www/campus-hub/backend
                chmod +x gradlew
                ./gradlew bootJar -x test --no-daemon
                sudo systemctl restart campus-hub
            elif [ -f "/var/www/campus-hub/docker-compose.yml" ]; then
                echo "==== Restarting Docker Compose ===="
                cd /var/www/campus-hub
                docker compose up -d --build
            fi

            # 4. Nạp lại Nginx
            sudo systemctl reload nginx

            # 5. Kiểm tra Healthcheck
            sleep 10
            echo "==== Kết quả Healthcheck ===="
            curl -s http://127.0.0.1:8080/actuator/health || echo "Cảnh báo: Healthcheck chưa phản hồi!"
            echo "==== TRIỂN KHAI HOÀN TẤT ===="
```

---

### Bước 2: Cấu hình GitHub Secrets trên Repository

Để GitHub Actions có thể SSH vào VPS của bạn một cách bảo mật mà không làm lộ mật khẩu/IP trong source code:

1. Mở Repository của bạn trên GitHub.
2. Vào **Settings** > ở menu bên trái chọn **Secrets and variables** > chọn **Actions**.
3. Bấm nút **New repository secret** màu xanh lá và lần lượt thêm các Secret sau:

| Tên Secret | Ý nghĩa & Giá trị cần điền | Bắt buộc |
| :--- | :--- | :---: |
| `VPS_HOST` | Địa chỉ IP Public của máy chủ VPS (ví dụ: `221.121.3.247`). | **Có** |
| `VPS_USERNAME` | Tên người dùng quản trị trên VPS (ví dụ: `deployer`). | **Có** |
| `VPS_SSH_KEY` | Toàn bộ nội dung file Private Key SSH (từ `-----BEGIN OPENSSH PRIVATE KEY-----` đến `-----END OPENSSH PRIVATE KEY-----`). *(Khuyên dùng)* | **Có** *(hoặc dùng PASSWORD)* |
| `VPS_PASSWORD` | Mật khẩu tài khoản `deployer` trên VPS (nếu không dùng SSH Key). | Tùy chọn |
| `VPS_PORT` | Cổng SSH của VPS (mặc định là `22`, có thể bỏ qua nếu dùng 22). | Tùy chọn |
| `DOCKERHUB_USERNAME` | Tên đăng nhập Docker Hub (nếu dùng thêm bước build & push Docker). | Tùy chọn |
| `DOCKERHUB_TOKEN` | Access Token của Docker Hub (tạo tại Docker Hub > Account Settings > Personal Access Tokens). | Tùy chọn |

#### 🔑 Cách tạo cặp SSH Key riêng cho GitHub Actions trên VPS:
Đăng nhập vào VPS với tài khoản `deployer` và chạy:
```bash
# 1. Sinh cặp khóa mới dành riêng cho GitHub Actions
ssh-keygen -t ed25519 -C "github-actions-deploy" -f ~/.ssh/github_actions -N ""

# 2. Thêm Public Key vào danh sách khóa được phép truy cập
cat ~/.ssh/github_actions.pub >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys

# 3. Hiển thị Private Key để sao chép vào Secret VPS_SSH_KEY trên GitHub
cat ~/.ssh/github_actions
```
*(Sao chép toàn bộ nội dung xuất hiện trên màn hình và dán vào ô Value của secret `VPS_SSH_KEY`).*

#### ⚙️ Cấp quyền không cần mật khẩu khi chạy `sudo systemctl` cho `deployer`:
Để GitHub Actions chạy được các lệnh `sudo systemctl reload nginx` và `sudo systemctl restart campus-hub` mà không bị hỏi mật khẩu:
```bash
sudo visudo /etc/sudoers.d/deployer
```
Dán dòng sau vào và lưu lại:
```text
deployer ALL=(ALL) NOPASSWD: /bin/systemctl restart campus-hub, /bin/systemctl reload nginx, /bin/chmod
```

---

### Bước 3: Cách Kích Hoạt và Chạy Workflow

Bạn có thể kích hoạt workflow theo 2 cách:

#### Cách 1: Tự động chạy khi có mã nguồn mới (Tự động kích hoạt)
Mỗi khi bạn thực hiện commit và push code lên nhánh `main`:
```bash
git add .
git commit -m "feat: add new feature and trigger ci/cd"
git push origin main
```
GitHub Actions sẽ tự động phát hiện sự kiện `push` và bắt đầu chạy pipeline.

#### Cách 2: Chạy thủ công trên giao diện web GitHub (Manual Run)
Rất hữu ích khi muốn test lại quy trình deploy mà không cần phải commit code:
1. Mở trang repository trên GitHub > Chọn tab **Actions**.
2. Ở danh sách workflows bên trái, chọn **Campus Hub CI/CD Pipeline**.
3. Ở góc trên bên phải màn hình, bấm vào menu xổ xuống **Run workflow**.
4. Chọn nhánh: **Branch: main**.
5. Bấm nút màu xanh **Run workflow**.

---

### Bước 4: Kiểm tra và Theo dõi Tiến trình Workflow

1. Tại tab **Actions**, bạn sẽ thấy một phiên chạy (Workflow run) mới xuất hiện với tên commit của bạn.
2. Bấm vào phiên chạy đó để xem sơ đồ các giai đoạn:
   - `build-and-test`: Kiểm tra cú pháp, build Spring Boot Java 21, build Vite React.
   - `deploy-to-vps`: Kết nối SSH vào máy chủ VPS và cập nhật ứng dụng.
3. Bấm trực tiếp vào từng Job để xem log thời gian thực:
   - Nếu biểu tượng hiện **dấu tích xanh (✓)**: Giai đoạn đó đã chạy thành công.
   - Nếu biểu tượng hiện **dấu gạch chéo đỏ (✗)**: Bấm vào step bị đỏ để đọc thông báo lỗi chi tiết.

---

### Bước 5: Kiểm chứng ứng dụng trên VPS sau khi Workflow hoàn tất

Sau khi GitHub Actions hiển thị trạng thái **Success (Màu xanh)**:

1. **Gọi API Healthcheck qua public để xác nhận:**
   ```bash
   curl -i http://<IP_SERVER>/actuator/health
   # Kết quả trả về: HTTP/1.1 200 OK với body {"status":"UP"}
   ```

2. **Kiểm tra commit mới nhất trên VPS:**
   ```bash
   cd /var/www/campus-hub
   git log -1 --oneline
   # Mã commit và thông điệp commit phải khớp với commit mới nhất trên GitHub
   ```

3. **Kiểm tra trạng thái các dịch vụ:**
   ```bash
   sudo systemctl status campus-hub
   sudo systemctl status nginx
   ```

---

### Bước 6: Trả lời các câu hỏi lý thuyết quan trọng (Ôn thi / Phỏng vấn DevOps)

| Câu hỏi | Câu trả lời chuẩn xác |
| :--- | :--- |
| **1. Sự kiện nào kích hoạt CI/CD Pipeline?** | Pipeline được kích hoạt bởi sự kiện `push` lên nhánh `main` (`on: push: branches: [main]`) hoặc được kích hoạt thủ công qua giao diện web bằng sự kiện `workflow_dispatch`. |
| **2. Vì sao bắt buộc phải sử dụng GitHub Secrets?** | Để bảo vệ các thông tin nhạy cảm (IP máy chủ, SSH private key, mật khẩu, Docker token). Nếu ghi trực tiếp các thông tin này vào mã nguồn (`deploy.yml`), kẻ xấu có thể đọc được và chiếm quyền điều khiển VPS hoặc Docker Hub. |
| **3. Nếu bước Build hoặc Test thất bại, Pipeline có nên tiếp tục Deploy không?** | **Tuyệt đối không.** Giai đoạn `deploy-to-vps` được cấu hình với thuộc tính `needs: [build-and-test]`. Nếu bước build có lỗi, pipeline sẽ dừng lại ngay lập tức nhằm bảo đảm không triển khai mã nguồn lỗi lên môi trường thực tế (production), tránh làm sập website. |
| **4. Làm thế nào để xác nhận VPS đang chạy đúng phiên bản mới nhất?** | 1. Đăng nhập vào VPS và kiểm tra commit hash bằng lệnh `git log -1`.<br>2. Gọi API mới vừa được bổ sung (ví dụ: `curl http://<IP_SERVER>/api/...`).<br>3. Kiểm tra thời gian khởi chạy của tiến trình bằng `systemctl status campus-hub` (Active time vừa mới restart). |

