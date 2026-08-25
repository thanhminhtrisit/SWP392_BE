DECLARE @constraintName SYSNAME;
DECLARE @sql NVARCHAR(MAX);

DECLARE subscription_status_constraints CURSOR LOCAL FAST_FORWARD FOR
    SELECT cc.name
    FROM sys.check_constraints cc
    WHERE cc.parent_object_id = OBJECT_ID(N'dbo.subscription')
      AND cc.definition LIKE N'%status%';

OPEN subscription_status_constraints;
FETCH NEXT FROM subscription_status_constraints INTO @constraintName;

WHILE @@FETCH_STATUS = 0
BEGIN
    SET @sql = N'ALTER TABLE dbo.subscription DROP CONSTRAINT '
            + QUOTENAME(@constraintName);
    EXEC sp_executesql @sql;

    FETCH NEXT FROM subscription_status_constraints INTO @constraintName;
END;

CLOSE subscription_status_constraints;
DEALLOCATE subscription_status_constraints;

IF NOT EXISTS (
    SELECT 1
    FROM sys.check_constraints
    WHERE parent_object_id = OBJECT_ID(N'dbo.subscription')
      AND name = N'CK_subscription_status'
)
BEGIN
    ALTER TABLE dbo.subscription
        ADD CONSTRAINT CK_subscription_status
        CHECK (status IN ('ACTIVE', 'PENDING', 'EXPIRED', 'CANCELLED'));
END;
