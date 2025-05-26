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

    val outputPath = args.getOrNull(1) ?: "missing_customers.csv"

    println("Processing raffle entries:")
    println("- Input file: $inputPath")
    println("- Output file: $outputPath")

    processRaffleEntriesFindMissingCustomers(inputPath, outputPath)
}

/**
 * Processes the CSV file containing raffle entries and generates
 * the appropriate number of tickets for each valid entry.
 *
 * @param inputPath Path to the input CSV file
 * @param outputPath Path to write the processed entries
 */
fun processRaffleEntriesFindMissingCustomers(inputPath: String, outputPath: String) {
    // Configuration
    val validCategories = setOf("Autumn Raffle")
    val validProducts = mapOf(
        "Autumn raffle ticket - 3x" to 3,
        "Autumn raffle ticket - single" to 1
    )

    var rowCount = 0
    var missingCount = 0

    // Ensure the output directory exists
    File(outputPath).parentFile?.mkdirs()

    csvWriter().openAndGetRawWriter(File(outputPath)).use { writer ->
        // Write header row
        val headerRow = listOf(
            "Date",
            "Time",
            "TransactionID",
            "CustomerName",
            "ProductSales",
            "CustomerID",
            "CustomerRefID"
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
                        entry.productName in validProducts.keys &&
                        entry.customerId.isEmpty()
                    ) {
                        missingCount++
                        writer.writeRow(
                            listOf(
                                entry.date,
                                entry.time,
                                entry.transactionId,
                                entry.customerName,
                                entry.productSales.toString(),
                                entry.customerId
                            )
                        )
                    }
                } catch (e: Exception) {
                    println("Error processing row $rowCount: ${e.message}")
                }
            }
        }
    }

    println("Processing complete:")
    println("- Total rows processed: $rowCount")
    println("- Total missing customers: $missingCount")
}

