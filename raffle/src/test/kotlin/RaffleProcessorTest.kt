import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
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
import org.junit.jupiter.api.assertDoesNotThrow
import java.io.ByteArrayOutputStream
import java.io.File // For csvReader compatibility
import java.io.OutputStreamWriter
import java.io.StringWriter

// Yes, gemini creates a bunch of comments about everything...

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
        "RandomID", "Date", "Time", "TransactionID", "CustomerName", "ProductSales", "CustomerID"
    )

    // Simplified input header based *only* on indices used by extractEntryData + a few placeholders
    // Indices:      3         4        5               9            14               22           23            24
    private val testInputHeader =
        listOf(
            "H1",
            "H2",
            "H3",
            "Category",
            "Product",
            "Quantity",
            "H7",
            "H8",
            "H9",
            "Gross Sales",
            "H11",
            "H12",
            "H13",
            "Transaction ID",
            "H15",
            "H16",
            "H17",
            "H18",
            "H19",
            "H20",
            "H21",
            "Customer ID",
            "Customer Name",
            "Customer Ref ID",
            "H25"
        )

    // Creates a dummy input CSV row string, allowing specific fields to be set easily.
    // Matches the structure required by extractEntryData based on the indices used.
    private fun createInputRow(
        category: String = "Welcome Event",
        product: String = "Welcome event raffle ticket - \$5",
        quantity: String = "1.0",
        grossSales: String = "\$5.00", // Simulate Square format
        transactionId: String = "TxnTest",
        customerId: String = "Cust1",
        customerName: String = "Test User"
    ): List<String> {
        val row = MutableList(25) { "DUMMY" } // Need 25 columns to reach index 24
        row[3] = category
        row[4] = product
        row[5] = quantity
        row[9] = grossSales
        row[14] = transactionId
        row[22] = customerId
        row[23] = customerName

        return row
    }

    // Creates an input file in the temp directory
    private fun createInputFile(fileName: String, vararg rows: List<String>): Path {
        val allRows = listOf(*rows)
        val inputFile = tempDir.resolve(fileName)

        val outputStream = ByteArrayOutputStream() // Create a ByteArrayOutputStream

        csvWriter {}.open(outputStream) {
            writeRow(testInputHeader)
            writeRows(allRows)
        }

        val csvContent = outputStream.toString()
        inputFile.writeText(csvContent)
        return inputFile
    }

    private fun createInputFileInvalid(fileName: String, vararg rows: List<String>): Path {
        val allRows = listOf(*rows)
        val inputFile = tempDir.resolve(fileName)

        val outputStream = ByteArrayOutputStream() // Create a ByteArrayOutputStream

        csvWriter {}.open(outputStream) {
            writeRow(testInputHeader)
            writeRows(allRows)
        }

        val csvContent = "$outputStream,invalid"
        inputFile.writeText(csvContent)
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
        val row = createInputRow(
            product = "Autumn raffle ticket - single",
            quantity = "1",
            category = "Autumn Raffle",
            transactionId = "Txn001",
            customerName = "Artur",
            customerId = "Cust01",
            grossSales = "\$5.00"
        )

        val inputFile = createInputFile(
            "input_tc01.csv",
            row,
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
        assertEquals("Txn001", ticket[3])
        assertEquals("Artur", ticket[4])
        assertEquals("5.0", ticket[5]) // Check '$' removal and number format
        assertEquals("Cust01", ticket[6])
        // There is no CustomerRefID data. Header only.
    }

    @Test
    fun `TC02 process single valid entry for three tickets`() {

        val inputFile = createInputFile(
            "input_tc02.csv",
            createInputRow(
                product = "Autumn raffle ticket - 3x",
                category = "Autumn Raffle",
                quantity = "1.0",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$10.00"
            )
        )
        val outputFile = tempDir.resolve("output_tc02.csv")

        try {
            processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())
        } catch (e: Throwable) {
            assertNull(e.message)
        }

        val outputData = readOutputCsv(outputFile)
        assertEquals(4, outputData.size, "Output should have header + 3 data rows") // 1 header + 3 tickets
        assertEquals(expectedOutputHeader, outputData[0], "Output header mismatch")

        val dataRows = outputData.drop(1)
        val uniqueIds = dataRows.map { it[0] }.toSet()
        assertEquals(3, uniqueIds.size, "RandomIDs should be unique for the 3 tickets")

        dataRows.forEach { ticket ->
            assertFalse(ticket[0].isBlank(), "RandomID should not be blank")
            assertEquals("Txn002", ticket[3])
            assertEquals("Bob", ticket[4])
            assertEquals("10.0", ticket[5])
            assertEquals("Cust02", ticket[6])
        }
    }

    /**
     * If you purchase 2x $10 and 1x $5 these products get split onto different lines. The count is 2 and 1.
     */
    @Test
    fun `TC03 process single valid entry count is 2`() {

        val inputFile = createInputFile(
            "input_tc02.csv",
            createInputRow(
                product = "Autumn raffle ticket - 3x",
                category = "Autumn Raffle",
                quantity = "2",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$20.00"
            )
        )
        val outputFile = tempDir.resolve("output_tc02.csv")

        try {
            processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())
        } catch (e: Throwable) {
            assertNull(e.message)
        }

        val outputData = readOutputCsv(outputFile)
        assertEquals(7, outputData.size, "Output should have header + 6 data rows") // 1 header + 3 tickets
        assertEquals(expectedOutputHeader, outputData[0], "Output header mismatch")

        val dataRows = outputData.drop(1)
        val uniqueIds = dataRows.map { it[0] }.toSet()
        assertEquals(6, uniqueIds.size, "RandomIDs should be unique for the 6 tickets")

        dataRows.forEach { ticket ->
            assertFalse(ticket[0].isBlank(), "RandomID should not be blank")
            assertEquals("Txn002", ticket[3])
            assertEquals("Bob", ticket[4])
            assertEquals("20.0", ticket[5])
            assertEquals("Cust02", ticket[6])
        }
    }


    /**
     * If you purchase 2x $10 and 1x $5 these products get split onto different lines. The count is 2 and 1.
     */
    @Test
    fun `TC04 two records, count 2 and count 1`() {

        val inputFile = createInputFile(
            "input_tc02.csv",
            createInputRow(
                product = "Autumn raffle ticket - 3x",
                category = "Autumn Raffle",
                quantity = "2",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$20.00"
            ),
            createInputRow(
                product = "Autumn raffle ticket - single",
                category = "Autumn Raffle",
                quantity = "1",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$5.00"
            ),
        )
        val outputFile = tempDir.resolve("output_tc02.csv")

        try {
            processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())
        } catch (e: Throwable) {
            assertNull(e.message)
        }

        val outputData = readOutputCsv(outputFile)
        assertEquals(8, outputData.size, "Output should have header + 7 data rows") // 1 header + 3 tickets
        assertEquals(expectedOutputHeader, outputData[0], "Output header mismatch")

        val dataRows = outputData.drop(1)
        val uniqueIds = dataRows.map { it[0] }.toSet()
        assertEquals(7, uniqueIds.size, "RandomIDs should be unique for the 7 tickets")

        dataRows.forEachIndexed { i, ticket ->
            assertFalse(ticket[0].isBlank(), "RandomID should not be blank")
            assertEquals("Txn002", ticket[3])
            assertEquals("Bob", ticket[4])
            if (i == 6) {
                assertEquals("5.0", ticket[5])

            } else {
                assertEquals("20.0", ticket[5])
            }
            assertEquals("Cust02", ticket[6])
        }
    }


    @Test
    fun `TC10 process single entry for three tickets invalid category`() {

        val inputFile = createInputFile(
            "input_tc02.csv",
            createInputRow(
                product = "Autumn raffle ticket - 3x, \$10",
                quantity = "1.0",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$10.00"
            )
        )
        val outputFile = tempDir.resolve("output_tc02.csv")

        try {
            processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())
        } catch (e: Throwable) {
            assertNull(e.message)
        }

        val outputData = readOutputCsv(outputFile)
        assertEquals(1, outputData.size, "Output should have header only") // 1 header + 3 tickets
        assertEquals(expectedOutputHeader, outputData[0], "Output header mismatch")

        val dataRows = outputData.drop(1)
        val uniqueIds = dataRows.map { it[0] }.toSet()
        assertEquals(0, uniqueIds.size, "RandomIDs should be unique for the 3 tickets")

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
    fun `TC11 process single entry for three tickets invalid product`() {

        val inputFile = createInputFile(
            "input_tc02.csv",
            createInputRow(
                product = "Winter raffle ticket - 3x, \$10",
                category = "Autumn Raffle",
                quantity = "1.0",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$10.00"
            )
        )
        val outputFile = tempDir.resolve("output_tc02.csv")

        try {
            processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())
        } catch (e: Throwable) {
            assertNull(e.message)
        }

        val outputData = readOutputCsv(outputFile)
        assertEquals(1, outputData.size, "Output should have header only") // 1 header + 3 tickets
        assertEquals(expectedOutputHeader, outputData[0], "Output header mismatch")

        val dataRows = outputData.drop(1)
        val uniqueIds = dataRows.map { it[0] }.toSet()
        assertEquals(0, uniqueIds.size, "RandomIDs should be unique for the 3 tickets")

        dataRows.forEach { ticket ->
            assertFalse(ticket[0].isBlank(), "RandomID should not be blank")
            assertEquals("Txn002", ticket[1])
            assertEquals("Bob", ticket[2])
            assertEquals("10.0", ticket[3])
            assertEquals("Cust02", ticket[4])
            assertEquals("Ref002", ticket[5])
        }
    }


    // Mix of raffle and unrelated products.


    @Test
    fun `TC12 process mixed valid and invalid rows`() {
        val inputFile = createInputFile(
            "input_tc04.csv",
            createInputRow(
                transactionId = "Txn004",
                product = "Welcome event raffle ticket - \$5",
                quantity = "1.0"
            ), // Valid 1 ticket
            createInputRow(
                product = "Autumn raffle ticket - 3x",
                category = "Autumn Raffle",
                quantity = "2",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$20.00"
            ),
            createInputRow(transactionId = "Txn006", category = "Invalid Category"), // Invalid category
            createInputRow(transactionId = "Txn007", product = "Invalid Product Name") // Invalid product
        )
        val outputFile = tempDir.resolve("output_tc04.csv")

        try {
            processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())
        } catch (e: Throwable) {
            assertNull(e.message)
        }

        val outputData = readOutputCsv(outputFile)
        // 1 Header + 1 ticket (Txn004) + 3 tickets (Txn005) = 5 rows total
        // Stops producing at the exception. However, there is a partially produced file which if the operator
        // is not paying attention could cause errors. Consider finally, rethrow. Beyond our effort level.
        assertEquals(7, outputData.size, "Output should have header + 2 x 3 data rows")
        assertEquals(expectedOutputHeader, outputData[0])
    }

    @Test
    fun `TC13 process valid entry with zero quantity`() {
        val inputFile = createInputFile(
            "input_tc05.csv",
            createInputRow(
                product = "Autumn raffle ticket - 3x",
                category = "Autumn Raffle",
                quantity = "0", // Zero quantity
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
        val inputFile = createInputFile(
            "input_tc15.csv",
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
        assertEquals(1, outputData.size, "Output should have header")
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

    @Test
    fun `TC17 incorrect number of columns create a partially written file`() {
        val invalidInput = createInputFileInvalid(
            "input_tc17.csv",
            createInputRow(transactionId = "Txn017") // Simple valid row
        )
        val outputDir = tempDir.resolve("new_dir")
        val outputFile = outputDir.resolve("output_tc15.csv")

        // Ensure input does not exist
        assertTrue(Files.exists(invalidInput))

        // Assert that processing a non-existent file throws an exception.
        // The exact exception type might depend on kotlin-csv implementation details,
        // so catching a general Exception or java.io.IOException might be necessary.
        val exception = assertThrows<Exception>("Should throw when the number of columns in a row is wrong.") {
            processRaffleEntries(invalidInput.absolutePathString(), outputFile.absolutePathString())
        }
        assertEquals("Fields num seems to be 25 on each row, but on 3th csv row, fields num is 2.", exception.message)

        // Check if output file was created (it might be, with just the header, before read fails)
        val outputLines = if (Files.exists(outputFile)) outputFile.readLines() else emptyList()
        assertTrue(outputLines.size <= 1, "Output file should be empty or contain only header")
        if (outputLines.isNotEmpty()) {
            assertEquals(expectedOutputHeader.joinToString(","), outputLines[0])
        }
    }

    @Test
    fun `TC17 invalid quantity`() {

        val inputFile = createInputFile(
            "input_tc02.csv",
            createInputRow(
                product = "Autumn raffle ticket - 3x, \$10",
                quantity = "ab",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$10.00"
            )
        )
        val outputFile = tempDir.resolve("output_tc02.csv")

        try {
            processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())
        } catch (e: Throwable) {
            assertNull(e.message)
        }

        val outputData = readOutputCsv(outputFile)
        assertEquals(1, outputData.size, "Output should have header only") // 1 header + 3 tickets
        assertEquals(expectedOutputHeader, outputData[0], "Output header mismatch")

        val dataRows = outputData.drop(1)
        val uniqueIds = dataRows.map { it[0] }.toSet()
        assertEquals(0, uniqueIds.size, "RandomIDs should be unique for the 3 tickets")

        dataRows.forEach { ticket ->
            assertFalse(ticket[0].isBlank(), "RandomID should not be blank")
            assertEquals("Txn002", ticket[1])
            assertEquals("Bob", ticket[2])
            assertEquals("10.0", ticket[3])
            assertEquals("Cust02", ticket[4])
        }
    }


    /*
    Refunds don't get subtracted out. Should be 3 tickets.

    3BDOgXsqej,2025-05-25,12:37:23,llnZK48tvoOFbRPYlZBne1IcVkbZY,Lizzie Grant,10.0,71HBBYA4C8GX3NYCEE6RX4C8PW,
7L5PGN2u0f,2025-05-25,12:37:23,llnZK48tvoOFbRPYlZBne1IcVkbZY,Lizzie Grant,10.0,71HBBYA4C8GX3NYCEE6RX4C8PW,
yrt9m8WgHa,2025-05-25,12:37:23,llnZK48tvoOFbRPYlZBne1IcVkbZY,Lizzie Grant,10.0,71HBBYA4C8GX3NYCEE6RX4C8PW,
BMLm2ICZwp,2025-05-25,12:36:08,d9tjXn0yZPvLVRvSQ2cMqtrumvFZY,Lizzie Grant,10.0,71HBBYA4C8GX3NYCEE6RX4C8PW,
vcfeRygShU,2025-05-25,12:36:08,d9tjXn0yZPvLVRvSQ2cMqtrumvFZY,Lizzie Grant,10.0,71HBBYA4C8GX3NYCEE6RX4C8PW,
Lgp2xxr5Dc,2025-05-25,12:36:08,d9tjXn0yZPvLVRvSQ2cMqtrumvFZY,Lizzie Grant,10.0,71HBBYA4C8GX3NYCEE6RX4C8PW,


     */
}
