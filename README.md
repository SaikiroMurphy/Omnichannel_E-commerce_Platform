# Omnichannel E-commerce Platform

Nền tảng thương mại điện tử được xây dựng theo kiến trúc microservices. Các dịch vụ Java/Spring Boot có cơ sở dữ liệu riêng; API Gateway là điểm truy cập chung cho client. Kafka xử lý sự kiện giữa các dịch vụ, Redis hỗ trợ các luồng cần lưu trữ hoặc đồng bộ trạng thái.

## Thành phần

| Thành phần | Trách nhiệm |
| --- | --- |
| `gateway` | Định tuyến API, kiểm tra JWT, CORS và giới hạn tốc độ. |
| `user-service` | Tài khoản, xác thực và token. |
| `product-service` | Danh mục và thông tin sản phẩm. |
| `order-service` | Tạo và quản lý đơn hàng; phối hợp với sản phẩm, tồn kho, thanh toán và thông báo. |
| `inventory-service` | Quản lý tồn kho và giữ/giải phóng hàng cho đơn hàng. |
| `payment-service` | Ghi nhận các lần thanh toán; hiện hỗ trợ mô phỏng kết quả trong môi trường sandbox. |
| `billing-service` | Tạo và quản lý hóa đơn; tách biệt với thanh toán. |
| `notification-service` | Ghi nhận và xếp hàng các thông báo. |
| PostgreSQL | Mỗi service có database riêng. |
| Kafka và Redis | Hạ tầng messaging và lưu trữ/đồng bộ trạng thái. |

Các service backend và gateway nằm trong các thư mục tương ứng ở repository root. Cấu hình quan sát hệ thống nằm trong `observability/`.

## Công nghệ

- Java 17, Spring Boot 4.1.1, Maven Wrapper
- PostgreSQL 16
- Apache Kafka 3.8.1 và Redis 7
- Docker Compose
- Prometheus, Grafana, Loki và Jaeger (trong cấu hình production)

## Chạy local bằng Docker Compose

Yêu cầu Docker Desktop hoặc Docker Engine có Docker Compose v2. Đảm bảo các cổng được liệt kê bên dưới chưa bị ứng dụng khác sử dụng.

1. Tạo file `.env` từ mẫu:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Mở `.env` và thay các giá trị mẫu. Ít nhất, hãy đặt mật khẩu PostgreSQL mạnh và tạo `JWT_SECRET_BASE64` từ ít nhất 32 byte ngẫu nhiên. Ví dụ tạo secret bằng PowerShell:

   ```powershell
   [Convert]::ToBase64String([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
   ```

   Tất cả service xác thực JWT phải dùng cùng `JWT_SECRET_BASE64`, `JWT_ISSUER_URI` và `JWT_AUDIENCE`. Có thể cấu hình `INITIAL_ADMIN_EMAIL` và `INITIAL_ADMIN_PASSWORD` để khởi tạo tài khoản admin. Nếu cần tải ảnh sản phẩm qua Cloudinary, hãy cấu hình `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` và `CLOUDINARY_API_SECRET`.

3. Build và khởi động toàn bộ stack:

   ```powershell
   docker compose up -d --build
   ```

4. Kiểm tra trạng thái và log:

   ```powershell
   docker compose ps
   docker compose logs -f gateway
   ```

API Gateway sẵn sàng tại `http://localhost:8080`. Kiểm tra health:

```text
http://localhost:8080/actuator/health
```

Dừng các container nhưng giữ dữ liệu trong volumes:

```powershell
docker compose down
```

> `docker compose down -v` sẽ xóa các volume dữ liệu của PostgreSQL và Redis. Chỉ dùng khi muốn xóa dữ liệu local.

### Cổng local

| Dịch vụ | URL/cổng từ máy host |
| --- | --- |
| API Gateway | `http://localhost:8080` |
| Product service | `http://localhost:8081` |
| Order service | `http://localhost:8083` |
| Inventory service | `http://localhost:8084` |
| Payment service | `http://localhost:8085` |
| Notification service | `http://localhost:8086` |
| Billing service | `http://localhost:8087` |
| Kafka | `localhost:9092` |
| Redis | `localhost:6379` |

`user-service` không publish cổng riêng trong cấu hình local; truy cập qua gateway. Nên gọi API qua gateway, ví dụ `http://localhost:8080/api/products`.

## API routes

Gateway chuyển tiếp các đường dẫn sau đến service tương ứng:

| Đường dẫn | Service |
| --- | --- |
| `/api/auth/**`, `/api/users/**` | User |
| `/api/products/**` | Product |
| `/api/orders/**` | Order |
| `/api/inventory/**` | Inventory |
| `/api/payments/**` | Payment |
| `/api/notifications/**` | Notification |
| `/api/billing/**` | Billing |

Hầu hết API cần access token JWT. Quyền truy cập cụ thể phụ thuộc endpoint và vai trò tài khoản. Xem README của từng service để biết chi tiết API và cấu hình riêng, ví dụ [payment-service](./payment-service/README.md) và [billing-service](./billing-service/README.md).

## Luồng checkout tham khảo

1. Tạo đơn hàng qua `POST /api/orders` với danh sách `productId` và số lượng.
2. `order-service` lấy giá sản phẩm và giữ tồn kho trước khi trả về đơn hàng.
3. Bắt đầu thanh toán qua `POST /api/orders/{orderId}/payments`.
4. Trong cấu hình local, mô phỏng kết quả thanh toán bằng `POST /api/payments/{paymentId}/simulate` với kết quả `APPROVED` hoặc `DECLINED`.
5. Kết quả thanh toán được gửi về order service để hoàn tất hoặc hủy đơn và xử lý giữ hàng.

**Lưu ý:** `payment-service` hiện chỉ mô phỏng thanh toán; không kết nối ngân hàng, ví điện tử hoặc payment provider và không chuyển tiền thật. `notification-service` lưu và xếp hàng thông báo, nhưng chưa tích hợp worker/provider gửi email, SMS hoặc push.

## Build và chạy test từng service

Các module Maven có Maven Wrapper riêng. Từ thư mục service cần kiểm tra:

```powershell
.\mvnw.cmd test
```

Trên Linux/macOS:

```bash
./mvnw test
```

## Cấu hình production

`docker-compose.prod.yml` sử dụng image đã build/publish sẵn (mặc định từ GitHub Container Registry), không build image từ source. Trước khi chạy, cấu hình `.env` với tối thiểu `POSTGRES_PASSWORD`, `JWT_SECRET_BASE64` và `GRAFANA_ADMIN_PASSWORD`; bảo đảm các image cần thiết đã được publish và tag đúng.

```powershell
docker compose -f docker-compose.prod.yml --env-file .env up -d
```

Cấu hình production publish gateway trên cổng `8080`, Grafana trên `3001` và Jaeger UI trên `16686`. Hãy thay các thông tin mặc định bằng secret an toàn và rà soát cấu hình triển khai phù hợp với môi trường thực tế trước khi đưa vào sử dụng.
