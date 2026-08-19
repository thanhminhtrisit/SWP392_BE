# SWP391-SE1908-GROUP-01

Repository dùng cho đồ án môn học **SWP391** của nhóm.

Tài liệu này hướng dẫn các thành viên trong nhóm cách clone project, tạo branch, code, commit, push và tạo Pull Request để leader review trước khi merge vào `main`.

---

## 1. Quy tắc làm việc chung

- Không code trực tiếp trên branch `main`.
- Mỗi thành viên phải tạo branch riêng từ `main` trước khi làm task.
- Sau khi code xong, push branch của mình lên GitHub.
- Tạo Pull Request từ branch cá nhân vào `main`.
- Leader sẽ review code, nếu ổn thì mới merge vào `main`.
- Trước khi bắt đầu task mới, luôn cập nhật code mới nhất từ `main`.

---

## 2. Clone project về máy

Mở Git Bash tại thư mục muốn lưu project, sau đó chạy:

```bash
git clone https://github.com/longb47/SWP391-SE1908-GROUP-01.git
```

Sau khi clone xong, vào thư mục project:

```bash
cd SWP391-SE1908-GROUP-01
```

Kiểm tra branch hiện tại:

```bash
git branch
```

Mặc định sau khi clone, bạn sẽ ở branch `main`.

---

## 3. Cập nhật code mới nhất từ main

Trước khi tạo branch hoặc bắt đầu code, luôn chạy:

```bash
git checkout main
git pull origin main
```

Lệnh này giúp máy bạn có code mới nhất từ GitHub.

---

## 4. Tạo branch mới để làm task

Mỗi task/chức năng nên có một branch riêng.

Cú pháp:

```bash
git checkout -b feature/ten-chuc-nang
```

Ví dụ:

```bash
git checkout -b feature/login
```

Một số cách đặt tên branch:

```text
feature/login
feature/register
feature/chat
feature/upload-file
feature/admin-manager
fix/login-error
fix/register-validation
```

Quy tắc đặt tên branch:

- Viết thường.
- Không dùng dấu tiếng Việt.
- Dùng dấu `-` thay cho khoảng trắng.
- Tên branch nên ngắn gọn, thể hiện chức năng đang làm.

---

## 5. Kiểm tra branch đang làm việc

Trước khi code, kiểm tra mình đang ở branch nào:

```bash
git branch
```

Branch hiện tại sẽ có dấu `*` phía trước.

Ví dụ:

```text
  main
* feature/login
```

Nghĩa là bạn đang làm việc trên branch `feature/login`.

---

## 6. Commit code sau khi làm xong

Sau khi code xong một phần chức năng, kiểm tra file thay đổi:

```bash
git status
```

Thêm toàn bộ file thay đổi vào commit:

```bash
git add .
```

Tạo commit:

```bash
git commit -m "type(scope): short description"
```

Ví dụ:

```bash
git commit -m "feat(login): add login form"
git commit -m "fix(register): validate duplicate email"
git commit -m "docs(readme): update git workflow guide"
```

### Quy chuẩn commit chuyên nghiệp

Nhóm sử dụng format commit phổ biến theo chuẩn **Conventional Commits**:

```text
type(scope): description
```

Trong đó:

- `type`: loại thay đổi.
- `scope`: khu vực/chức năng bị ảnh hưởng, ví dụ `login`, `register`, `chat`, `admin`, `upload`.
- `description`: mô tả ngắn gọn nội dung đã làm.

| Type       | Khi nào dùng                                                       | Ví dụ commit                                     |
| ---------- | ------------------------------------------------------------------ | ------------------------------------------------ |
| `feat`     | Thêm chức năng mới                                                 | `feat(login): add login form`                    |
| `fix`      | Sửa lỗi                                                            | `fix(login): handle wrong password message`      |
| `docs`     | Chỉ sửa tài liệu, README, hướng dẫn                                | `docs(readme): add commit convention`            |
| `style`    | Sửa format code, dấu cách, xuống dòng, không đổi logic             | `style(user): format user controller`            |
| `refactor` | Tối ưu/cấu trúc lại code nhưng không thêm tính năng, không sửa bug | `refactor(service): simplify user service logic` |
| `test`     | Thêm hoặc sửa test                                                 | `test(auth): add login service tests`            |
| `chore`    | Việc phụ trợ như config, dependency, setup project                 | `chore(config): update application properties`   |
| `build`    | Thay đổi liên quan build tool hoặc dependency                      | `build(maven): add mysql connector dependency`   |
| `ci`       | Thay đổi liên quan CI/CD, GitHub Actions                           | `ci(github): add build workflow`                 |
| `perf`     | Cải thiện hiệu năng                                                | `perf(query): optimize user search query`        |
| `revert`   | Hoàn tác commit trước đó                                           | `revert(login): remove old login validation`     |

