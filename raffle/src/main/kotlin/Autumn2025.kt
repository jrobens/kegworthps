import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import java.io.File

/**
 * Processes raffle entries from Square POS export and generates raffle tickets.
 */
fun main(args: Array<String>) {
    // Default paths with fallbacks
    val inputPath = args.firstOrNull()
        ?: listOf(
            "/Users/artur/Downloads/items-2025-04-01-2025-05-26.csv",
            "test-items.csv"
        ).firstOrNull { File(it).exists() }
        ?: "test-items.csv"

    val outputPath = args.getOrNull(1) ?: "raffle_entries.csv"

    println("Processing raffle entries:")
    println("- Input file: $inputPath")
    println("- Output file: $outputPath")

    processRaffleEntries(inputPath, outputPath)
}

/**
 * Processes the CSV file containing raffle entries and generates
 * the appropriate number of tickets for each valid entry.
 *
 * @param inputPath Path to the input CSV file
 * @param outputPath Path to write the processed entries
 */
fun processRaffleEntries(inputPath: String, outputPath: String) {
    // Configuration
    val validCategories = setOf("Autumn Raffle")
    val validProducts = mapOf(
        "Autumn raffle ticket - 3x" to 3,
        "Autumn raffle ticket - single" to 1
    )

    var rowCount = 0
    var orderCount = 0
    var ticketCount = 0
    val cancelledPayments: MutableSet<String> = mutableSetOf()

    // Ensure the output directory exists
    File(outputPath).parentFile?.mkdirs()

    csvWriter().openAndGetRawWriter(File(outputPath)).use { writer ->
        // Write header row
        val headerRow = listOf(
            "RandomID",
            "Date",
            "Time",
            "TransactionID",
            "CustomerName",
            "ProductSales",
            "CustomerID",
            "PaymentID",
        )
        writer.writeRow(headerRow)

        // Process the input CSV
        csvReader().open(inputPath) {
            readAllAsSequence().drop(1).forEach { row -> // Skip header row
                rowCount++

                try {
                    // Extract relevant fields based on the column mapping
                    val entry = extractEntryData(row)

                    // Check if this is a valid raffle entry
                    if (entry.categoryName in validCategories &&
                        entry.productName in validProducts.keys) {

                        // Get number of tickets per unit for this product
                        val ticketsPerUnit = validProducts[entry.productName] ?: 0

                        // Calculate total tickets
                        val totalTickets = (entry.quantity * ticketsPerUnit).toInt()

                        if (entry.productSales < 0)
                            cancelledPayments.add(entry.paymentId)

                        if (!cancelledPayments.contains(entry.paymentId) && totalTickets > 0) {
                            orderCount++

                            // Create entry for each ticket
                            repeat(totalTickets) {
                                ticketCount++
                                val randomId = randomStringByKotlinCollectionRandom()
                                writer.writeRow(listOf(
                                    randomId,
                                    entry.date,
                                    entry.time,
                                    entry.transactionId,
                                    entry.customerName,
                                    entry.productSales.toString(),
                                    entry.customerId,
                                    entry.paymentId,
                                ))
                            }
                        }
                    }
                } catch (e: Exception) {
                    println("Error processing row $rowCount: ${e.message}")
                }
            }
        }
    }

    println("Processing complete:")
    println("- Total rows processed: $rowCount")
    println("- Valid orders found: $orderCount")
    println("- Total tickets generated: $ticketCount")
}

/**
 * Data class to hold extracted CSV entry data
 */
data class EntryData(
    val date: String,
    val time: String,
    val transactionId: String,
    val customerName: String,
    val categoryName: String,
    val productName: String,
    val quantity: Double,
    val productSales: Double,
    val paymentId: String,
    val customerId: String
)

/**
 * Extracts relevant data from a CSV row
 */
fun extractEntryData(row: List<String>): EntryData = EntryData(
    date = row.getOrNull(0).orEmpty(),
    time = row.getOrNull(1).orEmpty(),
    transactionId = row.getOrNull(14).orEmpty(),
    customerName = row.getOrNull(23).orEmpty(),
    categoryName = row.getOrNull(3).orEmpty(),
    productName = row.getOrNull(4).orEmpty(),
    quantity = row.getOrNull(5)?.toDoubleOrNull() ?: 0.0,
    productSales = row.getOrNull(9)?.replace("$", "")?.toDoubleOrNull() ?: 0.0,
    paymentId = row.getOrNull(15).orEmpty(),
    customerId = row.getOrNull(22).orEmpty(),
)