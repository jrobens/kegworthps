1. Export the transactions - https://squareup.com/dashboard/sales/transactions
- payments only
- this year
- Item Detail CSV

2. Check MainKt
- validProducts = Item name. Use SKU if you have it.
- validCategories 
- set the file name

3. Run MainKt.

4. Run random sort. Macos 

> sort --random-sort --key=1,1 --field-separator=, raffle_entries.csv


5. Read off the winners. 

Check that the winning ticket has not already won.

The first column is a random string used to sort randomly. It can be ignored. 

The columns are
- date
- Time
- Revenue
- Transaction ID
- Customer ID
- Customer Name

