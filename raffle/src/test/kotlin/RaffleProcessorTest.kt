import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.readLines
import kotlin.io.path.writeText
import org.junit.jupiter.api.Assertions.*
import java.io.ByteArrayOutputStream

class RaffleProcessorTest {

    @TempDir
    lateinit var tempDir: Path

    private val expectedOutputHeader = listOf(
        "RandomID", "Date", "Time", "TransactionID", "CustomerName", "ProductSales", "CustomerID", "PaymentID"
    )

    // Header only needs the columns read by extractEntryData.
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
            "Payment ID",
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

    private fun createInputRow(
        category: String = "Welcome Event",
        product: String = "Welcome event raffle ticket - \$5",
        quantity: String = "1.0",
        grossSales: String = "\$5.00",
        transactionId: String = "TxnTest",
        customerId: String = "Cust1",
        paymentId: String = "PaymentId1",
        customerName: String = "Test User"
    ): List<String> {
        val row = MutableList(25) { "DUMMY" }
        row[3] = category
        row[4] = product
        row[5] = quantity
        row[9] = grossSales
        row[14] = transactionId
        row[15] = paymentId
        row[22] = customerId
        row[23] = customerName

        return row
    }

    private fun createInputFile(fileName: String, vararg rows: List<String>): Path {
        val allRows = listOf(*rows)
        val inputFile = tempDir.resolve(fileName)

        val outputStream = ByteArrayOutputStream()

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

        val outputStream = ByteArrayOutputStream()

        csvWriter {}.open(outputStream) {
            writeRow(testInputHeader)
            writeRows(allRows)
        }

        val csvContent = "$outputStream,invalid"
        inputFile.writeText(csvContent)
        return inputFile
    }

    private fun readOutputCsv(outputFile: Path): List<List<String>> {
        assertTrue(Files.exists(outputFile), "Output file should exist: ${outputFile.fileName}")
        return csvReader().open(outputFile.toFile()) {
            readAllAsSequence().toList()
        }
    }

    private fun processRows(vararg rows: List<String>, fileName: String = "input.csv"): List<List<String>> {
        val inputFile = createInputFile(fileName, *rows)
        val outputFile = tempDir.resolve(fileName.replace(".csv", "_output.csv"))

        processRaffleEntries(inputFile.absolutePathString(), outputFile.absolutePathString())

        return readOutputCsv(outputFile)
    }

    private fun assertHeaderOnly(outputData: List<List<String>>) {
        assertEquals(1, outputData.size, "Output should have header only")
        assertEquals(expectedOutputHeader, outputData[0], "Output header mismatch")
    }

    private fun assertTicketRows(
        outputData: List<List<String>>,
        expectedTickets: Int,
        transactionId: String,
        customerName: String,
        productSales: String,
        customerId: String,
        paymentId: String? = null
    ) {
        assertEquals(expectedTickets + 1, outputData.size, "Output should have header + $expectedTickets data rows")
        assertEquals(expectedOutputHeader, outputData[0], "Output header mismatch")

        val dataRows = outputData.drop(1)
        val uniqueIds = dataRows.map { it[0] }.toSet()
        assertEquals(expectedTickets, uniqueIds.size, "RandomIDs should be unique for the $expectedTickets tickets")

        dataRows.forEach { ticket ->
            assertFalse(ticket[0].isBlank(), "RandomID should not be blank")
            assertEquals(transactionId, ticket[3])
            assertEquals(customerName, ticket[4])
            assertEquals(productSales, ticket[5])
            assertEquals(customerId, ticket[6])
            if (paymentId != null) {
                assertEquals(paymentId, ticket[7])
            }
        }
    }

    @Test
    fun `TC01 process single valid entry for one ticket`() {
        val outputData = processRows(
            createInputRow(
                product = "Autumn raffle ticket - single",
                quantity = "1",
                category = "Autumn Raffle",
                transactionId = "Txn001",
                customerName = "Artur",
                customerId = "Cust01",
                paymentId = "PayId01",
                grossSales = "\$5.00"
            ),
            fileName = "input_tc01.csv"
        )

        assertTicketRows(outputData, 1, "Txn001", "Artur", "5.0", "Cust01", "PayId01")
    }

    @Test
    fun `TC02 process single valid entry for three tickets`() {
        val outputData = processRows(
            createInputRow(
                product = "Autumn raffle ticket - 3x",
                category = "Autumn Raffle",
                quantity = "1.0",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$10.00"
            ),
            fileName = "input_tc02.csv"
        )

        assertTicketRows(outputData, 3, "Txn002", "Bob", "10.0", "Cust02")
    }

    @Test
    fun `TC02b process single valid entry for seven tickets`() {
        val outputData = processRows(
            createInputRow(
                product = "Autumn raffle ticket -7x",
                category = "Autumn Raffle",
                quantity = "1.0",
                transactionId = "Txn007",
                customerName = "Charlie",
                customerId = "Cust07",
                paymentId = "PayId07",
                grossSales = "\$20.00"
            ),
            fileName = "input_tc02b.csv"
        )

        assertTicketRows(outputData, 7, "Txn007", "Charlie", "20.0", "Cust07", "PayId07")
    }

