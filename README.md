# AIHUBDOCs — Backend

An AI-powered document question answering system. Users upload documents, the system extracts
the content, generates semantic vectors, and lets users ask questions in natural language.
Answers are generated **strictly from the user's own documents**, with source citations.

Course project for **SWP391 — Software Development Project**.

---

## 1. Features

| Area | Description |
| --- | --- |
| Accounts | Registration with email OTP verification, login, Google sign-in, refresh tokens, password reset |
| Document management | Upload, folders, tags, starring, trash, time-limited share links |
| AI question answering | Ask against a single document, a whole folder, or the entire document library |
| Chat sessions | Persisted chat history, multi-turn conversation memory, per-answer source citations |
| Subscription plans | Free / Basic / Pro tiers with storage and token quotas, VNPay payment |
| Administration | User management, account blocking, subscription plan management |

## 2. RAG architecture

```text
Upload  →  Azure Blob Storage
             ↓
         Text extraction (PDFBox / Apache POI)
             ↓
         Chunking
             ↓
         Embedding  →  OpenAI text-embedding-3-small (1536 dimensions)
             ↓
         Stored in document_chunk

Question  →  Embed the question
                 ↓
             Cosine similarity, retrieve TOP_K nearest chunks
                 ↓
             Build prompt with retrieved context  →  OpenAI gpt-5.6-luna
                 ↓
             Answer with source citations
```

The system runs with `rag.user-storage.allow-general-knowledge: false`, meaning the AI may
**only answer from retrieved document content**. Questions outside the document scope return
an explicit "not found in this document" response rather than a fabricated answer.

## 3. Tech stack

| Component | Choice |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Database | Microsoft SQL Server 16 |
| ORM | Spring Data JPA, Hibernate 7 (`ddl-auto: validate`) |
| Schema migration | Flyway |
| File storage | Azure Blob Storage with SAS tokens for temporary access |
| AI | Spring AI 2.0 + OpenAI (`gpt-5.6-luna`, `text-embedding-3-small`) |
| Security | Spring Security, JWT, OAuth2 (Google) |
| API documentation | SpringDoc OpenAPI (Swagger UI) |
| Environment loading | `springboot4-dotenv` (reads `.env` automatically) |

---

## 4. Getting started

### 4.1. Prerequisites

- **JDK 21** — verify with `.\mvnw.cmd -v`; the `Java version` line must read 21.x
- **SQL Server** with SSMS or Azure Data Studio
- Maven Wrapper is included in the repository; no separate Maven installation needed

Optional, only required for the corresponding features:

- **FFmpeg** on PATH — required for video-to-transcript
- **Tesseract OCR** — for reading text from images and scanned PDFs
- **Docling** — for higher quality document parsing than the built-in fallback

### 4.2. Create the database

Open SSMS and run `doc/sql/setup_lms_ai.sql`. The script creates the `lms_ai` database with
21 tables, foreign keys, constraints, and seed data consisting of three subscription plans
and one administrator account.

> **Run this before starting the application.** Hibernate is configured with `validate`, so it
> will refuse to start if the schema does not already exist.

The script deliberately **does not** create the `flyway_schema_history` table. Flyway creates
it on first startup, at which point `baseline-on-migrate` records a baseline and applies
migrations V1 and V2.

### 4.3. Configure environment variables

Copy `.env.example` to `.env` and fill in the values. The project uses `springboot4-dotenv`,
so this file is **loaded automatically** — no manual export or shell script required.

Minimum required for the application to start:

```env
SQLSERVER_DATASOURCE_URL=jdbc:sqlserver://localhost:1433;databaseName=lms_ai;encrypt=true;trustServerCertificate=true
SQLSERVER_DATASOURCE_USERNAME=sa
SQLSERVER_DATASOURCE_PASSWORD=<your SQL Server password>
APP_JWT_SECRET=<random string, at least 32 characters>
```

Add these as you need the corresponding features:

| Variable | Unlocks |
| --- | --- |
| `OPENAI_API_KEY` + `SPRING_AI_MODEL_CHAT=openai` + `SPRING_AI_MODEL_EMBEDDING=openai` | AI answering and embedding generation |
| `AZURE_STORAGE_CONNECTION_STRING` + `AZURE_STORAGE_CONTAINER` | Document upload and download |
| `MAIL_USERNAME` + `MAIL_PASSWORD` | Registration OTP and password reset emails |
| `GOOGLE_CLIENT_ID` + `GOOGLE_CLIENT_SECRET` | Google sign-in |
| `VNPAY_TMN_CODE` + `VNPAY_HASH_SECRET` | Subscription upgrade payments |

> ⚠️ **Common pitfall: an empty variable is not the same as a missing one.**
> Writing `MY_VAR=` in `.env` creates a variable that **exists with an empty string value**, and
> that empty value **overrides** the default declared in `${MY_VAR:default}` inside
> `application.yaml`. If you are not using a service yet, **delete the line** rather than
> leaving it blank.

