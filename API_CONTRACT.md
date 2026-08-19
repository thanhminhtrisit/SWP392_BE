# API Contract - AI Study Hub Backend

This document describes the currently available backend APIs so the frontend team knows which endpoints exist, what to send, and what each API returns.

> Local base URL: `http://localhost:8080`

---

## 1. Standard response format

All REST APIs return responses using this format.

> Note: Google OAuth2 browser login is a redirect flow, so the browser is redirected back to the frontend instead of receiving this JSON format directly.

```json
{
  "success": true,
  "message": "Action successfully",
  "data": {},
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

Error response:

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
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Common fields

| Field       | Type                  | Description                                                 |
| ----------- | --------------------- | ----------------------------------------------------------- |
| `success`   | boolean               | `true` for successful requests, `false` for failed requests |
| `message`   | string                | General response message                                    |
| `data`      | object / array / null | Response payload                                            |
| `errors`    | array / null          | Detailed error list                                         |
| `timestamp` | string                | Response creation time                                      |

### Error object

| Field     | Type          | Description                                                     |
| --------- | ------------- | --------------------------------------------------------------- |
| `field`   | string / null | Invalid field, for example `email`, `password`, `authorization` |
| `message` | string        | Error message                                                   |

---

## 2. Authentication APIs

All authentication endpoints are under `/api/auth`.

---

## 2.1. Register

Register a new account using email and password.

### Request

- Method: `POST`
- URL: `/api/auth/register`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "fullName": "Long Nguyen",
  "email": "long@example.com",
  "password": "password123"
}
```

### Request fields

| Field      | Type   | Required | Rule                                     |
| ---------- | ------ | -------- | ---------------------------------------- |
| `fullName` | string | Yes      | Must not be blank                        |
| `email`    | string | Yes      | Must not be blank, must be a valid email |
| `password` | string | Yes      | Must not be blank, minimum 8 characters  |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Register successfully",
  "data": {
    "message": "Register successfully. OTP has been sent to your email.",
    "email": "long@example.com"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message                   | Reason                                             |
| ------ | ------------------------- | -------------------------------------------------- |
| `400`  | `Validation failed`       | Invalid email, password, or fullName               |
| `400`  | `Validation failed`       | Email already exists                               |
| `500`  | `Unexpected server error` | Unexpected error, for example mail service failure |

---

## 2.2. Verify OTP

Verify the OTP after registration.

### Request

- Method: `POST`
- URL: `/api/auth/verify-otp`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "email": "long@example.com",
  "otp": "123456"
}
```

### Request fields

| Field   | Type   | Required | Rule                                     |
| ------- | ------ | -------- | ---------------------------------------- |
| `email` | string | Yes      | Must not be blank, must be a valid email |
| `otp`   | string | Yes      | Exactly 6 characters                     |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": {
    "message": "OTP verified successfully. Account actived."
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                      |
| ------ | -------------------- | --------------------------- |
| `400`  | `Validation failed`  | Invalid email or OTP format |
| `400`  | `Validation failed`  | Invalid OTP                 |
| `400`  | `Validation failed`  | OTP has expired             |
| `400`  | `Validation failed`  | Too many failed attempts    |
| `404`  | `Resource not found` | User or OTP not found       |

---

## 2.3. Login

Log in using email and password, then receive an access token and refresh token.

### Request

- Method: `POST`
- URL: `/api/auth/login`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "email": "long@example.com",
  "password": "password123"
}
```

### Request fields

| Field      | Type   | Required | Rule                                     |
| ---------- | ------ | -------- | ---------------------------------------- |
| `email`    | string | Yes      | Must not be blank, must be a valid email |
| `password` | string | Yes      | Must not be blank                        |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Login successfully",
  "data": {
    "accessToken": "jwt-token",
    "refreshToken": "refresh-token",
    "tokenType": "Bearer",
    "userId": 1,
    "email": "long@example.com",
    "role": "USER"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Frontend usage

After a successful login, store both `data.accessToken` and `data.refreshToken`.

Use the access token when calling private APIs:

```text
Authorization: Bearer <accessToken>
```

Use the refresh token only when calling `/api/auth/refresh` or `/api/auth/logout`.

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing or invalid email/password |
| `400` | `Invalid email or password` | Invalid email or password |
| `400` | `Account is not verified. Please complete OTP verification.` | Account is still `PENDING` |
| `400` | `Account has been blocked. Please contact support.` | Account is `BLOCKED` |

---

## 2.4. Resend OTP

Resend the registration OTP.

### Request

- Method: `POST`
- URL: `/api/auth/resend-otp`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "email": "long@example.com"
}
```

### Request fields

| Field   | Type   | Required | Rule                                     |
| ------- | ------ | -------- | ---------------------------------------- |
| `email` | string | Yes      | Must not be blank, must be a valid email |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "OTP resent successfully",
  "data": {
    "mesage": "OTP has been resent to your email.",
    "email": "long@example.com"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

> Note: the response field is currently `mesage` because that is the current DTO field in the codebase.

### Error cases

| Status | Message              | Reason                      |
| ------ | -------------------- | --------------------------- |
| `400`  | `Validation failed`  | Missing or invalid email    |
| `400`  | `Validation failed`  | Account is already verified |
| `404`  | `Resource not found` | User not found              |

---

## 2.5. Google OAuth2 Login Redirect Flow

Google OAuth2 login is a browser redirect flow. The frontend should redirect the browser to the backend OAuth2 authorization URL. After Google login finishes, the backend redirects the browser back to the React app.

### Request

- Method: `GET`
- URL: `/oauth2/authorization/google`
- Auth: OAuth2 login flow

The frontend should open this URL in the browser, not call it with Axios as a normal JSON API.

### Success redirect

When Google login succeeds and the backend can create or find the user account, the backend redirects to:

```text
http://localhost:5173/oauth2/redirect?token=<accessToken>&refreshToken=<refreshToken>&email=<email>&userId=<userId>&role=<role>&fullName=<fullName>
```

### Success query parameters

| Parameter      | Type   | Description                   |
| -------------- | ------ | ----------------------------- |
| `token`        | string | JWT access token              |
| `refreshToken` | string | Refresh token                 |
| `email`        | string | User email                    |
| `userId`       | number | User ID                       |
| `role`         | string | User role, for example `USER` |
| `fullName`     | string | Google account display name   |

### Error redirect

When Google login fails, or when the Google email is already registered with another provider, the backend redirects to:

```text
http://localhost:5173/login?error=<error-message>
```

Example:

```text
http://localhost:5173/login?error=Email%20already%20registered%20with%20another%20provider
```

### Frontend usage

1. Redirect the browser to `http://localhost:8080/oauth2/authorization/google`.
2. React handles `/oauth2/redirect`.
3. Read `token`, `refreshToken`, `email`, `userId`, `role`, and `fullName` from query parameters.
4. Store the latest `token` and `refreshToken`.
5. Redirect the user to the dashboard.
6. If the browser returns to `/login?error=...`, show the error message on the login page.

### Backend note

`GET /api/auth/google/success` may still exist internally, but the current frontend flow should not depend on it. The expected frontend integration point is the redirect URL above.

---

## 2.6. Refresh token

Use a refresh token to get a new access token and a new refresh token.

### Request

- Method: `POST`
- URL: `/api/auth/refresh`
- Auth: No access token required
- Content-Type: `application/json`

```json
{
  "refreshToken": "refresh-token"
}
```

### Request fields

| Field          | Type   | Required | Rule              |
| -------------- | ------ | -------- | ----------------- |
| `refreshToken` | string | Yes      | Must not be blank |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Token refreshed successfully",
  "data": {
    "accessToken": "new-jwt-token",
    "refreshToken": "new-refresh-token",
    "tokenType": "Bearer"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Refresh token rotation rule

This backend uses refresh token rotation.

When `/api/auth/refresh` succeeds:

- The old refresh token is revoked.
- The backend returns a new access token.
- The backend returns a new refresh token.
- The frontend must replace both old tokens with the new tokens.

Example:

```text
Login returns: A1 + R1
POST /api/auth/refresh with R1
Backend returns: A2 + R2
Frontend must use A2 + R2 from now on
R1 must not be used again
```

If the frontend uses an old refresh token again, the backend may treat it as refresh token reuse and revoke sessions for security.

### Error cases

| Status | Message             | Reason                         |
| ------ | ------------------- | ------------------------------ |
| `400`  | `Validation failed` | Missing refresh token          |
| `400`  | `Validation failed` | Invalid refresh token          |
| `400`  | `Validation failed` | Refresh token expired          |
| `400`  | `Validation failed` | Refresh token was already used |

---

## 2.7. Logout

Logout by revoking the current refresh token.

### Request

- Method: `POST`
- URL: `/api/auth/logout`
- Auth: No access token required
- Content-Type: `application/json`

```json
{
  "refreshToken": "refresh-token"
}
```

### Request fields

| Field          | Type   | Required | Rule              |
| -------------- | ------ | -------- | ----------------- |
| `refreshToken` | string | Yes      | Must not be blank |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Logged out successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message             | Reason                |
| ------ | ------------------- | --------------------- |
| `400`  | `Validation failed` | Missing refresh token |

---

## 2.8. Forgot password

Send a forgot-password OTP to a local email/password account.

### Request

- Method: `POST`
- URL: `/api/auth/forgot-password`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "email": "long@example.com"
}
```

### Request fields

| Field   | Type   | Required | Rule                                     |
| ------- | ------ | -------- | ---------------------------------------- |
| `email` | string | Yes      | Must not be blank, must be a valid email |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Forgot password OTP sent successfully",
  "data": {
    "message": "OTP has been sent to your email.",
    "email": "long@example.com"
  },
  "errors": null,
  "timestamp": "2026-06-16T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                               |
| ------ | -------------------- | ---------------------------------------------------- |
| `400`  | `Validation failed`  | Missing or invalid email                             |
| `400`  | `Validation failed`  | Account does not use password login, for example Google account |
| `404`  | `Resource not found` | User not found                                       |
| `500`  | `Unexpected server error` | Mail service or unexpected server error          |

---

## 2.9. Verify forgot-password OTP

Verify the OTP before resetting the password.

### Request

- Method: `POST`
- URL: `/api/auth/verify-forgot-password-otp`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "email": "long@example.com",
  "otp": "123456"
}
```

### Request fields

| Field   | Type   | Required | Rule                                     |
| ------- | ------ | -------- | ---------------------------------------- |
| `email` | string | Yes      | Must not be blank, must be a valid email |
| `otp`   | string | Yes      | Exactly 6 characters                     |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Forgot password OTP verified successfully",
  "data": {
    "message": "OTP verified successfully. You can reset your password."
  },
  "errors": null,
  "timestamp": "2026-06-16T10:35:00Z"
}
```

### Error cases

| Status | Message              | Reason                    |
| ------ | -------------------- | ------------------------- |
| `400`  | `Validation failed`  | Missing or invalid email/OTP |
| `400`  | `Validation failed`  | Invalid OTP               |
| `400`  | `Validation failed`  | OTP has expired           |
| `400`  | `Validation failed`  | Too many failed attempts  |
| `404`  | `Resource not found` | User or OTP not found     |

---

## 2.10. Reset password

Reset the password after the forgot-password OTP has been verified.

### Request

- Method: `POST`
- URL: `/api/auth/reset-password`
- Auth: No JWT required
- Content-Type: `application/json`

```json
{
  "email": "long@example.com",
  "newPassword": "newPassword123"
}
```

### Request fields

| Field         | Type   | Required | Rule                                     |
| ------------- | ------ | -------- | ---------------------------------------- |
| `email`       | string | Yes      | Must not be blank, must be a valid email |
| `newPassword` | string | Yes      | Must not be blank, minimum 8 characters  |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Password reset sucessfully",
  "data": {
    "message": "Password reset successfully."
  },
  "errors": null,
  "timestamp": "2026-06-16T10:40:00Z"
}
```

> Note: the top-level message is currently `Password reset sucessfully` because that is the current message in the codebase.

### Error cases

| Status | Message              | Reason                                   |
| ------ | -------------------- | ---------------------------------------- |
| `400`  | `Validation failed`  | Missing or invalid email/newPassword     |
| `400`  | `Validation failed`  | OTP has not been verified                |
| `404`  | `Resource not found` | User not found or OTP verification missing |

---

## 2.11. Recommended frontend token flow

```text
Login or Google login
-> Store accessToken and refreshToken
-> Call private APIs with accessToken
-> If private API returns 401 because accessToken expired
-> Call POST /api/auth/refresh with refreshToken
-> Store new accessToken and new refreshToken
-> Retry the failed private API request
-> On logout, call POST /api/auth/logout with the latest refreshToken
```

Important frontend notes:

- Always store the latest refresh token returned by `/api/auth/refresh`.
- Do not reuse an old refresh token after a successful refresh.
- Avoid sending multiple refresh requests at the same time with the same refresh token.

