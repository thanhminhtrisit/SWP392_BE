/*
   The application historically stored several relationships as scalar IDs.
   These constraints protect the existing schema without changing the JPA model.
   chat_message_source is intentionally excluded because document ingestion
   replaces document_chunk rows during re-indexing.
*/

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
    JOIN sys.tables child_table
        ON child_table.object_id = fk.parent_object_id
    JOIN sys.columns child_column
        ON child_column.object_id = fkc.parent_object_id
       AND child_column.column_id = fkc.parent_column_id
    JOIN sys.tables parent_table
        ON parent_table.object_id = fk.referenced_object_id
    JOIN sys.columns parent_column
        ON parent_column.object_id = fkc.referenced_object_id
       AND parent_column.column_id = fkc.referenced_column_id
    WHERE child_table.name = N'document'
      AND child_column.name = N'user_id'
      AND parent_table.name = N'users'
      AND parent_column.name = N'user_id'
)
BEGIN
    ALTER TABLE dbo.document
        ADD CONSTRAINT FK_document_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
    JOIN sys.tables child_table
        ON child_table.object_id = fk.parent_object_id
    JOIN sys.columns child_column
        ON child_column.object_id = fkc.parent_object_id
       AND child_column.column_id = fkc.parent_column_id
    JOIN sys.tables parent_table
        ON parent_table.object_id = fk.referenced_object_id
    JOIN sys.columns parent_column
        ON parent_column.object_id = fkc.referenced_object_id
       AND parent_column.column_id = fkc.referenced_column_id
    WHERE child_table.name = N'document'
      AND child_column.name = N'folder_id'
      AND parent_table.name = N'document_folder'
      AND parent_column.name = N'folder_id'
)
BEGIN
    ALTER TABLE dbo.document
        ADD CONSTRAINT FK_document_folder
        FOREIGN KEY (folder_id) REFERENCES dbo.document_folder(folder_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
    JOIN sys.tables child_table
        ON child_table.object_id = fk.parent_object_id
    JOIN sys.columns child_column
        ON child_column.object_id = fkc.parent_object_id
       AND child_column.column_id = fkc.parent_column_id
    JOIN sys.tables parent_table
        ON parent_table.object_id = fk.referenced_object_id
    JOIN sys.columns parent_column
        ON parent_column.object_id = fkc.referenced_object_id
       AND parent_column.column_id = fkc.referenced_column_id
    WHERE child_table.name = N'document_folder'
      AND child_column.name = N'user_id'
      AND parent_table.name = N'users'
      AND parent_column.name = N'user_id'
)
BEGIN
    ALTER TABLE dbo.document_folder
        ADD CONSTRAINT FK_document_folder_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
    JOIN sys.tables child_table
        ON child_table.object_id = fk.parent_object_id
    JOIN sys.columns child_column
        ON child_column.object_id = fkc.parent_object_id
       AND child_column.column_id = fkc.parent_column_id
    JOIN sys.tables parent_table
        ON parent_table.object_id = fk.referenced_object_id
    JOIN sys.columns parent_column
        ON parent_column.object_id = fkc.referenced_object_id
       AND parent_column.column_id = fkc.referenced_column_id
    WHERE child_table.name = N'chat_session'
      AND child_column.name = N'user_id'
      AND parent_table.name = N'users'
      AND parent_column.name = N'user_id'
)
BEGIN
    ALTER TABLE dbo.chat_session
        ADD CONSTRAINT FK_chat_session_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
    JOIN sys.tables child_table
        ON child_table.object_id = fk.parent_object_id
    JOIN sys.columns child_column
        ON child_column.object_id = fkc.parent_object_id
       AND child_column.column_id = fkc.parent_column_id
    JOIN sys.tables parent_table
        ON parent_table.object_id = fk.referenced_object_id
    JOIN sys.columns parent_column
        ON parent_column.object_id = fkc.referenced_object_id
       AND parent_column.column_id = fkc.referenced_column_id
    WHERE child_table.name = N'chat_session'
      AND child_column.name = N'folder_id'
      AND parent_table.name = N'document_folder'
      AND parent_column.name = N'folder_id'
)
BEGIN
    ALTER TABLE dbo.chat_session
        ADD CONSTRAINT FK_chat_session_folder
        FOREIGN KEY (folder_id) REFERENCES dbo.document_folder(folder_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
    JOIN sys.tables child_table
        ON child_table.object_id = fk.parent_object_id
    JOIN sys.columns child_column
        ON child_column.object_id = fkc.parent_object_id
       AND child_column.column_id = fkc.parent_column_id
    JOIN sys.tables parent_table
        ON parent_table.object_id = fk.referenced_object_id
    JOIN sys.columns parent_column
        ON parent_column.object_id = fkc.referenced_object_id
       AND parent_column.column_id = fkc.referenced_column_id
    WHERE child_table.name = N'ai_token_usage'
      AND child_column.name = N'user_id'
      AND parent_table.name = N'users'
      AND parent_column.name = N'user_id'
)
BEGIN
    ALTER TABLE dbo.ai_token_usage
        ADD CONSTRAINT FK_ai_token_usage_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
    JOIN sys.tables child_table
        ON child_table.object_id = fk.parent_object_id
    JOIN sys.columns child_column
        ON child_column.object_id = fkc.parent_object_id
       AND child_column.column_id = fkc.parent_column_id
    JOIN sys.tables parent_table
        ON parent_table.object_id = fk.referenced_object_id
    JOIN sys.columns parent_column
        ON parent_column.object_id = fkc.referenced_object_id
       AND parent_column.column_id = fkc.referenced_column_id
    WHERE child_table.name = N'refresh_tokens'
      AND child_column.name = N'user_id'
      AND parent_table.name = N'users'
      AND parent_column.name = N'user_id'
)
BEGIN
    ALTER TABLE dbo.refresh_tokens
        ADD CONSTRAINT FK_refresh_tokens_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
    JOIN sys.tables child_table
        ON child_table.object_id = fk.parent_object_id
    JOIN sys.columns child_column
        ON child_column.object_id = fkc.parent_object_id
       AND child_column.column_id = fkc.parent_column_id
    JOIN sys.tables parent_table
        ON parent_table.object_id = fk.referenced_object_id
    JOIN sys.columns parent_column
        ON parent_column.object_id = fkc.referenced_object_id
       AND parent_column.column_id = fkc.referenced_column_id
    WHERE child_table.name = N'otp_verification'
      AND child_column.name = N'user_id'
      AND parent_table.name = N'users'
      AND parent_column.name = N'user_id'
)
BEGIN
    ALTER TABLE dbo.otp_verification
        ADD CONSTRAINT FK_otp_verification_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
    JOIN sys.tables child_table
        ON child_table.object_id = fk.parent_object_id
    JOIN sys.columns child_column
        ON child_column.object_id = fkc.parent_object_id
       AND child_column.column_id = fkc.parent_column_id
    JOIN sys.tables parent_table
        ON parent_table.object_id = fk.referenced_object_id
    JOIN sys.columns parent_column
        ON parent_column.object_id = fkc.referenced_object_id
       AND parent_column.column_id = fkc.referenced_column_id
    WHERE child_table.name = N'user_settings'
      AND child_column.name = N'user_id'
      AND parent_table.name = N'users'
      AND parent_column.name = N'user_id'
)
BEGIN
    ALTER TABLE dbo.user_settings
        ADD CONSTRAINT FK_user_settings_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id);
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys fk
    JOIN sys.foreign_key_columns fkc
        ON fkc.constraint_object_id = fk.object_id
    JOIN sys.tables child_table
        ON child_table.object_id = fk.parent_object_id
    JOIN sys.columns child_column
        ON child_column.object_id = fkc.parent_object_id
       AND child_column.column_id = fkc.parent_column_id
    JOIN sys.tables parent_table
        ON parent_table.object_id = fk.referenced_object_id
    JOIN sys.columns parent_column
        ON parent_column.object_id = fkc.referenced_object_id
       AND parent_column.column_id = fkc.referenced_column_id
    WHERE child_table.name = N'tag'
      AND child_column.name = N'user_id'
      AND parent_table.name = N'users'
      AND parent_column.name = N'user_id'
)
BEGIN
    ALTER TABLE dbo.tag
        ADD CONSTRAINT FK_tag_user
        FOREIGN KEY (user_id) REFERENCES dbo.users(user_id);
END;