### 4.4. Run

```powershell
.\mvnw.cmd spring-boot:run
```

A successful start prints `Started Group01Application` and `Tomcat started on port 8080`.

- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

Sign in with the administrator account seeded by the SQL script:
`admin@aistudyhub.local` / `Admin@123`. **Change this password immediately after first login.**

---

## 5. Code conventions

### 5.1. Request flow

```text
Controller  →  Service  →  Repository  →  Entity / Database
                       →  External Service (Azure Blob, OpenAI, Mail)
```

Controllers only accept the request, call a service, and return the response. No business
logic in controllers.

### 5.2. Package responsibilities

| Package | Responsibility |
| --- | --- |
| `controller/` | Accept requests, call services, return responses |
| `service/` | All business logic |
| `repository/` | Database access through Spring Data JPA |
| `entity/` | Table mapping only, no business logic |
| `dto/` | API request and response objects |
| `config/` | Database, Azure Storage, AI and security configuration |
| `exception/` | Centralised error handling |
| `util/` | Shared helpers with no heavy business logic |
| `enums/` | Shared enumerations |
| `security/` | JWT filter, OAuth2 handlers |

### 5.3. Good example

```text
DocumentController
  → DocumentService
      → DocumentRepository
      → FileStorageService      (interface, implemented by Azure Blob)
```

### 5.4. What to avoid

```text
DocumentController
  → validates the file itself
  → calls the Azure SDK directly
  → saves to the database itself
  → handles low-level errors itself
```

File storage always goes through the `FileStorageService` interface. Never call a provider SDK
directly from a controller or a business service — that indirection is what makes switching
storage providers a contained change rather than a rewrite.

### 5.5. Response format

Success:

```json
{
  "success": true,
  "message": "Action successfully",
  "data": {},
  "errors": null,
  "timestamp": "2026-08-12T10:30:00Z"
}
```

Failure:

```json
{
  "success": false,
  "message": "Validation failed",
  "data": null,
  "errors": [{ "field": "email", "message": "Email is invalid" }],
  "timestamp": "2026-08-12T10:30:00Z"
}
```

### 5.6. Hard rules

- **Never commit** `.env`, API keys, database passwords, connection strings or any secret
- Never store file contents in the database — physical files live in blob storage, the database
  holds metadata only
- New endpoints must follow RESTful naming
- Adding a field to an entity means checking the impact on migrations, DTOs, repositories and
  API responses
- Do not modify modules outside your task without discussing it with the team first

---

## 6. Git workflow

Never commit directly to `main`. One branch per task, then open a Pull Request for review
before merging.

```bash
# Starting a new task
git checkout main
git pull origin main
git checkout -b feature/your-feature-name

# While working
git add .
git commit -m "feat(chat): add session message endpoint"

# Push and open a Pull Request
git push -u origin feature/your-feature-name
```

Branch naming: `feature/`, `fix/`, `refactor/`, `docs/` followed by a short English description.

### Commit convention

**Conventional Commits**: `type(scope): description`

| Type | Use when |
| --- | --- |
| `feat` | Adding a new feature |
| `fix` | Fixing a bug |
| `refactor` | Restructuring code without changing behaviour |
| `docs` | Documentation changes |
| `test` | Adding or updating tests |
| `chore` | Configuration, dependencies, housekeeping |
| `build` | Build tooling changes |
| `perf` | Performance improvements |

Common scopes: `auth`, `chat`, `upload`, `document`, `user`, `admin`, `db`, `config`.

Three rules: write the description in English, do not capitalise the first word, do not end
with a period. Keep each commit focused on one thing.

```bash
# Good
git commit -m "feat(upload): validate file size before upload"
git commit -m "fix(auth): return 401 when refresh token expired"

# Bad
git commit -m "update"
git commit -m "fix bug"
```

### Resolving conflicts

```bash
git checkout main
git pull origin main
git checkout feature/your-feature-name
git merge main
# resolve the conflicting files, then:
git add .
git commit -m "chore(merge): resolve conflict with main"
git push
```

If you are unsure how to resolve a conflict, ask the team before overwriting anything.

---

## 7. Related documentation

| Document | Contents |
| --- | --- |
| `API_CONTRACT.md` | Full API contract for the frontend |
| `doc/sql/setup_lms_ai.sql` | Database setup script with seed data |
| `doc/GIAI_THICH_CAU_HINH.md` | Reference for every `.env` and `application.yaml` parameter (Vietnamese) |
| `doc/TON_DONG_VA_PHAT_HIEN.md` | Outstanding work and technical findings (Vietnamese) |

## 8. Notes

The Java package is still `com.se1908.group01`, inherited from the project's original
structure. Renaming it would touch all 244 source files, so it has not been done.

The seeded administrator account uses `admin@aistudyhub.local`. This value lives in
`doc/sql/setup_lms_ai.sql`; changing it requires updating both the script and any database
already created from it.