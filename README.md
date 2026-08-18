# AIHUBDOCs — Backend

Hệ thống hỏi đáp tài liệu bằng AI. Người dùng tải tài liệu lên, hệ thống bóc tách nội dung,
tạo vector ngữ nghĩa và cho phép đặt câu hỏi bằng ngôn ngữ tự nhiên. Câu trả lời được sinh
ra **chỉ dựa trên tài liệu của chính người dùng**, kèm trích dẫn nguồn.

Đồ án môn **SWP391 — Software Development Project**.

---

## 1. Tổng quan chức năng

| Nhóm chức năng | Mô tả |
| --- | --- |
| Tài khoản | Đăng ký kèm xác thực OTP qua email, đăng nhập, đăng nhập bằng Google, refresh token, đặt lại mật khẩu |
| Quản lý tài liệu | Tải lên, phân thư mục, gắn thẻ, đánh dấu sao, thùng rác, chia sẻ qua link có hạn |
| Hỏi đáp AI | Hỏi trên một tài liệu, một thư mục, hoặc toàn bộ kho tài liệu |
| Phiên hội thoại | Lưu lịch sử chat, có trí nhớ hội thoại nhiều lượt, trích dẫn nguồn từng câu trả lời |
| Gói dịch vụ | Phân hạng Free/Basic/Pro, giới hạn dung lượng và hạn mức token, thanh toán qua VNPay |
| Quản trị | Quản lý người dùng, khoá/mở tài khoản, quản lý gói dịch vụ |

## 2. Kiến trúc RAG

```text
Tải lên  →  Azure Blob Storage
              ↓
          Bóc tách văn bản (PDFBox / Apache POI)
              ↓
          Chia đoạn (chunking)
              ↓
          Sinh vector  →  OpenAI text-embedding-3-small (1536 chiều)
              ↓
          Lưu vào bảng document_chunk

Đặt câu hỏi  →  Sinh vector cho câu hỏi
                    ↓
                Tính cosine similarity, lấy TOP_K đoạn gần nhất
                    ↓
                Dựng prompt kèm ngữ cảnh  →  OpenAI gpt-5.6-luna
                    ↓
                Trả lời kèm trích dẫn nguồn
```

Hệ thống được cấu hình `rag.user-storage.allow-general-knowledge: false`, nghĩa là AI **chỉ
được trả lời trong phạm vi tài liệu đã truy hồi**. Câu hỏi nằm ngoài tài liệu sẽ nhận phản
hồi báo không tìm thấy thông tin, thay vì bịa ra câu trả lời.

## 3. Công nghệ sử dụng

| Thành phần | Lựa chọn |
| --- | --- |
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Cơ sở dữ liệu | Microsoft SQL Server 16 |
| ORM | Spring Data JPA, Hibernate 7 (`ddl-auto: validate`) |
| Quản lý schema | Flyway |
| Lưu trữ tệp | Azure Blob Storage, cấp quyền tạm bằng SAS token |
| AI | Spring AI 2.0 + OpenAI (`gpt-5.6-luna`, `text-embedding-3-small`) |
| Bảo mật | Spring Security, JWT, OAuth2 (Google) |
| Tài liệu API | SpringDoc OpenAPI (Swagger UI) |
| Nạp biến môi trường | `springboot4-dotenv` (tự đọc `.env`) |

---

## 4. Cài đặt và chạy

### 4.1. Yêu cầu

- **JDK 21** — kiểm tra bằng `.\mvnw.cmd -v`, dòng `Java version` phải là 21.x
- **SQL Server** kèm SSMS hoặc Azure Data Studio
- Maven Wrapper đã có sẵn trong repo, không cần cài Maven riêng

Tuỳ chọn, chỉ cần khi dùng tới chức năng tương ứng:

- **FFmpeg** trong PATH — bắt buộc nếu làm tính năng chuyển video thành văn bản
- **Tesseract OCR** — nếu cần đọc chữ trong ảnh và PDF scan
- **Docling** — nếu cần bóc tách tài liệu chất lượng cao hơn parser mặc định

### 4.2. Tạo cơ sở dữ liệu

Mở SSMS và chạy script `doc/sql/setup_lms_ai.sql`. Script tạo database `lms_ai`, 21 bảng,
khoá ngoại, ràng buộc, và dữ liệu khởi tạo gồm 3 gói cước cùng một tài khoản quản trị.