### Quy tắc viết commit message

| Quy tắc                                 | Đúng                                    | Sai                                     |
| --------------------------------------- | --------------------------------------- | --------------------------------------- |
| Viết ngắn gọn, rõ việc đã làm           | `feat(chat): add send message API`      | `update code`                           |
| Dùng tiếng Anh nếu có thể               | `fix(upload): validate file size`       | `sửa lỗi tùm lum`                       |
| Không viết hoa chữ đầu phần description | `feat(login): add remember me checkbox` | `feat(login): Add remember me checkbox` |
| Không thêm dấu chấm cuối câu            | `docs(readme): update setup guide`      | `docs(readme): update setup guide.`     |
| Một commit nên tập trung vào một việc   | `fix(register): validate email format`  | `fix login, update readme, add chat UI` |

### Gợi ý scope cho project

| Scope      | Ý nghĩa                      |
| ---------- | ---------------------------- |
| `login`    | Chức năng đăng nhập          |
| `register` | Chức năng đăng ký            |
| `chat`     | Chức năng chat               |
| `upload`   | Chức năng upload file        |
| `admin`    | Chức năng quản lý admin      |
| `user`     | Chức năng liên quan user     |
| `auth`     | Xác thực, phân quyền         |
| `db`       | Database, entity, repository |
| `ui`       | Giao diện                    |
| `config`   | Cấu hình project             |
| `readme`   | Tài liệu README              |

Ví dụ commit message nên dùng trong nhóm:

```bash
git commit -m "feat(login): add login page"
git commit -m "fix(register): show error when email already exists"
git commit -m "refactor(auth): move validation logic to service"
git commit -m "docs(readme): update pull request guide"
git commit -m "chore(config): update database connection"
```

Commit message nên mô tả đúng nội dung thay đổi. Không dùng các message quá chung chung như:

```bash
git commit -m "update"
git commit -m "fix bug"
git commit -m "done"
git commit -m "test"
```

---

## 7. Push branch lên GitHub

Nếu là lần đầu push branch đó:

```bash
git push -u origin ten-branch
```

Ví dụ:

```bash
git push -u origin feature/login
```

Những lần sau, chỉ cần:

```bash
git push
```

---

## 8. Tạo Pull Request

Sau khi push branch lên GitHub:

1. Mở repository trên GitHub.
2. GitHub sẽ hiện nút **Compare & pull request**.
3. Bấm **Compare & pull request**.
4. Chọn base là `main`.
5. Chọn compare là branch của bạn, ví dụ `feature/login`.
6. Viết tiêu đề và mô tả ngắn gọn.
7. Bấm **Create pull request**.

Ví dụ:

```text
base: main
compare: feature/login
```

Sau đó chờ leader review.

---

## 9. Quy trình review và merge

Leader sẽ kiểm tra Pull Request.

Nếu code ổn:

- Leader bấm **Approve**.
- Sau đó bấm **Merge pull request**.

Nếu code cần sửa:

- Leader comment vào Pull Request.
- Thành viên sửa code ở branch của mình.
- Commit và push lại.
- Pull Request sẽ tự cập nhật.

Không tự merge vào `main` nếu chưa được leader duyệt.

---

## 10. Sau khi Pull Request đã được merge

Sau khi branch của bạn đã được merge vào `main`, cập nhật lại code local:

```bash
git checkout main
git pull origin main
```

Nếu không dùng branch cũ nữa, có thể xóa branch local:

```bash
git branch -d ten-branch
```

Ví dụ:

```bash
git branch -d feature/login
```

---

## 11. Khi muốn làm task mới

Luôn bắt đầu từ `main` mới nhất:

```bash
git checkout main
git pull origin main
git checkout -b feature/ten-task-moi
```

Ví dụ:

```bash
git checkout main
git pull origin main
git checkout -b feature/product-management
```

---

## 12. Một số lỗi thường gặp

### Lỗi: đang ở sai branch

Kiểm tra branch:

```bash
git branch
```

Chuyển sang branch đúng:

```bash
git checkout ten-branch
```

Ví dụ:

```bash
git checkout feature/login
```

---

### Lỗi: quên pull code mới nhất

Nếu chưa cập nhật `main`, chạy:

```bash
git checkout main
git pull origin main
```

Sau đó tạo branch mới lại từ `main`.

---

### Lỗi: push bị từ chối

Thử pull code mới nhất của branch hiện tại:

```bash
git pull
```

Sau đó push lại:

```bash
git push
```

Nếu vẫn lỗi, báo leader để được hỗ trợ.

---

