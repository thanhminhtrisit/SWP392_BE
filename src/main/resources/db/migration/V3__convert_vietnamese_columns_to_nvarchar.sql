/*
   [SUA NGAY 2026-08-20 - co ho tro cua AI]

   VAN DE
   ------------------------------------------------------------------------------------
   Sau khi khao sat toan bo 40 cot chuoi cua schema, co 6 cot chua van ban do NGUOI DUNG
   nhap hoac do he thong trich xuat tu tai lieu, nhung dang la VARCHAR. VARCHAR phu thuoc
   collation cua server; voi collation mac dinh (SQL_Latin1_General_CP1_CI_AS) thi ky tu
   tieng Viet chi co trong Unicode se bi thay bang dau '?'.

   Ky tu nam trong Latin-1 (e, o, a, i) song sot. Ky tu chi co trong Unicode (e, a, a, o,
   u, y co dau day du) chet.

   BANG CHUNG THUC TE:
     - chat_session.title    luu "On tap SWD392" thanh "On t?p SWD392"
     - document_chunk.content do tren doc 30008: 38 dau '?' tren 8/44 chunk,
       chi 9 ky tu tieng Viet song sot trong 60.483 ky tu. Vi du: "D?ng Thanh Minh Tri",
       "Le Quang H?i", "H? th?ng Qu?n ly Cu?c thi".

   VI SAO document_chunk.content LA COT NGUY HAI NHAT
   ------------------------------------------------------------------------------------
   Van ban trong cot nay duoc dua THANG vao prompt cua LLM. Diem tinh te: vector embedding
   VAN DUNG, vi DocumentIngestionServiceImpl embed tu text con trong RAM ROI MOI ghi xuong
   database. Nghia la truy hoi tim dung chunk, nhung van ban dua cho AI la ban da hong ->
   AI tra loi sai ten nguoi trong tai lieu.

   ⚠️ MIGRATION NAY KHONG PHUC HOI DUOC DU LIEU CU
   ------------------------------------------------------------------------------------
   ALTER COLUMN chi mo rong cho chua, khong hoi phuc ky tu. Dau '?' da ghi xuong dia roi.
   Sau khi chay migration nay:
     - Du lieu MOI se luu dung tieng Viet
     - Du lieu CU van hong vinh vien
     - Rieng document_chunk.content: phai RE-INDEX lai toan bo tai lieu (xoa chunk cu,
       chay lai pipeline ingestion). Viec nay TON TIEN embedding OpenAI va duoc lam sau,
       khong nam trong migration nay.

   CAC COT CO Y KHONG DOI
   ------------------------------------------------------------------------------------
   34 cot VARCHAR con lai deu la ASCII thuan: enum (role, status, chat_mode), hash
   (password_hash, token_hash), MIME type, ma mau, ma ngon ngu, khoa blob.

   Dac biet KHONG doi document_chunk.embedding_vector: no chua chuoi JSON toan chu so,
   doi sang NVARCHAR se GAP DOI dung luong luu tru (~25 KB moi chunk) ma khong duoc gi.
   Rat dang ke tren Azure SQL Basic 2GB.

   document.original_file_name cung giu VARCHAR: FilenameSanitizer da thay dau tieng Viet
   bang '_' TRUOC KHI cham database, nen doi cot cung vo ich. Neu sau nay sua sanitizer
   thi phai doi cot nay CUNG LUC.

   subscription_plan.description (kieu TEXT) TACH RA MIGRATION RIENG, vi entity co khai
   @Column(columnDefinition = "TEXT") -> doi cot phai sua ca code.

   TINH IDEMPOTENT
   ------------------------------------------------------------------------------------
   Moi khoi deu kiem tra kieu hien tai truoc khi doi, va kiem tra su ton tai cua constraint
   truoc khi drop/tao. Chay lai nhieu lan an toan, ke ca khi lan truoc that bai giua chung.

   LUU Y KY THUAT: SQL Server yeu cau khai lai NULL/NOT NULL khi ALTER COLUMN. Bo qua se
   AM THAM bien cot NOT NULL thanh nullable. Nullability duoi day lay dung tu schema hien tai.
*/


/* ===== 1. document_chunk.content : varchar(max) NULL -> nvarchar(max) NULL ==========
   Cot quan trong nhat. Khong co index hay constraint nao tham chieu. */

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    JOIN sys.types t ON t.user_type_id = c.user_type_id
    WHERE c.object_id = OBJECT_ID(N'dbo.document_chunk')
      AND c.name = N'content'
      AND t.name = N'varchar'
)
BEGIN
    ALTER TABLE dbo.document_chunk ALTER COLUMN content NVARCHAR(MAX) NULL;
