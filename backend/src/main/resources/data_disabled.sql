

-- 1. Alice (Standard User)
INSERT INTO accounts (account_id, holder_name, balance, status, version, last_updated, password)
VALUES ('ACC001', 'Alice', 5000.0000, 'ACTIVE', 0, CURRENT_TIMESTAMP,"Alice@123");

-- 2. Bob (Standard Receiver)
INSERT INTO accounts (account_id, holder_name, balance, status, version, last_updated, password)
VALUES ('ACC002', 'Bob', 1000.0000, 'ACTIVE', 0, CURRENT_TIMESTAMP,"Bob@123");

-- 3. Charlie (The "Broke" User - Has only $10)
INSERT INTO accounts (account_id, holder_name, balance, status, version, last_updated, password)
VALUES ('ACC003', 'Charlie', 10.0000, 'ACTIVE', 0, CURRENT_TIMESTAMP,"Charlie@123");

-- 4. Diana (The "Closed" Account - Validation Test)
INSERT INTO accounts (account_id, holder_name, balance, status, version, last_updated , password)
VALUES ('ACC004', 'Diana', 0.0000, 'CLOSED', 0, CURRENT_TIMESTAMP,"Diana@123");

-- 5. Eve (The "Whale" - High Balance)
INSERT INTO accounts (account_id, holder_name, balance, status, version, last_updated,password)
VALUES ('ACC005', 'Eve', 1000000.0000, 'ACTIVE', 0, CURRENT_TIMESTAMP,"Eve@123");