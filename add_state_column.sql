-- Add state column to transaction_detail table
-- This column will track "open" or "closed" status for each transaction

-- Step 1: Add the state column (if not already exists)
ALTER TABLE transaction_detail ADD COLUMN IF NOT EXISTS state VARCHAR(20) DEFAULT 'closed';

-- Step 2: Set all existing transactions to "closed" by default
UPDATE transaction_detail SET state = 'closed' WHERE state IS NULL;

-- Step 3: Create index for better performance on state queries
CREATE INDEX IF NOT EXISTS idx_transaction_detail_state ON transaction_detail(state);

-- Step 4: Verify the changes
SELECT COUNT(*) as total_transactions, 
       COUNT(CASE WHEN state = 'open' THEN 1 END) as open_transactions,
       COUNT(CASE WHEN state = 'closed' THEN 1 END) as closed_transactions
FROM transaction_detail;

-- Expected result: All transactions should be 'closed' initially
PRINT 'State column added successfully. All existing transactions set to closed.'; 