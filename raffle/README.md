## How to do the raffle draw

This converter reads a Square Item Sales CSV export and creates one output row per raffle ticket. For example, a single `Autumn raffle ticket -7x` purchase creates seven raffle ticket rows.

### Current raffle settings

The Square report must use dates that match the current raffle period.

Current raffle period:
- Start date: `2026-01-01`
- End date: `2026-05-25`

Current Square setup:
- Category: `Autumn Raffle`
- Product: `Autumn raffle ticket - single` = 1 ticket
- Product: `Autumn raffle ticket - 3x` = 3 tickets
- Product: `Autumn raffle ticket -7x` = 7 tickets

Before running the draw, check [RaffleProcessor.kt](src/main/kotlin/RaffleProcessor.kt) and confirm the category and product names still exactly match Square.

### Run tests

From the `raffle` directory:

```bash
./gradlew test
```

### 1. Download the Square report

1. Visit [Square Item Sales Report](https://app.squareup.com/dashboard/sales/reports/item-sales).
2. Set the report dates to the current raffle period:
   - Start date: `2026-01-01`
   - End date: `2026-05-25`
3. Use these report settings:
   - All day
   - Summary
   - Display By: None
   - Filter By: None
4. Click **Detail CSV** and wait for the file to download.

### 2. Run the converter

From the `raffle` directory, pass the downloaded Square CSV path as the first argument:

```bash
./gradlew run --args="/Users/artur/Downloads/items.csv"
```

By default, this writes `raffle_entries.csv` in the `raffle` directory.

To choose a different output path, pass it as the second argument:

```bash
./gradlew run --args="/Users/artur/Downloads/items.csv /Users/artur/Downloads/raffle_entries.csv"
```

### 3. Check the generated file

The output CSV columns are:
- `RandomID`
- `Date`
- `Time`
- `TransactionID`
- `CustomerName`
- `ProductSales`
- `CustomerID`
- `PaymentID`

`RandomID` exists only to support random sorting. It can be ignored when announcing winners.

### 4. Randomize the entries

This command prints the randomized entries to the terminal by default:

```shell
sort --random-sort --key=1,1 --field-separator=, raffle_entries.csv
```

To save the randomized result to a file instead, redirect stdout:

```shell
sort --random-sort --key=1,1 --field-separator=, raffle_entries.csv > raffle_winners.csv
```

### 5. Announce winners

Read winners from the top of the randomized output.

Use the customer ID to look up contact details:

```text
https://app.squareup.com/dashboard/customers/directory/customer/ID
```

Use the transaction ID to look up the Square transaction:

```text
https://app.squareup.com/dashboard/sales/transactions/ID
```