END;


/* ===== 2. chat_session.title : varchar(200) NOT NULL -> nvarchar(200) NOT NULL ====== */

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    JOIN sys.types t ON t.user_type_id = c.user_type_id
    WHERE c.object_id = OBJECT_ID(N'dbo.chat_session')
      AND c.name = N'title'
      AND t.name = N'varchar'
)
BEGIN
    ALTER TABLE dbo.chat_session ALTER COLUMN title NVARCHAR(200) NOT NULL;
END;


/* ===== 3. users.bio : varchar(500) NULL -> nvarchar(500) NULL ====================== */

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    JOIN sys.types t ON t.user_type_id = c.user_type_id
    WHERE c.object_id = OBJECT_ID(N'dbo.users')
      AND c.name = N'bio'
      AND t.name = N'varchar'
)
BEGIN
    ALTER TABLE dbo.users ALTER COLUMN bio NVARCHAR(500) NULL;
END;


/* ===== 4. subscription_plan.name : varchar(100) NULL -> nvarchar(100) NULL ========= */

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    JOIN sys.types t ON t.user_type_id = c.user_type_id
    WHERE c.object_id = OBJECT_ID(N'dbo.subscription_plan')
      AND c.name = N'name'
      AND t.name = N'varchar'
)
BEGIN
    ALTER TABLE dbo.subscription_plan ALTER COLUMN name NVARCHAR(100) NULL;
END;


/* ===== 5. document_folder.name : varchar(100) NOT NULL -> nvarchar(100) NOT NULL ====
   Cot nay nam trong rang buoc UNIQUE uk_document_folder_user_name (user_id, name).
   SQL Server khong cho ALTER COLUMN khi cot dang bi rang buoc tham chieu, nen phai
   DROP -> ALTER -> TAO LAI. Tach thanh 2 khoi de neu lan truoc chay do giua chung
   (da drop nhung chua tao lai) thi lan sau van tao lai duoc. */

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    JOIN sys.types t ON t.user_type_id = c.user_type_id
    WHERE c.object_id = OBJECT_ID(N'dbo.document_folder')
      AND c.name = N'name'
      AND t.name = N'varchar'
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM sys.key_constraints
        WHERE name = N'uk_document_folder_user_name'
          AND parent_object_id = OBJECT_ID(N'dbo.document_folder')
    )
    BEGIN
        ALTER TABLE dbo.document_folder DROP CONSTRAINT uk_document_folder_user_name;
    END;

    ALTER TABLE dbo.document_folder ALTER COLUMN name NVARCHAR(100) NOT NULL;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.key_constraints
    WHERE name = N'uk_document_folder_user_name'
      AND parent_object_id = OBJECT_ID(N'dbo.document_folder')
)
BEGIN
    ALTER TABLE dbo.document_folder
        ADD CONSTRAINT uk_document_folder_user_name UNIQUE NONCLUSTERED (user_id ASC, name ASC);
END;


/* ===== 6. tag.name : varchar(100) NOT NULL -> nvarchar(100) NOT NULL ===============
   Tuong tu muc 5, vuong rang buoc uk_tag_user_name (user_id, name). */

IF EXISTS (
    SELECT 1
    FROM sys.columns c
    JOIN sys.types t ON t.user_type_id = c.user_type_id
    WHERE c.object_id = OBJECT_ID(N'dbo.tag')
      AND c.name = N'name'
      AND t.name = N'varchar'
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM sys.key_constraints
        WHERE name = N'uk_tag_user_name'
          AND parent_object_id = OBJECT_ID(N'dbo.tag')
    )
    BEGIN
        ALTER TABLE dbo.tag DROP CONSTRAINT uk_tag_user_name;
    END;

    ALTER TABLE dbo.tag ALTER COLUMN name NVARCHAR(100) NOT NULL;
END;

IF NOT EXISTS (
    SELECT 1 FROM sys.key_constraints
    WHERE name = N'uk_tag_user_name'
      AND parent_object_id = OBJECT_ID(N'dbo.tag')
)
BEGIN
    ALTER TABLE dbo.tag
        ADD CONSTRAINT uk_tag_user_name UNIQUE NONCLUSTERED (user_id ASC, name ASC);
END;
