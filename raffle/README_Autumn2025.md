## How to do the raffle draw

### Download item sales report
1. Visit [Square Item Sales Report](https://app.squareup.com/dashboard/sales/reports/item-sales)
2. Set report dates: <br>
   Start date: 2025-04-28 <br>
   End date: 2025-05-31
3. Other settings: <br>
   All day (default) <br>
   Summary <br>
   Display By: None <br>
   Filter By: None <br>
4. Click **Detail CSV** and wait for report to download

### Setup the converter
Check `Autumn2025.kt`:
1. Set path to downloaded report in `main()`
2. Make sure that category and products are up to date in `processRaffleEntries()`

### Run the converter
Run `Autumn2025.kt` in IntelliJ or from the command line via:

```bash
./gradlew clean run
```

### Run random sort

```shell 
sort --random-sort --key=1,1 --field-separator=, raffle_entries.csv
```

### Announce winners

The first column is a random string used to sort randomly. It can be ignored.

The columns are:
- Date
- Time
- Transaction ID
- Customer Name
- Revenue
- Customer ID

Customer details to lookup phone or email:
https://app.squareup.com/dashboard/customers/directory/customer/ID

Transaction lookup:
https://app.squareup.com/dashboard/sales/transactions/ID