---

## 3. Document APIs

Private APIs require this header:

```text
Authorization: Bearer <accessToken>
```

Current public document APIs:

- `GET /api/documents/public`
- `GET /api/documents/public/{documentId}`
- `GET /api/documents/share-link/{token}`
- `GET /api/documents/share-link/{token}/preview-url`
- `GET /api/documents/share-link/{token}/download-url`

---

## 3.1. Document response object

Document APIs usually return a `DocumentUploadResponse` object:

```json
{
  "documentId": 1,
  "userId": 1,
  "folderId": null,
  "originalFileName": "example.pdf",
  "s3Key": "documents/1/uuid-example.pdf",
  "contentType": "application/pdf",
  "fileSize": 123456,
  "isPublic": false,
  "isDeleted": false,
  "isStarred": false,
  "status": "READY",
  "uploadedAt": "2026-06-14T10:30:00Z",
  "deletedAt": null
}
```

### Document fields

| Field              | Type          | Description                                  |
| ------------------ | ------------- | -------------------------------------------- |
| `documentId`       | number        | Document ID                                  |
| `userId`           | number        | Owner user ID                                |
| `folderId`         | number / null | Folder ID if the document is inside a folder |
| `originalFileName` | string        | Sanitized original file name                 |
| `s3Key`            | string        | S3 object key                                |
| `contentType`      | string        | MIME type                                    |
| `fileSize`         | number        | File size in bytes                           |
| `isPublic`         | boolean       | Public/private visibility                    |
| `isDeleted`        | boolean       | Soft delete flag                             |
| `isStarred`        | boolean       | Whether the document is starred by the owner |
| `status`           | string        | Document processing status                   |
| `uploadedAt`       | string        | Upload time                                  |
| `deletedAt`        | string / null | Soft delete time                             |

### Document status

| Status     | Description                                     |
| ---------- | ----------------------------------------------- |
| `UPLOADED` | File was uploaded and metadata was saved        |
| `PARSING`  | The system is extracting document text          |
| `INDEXING` | The system is chunking/embedding/indexing       |
| `READY`    | The document is ready for future RAG/chat usage |
| `FAILED`   | Parsing or indexing failed                      |

---

## 3.2. Upload document

Upload a file to S3 and save metadata. Parsing, chunking, and embedding run in a background job after the upload request succeeds.

### Request

- Method: `POST`
- URL: `/api/documents/upload`
- Auth: JWT required
- Content-Type: `multipart/form-data`

### Form-data fields

| Field      | Type    | Required | Rule                                             |
| ---------- | ------- | -------- | ------------------------------------------------ |
| `file`     | File    | Yes      | Uploaded file                                    |
| `isPublic` | boolean | No       | `true` or `false`; private by default if omitted |

### Supported files

Limits:

- Normal document/image maximum size: `20MB`.
- Video maximum size: controlled by `APP_MAX_VIDEO_FILE_SIZE`, default `52428800` bytes (`50MB`).
- Backend multipart limit is controlled by `APP_MAX_MULTIPART_FILE_SIZE` and `APP_MAX_MULTIPART_REQUEST_SIZE`, default `50MB`.
- Empty files are rejected.

Directly supported extensions:

- `pdf`
- `doc`
- `docx`
- `pptx`
- `xls`
- `xlsx`
- `png`
- `mp4`
- `mov`
- `avi`
- `webm`

Files whose `Content-Type` starts with `image/` are also accepted.
Files whose `Content-Type` starts with `video/` are also accepted.

### Upload processing behavior

| File type | Storage | Parsing/indexing behavior |
| --------- | ------- | ------------------------- |
| PDF / DOC / DOCX / PPTX / XLS / XLSX | Uploaded to private S3 and metadata saved to database | Text is extracted, chunked, embedded, then saved to `document_chunk` |
| Image files | Uploaded to private S3 and metadata saved to database | OCR can extract text if Tesseract is enabled; otherwise indexing may fail or produce no useful text |
| Video files | Uploaded to private S3 and metadata saved to database | Current backend stores a placeholder chunk: `[VIDEO] Transcript pending...`; real video transcription is not implemented yet |

Important behavior:

- Upload response returns quickly after S3 upload and metadata save.
- The returned document status is usually `UPLOADED`.
- Backend continues processing asynchronously: `UPLOADED` -> `PARSING` -> `INDEXING` -> `READY`.
- If parsing, embedding, or indexing fails, status becomes `FAILED`.
- Frontend should poll document detail/list APIs and enable AI chat only when status is `READY`.

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Upload document successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": null,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": true,
    "isDeleted": false,
    "isStarred": false,
    "status": "UPLOADED",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                 |
| ------ | -------------------- | ------------------------------------------------------ |
| `400`  | `Validation failed`  | Empty file, missing filename, or unsupported extension |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                 |
| `413`  | `File upload failed` | Uploaded file exceeds the size limit                   |
| `500`  | `File read failed`   | Backend failed to read the file                        |
| `503`  | `S3 upload failed`   | S3 upload failed                                       |

---

## 3.2.1. Filter my documents

Filter and paginate active documents owned by the authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/filter`
- Auth: JWT required

### Query parameters

| Field | Type | Required | Rule |
|---|---|---|---|
| `tagIds` | repeated number / null | No | Example: `tagIds=1&tagIds=2`; documents matching any supplied tag are returned |
| `contentType` | string / null | No | Exact MIME-type match, for example `application/pdf` |
| `createdFrom` | ISO-8601 datetime / null | No | Inclusive lower bound for `uploadedAt` |
| `createdTo` | ISO-8601 datetime / null | No | Inclusive upper bound for `uploadedAt` |
| `sort` | string | No | `NEWEST` (default) or `OLDEST` |
| `page` | integer | No | Default `0`; must be at least `0` |
| `size` | integer | No | Default `20`; must be between `1` and `100` |

Example:

```text
GET /api/documents/filter?tagIds=1&tagIds=2&contentType=application/pdf&sort=NEWEST&page=0&size=20
```

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Filter documents successfully",
  "data": {
    "documents": [
      {
        "documentId": 1,
        "userId": 1,
        "folderId": null,
        "originalFileName": "example.pdf",
        "s3Key": "documents/1/uuid-example.pdf",
        "contentType": "application/pdf",
        "fileSize": 123456,
        "isPublic": false,
        "isDeleted": false,
        "isStarred": false,
        "status": "READY",
        "uploadedAt": "2026-07-06T10:30:00Z",
        "deletedAt": null
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "errors": null,
  "timestamp": "2026-07-06T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Page must be greater than or equal to 0` | Invalid page |
| `400` | `Size must be between 1 and 100` | Invalid page size |
| `400` | `Sort must be NEWEST or OLDEST` | Invalid sort value |
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 3.3. Get my documents

