IF NOT EXISTS (
    SELECT 1
    FROM sys.columns
    WHERE object_id = OBJECT_ID(N'dbo.subscription')
      AND name = N'payment_id'
)
BEGIN
    ALTER TABLE dbo.subscription
        ADD payment_id BIGINT NULL;
END;

IF NOT EXISTS (
    SELECT 1
    FROM sys.foreign_keys
    WHERE name = N'FK_subscription_payment'
)
BEGIN
    ALTER TABLE dbo.subscription
        ADD CONSTRAINT FK_subscription_payment
        FOREIGN KEY (payment_id) REFERENCES dbo.payment(id);
END;