### Lỗi: conflict khi merge hoặc pull

Khi có conflict:

1. Mở file bị conflict.
2. Tìm các đoạn có dạng:

```text
<<<<<<< HEAD
code của bạn
=======
code từ branch khác
>>>>>>> branch-name
```

3. Chỉnh lại code đúng.
4. Sau đó chạy:

```bash
git add .
git commit -m "Resolve conflict"
git push
```

Nếu không chắc cách xử lý conflict, hãy báo leader trước khi sửa.

---

## 13. Các lệnh Git hay dùng

```bash
git status
```

Kiểm tra trạng thái file.

```bash
git branch
```

Xem danh sách branch local.

```bash
git branch -a
```

Xem cả branch local và branch trên GitHub.

```bash
git checkout main
```

Chuyển sang branch `main`.

```bash
git checkout -b feature/ten-chuc-nang
```

Tạo branch mới và chuyển sang branch đó.

```bash
git pull origin main
```

Lấy code mới nhất từ branch `main` trên GitHub.

```bash
git add .
```

Thêm file thay đổi vào commit.

```bash
git commit -m "message"
```

Tạo commit.

```bash
git push -u origin ten-branch
```

Push branch mới lên GitHub lần đầu.

```bash
git push
```

Push code sau khi branch đã được liên kết với GitHub.

---

## 14. Quy trình ngắn gọn mỗi lần làm task

```bash
git checkout main
git pull origin main
git checkout -b feature/ten-task
```

Code xong:

```bash
git status
git add .
git commit -m "Mo ta noi dung da lam"
git push -u origin feature/ten-task
```

Sau đó lên GitHub tạo Pull Request vào `main`.

---

## 15. Lưu ý quan trọng

- Không push trực tiếp vào `main`.
- Không sửa code trên branch của người khác nếu chưa thống nhất.
- Luôn pull `main` mới nhất trước khi tạo branch.
- Commit thường xuyên, mỗi commit nên tập trung vào một nội dung cụ thể.
- Trước khi hỏi lỗi, hãy gửi kèm ảnh màn hình hoặc nội dung lỗi trong terminal.

---

## 16. Quy tắc cấu trúc code backend

Khi code backend Spring Boot, mọi thành viên cần giữ cấu trúc code thống nhất để dễ review, dễ bảo trì và tránh làm rối project.

### 16.1. Luồng xử lý chuẩn

Luồng xử lý chính nên đi theo mô hình:

```text
Controller -> Service -> Repository -> Entity / Database / External Service
```

Không viết toàn bộ logic trong `Controller`. Controller chỉ nên nhận request, gọi xuống `Service`, sau đó trả response cho frontend.

### 16.2. Trách nhiệm từng package

- `controller/`: chỉ nhận request, lấy parameter/body, gọi service và trả response cho frontend.
- `service/`: xử lý business logic chính của chức năng.
- `repository/`: chỉ thao tác với database thông qua Spring Data JPA.
- `entity/`: chỉ mapping với table trong database, không viết business logic.
- `dto/`: chứa request/response object dùng cho API.
- `config/`: chứa cấu hình như database, S3, AI, security hoặc các config khác.
- `exception/`: xử lý lỗi tập trung và trả response lỗi thống nhất.
- `util/`: chứa các hàm tiện ích dùng chung, không phụ thuộc business logic nặng.

### 16.3. Ví dụ cấu trúc đúng

```text
DocumentController
  -> DocumentService
    -> DocumentRepository
    -> S3StorageService
```

### 16.4. Ví dụ không nên làm

```text
DocumentController
  -> tự validate file
  -> tự upload S3
  -> tự save database
  -> tự xử lý lỗi chi tiết
```

### 16.5. Chuẩn API response

Tất cả API nên trả response theo format thống nhất.

Khi thành công:

```json
{
  "success": true,
  "message": "Action successfully",
  "data": {},
  "errors": null,
  "timestamp": "2026-06-01T10:30:00Z"
}
```

Khi có lỗi:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": [
    {
      "field": "email",
      "message": "Email is invalid"
    }
  ],
  "timestamp": "2026-06-01T10:30:00Z"
}
```

### 16.6. Lưu ý bắt buộc

- Không commit file `.env`, API key, password database, AWS key hoặc secret key.
- Không lưu file upload trực tiếp vào database; file vật lý phải lưu ở storage, metadata mới lưu database.
- Không chỉnh sửa module không liên quan đến task nếu chưa thống nhất với leader.
- Nếu thêm API mới, cần đặt tên endpoint rõ ràng, đúng RESTful nhất có thể.
- Nếu thêm field mới vào entity, cần kiểm tra ảnh hưởng tới database, DTO, repository và API response.
