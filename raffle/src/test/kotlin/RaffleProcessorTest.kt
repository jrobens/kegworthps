import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.util.CSVFieldNumDifferentException
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows // For checking exceptions
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.* // Standard JUnit 5 assertions
import java.io.File // For csvReader compatibility

// --- !!! IMPORTANT !!! ---
// If your raffle script functions are inside a package, uncomment and adjust the import below:
// import com.yourpackage.processRaffleEntries
// import com.yourpackage.randomStringByKotlinCollectionRandom // Might not be needed directly

class RaffleProcessorTest {

    @TempDir // JUnit 5 injects a temporary directory for each test
    lateinit var tempDir: Path

    // --- Helper Functions ---

    // Defines the expected header row in the output CSV
    private val expectedOutputHeader = listOf(
        "RandomID", "TransactionID", "CustomerName", "ProductSales", "CustomerID", "CustomerRefID"
    )

    // Simplified input header based *only* on indices used by extractEntryData + a few placeholders
    // Indices:      3         4        5               9            14               22           23            24
    private val testInputHeader = "H1,H2,H3,Category,Product,Quantity,H7,H8,H9,Gross Sales,H11,H12,H13,Transaction ID,H15,H16,H17,H18,H19,H20,H21,Customer ID,Customer Name,Customer Ref ID,H25"

    // Creates a dummy input CSV row string, allowing specific fields to be set easily.
    // Matches the structure required by extractEntryData based on the indices used.
    private fun createInputRow(
        category: String = "Welcome Event",
        product: String = "Welcome event raffle ticket - \$5",
        quantity: String = "1.0",
        grossSales: String = "\$5.00", // Simulate Square format
        transactionId: String = "TxnTest",
        customerId: String = "Cust1",
        customerName: String = "Test User",
        customerRefId: String = "Ref1"
    ): String {
        val row = MutableList(25) { "DUMMY" } // Need 25 columns to reach index 24
        row[3] = category
        row[4] = product
        row[5] = quantity
        row[9] = grossSales
        row[14] = transactionId
        row[22] = customerId
        row[23] = customerName
        row[24] = customerRefId
        // Simple CSV formatting (no special char handling needed for this test data)
        return row.joinToString(",")
    }

    // Creates an input file in the temp directory
    private fun createInputFile(fileName: String, vararg rows: String): Path {
        val inputFile = tempDir.resolve(fileName)
        val content = listOf(testInputHeader, *rows).joinToString("\n")
        inputFile.writeText(content)
        return inputFile
    }

    // Reads the generated output CSV file for verification
    private fun readOutputCsv(outputFile: Path): List<List<String>> {
        assertTrue(Files.exists(outputFile), "Output file should exist: ${outputFile.fileName}")
        return csvReader().open(outputFile.toFile()) {
            readAllAsSequence().toList()
        }
    }

    // --- Test Cases ---

    @Test
    fun `TC01 process single valid entry for one ticket`() {
        val inputFile = createInputFile("input_tc01.csv",
            createInputRow(
                product = "Welcome event raffle ticket - \$5", // 1 ticket product
                quantity = "1.0",
                transactionId = "Txn001",
                customerName = "Artur",
                customerId = "Cust01",
                customerRefId = "Ref001",
                grossSales = "\$5.00"
            )
        )
        val outputFile = tempDir.resolve("output_tc01.csv")

        // Execute the function under test
        processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())

        // Verify the output
        val outputData = readOutputCsv(outputFile)
        assertEquals(2, outputData.size, "Output should have header + 1 data row")
        assertEquals(expectedOutputHeader, outputData[0], "Output header mismatch")