    @Test
    fun `TC03 process single valid entry count is 2`() {
        val outputData = processRows(
            createInputRow(
                product = "Autumn raffle ticket - 3x",
                category = "Autumn Raffle",
                quantity = "2",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$20.00"
            ),
            fileName = "input_tc03.csv"
        )

        assertTicketRows(outputData, 6, "Txn002", "Bob", "20.0", "Cust02")
    }

    @Test
    fun `TC04 two records, count 2 and count 1`() {
        val outputData = processRows(
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
            fileName = "input_tc04.csv"
        )

        assertEquals(8, outputData.size, "Output should have header + 7 data rows")
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
        val outputData = processRows(
            createInputRow(
                product = "Autumn raffle ticket - 3x, \$10",
                quantity = "1.0",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$10.00"
            ),
            fileName = "input_tc10.csv"
        )

        assertHeaderOnly(outputData)
    }

    @Test
    fun `TC11 process single entry for three tickets invalid product`() {
        val outputData = processRows(
            createInputRow(
                product = "Winter raffle ticket - 3x, \$10",
                category = "Autumn Raffle",
                quantity = "1.0",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$10.00"
            ),
            fileName = "input_tc11.csv"
        )

        assertHeaderOnly(outputData)
    }

    @Test
    fun `TC12 process mixed valid and invalid rows`() {
        val outputData = processRows(
            createInputRow(
                transactionId = "Txn004",
                product = "Welcome event raffle ticket - \$5",
                quantity = "1.0"
            ),
            createInputRow(
                product = "Autumn raffle ticket - 3x",
                category = "Autumn Raffle",
                quantity = "2",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$20.00"
            ),
            createInputRow(transactionId = "Txn006", category = "Invalid Category"),
            createInputRow(transactionId = "Txn007", product = "Invalid Product Name"),
            fileName = "input_tc12.csv"
        )

        assertTicketRows(outputData, 6, "Txn002", "Bob", "20.0", "Cust02")
    }

    @Test
    fun `TC13 process valid entry with zero quantity`() {
        val outputData = processRows(
            createInputRow(
                product = "Autumn raffle ticket - 3x",
                category = "Autumn Raffle",
                quantity = "0",
                transactionId = "Txn006"
            ),
            fileName = "input_tc13.csv"
        )

        assertHeaderOnly(outputData)
    }

    @Test
    fun `TC15 output directory is created if not exists`() {
        val inputFile = createInputFile(
            "input_tc15.csv",
            createInputRow(transactionId = "Txn015")
        )
        val outputDir = tempDir.resolve("new_dir")
        val outputFile = outputDir.resolve("output_tc15.csv")

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

        assertFalse(Files.exists(nonExistentInput))

        assertThrows<Exception>("Should throw when input file doesn't exist") {
            processRaffleEntries(nonExistentInput.absolutePathString(), outputFile.absolutePathString())
        }

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
            createInputRow(transactionId = "Txn017")
        )
        val outputDir = tempDir.resolve("new_dir")
        val outputFile = outputDir.resolve("output_tc15.csv")

        assertTrue(Files.exists(invalidInput))

        val exception = assertThrows<Exception>("Should throw when the number of columns in a row is wrong.") {
            processRaffleEntries(invalidInput.absolutePathString(), outputFile.absolutePathString())
        }
        assertEquals("Fields num seems to be 25 on each row, but on 3th csv row, fields num is 2.", exception.message)

        val outputLines = if (Files.exists(outputFile)) outputFile.readLines() else emptyList()
        assertTrue(outputLines.size <= 1, "Output file should be empty or contain only header")
        if (outputLines.isNotEmpty()) {
            assertEquals(expectedOutputHeader.joinToString(","), outputLines[0])
        }
    }

    @Test
    fun `TC17 invalid quantity`() {
        val outputData = processRows(
            createInputRow(
                product = "Autumn raffle ticket - 3x, \$10",
                quantity = "ab",
                transactionId = "Txn002",
                customerName = "Bob",
                customerId = "Cust02",
                grossSales = "\$10.00"
            ),
            fileName = "input_tc17_invalid_quantity.csv"
        )

        assertHeaderOnly(outputData)
    }

    @Test
    fun `TC18 refund creates no tickets`() {
        val outputData = processRows(
            createInputRow (
                product = "Autumn raffle ticket - single",
                category = "Autumn Raffle",
                transactionId = "Txn001",
                customerName = "Artur",
                grossSales = "-\$5.00"
            ),
            createInputRow(
                product = "Autumn raffle ticket - single",
                category = "Autumn Raffle",
                transactionId = "Txn002",
                customerName = "Artur",
                grossSales = "\$5.00"
            ),
            fileName = "input_tc18.csv"
        )

        assertHeaderOnly(outputData)
    }
}
