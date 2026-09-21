-- Forward migration for existing ticket_booking deployments.
-- Existing rows MUST remain NULL (unknown). Reconcile each historical order
-- against reservation/cancellation records before explicitly filling 0 or 1.
-- Do not infer this fact from mutable order_status. Cleanup fails closed on NULL.
-- The final DEFAULT 0 applies only to future inserts. Safe to run repeatedly.
SET @exists := (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order' AND COLUMN_NAME = 'inventory_restored');
SET @sql := IF(@exists = 0,
  'ALTER TABLE `order` ADD COLUMN `inventory_restored` TINYINT(1) NULL DEFAULT NULL COMMENT ''库存是否已回补，NULL待审计''',
  'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
ALTER TABLE `order` ALTER COLUMN `inventory_restored` SET DEFAULT 0;
