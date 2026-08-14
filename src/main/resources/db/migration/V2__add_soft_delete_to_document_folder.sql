IF NOT EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE object_id = OBJECT_ID(N'dbo.document_folder') 
      AND name = N'is_deleted'
)
BEGIN
    ALTER TABLE dbo.document_folder
        ADD is_deleted BIT NOT NULL DEFAULT 0;
END;

IF NOT EXISTS (
    SELECT 1 
    FROM sys.columns 
    WHERE object_id = OBJECT_ID(N'dbo.document_folder') 
      AND name = N'deleted_at'
)
BEGIN
    ALTER TABLE dbo.document_folder
        ADD deleted_at DATETIME2 NULL;
END;
