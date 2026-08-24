IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.document_shares')
      AND name = N'status'
)
BEGIN
ALTER TABLE dbo.document_shares
    ADD status VARCHAR(50) DEFAULT 'PENDING_APPROVAL' NOT NULL;
END;