> **Bắt buộc chạy trước khi khởi động ứng dụng.** Hibernate đang ở chế độ `validate` nên sẽ
> từ chối khởi động nếu schema chưa tồn tại.

Script **không** tạo bảng `flyway_schema_history`. Đây là chủ ý: để Flyway tự tạo lúc ứng
dụng chạy lần đầu, khi đó `baseline-on-migrate` sẽ ghi mốc khởi điểm rồi áp dụng V1 và V2.

### 4.3. Cấu hình biến môi trường

Sao chép `.env.example` thành `.env` rồi điền giá trị. Dự án dùng `springboot4-dotenv` nên
file này **được nạp tự động**, không cần script hay thao tác thủ công nào.

Tối thiểu phải điền để ứng dụng khởi động được:

```env
SQLSERVER_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=lms_ai;encrypt=true;trustServerCertificate=true
SQLSERVER_DATASOURCE_USERNAME=sa
SQLSERVER_DATASOURCE_PASSWORD=<mật khẩu SQL Server>
APP_JWT_SECRET=<chuỗi ngẫu nhiên tối thiểu 32 ký tự>
```

Điền thêm khi cần chức năng tương ứng:

| Biến | Mở khoá chức năng |
| --- | --- |
| `OPENAI_API_KEY` + `SPRING_AI_MODEL_CHAT=openai` + `SPRING_AI_MODEL_EMBEDDING=openai` | Hỏi đáp AI và tạo vector |
| `AZURE_STORAGE_CONNECTION_STRING` + `AZURE_STORAGE_CONTAINER` | Tải lên và tải xuống tài liệu |
| `MAIL_USERNAME` + `MAIL_PASSWORD` | Gửi OTP đăng ký, quên mật khẩu |
| `GOOGLE_CLIENT_ID` + `GOOGLE_CLIENT_SECRET` | Đăng nhập bằng Google |
| `VNPAY_TMN_CODE` + `VNPAY_HASH_SECRET` | Thanh toán nâng gói |

> ⚠️ **Cạm bẫy hay gặp: biến rỗng khác biến không tồn tại.**
> Viết `TEN_BIEN=` trong `.env` tạo ra một biến **tồn tại với giá trị chuỗi rỗng**, và nó sẽ
> **ghi đè** giá trị mặc định khai trong `${TEN_BIEN:mac_dinh}` của `application.yaml`.
> Nếu chưa dùng tới một dịch vụ, hãy **xoá hẳn dòng đó** thay vì để trống.

### 4.4. Chạy

```powershell
.\mvnw.cmd spring-boot:run
```

Khởi động thành công sẽ thấy `Started Group01Application` và `Tomcat started on port 8080`.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Đăng nhập bằng tài khoản quản trị đã khởi tạo sẵn trong script SQL:
`admin@aistudyhub.local` / `Admin@123`. **Đổi mật khẩu ngay sau lần đăng nhập đầu tiên.**

---

## 5. Quy ước cấu trúc code

### 5.1. Luồng xử lý

```text
Controller  →  Service  →  Repository  →  Entity / Database
                       →  External Service (Azure Blob, OpenAI, Mail)
```

Controller chỉ nhận request, gọi Service, trả response. Không viết business logic trong
Controller.

### 5.2. Trách nhiệm từng package

| Package | Trách nhiệm |
| --- | --- |
| `controller/` | Nhận request, gọi service, trả response |
| `service/` | Toàn bộ business logic |
| `repository/` | Thao tác database qua Spring Data JPA |
| `entity/` | Chỉ mapping bảng, không chứa business logic |
| `dto/` | Object request/response của API |
| `config/` | Cấu hình database, Azure Storage, AI, security |
| `exception/` | Xử lý lỗi tập trung |
| `util/` | Hàm tiện ích dùng chung |
| `enums/` | Các kiểu liệt kê dùng chung |
| `security/` | JWT filter, handler cho OAuth2 |

### 5.3. Ví dụ đúng

```text
DocumentController
  → DocumentService
      → DocumentRepository
      → FileStorageService      (interface, cài đặt bằng Azure Blob)
```

### 5.4. Ví dụ nên tránh

```text
DocumentController
  → tự validate file
  → tự gọi SDK Azure
  → tự lưu database
  → tự xử lý lỗi chi tiết
```