Get documents that belong to the currently authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/my`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get my documents successfully",
  "data": [
    {
      "documentId": 1,
      "userId": 1,
      "folderId": null,
      "originalFileName": "example.pdf",
      "s3Key": "documents/1/uuid-example.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": false,
      "isDeleted": false,
      "isStarred": false,
      "status": "READY",
      "uploadedAt": "2026-06-14T10:30:00Z",
      "deletedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message        | Reason                 |
| ------ | -------------- | ---------------------- |
| `401`  | `Unauthorized` | Missing or invalid JWT |

---

## 3.3.1. Get starred documents

Get active starred documents of the currently authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/starred`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get starred documents successfully",
  "data": [
    {
      "documentId": 1,
      "userId": 1,
      "folderId": null,
      "originalFileName": "example.pdf",
      "s3Key": "documents/1/uuid-example.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": false,
      "isDeleted": false,
      "isStarred": true,
      "status": "READY",
      "uploadedAt": "2026-06-15T10:30:00Z",
      "deletedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-15T10:30:00Z"
}
```

### Error cases

| Status | Message        | Reason                 |
| ------ | -------------- | ---------------------- |
| `401`  | `Unauthorized` | Missing or invalid JWT |

---

## 3.4. Get my document detail

Get the detail of a document owned by the current user.

### Request

- Method: `GET`
- URL: `/api/documents/{documentId}`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get document detail successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": null,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                                    |
| ------ | -------------------- | ------------------------------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                                    |
| `404`  | `Resource not found` | Document does not exist, does not belong to the user, or was soft-deleted |

---

## 3.4.1. Rename my document

Rename the document metadata field `originalFileName`. This does not rename or move the physical file in S3.

### Request

- Method: `PATCH`
- URL: `/api/documents/{documentId}/rename`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "originalFileName": "new-file-name.pdf"
}
```

### Request fields

| Field              | Type   | Required | Rule                                      |
| ------------------ | ------ | -------- | ----------------------------------------- |
| `originalFileName` | string | Yes      | Must not be blank, maximum 512 characters |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Rename document successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": null,
    "originalFileName": "new-file-name.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                                    |
| ------ | -------------------- | ------------------------------------------------------------------------- |
| `400`  | `Validation failed`  | Missing or invalid originalFileName                                       |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                                    |
| `404`  | `Resource not found` | Document does not exist, does not belong to the user, or was soft-deleted |

---

## 3.4.2. Move my document to folder

Move an owned active document into a folder, or remove it from the current folder.

### Request

- Method: `PATCH`
- URL: `/api/documents/{documentId}/folder`
- Auth: JWT required
- Content-Type: `application/json`

Move into a folder:

```json
{
  "folderId": 1
}
```

Remove from folder:

```json
{
  "folderId": null
}
```

### Request fields

| Field      | Type          | Required | Rule                                        |
| ---------- | ------------- | -------- | ------------------------------------------- |
| `folderId` | number / null | No       | Must belong to the current user if provided |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Move document to folder successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": 1,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                        |
| ------ | -------------------- | ------------------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                        |
| `404`  | `Resource not found` | Document/folder does not exist or does not belong to the user |

---

## 3.4.3. Get my document preview URL

Get a temporary pre-signed URL for previewing an owned document.

### Request

- Method: `GET`
- URL: `/api/documents/{documentId}/preview-url`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get document preview URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-14T10:40:00Z",
    "fileName": "example.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                                    |
| ------ | -------------------- | ------------------------------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                                    |
| `404`  | `Resource not found` | Document does not exist, does not belong to the user, or was soft-deleted |
| `500`  | `Request failed`     | S3 pre-signer is not configured                                           |

---

## 3.4.4. Get my document download URL

Get a temporary pre-signed URL for downloading an owned document.

### Request

- Method: `GET`
- URL: `/api/documents/{documentId}/download-url`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get document download URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-14T10:40:00Z",
    "fileName": "example.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                                    |
| ------ | -------------------- | ------------------------------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                                    |
| `404`  | `Resource not found` | Document does not exist, does not belong to the user, or was soft-deleted |
| `500`  | `Request failed`     | S3 pre-signer is not configured                                           |

---

## 3.5. Get public documents

Get public documents for the community page.

### Request

- Method: `GET`
- URL: `/api/documents/public`
- Auth: Public, no JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get public documents successfully",
  "data": [
    {
      "documentId": 2,
      "userId": 1,
      "folderId": null,
      "originalFileName": "public-file.pdf",
      "s3Key": "documents/1/uuid-public-file.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": true,
      "isDeleted": false,
      "isStarred": false,
      "status": "READY",
      "uploadedAt": "2026-06-14T10:30:00Z",
      "deletedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

---

## 3.6. Get public document detail

Get the detail of a public document.

### Request

- Method: `GET`
- URL: `/api/documents/public/{documentId}`
- Auth: Public, no JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get public document detail successfully",
  "data": {
    "documentId": 2,
    "userId": 1,
    "folderId": null,
    "originalFileName": "public-file.pdf",
    "s3Key": "documents/1/uuid-public-file.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": true,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                          |
| ------ | -------------------- | ----------------------------------------------- |
| `404`  | `Resource not found` | Public document does not exist or is not public |

---

## 3.6.1. Get public document preview URL

Get a temporary pre-signed URL for previewing a public document.

### Request

- Method: `GET`
- URL: `/api/documents/public/{documentId}/preview-url`
- Auth: Public, no JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get public document preview URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-14T10:40:00Z",
    "fileName": "public-file.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                          |
| ------ | -------------------- | ----------------------------------------------- |
| `404`  | `Resource not found` | Public document does not exist or is not public |
| `500`  | `Request failed`     | S3 pre-signer is not configured                 |

---

## 3.6.2. Get public document download URL

Get a temporary pre-signed URL for downloading a public document.

### Request

- Method: `GET`
- URL: `/api/documents/public/{documentId}/download-url`
- Auth: Public, no JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get public document download URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-14T10:40:00Z",
    "fileName": "public-file.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                          |
| ------ | -------------------- | ----------------------------------------------- |
| `404`  | `Resource not found` | Public document does not exist or is not public |
| `500`  | `Request failed`     | S3 pre-signer is not configured                 |

---

## 3.7. Update document visibility

Update a document to public or private.

### Request

- Method: `PATCH`
- URL: `/api/documents/{documentId}/visibility?isPublic=true`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Query params

| Name       | Type    | Required | Example           |
| ---------- | ------- | -------- | ----------------- |
| `isPublic` | boolean | Yes      | `true` or `false` |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Update document visibility successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": true,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                               |
| ------ | -------------------- | -------------------------------------------------------------------- |
| `400`  | `Validation failed`  | Missing `isPublic`                                                   |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                               |
| `404`  | `Resource not found` | Document does not exist, does not belong to the user, or is in Trash |

---

## 3.7.1. Update document starred

Star or unstar an owned active document.

### Request

- Method: `PATCH`
- URL: `/api/documents/{documentId}/star?isStarred=true`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Query params

| Name        | Type    | Required | Example           |
| ----------- | ------- | -------- | ----------------- |
| `isStarred` | boolean | Yes      | `true` or `false` |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Update document starred successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": null,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": true,
    "status": "READY",
    "uploadedAt": "2026-06-15T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-15T10:30:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                               |
| ------ | -------------------- | -------------------------------------------------------------------- |
| `400`  | `Validation failed`  | Missing `isStarred`                                                  |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                               |
| `404`  | `Resource not found` | Document does not exist, does not belong to the user, or is in Trash |

---

## 3.7.2. Get document share approvals (admin)

Get paginated document approvals for admin review.

### Request

- Method: `GET`
- URL: `/api/documents/document-share-approvals`
- Auth: JWT required (ADMIN role)

### Query params

| Name       | Type                                | Required | Default             | Example                |
| ---------- | ----------------------------------- | -------- | ------------------- | ---------------------- |
| `status`   | `ShareApprovalStatus`              | No       | `PENDING_APPROVAL`  | `PENDING_APPROVAL`     |
| `shareType`| `DocumentShareApprovalType` / null | No       | `null`              | `PUBLIC`               |
| `page`     | number                             | No       | `0`                 | `0`                    |
| `size`     | number                             | No       | Spring default size | `10`                   |
| `sort`     | string                             | No       | `createdAt,asc`     | `createdAt,asc`        |

### Enum values

`ShareApprovalStatus`:

- `UNREVIEWED`
- `PENDING_APPROVAL`
- `APPROVED`
- `REJECTED`

`DocumentShareApprovalType`:

- `PUBLIC`
- `LINK`
- `DIRECT`

### Example request

```text
GET /api/documents/document-share-approvals?status=PENDING_APPROVAL&shareType=PUBLIC&page=0&size=10&sort=createdAt,asc
```

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get document share approvals successfully",
  "data": {
    "content": [
      {
        "approvalId": 15,
        "documentId": 123,
        "documentName": "policy.pdf",
        "ownerId": 5,
        "ownerEmail": "owner@example.com",
        "shareType": "PUBLIC",
        "status": "PENDING_APPROVAL",
        "createdAt": "2026-08-19T08:30:00Z",
        "updatedAt": "2026-08-19T08:30:00Z"
      }
    ],
    "pageable": {
      "pageNumber": 0,
      "pageSize": 10
    },
    "totalElements": 1,
    "totalPages": 1,
    "first": true,
    "last": true,
    "number": 0,
    "size": 10,
    "empty": false
  },
  "errors": null,
  "timestamp": "2026-08-19T08:30:00Z"
}
```

### Approval item fields

| Field         | Type                  | Description |
| ------------- | --------------------- | ----------- |
| `approvalId`  | number                | Approval row ID |
| `documentId`  | number                | Document ID |
| `documentName`| string                | Original file name |
| `ownerId`     | number                | Owner user ID |
| `ownerEmail`  | string / null         | Owner email |
| `shareType`   | `DocumentShareApprovalType` | Share approval type |
| `status`      | `ShareApprovalStatus` | Current approval status |
| `createdAt`   | string (ISO datetime) | Creation timestamp |
| `updatedAt`   | string (ISO datetime) | Last update timestamp |

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` or specific validation message | Invalid enum/query format, or bad request parameters |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `403` | `Forbidden` | Authenticated user is not allowed to access admin APIs |
| `500` | `Request failed` | Unexpected server error |

---

## 3.7.3. Review document share approval (admin)

Approve or reject pending approval records of one document.

### Request

- Method: `PATCH`
- URL: `/api/documents/{documentId}/share-approval?status=APPROVED`
- Auth: JWT required (ADMIN role)

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Query params

| Name     | Type                  | Required | Allowed values |
| -------- | --------------------- | -------- | -------------- |
| `status` | `ShareApprovalStatus` | Yes      | `APPROVED`, `REJECTED` |

### Example requests

```text
PATCH /api/documents/123/share-approval?status=APPROVED
PATCH /api/documents/123/share-approval?status=REJECTED
```

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Review document share approval successfully",
  "data": {
    "documentId": 123,
    "userId": 5,
    "ownerEmail": "owner@example.com",
    "folderId": null,
    "originalFileName": "policy.pdf",
    "s3Key": "documents/5/policy.pdf",
    "contentType": "application/pdf",
    "fileSize": 209715,
    "isPublic": true,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "shareApprovalStatus": "APPROVED",
    "uploadedAt": "2026-08-18T10:00:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-08-19T08:45:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Approval status is required` | Missing `status` query parameter |
| `400` | `Approval status must be APPROVED or REJECTED` | Unsupported status value |
| `400` | `Only admin can review document approval` | Non-admin attempted review |
| `400` | `Document is deleted` | Cannot review deleted document |
| `400` | `Document is not pending admin approval` | No pending approvals found for document |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `403` | `Forbidden` | Access denied by security rules |
| `404` | `Document not found` | Document ID does not exist |

---

## 3.8. Move document to Trash

Soft-delete a document. The physical file is not deleted from S3.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Move document to trash successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": true,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": "2026-06-14T10:40:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:40:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                 |
| ------ | -------------------- | ------------------------------------------------------ |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                 |
| `404`  | `Resource not found` | Document does not exist or does not belong to the user |

---

## 3.9. Get trash documents

Get soft-deleted documents of the current user.

### Request

- Method: `GET`
- URL: `/api/documents/trash`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get trash documents successfully",
  "data": [
    {
      "documentId": 1,
      "userId": 1,
      "originalFileName": "example.pdf",
      "s3Key": "documents/1/uuid-example.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": false,
      "isDeleted": true,
      "isStarred": false,
      "status": "READY",
      "uploadedAt": "2026-06-14T10:30:00Z",
      "deletedAt": "2026-06-14T10:40:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-14T10:40:00Z"
}
```

### Error cases

| Status | Message        | Reason                 |
| ------ | -------------- | ---------------------- |
| `401`  | `Unauthorized` | Missing or invalid JWT |

---

## 3.10. Restore document

Restore a document from Trash.

### Request

- Method: `POST`
- URL: `/api/documents/{documentId}/restore`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Restore document successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "originalFileName": "example.pdf",
    "s3Key": "documents/1/uuid-example.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-14T10:30:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-14T10:45:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                 |
| ------ | -------------------- | ------------------------------------------------------ |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                 |
| `404`  | `Resource not found` | Document does not exist or does not belong to the user |

---

## 3.11. Delete document permanently

Permanently delete a document. Only documents already in Trash can be permanently deleted.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}/permanent`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Delete document permanently successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-14T10:50:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                 |
| ------ | -------------------- | ------------------------------------------------------ |
| `400`  | `Validation failed`  | Document is not in Trash                               |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                 |
| `404`  | `Resource not found` | Document does not exist or does not belong to the user |
| `503`  | `S3 delete failed`   | Failed to delete the file from S3                      |

---

## 3.12. Share document APIs

Document sharing supports two flows:

1. Share by public token link.
2. Share directly with another user by email.

Private share management APIs require:

```text
Authorization: Bearer <accessToken>
```

Share-link read APIs are public because anyone with the valid token can access the shared document.

---

## 3.12.1. Document share link response object

Share link APIs return a `DocumentShareLinkResponse` object:

```json
{
  "shareLinkId": 1,
  "documentId": 1,
  "token": "c1a2b3token",
  "accessPath": "/api/documents/share-link/c1a2b3token",
  "enabled": true,
  "expiresAt": null,
  "createdAt": "2026-06-19T10:30:00Z"
}
```

### Share link fields

| Field | Type | Description |
|---|---|---|
| `shareLinkId` | number | Share link ID |
| `documentId` | number | Shared document ID |
| `token` | string | Random share token |
| `accessPath` | string | Backend path for opening the shared document |
| `enabled` | boolean | Whether the share link is currently active |
| `expiresAt` | string / null | Expiration time; currently may be `null` |
| `createdAt` | string | Share link creation time |

---

## 3.12.2. Document user share response object

User share APIs return a `DocumentShareResponse` object:

```json
{
  "documentShareId": 1,
  "documentId": 1,
  "ownerId": 1,
  "sharedWithUserId": 2,
  "sharedWithEmail": "friend@example.com",
  "sharedWithName": "Friend User",
  "createdAt": "2026-06-19T10:30:00Z"
}
```

### User share fields

| Field | Type | Description |
|---|---|---|
| `documentShareId` | number | Document share record ID |
| `documentId` | number | Shared document ID |
| `ownerId` | number | Owner user ID |
| `sharedWithUserId` | number | User ID of the receiver |
| `sharedWithEmail` | string | Email of the receiver |
| `sharedWithName` | string | Full name of the receiver |
| `createdAt` | string | Share creation time |

---

## 3.12.3. Create document share link

Create or reuse an active share link for an owned active document.

### Request

- Method: `POST`
- URL: `/api/documents/{documentId}/share-link`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Create document share link successfully",
  "data": {
    "shareLinkId": 1,
    "documentId": 1,
    "token": "c1a2b3token",
    "accessPath": "/api/documents/share-link/c1a2b3token",
    "enabled": true,
    "expiresAt": null,
    "createdAt": "2026-06-19T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document does not exist, does not belong to the user, or was soft-deleted |

---

## 3.12.4. Disable document share link

Disable the active share link of an owned active document.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}/share-link`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Disable document share link successfully",
  "data": {
    "shareLinkId": 1,
    "documentId": 1,
    "token": "c1a2b3token",
    "accessPath": "/api/documents/share-link/c1a2b3token",
    "enabled": false,
    "expiresAt": null,
    "createdAt": "2026-06-19T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:35:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document or active share link not found |

---

## 3.12.5. Get document by share link

Get shared document metadata using a share token.

### Request

- Method: `GET`
- URL: `/api/documents/share-link/{token}`
- Auth: Public, no JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `token` | string | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get shared document successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": null,
    "originalFileName": "shared-file.pdf",
    "s3Key": "documents/1/uuid-shared-file.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-19T10:00:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-19T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing token |
| `404` | `Resource not found` | Share link not found, disabled, expired, or document was soft-deleted |

---

## 3.12.6. Get share-link preview URL

Get a temporary pre-signed URL for previewing a document by share token.

### Request

- Method: `GET`
- URL: `/api/documents/share-link/{token}/preview-url`
- Auth: Public, no JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `token` | string | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get shared document preview URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-19T10:50:00Z",
    "fileName": "shared-file.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing token |
| `404` | `Resource not found` | Share link not found, disabled, expired, or document was soft-deleted |
| `500` | `Request failed` | S3 pre-signer is not configured |

---

## 3.12.7. Get share-link download URL

Get a temporary pre-signed URL for downloading a document by share token.

### Request

- Method: `GET`
- URL: `/api/documents/share-link/{token}/download-url`
- Auth: Public, no JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `token` | string | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get shared document download URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-19T10:50:00Z",
    "fileName": "shared-file.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing token |
| `404` | `Resource not found` | Share link not found, disabled, expired, or document was soft-deleted |
| `500` | `Request failed` | S3 pre-signer is not configured |

---

## 3.12.7.1. Save share-link document to Shared with me

Save a document opened through a valid public share token to the authenticated user's **Shared with me** list. This creates a document-share record; it does not copy the S3 object.

### Request

- Method: `POST`
- URL: `/api/documents/share-link/{token}/save`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `token` | string | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Save shared document successfully",
  "data": {
    "documentShareId": 10,
    "documentId": 2,
    "ownerId": 1,
    "sharedWithUserId": 3,
    "sharedWithEmail": "student@example.com",
    "sharedWithName": "Student",
    "createdAt": "2026-07-06T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-07-06T10:30:00Z"
}
```

Calling this endpoint again returns the existing share record. The document owner cannot save their own share link.

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `You already own this document` | The current user owns the shared document |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Shared document not found` | Token is missing, disabled, expired, or its document was deleted |

---

## 3.12.8. Share document with user

Share an owned active document with another user by email. The receiver must already be a friend.

### Request

- Method: `POST`
- URL: `/api/documents/{documentId}/shares/users`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "email": "friend@example.com"
}
```

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Request fields

| Field | Type | Required | Rule |
|---|---|---|---|
| `email` | string | Yes | Must not be blank, must be a valid email |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Share document with user successfully",
  "data": {
    "documentShareId": 1,
    "documentId": 1,
    "ownerId": 1,
    "sharedWithUserId": 2,
    "sharedWithEmail": "friend@example.com",
    "sharedWithName": "Friend User",
    "createdAt": "2026-06-19T10:45:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:45:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing or invalid email |
| `400` | `Validation failed` | Sharing with yourself, receiver is not a friend, or document already shared with this user |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document or receiver user not found |

---

## 3.12.9. Remove user share

Remove a direct document share from a user.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}/shares/users/{userId}`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |
| `userId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Remove document share successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-19T10:50:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document or document share not found |

---

## 3.12.10. Get documents shared with me

Get active documents directly shared with the authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/shared-with-me`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get documents shared with me successfully",
  "data": [
    {
      "documentId": 1,
      "userId": 1,
      "folderId": null,
      "originalFileName": "shared-file.pdf",
      "s3Key": "documents/1/uuid-shared-file.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": false,
      "isDeleted": false,
      "isStarred": false,
      "status": "READY",
      "uploadedAt": "2026-06-19T10:00:00Z",
      "deletedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-19T10:55:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 3.12.11. Get shared-with-me document detail

Get detail of an active document directly shared with the authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/shared-with-me/{documentId}`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get shared document detail successfully",
  "data": {
    "documentId": 1,
    "userId": 1,
    "folderId": null,
    "originalFileName": "shared-file.pdf",
    "s3Key": "documents/1/uuid-shared-file.pdf",
    "contentType": "application/pdf",
    "fileSize": 123456,
    "isPublic": false,
    "isDeleted": false,
    "isStarred": false,
    "status": "READY",
    "uploadedAt": "2026-06-19T10:00:00Z",
    "deletedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-19T10:55:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Shared document not found or was soft-deleted |

---

## 3.12.12. Get shared-with-me preview URL

Get a temporary pre-signed URL for previewing a document directly shared with the authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/shared-with-me/{documentId}/preview-url`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get shared document preview URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-19T11:05:00Z",
    "fileName": "shared-file.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:55:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Shared document not found or was soft-deleted |
| `500` | `Request failed` | S3 pre-signer is not configured |

---

## 3.12.13. Get shared-with-me download URL

Get a temporary pre-signed URL for downloading a document directly shared with the authenticated user.

### Request

- Method: `GET`
- URL: `/api/documents/shared-with-me/{documentId}/download-url`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `documentId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get shared document download URL successfully",
  "data": {
    "url": "https://s3-presigned-url",
    "expiresAt": "2026-06-19T11:05:00Z",
    "fileName": "shared-file.pdf",
    "contentType": "application/pdf"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:55:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Shared document not found or was soft-deleted |
| `500` | `Request failed` | S3 pre-signer is not configured |

---

## 4. Document Folder APIs

Document folder APIs allow users to organize documents into simple personal folders. A folder only has a name. Folders are private and require JWT.

Private folder APIs require:

```text
Authorization: Bearer <accessToken>
```

---

## 4.1. Document folder response object

Folder APIs return a `DocumentFolderResponse` object:

```json
{
  "folderId": 1,
  "userId": 1,
  "name": "Semester 1",
  "isStarred": false,
  "createdAt": "2026-06-15T10:30:00Z",
  "updatedAt": "2026-06-15T10:30:00Z"
}
```

### Folder fields

| Field       | Type    | Description                                |
| ----------- | ------- | ------------------------------------------ |
| `folderId`  | number  | Folder ID                                  |
| `userId`    | number  | Owner user ID                              |
| `name`      | string  | Folder name                                |
| `isStarred` | boolean | Whether the folder is starred by the owner |
| `createdAt` | string  | Folder creation time                       |
| `updatedAt` | string  | Last update time                           |

---

## 4.2. Create document folder

Create a new personal document folder.

### Request

- Method: `POST`
- URL: `/api/document-folders`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "name": "Semester 1"
}
```

### Request fields

| Field  | Type   | Required | Rule                                      |
| ------ | ------ | -------- | ----------------------------------------- |
| `name` | string | Yes      | Must not be blank, maximum 100 characters |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Create document folder successfully",
  "data": {
    "folderId": 1,
    "userId": 1,
    "name": "Semester 1",
    "createdAt": "2026-06-15T10:30:00Z",
    "updatedAt": "2026-06-15T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-15T10:30:00Z"
}
```

### Error cases

| Status | Message             | Reason                                          |
| ------ | ------------------- | ----------------------------------------------- |
| `400`  | `Validation failed` | Missing or invalid name                         |
| `400`  | `Validation failed` | Folder name already exists for the current user |
| `401`  | `Unauthorized`      | Missing or invalid JWT                          |

---

## 4.3. Get my document folders

Get all folders owned by the authenticated user.

### Request

- Method: `GET`
- URL: `/api/document-folders`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get my document folders successfully",
  "data": [
    {
      "folderId": 1,
      "userId": 1,
      "name": "Semester 1",
      "isStarred": false,
      "createdAt": "2026-06-15T10:30:00Z",
      "updatedAt": "2026-06-15T10:30:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-15T10:30:00Z"
}
```

### Error cases

| Status | Message        | Reason                 |
| ------ | -------------- | ---------------------- |
| `401`  | `Unauthorized` | Missing or invalid JWT |

---

## 4.3.1. Get starred document folders

Get starred folders owned by the authenticated user.

### Request

- Method: `GET`
- URL: `/api/document-folders/starred`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get starred document folders successfully",
  "data": [
    {
      "folderId": 1,
      "userId": 1,
      "name": "Semester 1",
      "isStarred": true,
      "createdAt": "2026-06-15T10:30:00Z",
      "updatedAt": "2026-06-15T10:30:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-15T10:30:00Z"
}
```

### Error cases

| Status | Message        | Reason                 |
| ------ | -------------- | ---------------------- |
| `401`  | `Unauthorized` | Missing or invalid JWT |

---

## 4.4. Update document folder

Rename a folder owned by the authenticated user.

### Request

- Method: `PATCH`
- URL: `/api/document-folders/{folderId}`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "name": "Semester 2"
}
```

### Path variables

| Name       | Type   | Required |
| ---------- | ------ | -------- |
| `folderId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Update document folder successfully",
  "data": {
    "folderId": 1,
    "userId": 1,
    "name": "Semester 2",
    "isStarred": false,
    "createdAt": "2026-06-15T10:30:00Z",
    "updatedAt": "2026-06-15T10:40:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-15T10:40:00Z"
}
```

### Error cases

| Status | Message              | Reason                                               |
| ------ | -------------------- | ---------------------------------------------------- |
| `400`  | `Validation failed`  | Missing or invalid name                              |
| `400`  | `Validation failed`  | Folder name already exists for the current user      |
| `401`  | `Unauthorized`       | Missing or invalid JWT                               |
| `404`  | `Resource not found` | Folder does not exist or does not belong to the user |

---

## 4.4.1. Update document folder starred

Star or unstar a folder owned by the authenticated user.

### Request

- Method: `PATCH`
- URL: `/api/document-folders/{folderId}/star?isStarred=true`
- Auth: JWT required

### Path variables

| Name       | Type   | Required |
| ---------- | ------ | -------- |
| `folderId` | number | Yes      |

### Query params

| Name        | Type    | Required | Example           |
| ----------- | ------- | -------- | ----------------- |
| `isStarred` | boolean | Yes      | `true` or `false` |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Update document folder starred successfully",
  "data": {
    "folderId": 1,
    "userId": 1,
    "name": "Semester 1",
    "isStarred": true,
    "createdAt": "2026-06-15T10:30:00Z",
    "updatedAt": "2026-06-15T10:40:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-15T10:40:00Z"
}
```

### Error cases

| Status | Message              | Reason                                               |
| ------ | -------------------- | ---------------------------------------------------- |
| `400`  | `Validation failed`  | Missing `isStarred`                                  |
| `401`  | `Unauthorized`       | Missing or invalid JWT                               |
| `404`  | `Resource not found` | Folder does not exist or does not belong to the user |

---

## 4.5. Delete document folder

Delete a folder owned by the authenticated user. Documents inside the folder are not deleted; their `folderId` is set to `null`.

### Request

- Method: `DELETE`
- URL: `/api/document-folders/{folderId}`
- Auth: JWT required

### Path variables

| Name       | Type   | Required |
| ---------- | ------ | -------- |
| `folderId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Delete document folder successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-15T10:45:00Z"
}
```

### Error cases

| Status | Message              | Reason                                               |
| ------ | -------------------- | ---------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                               |
| `404`  | `Resource not found` | Folder does not exist or does not belong to the user |

---

## 4.6. Get documents in folder

Get active documents inside an owned folder.

### Request

- Method: `GET`
- URL: `/api/document-folders/{folderId}/documents`
- Auth: JWT required

### Path variables

| Name       | Type   | Required |
| ---------- | ------ | -------- |
| `folderId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get document folder documents successfully",
  "data": [
    {
      "documentId": 1,
      "userId": 1,
      "folderId": 1,
      "originalFileName": "example.pdf",
      "s3Key": "documents/1/uuid-example.pdf",
      "contentType": "application/pdf",
      "fileSize": 123456,
      "isPublic": false,
      "isDeleted": false,
      "isStarred": false,
      "status": "READY",
      "uploadedAt": "2026-06-15T10:30:00Z",
      "deletedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-15T10:50:00Z"
}
```

### Error cases

| Status | Message              | Reason                                               |
| ------ | -------------------- | ---------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                               |
| `404`  | `Resource not found` | Folder does not exist or does not belong to the user |

---

## 5. Tag APIs

Tag APIs allow users to create custom document tags with custom colors and attach them to documents.

Private tag APIs require:

```text
Authorization: Bearer <accessToken>
```

---

## 5.1. Tag response object

Tag APIs return a `TagResponse` object:

```json
{
  "tagId": 1,
  "userId": 1,
  "name": "AI",
  "color": "#8B5CF6",
  "createdAt": "2026-06-14T10:30:00Z"
}
```

### Tag fields

| Field       | Type   | Description                      |
| ----------- | ------ | -------------------------------- |
| `tagId`     | number | Tag ID                           |
| `userId`    | number | Owner user ID                    |
| `name`      | string | Tag name                         |
| `color`     | string | HEX color, for example `#8B5CF6` |
| `createdAt` | string | Tag creation time                |

---

## 5.2. Create tag

Create a new personal tag for the authenticated user.

### Request

- Method: `POST`
- URL: `/api/tags`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "name": "AI",
  "color": "#8B5CF6"
}
```

### Request fields

| Field   | Type   | Required | Rule                                                       |
| ------- | ------ | -------- | ---------------------------------------------------------- |
| `name`  | string | Yes      | Must not be blank, maximum 100 characters                  |
| `color` | string | Yes      | Must be a valid HEX color, for example `#8B5CF6` or `#FFF` |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Create tag successfully",
  "data": {
    "tagId": 1,
    "userId": 1,
    "name": "AI",
    "color": "#8B5CF6",
    "createdAt": "2026-06-14T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message             | Reason                                       |
| ------ | ------------------- | -------------------------------------------- |
| `400`  | `Validation failed` | Missing/invalid name or color                |
| `400`  | `Validation failed` | Tag name already exists for the current user |
| `401`  | `Unauthorized`      | Missing or invalid JWT                       |

---

## 5.3. Get my tags

Get all tags owned by the authenticated user.

### Request

- Method: `GET`
- URL: `/api/tags`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get my tags successfully",
  "data": [
    {
      "tagId": 1,
      "userId": 1,
      "name": "AI",
      "color": "#8B5CF6",
      "createdAt": "2026-06-14T10:30:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-14T10:30:00Z"
}
```

### Error cases

| Status | Message        | Reason                 |
| ------ | -------------- | ---------------------- |
| `401`  | `Unauthorized` | Missing or invalid JWT |

---

## 5.4. Update tag

Update a tag owned by the authenticated user.

### Request

- Method: `PATCH`
- URL: `/api/tags/{tagId}`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "name": "Machine Learning",
  "color": "#22C55E"
}
```

### Path variables

| Name    | Type   | Required |
| ------- | ------ | -------- |
| `tagId` | number | Yes      |

### Request fields

| Field   | Type   | Required | Rule                                      |
| ------- | ------ | -------- | ----------------------------------------- |
| `name`  | string | Yes      | Must not be blank, maximum 100 characters |
| `color` | string | Yes      | Must be a valid HEX color                 |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Update tag successfully",
  "data": {
    "tagId": 1,
    "userId": 1,
    "name": "Machine Learning",
    "color": "#22C55E",
    "createdAt": "2026-06-14T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:35:00Z"
}
```

### Error cases

| Status | Message              | Reason                                            |
| ------ | -------------------- | ------------------------------------------------- |
| `400`  | `Validation failed`  | Missing/invalid name or color                     |
| `400`  | `Validation failed`  | Tag name already exists for the current user      |
| `401`  | `Unauthorized`       | Missing or invalid JWT                            |
| `404`  | `Resource not found` | Tag does not exist or does not belong to the user |

---

## 5.5. Delete tag

Delete a tag owned by the authenticated user. Existing document-tag links for that tag are also removed.

### Request

- Method: `DELETE`
- URL: `/api/tags/{tagId}`
- Auth: JWT required

### Path variables

| Name    | Type   | Required |
| ------- | ------ | -------- |
| `tagId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Delete tag successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-14T10:40:00Z"
}
```

### Error cases

| Status | Message              | Reason                                            |
| ------ | -------------------- | ------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                            |
| `404`  | `Resource not found` | Tag does not exist or does not belong to the user |

---

## 5.6. Add tag to document

Attach an owned tag to an owned active document.

### Request

- Method: `POST`
- URL: `/api/documents/{documentId}/tags/{tagId}`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |
| `tagId`      | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Add tag to document successfully",
  "data": {
    "tagId": 1,
    "userId": 1,
    "name": "AI",
    "color": "#8B5CF6",
    "createdAt": "2026-06-14T10:30:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-14T10:45:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                     |
| ------ | -------------------- | ---------------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                     |
| `404`  | `Resource not found` | Document/tag does not exist or does not belong to the user |

---

## 5.7. Remove tag from document

Detach a tag from an owned active document.

### Request

- Method: `DELETE`
- URL: `/api/documents/{documentId}/tags/{tagId}`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |
| `tagId`      | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Remove tag from document successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-14T10:50:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                     |
| ------ | -------------------- | ---------------------------------------------------------- |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                     |
| `404`  | `Resource not found` | Document/tag does not exist or does not belong to the user |

---

## 5.8. Get document tags

Get tags attached to an owned active document.

### Request

- Method: `GET`
- URL: `/api/documents/{documentId}/tags`
- Auth: JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get document tags successfully",
  "data": [
    {
      "tagId": 1,
      "userId": 1,
      "name": "AI",
      "color": "#8B5CF6",
      "createdAt": "2026-06-14T10:30:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-14T10:55:00Z"
}
```

### Error cases

| Status | Message              | Reason                                                 |
| ------ | -------------------- | ------------------------------------------------------ |
| `401`  | `Unauthorized`       | Missing or invalid JWT                                 |
| `404`  | `Resource not found` | Document does not exist or does not belong to the user |

---

## 5.9. Get public document tags

Get tags attached to a public document.

### Request

- Method: `GET`
- URL: `/api/documents/public/{documentId}/tags`
- Auth: Public, no JWT required

### Path variables

| Name         | Type   | Required |
| ------------ | ------ | -------- |
| `documentId` | number | Yes      |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get public document tags successfully",
  "data": [
    {
      "tagId": 1,
      "userId": 1,
      "name": "AI",
      "color": "#8B5CF6",
      "createdAt": "2026-06-14T10:30:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-14T11:00:00Z"
}
```

### Error cases

| Status | Message              | Reason                                          |
| ------ | -------------------- | ----------------------------------------------- |
| `404`  | `Resource not found` | Public document does not exist or is not public |

---

## 6. AI Chat APIs

AI chat APIs answer questions from the selected document content using the indexed document chunks.

Private chat APIs require:

```text
Authorization: Bearer <accessToken>
```

Current scope:

- Chat supports one selected document through `/api/chat/ask`.
- Chat supports multi-document / storage-based retrieval through `/api/chat/ask-multi`.
- Documents used for chat must be accessible by the authenticated user.
- Documents used for chat must have status `READY`.
- Retrieval is scoped to the selected document(s) or accessible storage scope.
- The AI is instructed to answer only from the retrieved document context.
- Persistent chat sessions, messages, and RAG sources are supported through `/api/chat/sessions`.
- For each new session message, only the latest five completed messages are used as conversational memory.

---

## 6.1. Ask a question about a document

Ask the AI a question using one selected document as context.

### Request

- Method: `POST`
- URL: `/api/chat/ask`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "documentId": 1,
  "question": "What is the main idea of this document?",
  "model": "gpt-5.6-luna",
  "temperature": 0.2
}
```

### Request fields

| Field | Type | Required | Rule |
|---|---|---|---|
| `documentId` | number | Yes | Must point to an accessible document |
| `question` | string | Yes | Must not be blank |
| `model` | string / null | No | One of the three supported chat models; backend default if omitted |
| `temperature` | number / null | No | AI creativity from `0.0` to `1.0`; backend default `0.2` if omitted |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Ask document successfully",
  "data": {
    "documentId": 1,
    "answer": "The main idea of the document is ...",
    "model": "gpt-5.6-luna",
    "temperature": 0.2,
    "sources": [
      {
        "chunkId": 10,
        "chunkIndex": 0,
        "pageNumber": 1,
        "score": 0.8732
      }
    ]
  },
  "errors": null,
  "timestamp": "2026-06-16T10:30:00Z"
}
```

### Response fields

| Field | Type | Description |
|---|---|---|
| `documentId` | number | The selected document ID |
| `answer` | string | AI answer grounded by retrieved chunks |
| `model` | string | Model actually used by the backend |
| `temperature` | number | Temperature actually used by the backend |
| `sources` | array | Retrieved chunks used as context |
| `sources[].chunkId` | number | Source chunk ID |
| `sources[].chunkIndex` | number | Chunk order in the document |
| `sources[].pageNumber` | number / null | Source page number if available |
| `sources[].score` | number | Cosine similarity score |

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing documentId or blank question |
| `400` | `Validation failed` | Unsupported model or temperature outside `0.0`–`1.0` |
| `400` | `Validation failed` | Document is not `READY` |
| `400` | `Validation failed` | Document has no indexed content for chat |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Document does not exist or is not accessible |
| `503` | `Spring AI chat model is not configured...` | Missing Spring AI chat configuration |
| `503` | `AI service is unavailable` | OpenAI/Spring AI call failed |

### Frontend usage

1. Let the user select a document.
2. Only enable chat when the selected document has status `READY`.
3. Send the selected `documentId`, question, optional model, and optional temperature to `/api/chat/ask`.
4. Render `data.answer`.
5. Optionally show `data.sources` for debugging or future citation UI.

Important:

- Do not send the full document content from the frontend.
- Do not call OpenAI directly from the frontend.
- The backend handles embedding, vector search, prompt building, and AI calling.

---

## 6.2. Ask a question using multiple documents or user storage

Ask the AI a question using multiple selected documents, or using the user's accessible document storage.

### Request

- Method: `POST`
- URL: `/api/chat/ask-multi`
- Auth: JWT required
- Content-Type: `application/json`

### Case 1: selected documents

Use this when the user manually selects one or more documents.
Every requested document must exist, be owned by the user or public, not be deleted, and have status `READY`.
The backend searches only the selected document IDs.

```json
{
  "mode": "SelectedDocuments",
  "selectedDocumentIds": [1, 2, 3],
  "folderId": null,
  "question": "Summarize the common topic across these documents.",
  "model": "gpt-5.6-luna",
  "temperature": 0.2
}
```

### Case 2: user storage

Use this when the user does not manually select documents and wants to search only their own `READY` documents.
Public Community documents are not included.

```json
{
  "mode": "UserStorage",
  "selectedDocumentIds": null,
  "folderId": null,
  "question": "Which of my documents mention machine learning?",
  "model": "gpt-5.6-terra",
  "temperature": 0.2
}
```

### Retrieval scope

| Mode | Documents searched |
|---|---|
| `SelectedDocuments` | Only all requested accessible `READY` documents |
| `UserStorage` | Current user's `READY` documents only |

When `folderId` is supplied:

- The folder must belong to the authenticated user.
- Only the user's `READY` documents in that folder are searched.
- Documents with `UPLOADED`, `PARSING`, `INDEXING`, `FAILED`, or deleted status are excluded.
- If no eligible documents or chunks remain, the API returns `200 OK` with a no-context answer and does not call OpenAI.

> `POST /api/chat/ask-multi` currently has no `useGeneralKnowledge` request field. The public Community option exists only when creating a persistent chat session through `POST /api/chat/sessions`.

### Request fields

| Field | Type | Required | Rule |
|---|---|---|---|
| `mode` | string | Yes | Must be `SelectedDocuments` or `UserStorage` |
| `selectedDocumentIds` | array / null | Required for `SelectedDocuments` | List of accessible `READY` document IDs |
| `folderId` | number / null | No | Optional owned folder filter for the user's document scope in `UserStorage` |
| `question` | string | Yes | Must not be blank |
| `model` | string / null | No | One of the three supported chat models; backend default if omitted |
| `temperature` | number / null | No | AI creativity from `0.0` to `1.0`; backend default `0.2` if omitted |

### Supported AI generation options

> **Changed 2026-08-11 (AI-assisted):** the AI provider moved from Google Gemini to
> OpenAI. The accepted `model` values below are a **breaking change** for any frontend
> that hardcoded the old Gemini names.

Supported `model` values:

```text
gpt-5.6-luna
gpt-5.6-terra
gpt-5.6-sol
```

The frontend may display these as:

```text
GPT-5.6 Luna    (cost optimised - backend default)
GPT-5.6 Terra   (balanced)
GPT-5.6 Sol     (most capable)
```

> ### ⚠️ `temperature` currently has NO EFFECT (as of 2026-08-11)
>
> The GPT-5.6 model family does not allow `temperature` to be set. Sending it to the
> OpenAI API returns:
>
> ```
> Unsupported value: 'temperature' does not support 0.2 with this model.
> Only the default (1) value is supported.
> ```
>
> The backend therefore **no longer forwards `temperature` to OpenAI**
> (`AiChatClientServiceImpl`). The field is still accepted in the request, still
> validated against `0.0`–`1.0`, and still persisted on the chat session — but it does
> not change the generated answer.
>
> **For frontend:** you may keep the control in the UI, but do not promise users that it
> changes anything. Consider hiding or disabling it until the backend moves to a model
> family that supports the parameter (for example the `gpt-4.1` family), at which point
> re-enabling is a one-line change on the backend.

Rules:

- `model` and `temperature` are optional.
- If omitted, the backend uses `OPENAI_CHAT_MODEL` and `OPENAI_CHAT_TEMPERATURE`.
- The backend defaults are `gpt-5.6-luna` and `0.2`.
- `temperature` is accepted and stored but **ignored** — see the warning above.
- Model affects answer generation only. It does not change parsing, embeddings, cosine similarity, retrieval scope, or access control.
- The OpenAI API key is configured only on the backend and must never be sent by the frontend.

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Multi-document chat processed",
  "data": {
    "answer": "The selected documents mainly discuss ...",
    "mode": "SELECTED_DOCUMENTS",
    "model": "gpt-5.6-luna",
    "temperature": 0.2,
    "usedDocumentIds": [1, 2],
    "sources": [
      {
        "documentId": 1,
        "documentName": "example.pdf",
        "chunkId": 15,
        "contentPreview": "Relevant extracted content...",
        "similarityScore": 0.91
      }
    ]
  },
  "errors": null,
  "timestamp": "2026-06-26T10:30:00Z"
}
```

### Response fields

| Field | Type | Description |
|---|---|---|
| `answer` | string | AI answer grounded by retrieved chunks |
| `mode` | string | Resolved backend mode, for example `SELECTED_DOCUMENTS` or `USER_STORAGE` |
| `model` | string | Model actually used by the backend |
| `temperature` | number | Temperature actually used by the backend |
| `usedDocumentIds` | array | Document IDs whose chunks were used as context |
| `sources` | array | Retrieved chunk citations with document, preview, and cosine-similarity score |

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing/invalid mode or blank question |
| `400` | `Validation failed` | Unsupported model or temperature outside `0.0`–`1.0` |
| `400` | `Validation failed` | `selectedDocumentIds` is missing for `SelectedDocuments` mode |
| `400` | `Validation failed` | One or more selected documents do not exist, are deleted, are not `READY`, or are not accessible |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | The supplied `folderId` does not belong to the authenticated user |
| `503` | `AI service is unavailable` | OpenAI/Spring AI call failed |

### Frontend usage

1. If the user selects documents, call `/api/chat/ask-multi` with `mode = "SelectedDocuments"` and `selectedDocumentIds`.
2. If the user does not select documents, call `/api/chat/ask-multi` with `mode = "UserStorage"`.
3. Send one of the supported model IDs and a temperature between `0.0` and `1.0`, or omit them to use backend defaults.
4. Render `data.answer`, `data.sources`, and optionally `data.usedDocumentIds`.

Important:

- Do not send file content from the frontend.
- Only send IDs, mode, optional folder filter, and the user's question.
- The backend handles embedding, retrieval, prompt building, and AI calling.
- In `SelectedDocuments` mode, do not include documents that are still processing or have status `FAILED`.

---

## 6.3. Persistent chat sessions and history

Session APIs store the complete chat history in the database. When generating a new answer, the backend uses at most the latest five completed messages as Spring AI conversational memory. Document retrieval remains restricted by the session's saved RAG scope.

Private session APIs require:

```text
Authorization: Bearer <accessToken>
```

### 6.3.1. Create chat session

- Method: `POST`
- URL: `/api/chat/sessions`
- Auth: JWT required
- Content-Type: `application/json`

Selected documents example:

```json
{
  "title": "Spring AI revision",
  "mode": "SelectedDocuments",
  "selectedDocumentIds": [1, 2],
  "folderId": null,
  "useGeneralKnowledge": null,
  "model": "gpt-5.6-luna",
  "temperature": 0.2
}
```

User storage example:

```json
{
  "title": "My study assistant",
  "mode": "UserStorage",
  "selectedDocumentIds": null,
  "folderId": 3,
  "useGeneralKnowledge": true,
  "model": "gpt-5.6-terra",
  "temperature": 0.3
}
```

Rules:

- `title` is optional and defaults to `New chat`; maximum 200 characters.
- `SelectedDocuments` requires all selected documents to be accessible and `READY`.
- `folderId` is not accepted in `SelectedDocuments`.
- `selectedDocumentIds` is not accepted in `UserStorage`.
- Temperature, retrieval mode, and document scope are fixed when the session is created. The model can be changed when sending a message and becomes the session default.

Success data:

```json
{
  "sessionId": 10,
  "title": "Spring AI revision",
  "mode": "SELECTED_DOCUMENTS",
  "folderId": null,
  "policy": "DOCUMENTS_ONLY",
  "model": "gpt-5.6-luna",
  "temperature": 0.2,
  "selectedDocumentIds": [1, 2],
  "createdAt": "2026-06-28T10:30:00Z",
  "updatedAt": "2026-06-28T10:30:00Z"
}
```

### 6.3.2. Get my chat sessions

- Method: `GET`
- URL: `/api/chat/sessions`
- Auth: JWT required

Returns active sessions owned by the authenticated user, ordered by most recently updated.

### 6.3.3. Rename chat session

- Method: `PATCH`
- URL: `/api/chat/sessions/{sessionId}`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "title": "Updated study session"
}
```

The title must not be blank and must not exceed 200 characters.

### 6.3.4. Delete chat session

- Method: `DELETE`
- URL: `/api/chat/sessions/{sessionId}`
- Auth: JWT required

The session is soft-deleted. Existing messages remain in the database but the session is no longer accessible through user APIs.

### 6.3.5. Get session messages

- Method: `GET`
- URL: `/api/chat/sessions/{sessionId}/messages?page=0&size=50`
- Auth: JWT required

Query parameters:

| Field | Type | Required | Rule |
|---|---|---|---|
| `page` | number | No | Default `0`; must be at least `0` |
| `size` | number | No | Default `50`; must be between `1` and `100` |

Success data:

```json
{
  "messages": [
    {
      "messageId": 100,
      "role": "USER",
      "content": "What is dependency injection?",
      "status": "COMPLETED",
      "createdAt": "2026-06-28T10:31:00Z",
      "sources": []
    },
    {
      "messageId": 101,
      "role": "ASSISTANT",
      "content": "According to the selected documents...",
      "status": "COMPLETED",
      "createdAt": "2026-06-28T10:31:03Z",
      "sources": [
        {
          "documentId": 1,
          "chunkId": 25,
          "pageNumber": 4,
          "score": 0.9123
        }
      ]
    }
  ],
  "page": 0,
  "size": 50,
  "totalElements": 2,
  "totalPages": 1
}
```

### 6.3.6. Send message to session

- Method: `POST`
- URL: `/api/chat/sessions/{sessionId}/messages`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "question": "Can you explain that more simply?",
  "model": "gpt-5.6-terra",
  "useGeneralKnowledge": true,
  "temperature": 0.7
}
```

`model` is optional. When provided, it must be one of the supported chat model IDs listed in section 6.2. The selected model is used for this message and saved as the session default for subsequent messages. When omitted, the session's current model is used.

`useGeneralKnowledge` is optional and only supported for `UserStorage` sessions. When provided, it updates whether the session retrieves from private files only or private files plus public Community documents. When omitted, the session's current knowledge policy is used.

`temperature` is optional and must be between `0.0` and `1.0`. When provided, it is used for this message and saved as the session default for subsequent messages. When omitted, the session's current temperature is used.

Flow:

1. Verify the session belongs to the authenticated user.
2. Validate the requested model, knowledge scope, and temperature, if provided, and update the session configuration.
3. Load at most the five latest completed messages as Spring AI chat memory.
4. Save the current user message.
5. Resolve the session's document scope and retrieve relevant chunks.
6. Build a grounded prompt containing memory, document context, and the current question.
7. Call the selected OpenAI model and the session's configured temperature.
8. Save the assistant message and RAG source chunks.

The complete message history remains in the database, but only five recent completed messages are sent as conversational memory. Full private document context is not stored as a chat message.

Error cases:

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Blank question, unsupported model, invalid scope/temperature, or invalid session configuration |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Session does not exist, was deleted, or belongs to another user |
| `503` | `AI service is unavailable` | OpenAI/Spring AI call failed; the failed assistant attempt is recorded with `FAILED` status |

---

## 7. Friend APIs

Friend APIs allow authenticated users to send, manage, and list friendship relationships.

Private friend APIs require:

```text
Authorization: Bearer <accessToken>
```

---

## 7.1. Friend request response object

Friend request APIs return a `FriendRequestResponse` object:

```json
{
  "requestId": 1,
  "senderId": 1,
  "senderName": "Long Nguyen",
  "senderEmail": "long@example.com",
  "receiverId": 2,
  "receiverName": "Teammate",
  "receiverEmail": "teammate@example.com",
  "status": "PENDING",
  "createdAt": "2026-06-19T10:30:00Z",
  "respondedAt": null
}
```

### Friend request fields

| Field | Type | Description |
|---|---|---|
| `requestId` | number | Friend request ID |
| `senderId` | number | User ID of the sender |
| `senderName` | string | Full name of the sender |
| `senderEmail` | string | Email of the sender |
| `receiverId` | number | User ID of the receiver |
| `receiverName` | string | Full name of the receiver |
| `receiverEmail` | string | Email of the receiver |
| `status` | string | `PENDING`, `ACCEPTED`, `REJECTED`, or `CANCELLED` |
| `createdAt` | string | Request creation time |
| `respondedAt` | string / null | Time when the request was accepted, rejected, or cancelled |

---

## 7.2. Friend response object

Friend list APIs return a `FriendResponse` object:

```json
{
  "friendshipId": 1,
  "userId": 2,
  "fullName": "Teammate",
  "email": "teammate@example.com",
  "createdAt": "2026-06-19T10:30:00Z"
}
```

### Friend fields

| Field | Type | Description |
|---|---|---|
| `friendshipId` | number | Friendship ID |
| `userId` | number | Friend user ID |
| `fullName` | string | Friend full name |
| `email` | string | Friend email |
| `createdAt` | string | Friendship creation time |

---

## 7.3. Send friend request

Send a friend request to another user by email.

### Request

- Method: `POST`
- URL: `/api/friends/request`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "email": "teammate@example.com"
}
```

### Request fields

| Field | Type | Required | Rule |
|---|---|---|---|
| `email` | string | Yes | Must not be blank, must be a valid email |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Send friend request successfully",
  "data": {
    "requestId": 1,
    "senderId": 1,
    "senderName": "Long Nguyen",
    "senderEmail": "long@example.com",
    "receiverId": 2,
    "receiverName": "Teammate",
    "receiverEmail": "teammate@example.com",
    "status": "PENDING",
    "createdAt": "2026-06-19T10:30:00Z",
    "respondedAt": null
  },
  "errors": null,
  "timestamp": "2026-06-19T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing or invalid email |
| `400` | `Validation failed` | Sending request to yourself, already friends, duplicate pending request, or reverse pending request exists |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Receiver or sender not found |

---

## 7.4. Get incoming friend requests

Get pending friend requests received by the authenticated user.

### Request

- Method: `GET`
- URL: `/api/friends/requests/incoming`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get incoming friend requests successfully",
  "data": [
    {
      "requestId": 1,
      "senderId": 2,
      "senderName": "Teammate",
      "senderEmail": "teammate@example.com",
      "receiverId": 1,
      "receiverName": "Long Nguyen",
      "receiverEmail": "long@example.com",
      "status": "PENDING",
      "createdAt": "2026-06-19T10:30:00Z",
      "respondedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-19T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 7.5. Get outgoing friend requests

Get pending friend requests sent by the authenticated user.

### Request

- Method: `GET`
- URL: `/api/friends/requests/outgoing`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get outgoing friend requests successfully",
  "data": [
    {
      "requestId": 1,
      "senderId": 1,
      "senderName": "Long Nguyen",
      "senderEmail": "long@example.com",
      "receiverId": 2,
      "receiverName": "Teammate",
      "receiverEmail": "teammate@example.com",
      "status": "PENDING",
      "createdAt": "2026-06-19T10:30:00Z",
      "respondedAt": null
    }
  ],
  "errors": null,
  "timestamp": "2026-06-19T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 7.6. Accept friend request

Accept a pending friend request received by the authenticated user.

### Request

- Method: `POST`
- URL: `/api/friends/requests/{requestId}/accept`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `requestId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Accept friend request successfully",
  "data": {
    "requestId": 1,
    "senderId": 2,
    "senderName": "Teammate",
    "senderEmail": "teammate@example.com",
    "receiverId": 1,
    "receiverName": "Long Nguyen",
    "receiverEmail": "long@example.com",
    "status": "ACCEPTED",
    "createdAt": "2026-06-19T10:30:00Z",
    "respondedAt": "2026-06-19T10:35:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:35:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | User is not allowed to respond, request is not pending, or users are already friends |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Friend request or user not found |

---

## 7.7. Reject friend request

Reject a pending friend request received by the authenticated user.

### Request

- Method: `POST`
- URL: `/api/friends/requests/{requestId}/reject`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `requestId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Reject friend request successfully",
  "data": {
    "requestId": 1,
    "senderId": 2,
    "senderName": "Teammate",
    "senderEmail": "teammate@example.com",
    "receiverId": 1,
    "receiverName": "Long Nguyen",
    "receiverEmail": "long@example.com",
    "status": "REJECTED",
    "createdAt": "2026-06-19T10:30:00Z",
    "respondedAt": "2026-06-19T10:35:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:35:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | User is not allowed to respond or request is not pending |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Friend request not found |

---

## 7.8. Cancel friend request

Cancel a pending friend request sent by the authenticated user.

### Request

- Method: `DELETE`
- URL: `/api/friends/requests/{requestId}/cancel`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `requestId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Cancel friend request successfully",
  "data": {
    "requestId": 1,
    "senderId": 1,
    "senderName": "Long Nguyen",
    "senderEmail": "long@example.com",
    "receiverId": 2,
    "receiverName": "Teammate",
    "receiverEmail": "teammate@example.com",
    "status": "CANCELLED",
    "createdAt": "2026-06-19T10:30:00Z",
    "respondedAt": "2026-06-19T10:35:00Z"
  },
  "errors": null,
  "timestamp": "2026-06-19T10:35:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | User is not allowed to cancel or request is not pending |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Friend request not found |

---

## 7.9. Unfriend

Remove an existing friendship.

### Request

- Method: `DELETE`
- URL: `/api/friends/{friendId}`
- Auth: JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `friendId` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Unfriend successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-06-19T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | User tries to unfriend themselves |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Resource not found` | Friendship not found |

---

## 7.10. Get friends

Get the authenticated user's friend list.

### Request

- Method: `GET`
- URL: `/api/friends`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get friends successfully",
  "data": [
    {
      "friendshipId": 1,
      "userId": 2,
      "fullName": "Teammate",
      "email": "teammate@example.com",
      "createdAt": "2026-06-19T10:30:00Z"
    }
  ],
  "errors": null,
  "timestamp": "2026-06-19T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 8. Subscription Plan APIs

Subscription Plan APIs define the available plans and their upload, storage, video, multi-document chat, and monthly token limits.

Access rules:

- `GET /api/subscription-plans` and `GET /api/subscription-plans/{id}` are public.
- Creating, updating, and deleting plans require an authenticated user with the `ADMIN` role.
- Plan names are trimmed and compared without case sensitivity.
- Only one active plan may use a given name.
- After a plan is soft-deleted, its name may be reused by a new plan.
- The `FREE` plan must have price `0` and cannot be renamed or deleted.
- An active `FREE` plan must be configured before new users complete account activation.

---

## 8.1. Subscription plan response object

```json
{
  "id": 1,
  "name": "PLUS",
  "price": 99000,
  "durationDays": 30,
  "description": "Plan for advanced study features",
  "storageLimitGb": 10,
  "allowedFormats": "pdf,doc,docx,pptx,xls,xlsx,png,mp4",
  "maxUploadSizeMb": 50,
  "multipleDocuments": true,
  "videoUpload": true,
  "monthlyTokenLimit": 100000,
  "active": true
}
```

### Subscription plan fields

| Field | Type | Description |
|---|---|---|
| `id` | number | Subscription plan ID |
| `name` | string | Unique name among active plans |
| `price` | number | Plan price; greater than or equal to `0` |
| `durationDays` | number | Subscription duration in days |
| `description` | string / null | Plan description |
| `storageLimitGb` | number | Total storage limit in GB |
| `allowedFormats` | string | Comma-separated supported file formats |
| `maxUploadSizeMb` | number | Maximum size of one uploaded file in MB |
| `multipleDocuments` | boolean | Whether multi-document chat is enabled |
| `videoUpload` | boolean | Whether video upload is enabled |
| `monthlyTokenLimit` | number | Monthly AI token limit; may be `0` |
| `active` | boolean | Whether the plan is currently available |

---

## 8.2. Create subscription plan

Create a new active subscription plan.

### Request

- Method: `POST`
- URL: `/api/subscription-plans`
- Auth: JWT with `ADMIN` role required
- Content-Type: `application/json`

```json
{
  "name": "PLUS",
  "price": 99000,
  "durationDays": 30,
  "description": "Plan for advanced study features",
  "storageLimitGb": 10,
  "allowedFormats": "pdf,doc,docx,pptx,xls,xlsx,png,mp4",
  "maxUploadSizeMb": 50,
  "multipleDocuments": true,
  "videoUpload": true,
  "monthlyTokenLimit": 100000
}
```

### Request fields

| Field | Type | Required | Rule |
|---|---|---|---|
| `name` | string | Yes | Not blank, maximum 100 characters |
| `price` | number | Yes | Greater than or equal to `0` |
| `durationDays` | number | Yes | Greater than `0` |
| `description` | string | No | Maximum 2000 characters |
| `storageLimitGb` | number | Yes | Greater than `0` |
| `allowedFormats` | string | Yes | Not blank, maximum 500 characters |
| `maxUploadSizeMb` | number | Yes | Greater than `0` |
| `multipleDocuments` | boolean | Yes | `true` or `false` |
| `videoUpload` | boolean | Yes | `true` or `false` |
| `monthlyTokenLimit` | number | Yes | Greater than or equal to `0` |

If `name` is `FREE`, `price` must be `0`.

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Create subscription plan successfully",
  "data": {
    "id": 1,
    "name": "PLUS",
    "price": 99000,
    "durationDays": 30,
    "description": "Plan for advanced study features",
    "storageLimitGb": 10,
    "allowedFormats": "pdf,doc,docx,pptx,xls,xlsx,png,mp4",
    "maxUploadSizeMb": 50,
    "multipleDocuments": true,
    "videoUpload": true,
    "monthlyTokenLimit": 100000,
    "active": true
  },
  "errors": null,
  "timestamp": "2026-07-03T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing or invalid plan data |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `403` | `Forbidden` | Authenticated user does not have the `ADMIN` role |
| `409` | `An active subscription plan with this name already exists` | Another active plan has the same name |

---

## 8.3. Get active subscription plans

Get all active plans for the pricing or subscription selection page.

### Request

- Method: `GET`
- URL: `/api/subscription-plans`
- Auth: Public, no JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get subscription plans successfully",
  "data": [
    {
      "id": 1,
      "name": "PLUS",
      "price": 99000,
      "durationDays": 30,
      "description": "Plan for advanced study features",
      "storageLimitGb": 10,
      "allowedFormats": "pdf,doc,docx,pptx,xls,xlsx,png,mp4",
      "maxUploadSizeMb": 50,
      "multipleDocuments": true,
      "videoUpload": true,
      "monthlyTokenLimit": 100000,
      "active": true
    }
  ],
  "errors": null,
  "timestamp": "2026-07-03T10:30:00Z"
}
```

---

## 8.4. Get subscription plan detail

Get one active subscription plan by ID.

### Request

- Method: `GET`
- URL: `/api/subscription-plans/{id}`
- Auth: Public, no JWT required

### Path variables

| Name | Type | Required |
|---|---|---|
| `id` | number | Yes |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get subscription plan successfully",
  "data": {
    "id": 1,
    "name": "PLUS",
    "price": 99000,
    "durationDays": 30,
    "description": "Plan for advanced study features",
    "storageLimitGb": 10,
    "allowedFormats": "pdf,doc,docx,pptx,xls,xlsx,png,mp4",
    "maxUploadSizeMb": 50,
    "multipleDocuments": true,
    "videoUpload": true,
    "monthlyTokenLimit": 100000,
    "active": true
  },
  "errors": null,
  "timestamp": "2026-07-03T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `404` | `Subscription plan not found` | Plan does not exist or was soft-deleted |

---

## 8.5. Update subscription plan

Replace the configurable fields of an active subscription plan.

### Request

- Method: `PUT`
- URL: `/api/subscription-plans/{id}`
- Auth: JWT with `ADMIN` role required
- Content-Type: `application/json`

The request body uses the same fields and validation rules as the create API. All fields except `description` are required.

```json
{
  "name": "PLUS",
  "price": 129000,
  "durationDays": 30,
  "description": "Updated PLUS plan",
  "storageLimitGb": 20,
  "allowedFormats": "pdf,doc,docx,pptx,xls,xlsx,png,mp4",
  "maxUploadSizeMb": 100,
  "multipleDocuments": true,
  "videoUpload": true,
  "monthlyTokenLimit": 200000
}
```

### Success response

Status: `200 OK`

The response uses the standard `ApiResponse` format and returns the updated subscription plan in `data`.

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing or invalid plan data |
| `400` | `FREE subscription plan cannot be renamed` | Attempted to rename the default FREE plan |
| `400` | `FREE subscription plan price must be 0` | FREE plan was assigned a non-zero price |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `403` | `Forbidden` | Authenticated user does not have the `ADMIN` role |
| `404` | `Subscription plan not found` | Plan does not exist or was soft-deleted |
| `409` | `An active subscription plan with this name already exists` | Another active plan has the same name |

---

## 8.6. Delete subscription plan

Soft-delete an active subscription plan. Existing payment and subscription history is preserved. The deleted plan's name can be reused for a new plan.

### Request

- Method: `DELETE`
- URL: `/api/subscription-plans/{id}`
- Auth: JWT with `ADMIN` role required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Delete subscription plan successfully",
  "data": null,
  "errors": null,
  "timestamp": "2026-07-03T10:30:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `403` | `Forbidden` | Authenticated user does not have the `ADMIN` role |
| `400` | `FREE subscription plan cannot be deleted` | Attempted to delete the default FREE plan |
| `404` | `Subscription plan not found` | Plan does not exist |
| `409` | `Subscription plan is already deleted` | Plan was previously soft-deleted |

---

## 9. Payment and Subscription APIs

Payment APIs create VNPay Sandbox transactions, process signed VNPay return callbacks, expose payment history, and return the user's active subscription.

Access rules:

- Purchase, history, and current-subscription APIs require JWT authentication.
- Revenue is available only to users with the `ADMIN` role.
- The VNPay return endpoint is public because VNPay redirects the browser to it.
- The backend verifies the callback signature, merchant code, transaction reference, and amount before updating payment data.

---

## 9.1. Purchase a subscription plan

Create a pending payment and generate a VNPay payment URL.

### Request

- Method: `POST`
- URL: `/api/payments/purchase`
- Auth: JWT required
- Content-Type: `application/json`

```json
{
  "planId": 1,
  "paymentMethod": "VNPAY"
}
```

### Request fields

| Field | Type | Required | Rule |
|---|---|---|---|
| `planId` | number | Yes | Must be greater than `0` and reference an active plan |
| `paymentMethod` | string | Yes | Currently only `VNPAY` is accepted |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Create payment successfully",
  "data": {
    "paymentId": 1,
    "transactionNo": "c0b1527ca7a847be9c32d19f45eb8d89",
    "paymentUrl": "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html?...",
    "status": "PENDING"
  },
  "errors": null,
  "timestamp": "2026-07-03T10:30:00Z"
}
```

The frontend should redirect the browser to `data.paymentUrl`.

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Validation failed` | Missing/invalid plan ID or payment method |
| `400` | `Subscription plan is no longer available` | Plan was soft-deleted |
| `400` | `FREE subscription plan does not require payment` | Attempted to purchase the default FREE plan |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `404` | `Subscription plan not found` | Plan does not exist |
| `500` | `VNPay configuration is incomplete` | Required VNPay environment variables are missing |

---

## 9.2. VNPay return callback

VNPay redirects the browser to this endpoint after the customer finishes or cancels payment. The frontend must not construct or call this URL manually.

### Request

- Method: `GET`
- URL: `/api/payments/vnpay-return`
- Auth: Public callback from VNPay
- Query parameters: Supplied by VNPay

Important callback parameters include:

| Parameter | Description |
|---|---|
| `vnp_TxnRef` | Backend transaction number |
| `vnp_Amount` | Paid amount multiplied by 100 |
| `vnp_TmnCode` | VNPay merchant code |
| `vnp_ResponseCode` | `00` means the payment response succeeded |
| `vnp_TransactionStatus` | `00` means the transaction succeeded |
| `vnp_SecureHash` | HMAC-SHA512 signature generated by VNPay |

### Redirect response

After validating and processing the callback, the backend returns:

```http
302 Found
Location: http://localhost:5173/payment-result?status=SUCCESS&transactionNo=c0b1527ca7a847be9c32d19f45eb8d89&alreadyProcessed=false
```

### Frontend redirect query parameters

| Parameter | Type | Description |
|---|---|---|
| `status` | string | Final backend payment status: `SUCCESS` or `FAILED` |
| `transactionNo` | string | Backend payment transaction number |
| `alreadyProcessed` | boolean | `true` if this valid callback was already processed |

The frontend result page is configured by `APP_FRONTEND_BASE_URL` and defaults to:

```text
http://localhost:5173/payment-result
```

When the same valid callback is received again, no duplicate subscription is created.

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Invalid VNPay signature` | Callback data was changed or signed with a different secret |
| `400` | `Invalid VNPay merchant code` | Merchant code does not match backend configuration |
| `400` | `VNPay payment amount does not match` | Callback amount differs from the stored payment |
| `400` | `Missing VNPay parameter: ...` | A required callback parameter is missing |
| `404` | `Payment not found` | `vnp_TxnRef` does not match a stored payment |

---

## 9.3. Get my payment history

### Request

- Method: `GET`
- URL: `/api/payments/history`
- Auth: JWT required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get payment history successfully",
  "data": [
    {
      "paymentId": 1,
      "planName": "PLUS",
      "amount": 99000,
      "paymentMethod": "VNPAY",
      "status": "SUCCESS",
      "paidAt": "2026-07-03T10:35:00"
    }
  ],
  "errors": null,
  "timestamp": "2026-07-03T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |

---

## 9.4. Get all payments

Get a paginated list of all payments in the system. Results are ordered by `createdAt` descending.

### Request

- Method: `GET`
- URL: `/api/payments?page=0&size=20&status=SUCCESS`
- Auth: JWT with `ADMIN` role required

### Query parameters

| Parameter | Type | Required | Default | Rule |
|---|---|---|---|---|
| `page` | number | No | `0` | Greater than or equal to `0` |
| `size` | number | No | `20` | Between `1` and `100` |
| `status` | string | No | All statuses | `PENDING`, `SUCCESS`, or `FAILED` |

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get all payments successfully",
  "data": {
    "payments": [
      {
        "paymentId": 1,
        "transactionNo": "c0b1527ca7a847be9c32d19f45eb8d89",
        "userId": 2,
        "userEmail": "user@example.com",
        "planId": 1,
        "planName": "PLUS",
        "amount": 99000,
        "paymentMethod": "VNPAY",
        "status": "SUCCESS",
        "responseCode": "00",
        "createdAt": "2026-07-03T10:30:00",
        "paidAt": "2026-07-03T10:35:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "errors": null,
  "timestamp": "2026-07-03T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Page must be greater than or equal to 0` | Negative page number |
| `400` | `Size must be between 1 and 100` | Invalid page size |
| `400` | `Payment status must be PENDING, SUCCESS, or FAILED` | Invalid status filter |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `403` | `Forbidden` | Authenticated user does not have the `ADMIN` role |

---

## 9.5. Get payment revenue

Return total successful payment revenue and transaction count.

### Request

- Method: `GET`
- URL: `/api/payments/revenue`
- Auth: JWT with `ADMIN` role required

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get payment revenue successfully",
  "data": {
    "totalRevenue": 99000,
    "totalTransactions": 1
  },
  "errors": null,
  "timestamp": "2026-07-03T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `403` | `Forbidden` | Authenticated user does not have the `ADMIN` role |

---

## 9.6. Get my active subscription

Two equivalent endpoints currently expose the authenticated user's active subscription:

```text
GET /api/payments/my-subscription
GET /api/subscriptions/me
```

- Auth: JWT required

If the user has no active subscription, the backend assigns the active `FREE` plan automatically. If a paid subscription has passed its `endDate`, it is marked `EXPIRED` and the user falls back to `FREE`.

### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get my subscription successfully",
  "data": {
    "subscriptionId": 1,
    "status": "ACTIVE",
    "startDate": "2026-07-03",
    "endDate": "2026-08-02",
    "planName": "PLUS",
    "price": 99000,
    "durationDays": 30,
    "storageLimitGb": 10,
    "allowedFormats": "pdf,doc,docx,pptx,xls,xlsx,png,mp4",
    "maxUploadSizeMb": 50,
    "multipleDocuments": true,
    "videoUpload": true,
    "monthlyTokenLimit": 100000
  },
  "errors": null,
  "timestamp": "2026-07-03T10:40:00Z"
}
```

### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `500` | `Active FREE subscription plan is not configured` | The system has no active FREE plan |

---

## 10. User Profile and Settings APIs

All endpoints in this section require:

```text
Authorization: Bearer <accessToken>
```

### 10.1. Get my profile

- Method: `GET`
- URL: `/api/users/me`

Success data:

```json
{
  "userId": 1,
  "fullName": "Long Nguyen",
  "email": "long@example.com",
  "bio": "Software engineering student",
  "avatarUrl": "https://temporary-s3-presigned-url",
  "role": "USER",
  "status": "ACTIVE",
  "createdAt": "2026-06-01T10:30:00",
  "updatedAt": "2026-07-06T10:30:00"
}
```

`avatarUrl` is `null` when no avatar exists. Otherwise, it is a temporary S3 pre-signed URL.

### 10.2. Update my profile

- Method: `PATCH`
- URL: `/api/users/me`
- Content-Type: `application/json`

```json
{
  "fullName": "Long Nguyen",
  "bio": "Updated biography"
}
```

| Field | Type | Required | Rule |
|---|---|---|---|
| `fullName` | string / null | No | Maximum 100 characters; blank values are ignored |
| `bio` | string / null | No | Maximum 500 characters; `null` keeps the current value, empty string clears it |

Returns the updated profile object with message `Update profile successfully`.

### 10.3. Change password

- Method: `PATCH`
- URL: `/api/users/me/password`
- Content-Type: `application/json`

```json
{
  "currentPassword": "old-password",
  "newPassword": "new-password",
  "confirmNewPassword": "new-password"
}
```

All fields are required and each password must contain at least 8 characters.

Success response data is `null` with message `Change password successfully`.

Error cases:

| Status | Message | Reason |
|---|---|---|
| `400` | `Current password is incorrect` | Current password does not match |
| `400` | `New password and confirm password do not match` | Confirmation mismatch |
| `400` | `Validation failed` | Missing or short password field |
| `401` | `Unauthorized` | Missing or invalid JWT |

### 10.4. Update avatar

- Method: `POST`
- URL: `/api/users/me/avatar`
- Content-Type: `multipart/form-data`

| Field | Type | Required | Rule |
|---|---|---|---|
| `file` | File | Yes | `png`, `jpg`, `jpeg`, or `webp`; MIME type must be `image/*`; maximum configured size is 5MB |

The new image is uploaded privately to S3, the profile is updated, and the previous avatar is deleted when possible. Returns the updated profile object.

Error cases:

| Status | Message | Reason |
|---|---|---|
| `400` | `Unsupported avatar file type: ...` | Invalid extension or MIME type |
| `400` | `Avatar file exceeds 5MB limit` | File exceeds configured avatar limit |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `503` | `S3 upload failed` | S3 operation failed |

### 10.5. Get my settings

- Method: `GET`
- URL: `/api/users/me/settings`

If settings do not exist, the backend creates these defaults:

```json
{
  "theme": "SYSTEM",
  "profileVisibility": "PUBLIC",
  "activityVisibility": "PUBLIC",
  "allowFriendRequests": true,
  "showOnlineStatus": true,
  "updatedAt": "2026-07-06T10:30:00Z"
}
```

### 10.6. Update my settings

- Method: `PATCH`
- URL: `/api/users/me/settings`
- Content-Type: `application/json`

All fields are optional; omitted fields keep their current values.

```json
{
  "theme": "DARK",
  "profileVisibility": "FRIENDS_ONLY",
  "activityVisibility": "PRIVATE",
  "allowFriendRequests": true,
  "showOnlineStatus": false
}
```

Allowed enum values:

```text
theme: LIGHT, DARK, SYSTEM
profileVisibility/activityVisibility: PUBLIC, FRIENDS_ONLY, PRIVATE
```

Returns the complete updated settings object.

---

## 11. Admin User Management APIs

All APIs in this section require an authenticated account with `ROLE_ADMIN`:

```text
Authorization: Bearer <adminAccessToken>
```

The API never returns password hashes, refresh tokens, avatar S3 keys, or other credentials.

### 11.1. Get users

Get a filtered and paginated user list, sorted by newest account first.

- Method: `GET`
- URL: `/api/admin/users`
- Auth: Admin JWT required

#### Query parameters

| Field | Type | Required | Rule |
|---|---|---|---|
| `keyword` | string / null | No | Case-insensitive partial match against full name or email |
| `status` | string / null | No | `PENDING`, `ACTIVE`, or `BLOCKED` |
| `role` | string / null | No | `USER` or `ADMIN` |
| `page` | integer | No | Default `0`; must be at least `0` |
| `size` | integer | No | Default `20`; must be between `1` and `100` |

Example:

```text
GET /api/admin/users?keyword=long&status=ACTIVE&role=USER&page=0&size=20
```

#### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get users successfully",
  "data": {
    "users": [
      {
        "userId": 2,
        "fullName": "Long Nguyen",
        "email": "long@example.com",
        "provider": "LOCAL",
        "role": "USER",
        "status": "ACTIVE",
        "verified": true,
        "bio": "Software engineering student",
        "createdAt": "2026-07-01T10:30:00",
        "updatedAt": "2026-07-06T10:30:00"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 1,
    "totalPages": 1
  },
  "errors": null,
  "timestamp": "2026-07-06T10:30:00Z"
}
```

#### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Status must be PENDING, ACTIVE, or BLOCKED` | Invalid status filter |
| `400` | `Role must be USER or ADMIN` | Invalid role filter |
| `400` | `Page must be greater than or equal to 0` | Invalid page |
| `400` | `Size must be between 1 and 100` | Invalid size |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `403` | `Forbidden` | Authenticated account is not an admin |

### 11.2. Get user detail

- Method: `GET`
- URL: `/api/admin/users/{userId}`
- Auth: Admin JWT required

#### Path variables

| Name | Type | Required |
|---|---|---|
| `userId` | number | Yes |

#### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Get user detail successfully",
  "data": {
    "userId": 2,
    "fullName": "Long Nguyen",
    "email": "long@example.com",
    "provider": "GOOGLE",
    "role": "USER",
    "status": "ACTIVE",
    "verified": true,
    "bio": null,
    "createdAt": "2026-07-01T10:30:00",
    "updatedAt": null
  },
  "errors": null,
  "timestamp": "2026-07-06T10:30:00Z"
}
```

#### Error cases

| Status | Message | Reason |
|---|---|---|
| `401` | `Unauthorized` | Missing or invalid JWT |
| `403` | `Forbidden` | Authenticated account is not an admin |
| `404` | `User not found` | User does not exist |

### 11.3. Block or unblock user

Change an account between `ACTIVE` and `BLOCKED`.

- Method: `PATCH`
- URL: `/api/admin/users/{userId}/status`
- Auth: Admin JWT required
- Content-Type: `application/json`

Block:

```json
{
  "status": "BLOCKED"
}
```

Unblock:

```json
{
  "status": "ACTIVE"
}
```

When an account is blocked:

- All refresh tokens belonging to that user are revoked.
- Existing JWTs no longer authenticate on subsequent requests.
- Local and Google login are rejected.

When an account is activated, `verified` is set to `true`. An admin cannot block their own account.

#### Success response

Status: `200 OK`

```json
{
  "success": true,
  "message": "Update user status successfully",
  "data": {
    "userId": 2,
    "fullName": "Long Nguyen",
    "email": "long@example.com",
    "provider": "LOCAL",
    "role": "USER",
    "status": "BLOCKED",
    "verified": true,
    "bio": null,
    "createdAt": "2026-07-01T10:30:00",
    "updatedAt": "2026-07-06T10:30:00"
  },
  "errors": null,
  "timestamp": "2026-07-06T10:30:00Z"
}
```

#### Error cases

| Status | Message | Reason |
|---|---|---|
| `400` | `Status must be ACTIVE or BLOCKED` | Missing or unsupported target status |
| `400` | `You cannot block your own account` | Admin attempts to block themselves |
| `401` | `Unauthorized` | Missing or invalid JWT |
| `403` | `Forbidden` | Authenticated account is not an admin |
| `404` | `User not found` | User does not exist |

---

## 12. Common HTTP status codes

| Status                      | Description                                          |
| --------------------------- | ---------------------------------------------------- |
| `200 OK`                    | Request succeeded                                    |
| `400 Bad Request`           | Missing or invalid request data                      |
| `401 Unauthorized`          | Missing, invalid, or expired JWT                     |
| `403 Forbidden`             | Authenticated but not allowed to access the resource |
| `404 Not Found`             | Resource not found                                   |
| `409 Conflict`              | Request conflicts with the current resource state    |
| `413 Payload Too Large`     | Uploaded file exceeds the size limit                 |
| `500 Internal Server Error` | Unexpected server error                              |
| `503 Service Unavailable`   | S3 or external service failure                       |

---

## 13. Frontend notes

- Private APIs do not require `userId`; the backend reads the current user from JWT.
- After login or Google login, store both `accessToken` and `refreshToken`.
- When refreshing tokens, always replace the old refresh token with the new one from the response.
- Header for private APIs:

```text
Authorization: Bearer <accessToken>
```

- File upload must use `multipart/form-data`, not raw JSON.
- Send `isPublic` as a boolean-like string in form-data/query:

```text
true
false
```

- `s3Key` is an internal object key for backend/S3 usage. The frontend should not use `s3Key` to access files directly.
- Use preview/download URL APIs to access files from private S3 safely.
- Pre-signed URLs are temporary. If a URL expires, call the backend again to get a new one.
- Preview rendering and lazy loading pages are frontend responsibilities.
- Share-link APIs are public by token. Anyone with a valid enabled share token can open the shared document metadata and request preview/download URLs.
- Direct user sharing requires friendship. The backend rejects sharing with non-friends.
- Documents in `shared-with-me` can be previewed/downloaded through the shared-with-me URL APIs.
- Use `POST /api/chat/ask` for single-document grounded AI chat.
- Use `POST /api/chat/ask-multi` for multi-document or user-storage grounded AI chat.
- Chat requires documents to be accessible and have status `READY`.
- Chat currently supports owned documents and public documents; shared-with-me document chat access is not documented as supported yet.
- Use `/api/chat/sessions` for persistent chat history; only the latest five completed messages are used as conversational memory for each new answer.
- Subscription plan listing and detail APIs are public; plan management APIs require an `ADMIN` JWT.
- Only active plans are returned. If a plan is soft-deleted, refresh the plan list instead of continuing to display it.
- Every activated user receives the `FREE` plan. Local accounts receive it after OTP verification; Google accounts receive it during Google login.
- Paid subscriptions fall back to `FREE` after expiration. The FREE plan has no end date and does not go through VNPay.
- Redirect the browser to the `paymentUrl` returned by the purchase API; do not call the VNPay return endpoint manually.
- VNPay return query parameters are signed. Changing the amount, transaction reference, merchant code, or signature causes the backend to reject the callback.
- `ResendOtpResponse.mesage` is currently misspelled according to the existing DTO. If the team wants `message`, the DTO/backend should be updated later.
- Tag colors should be sent as HEX values such as `#8B5CF6`, `#22C55E`, or `#FFF`.
- Video upload currently supports storing the video file and metadata, but real transcript extraction is not available yet.
- For video upload above `20MB`, the backend must run with multipart env values such as `APP_MAX_MULTIPART_FILE_SIZE=50MB` and `APP_MAX_MULTIPART_REQUEST_SIZE=50MB`.
