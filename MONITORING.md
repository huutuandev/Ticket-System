# Monitoring & Logging Guide

Hệ thống Ticket Booking đã được tích hợp sẵn Logging (SLF4J + Logback), Spring Boot Actuator, và Prometheus/Grafana để theo dõi trạng thái và hiệu suất.

## 1. Logging
- **Log file**: Các log được ghi vào file `logs/ticket-app.log`. File log sẽ tự động xoay vòng (rolling) theo ngày và giữ tối đa 30 ngày.
- **Log level**: 
  - Profile `dev`: Log level của `com.example.ticket` và `org.hibernate.SQL` được đặt là `DEBUG`.
  - Profile `prod`: Log level của `com.example.ticket` được đặt là `INFO`.
- **Cấu hình**: Xem thêm tại `src/main/resources/logback-spring.xml`.

## 2. Spring Boot Actuator
- Các endpoint actuator được mở tại: `http://localhost:8080/actuator`
- Kiểm tra Health (kèm chi tiết Redis & RabbitMQ): `http://localhost:8080/actuator/health`
- Kiểm tra Metrics: `http://localhost:8080/actuator/metrics`
- Endpoint cho Prometheus scrape: `http://localhost:8080/actuator/prometheus`

## 3. Prometheus & Grafana (Docker Compose)
Dịch vụ monitoring đã được thêm vào `docker-compose.yml`.

### Cách chạy:
1. Chạy các container bằng lệnh:
   ```bash
   docker-compose up -d
   ```
2. **Prometheus** sẽ khả dụng tại: `http://localhost:9090`
3. **Grafana** sẽ khả dụng tại: `http://localhost:3000`
   - **Tài khoản mặc định**: `admin` / `admin` (bạn có thể được yêu cầu đổi mật khẩu ở lần đăng nhập đầu tiên).

### Cấu hình Dashboard cho Spring Boot trong Grafana:
1. Truy cập Grafana tại `http://localhost:3000`.
2. Đăng nhập bằng `admin` / `admin`.
3. Vào phần **Connections -> Data sources** và thêm data source mới là **Prometheus**.
   - URL: `http://prometheus:9090` (hoặc `http://ticket_prometheus:9090` tuỳ cấu hình mạng, nếu cùng compose network thì là `http://prometheus:9090`).
   - Click **Save & Test**.
4. Vào phần **Dashboards -> Import**.
5. Nhập ID của một Spring Boot dashboard phổ biến. Khuyến nghị sử dụng Dashboard ID **4701** (JVM (Micrometer)) hoặc **11378** (Spring Boot 2.1 System Monitor, dùng tốt cho cả Spring Boot 3).
6. Bấm Load, chọn Data source Prometheus vừa tạo, và bấm **Import**.

Bây giờ bạn có thể theo dõi trực tiếp các thông số JVM, CPU, Memory, API request time, v.v. của ứng dụng.