Lưu trữ tệp luôn đi qua interface `FileStorageService`. Không gọi thẳng SDK của nhà cung cấp
từ tầng controller hay service nghiệp vụ — đó là điều kiện để sau này đổi nhà cung cấp mà
không phải sửa lan man.

### 5.5. Chuẩn response

Thành công:

```json
{
  "success": true,
  "message": "Action successfully",
  "data": {},
  "errors": null,
  "timestamp": "2026-08-12T10:30:00Z"
}
```

Thất bại:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": [{ "field": "email", "message": "Email is invalid" }],
  "timestamp": "2026-08-12T10:30:00Z"
}
```

### 5.6. Quy tắc bắt buộc

- **Không commit** `.env`, API key, mật khẩu database, connection string hay bất kỳ secret nào
- Không lưu nội dung tệp vào database — tệp vật lý nằm ở storage, database chỉ giữ metadata
- Thêm endpoint mới thì đặt tên đúng chuẩn RESTful
- Thêm field vào entity thì kiểm tra ảnh hưởng tới migration, DTO, repository và response
- Không sửa module ngoài phạm vi task nếu chưa trao đổi với nhóm

---

## 6. Quy trình Git

Không code trực tiếp trên `main`. Mỗi task một branch riêng, xong thì tạo Pull Request để
người khác review trước khi merge.

```bash
# Bắt đầu task mới
git checkout main
git pull origin main
git checkout -b feature/ten-chuc-nang

# Trong lúc làm
git add .
git commit -m "feat(chat): add session message endpoint"

# Đẩy lên và tạo Pull Request
git push -u origin feature/ten-chuc-nang
```

Quy ước tên branch: `feature/`, `fix/`, `refactor/`, `docs/` kèm mô tả ngắn bằng tiếng Anh
không dấu.

### Quy ước commit

Theo chuẩn **Conventional Commits**: `type(scope): description`

| Type | Dùng khi |
| --- | --- |
| `feat` | Thêm chức năng mới |
| `fix` | Sửa lỗi |
| `refactor` | Cấu trúc lại code, không đổi hành vi |
| `docs` | Sửa tài liệu |
| `test` | Thêm hoặc sửa test |
| `chore` | Cấu hình, dependency, việc phụ trợ |
| `build` | Thay đổi build tool |
| `perf` | Cải thiện hiệu năng |

Scope thường dùng: `auth`, `chat`, `upload`, `document`, `user`, `admin`, `db`, `config`.

Ba quy tắc: mô tả bằng tiếng Anh, không viết hoa chữ đầu, không chấm cuối câu. Mỗi commit
tập trung một việc.

```bash
# Nên
git commit -m "feat(upload): validate file size before upload"
git commit -m "fix(auth): return 401 when refresh token expired"

# Không nên
git commit -m "update"
git commit -m "fix bug"
```

### Xử lý conflict

```bash
git checkout main
git pull origin main
git checkout feature/ten-chuc-nang
git merge main
# sửa các file conflict, rồi:
git add .
git commit -m "chore(merge): resolve conflict with main"
git push
```

Không chắc cách xử lý thì hỏi nhóm trước khi sửa, đừng tự ý ghi đè.

---

## 7. Tài liệu liên quan

| Tài liệu | Nội dung |
| --- | --- |
| `API_CONTRACT.md` | Hợp đồng API đầy đủ cho frontend |
| `doc/sql/setup_lms_ai.sql` | Script dựng database kèm dữ liệu khởi tạo |
| `doc/GIAI_THICH_CAU_HINH.md` | Giải thích từng tham số trong `.env` và `application.yaml` |
| `doc/TON_DONG_VA_PHAT_HIEN.md` | Danh sách tồn đọng và các phát hiện kỹ thuật |

## 8. Ghi chú

Package Java hiện vẫn là `com.se1908.group01`, kế thừa từ cấu trúc ban đầu của dự án. Đổi
tên package sẽ ảnh hưởng toàn bộ 244 file nguồn nên chưa thực hiện.

Tài khoản quản trị khởi tạo dùng địa chỉ `admin@aistudyhub.local`. Đây là giá trị được ghi
trong `doc/sql/setup_lms_ai.sql`; muốn đổi thì sửa cả script lẫn dữ liệu đã tạo trong
database.