        val ticket = outputData[1]
        assertFalse(ticket[0].isBlank(), "RandomID should not be blank") // Check ID exists
        assertEquals("Txn001", ticket[1])
        assertEquals("Artur", ticket[2])
        assertEquals("5.0", ticket[3]) // Check '$' removal and number format
        assertEquals("Cust01", ticket[4])
        assertEquals("Ref001", ticket[5])
    }

    @Test
    fun `TC02 process single valid entry for three tickets`() {
        val inputFile = createInputFile("input_tc02.csv",
            createInputRow(
                product = "Welcome event raffle ticket - 3x, \$10", // 3 ticket product
                quantity = "1.0",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                customerRefId = "Ref002",
                grossSales = "\$10.00"
            )
        )
        val outputFile = tempDir.resolve("output_tc02.csv")

        val exception = assertThrows<CSVFieldNumDifferentException> {
            processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())
        }

        val outputData = readOutputCsv(outputFile)
        assertEquals(4, outputData.size, "Output should have header + 3 data rows") // 1 header + 3 tickets
        assertEquals(expectedOutputHeader, outputData[0], "Output header mismatch")

        val dataRows = outputData.drop(1)
        val uniqueIds = dataRows.map { it[0] }.toSet()
        assertEquals(3, uniqueIds.size, "RandomIDs should be unique for the 3 tickets")

        dataRows.forEach { ticket ->
            assertFalse(ticket[0].isBlank(), "RandomID should not be blank")
            assertEquals("Txn002", ticket[1])
            assertEquals("Bob", ticket[2])
            assertEquals("10.0", ticket[3])
            assertEquals("Cust02", ticket[4])
            assertEquals("Ref002", ticket[5])
        }
    }

    @Test
    fun `TC04 process mixed valid and invalid rows`() {
        val inputFile = createInputFile("input_tc04.csv",
            createInputRow(transactionId = "Txn004", product = "Welcome event raffle ticket - \$5", quantity = "1.0"), // Valid 1 ticket
            createInputRow(transactionId = "Txn005", product = "Welcome event raffle ticket - 3x, \$10", quantity = "1.0"),// Valid 3 tickets
            createInputRow(transactionId = "Txn006", category = "Invalid Category"), // Invalid category
            createInputRow(transactionId = "Txn007", product = "Invalid Product Name") // Invalid product
        )
        val outputFile = tempDir.resolve("output_tc04.csv")

        processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())

        val outputData = readOutputCsv(outputFile)
        // 1 Header + 1 ticket (Txn004) + 3 tickets (Txn005) = 5 rows total
        assertEquals(5, outputData.size, "Output should have header + 4 data rows")
        assertEquals(expectedOutputHeader, outputData[0])

        val transactionIds = outputData.drop(1).map { it[1] }.toSet()
        assertEquals(setOf("Txn004", "Txn005"), transactionIds, "Only valid transactions should appear")

        val txn004Count = outputData.drop(1).count { it[1] == "Txn004" }
        val txn005Count = outputData.drop(1).count { it[1] == "Txn005" }
        assertEquals(1, txn004Count, "Txn004 should generate 1 ticket")
        assertEquals(3, txn005Count, "Txn005 should generate 3 tickets")
    }

    @Test
    fun `TC05 process valid entry with zero quantity`() {
        val inputFile = createInputFile("input_tc05.csv",
            createInputRow(
                product = "Welcome event raffle ticket - \$5",
                quantity = "0.0", // Zero quantity
                transactionId = "Txn006"
            )
        )
        val outputFile = tempDir.resolve("output_tc05.csv")

        processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())

        val outputData = readOutputCsv(outputFile)
        assertEquals(1, outputData.size, "Output should have only the header row")
        assertEquals(expectedOutputHeader, outputData[0])
    }

    @Test
    fun `TC15 output directory is created if not exists`() {
        val inputFile = createInputFile("input_tc15.csv",
            createInputRow(transactionId = "Txn015") // Simple valid row
        )
        // Define output path inside a *new* subdirectory of tempDir
        val outputDir = tempDir.resolve("new_dir")
        val outputFile = outputDir.resolve("output_tc15.csv")

        // Ensure directory doesn't exist before test
        assertFalse(Files.exists(outputDir), "Output directory should not exist before test")

        processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())

        assertTrue(Files.exists(outputDir), "Output directory should have been created")
        assertTrue(Files.exists(outputFile), "Output file should exist in new directory")

        val outputData = readOutputCsv(outputFile)
        assertEquals(2, outputData.size, "Output should have header + 1 data row")
        assertEquals("Txn015", outputData[1][1]) // Verify correct data was processed
    }

    @Test
    fun `TC16 throws exception for non-existent input file`() {
        val nonExistentInput = tempDir.resolve("non_existent_input.csv")
        val outputFile = tempDir.resolve("output_tc16.csv")

        // Ensure input does not exist
        assertFalse(Files.exists(nonExistentInput))

        // Assert that processing a non-existent file throws an exception.
        // The exact exception type might depend on kotlin-csv implementation details,
        // so catching a general Exception or java.io.IOException might be necessary.
        assertThrows<Exception>("Should throw when input file doesn't exist") {
            processRaffleEntries(nonExistentInput.absolutePathString(), outputFile.absolutePathString())
        }

        // Check if output file was created (it might be, with just the header, before read fails)
        val outputLines = if (Files.exists(outputFile)) outputFile.readLines() else emptyList()
        assertTrue(outputLines.size <= 1, "Output file should be empty or contain only header")
        if (outputLines.isNotEmpty()) {
            assertEquals(expectedOutputHeader.joinToString(","), outputLines[0])
        }
    }

    // Add more test cases here for TC03, TC06, TC07, TC08, TC09, TC10, TC11, etc.
    // following the same pattern: prepare input -> execute -> verify output.
}
