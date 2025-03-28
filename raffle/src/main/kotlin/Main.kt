import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter


fun main(args: Array<String>) {
    readValidCsv()
}

fun isNumericToX(toCheck: String): Boolean {
    return toCheck.toDoubleOrNull() != null
}

// sort --random-sort --key=1,1 --field-separator=, raffle_winners.csv
val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')
const val STRING_LENGTH = 10

fun randomStringByKotlinCollectionRandom() = List(STRING_LENGTH) { charPool.random() }.joinToString("")


private fun readValidCsv() {
    val writer = csvWriter().openAndGetRawWriter("raffle_entries.csv")

    val validCategories = listOf("Autumn Fair");
    val validProducts = listOf("3x Raffle Tickets", "1x Raffle Ticket")
    var rowCount = 0
    var orderCount = 0
    var ticketCount = 0

    csvReader().open("/Users/jrobens/Downloads/items-2024-01-01-2025-01-01 (7).csv") {
        // csvReader().open("/Users/jrobens/NetBeansProjects/kegRaffle/test-multiple.csv") {
        readAllAsSequence().forEach { row ->
            row.map { println(it) }
            val newRow = listOf(row.get(0),row.get(1),row.get(9), row.get(14), row.get(22), row.get(23))
            val categoryName = row.get(3)
            val productName = row.get(4)
            val total = row.get(9).replace("$", "")
            // $10 a ticket
            if (isNumericToX(total) && validProducts.contains(productName) && validCategories.contains(categoryName)) {
                orderCount++
                val tickets = total.toBigDecimal().div(10.toBigDecimal());
                for (i in 1..tickets.toInt()) {
                    ticketCount++
                    val rowWithRandom = listOf(randomStringByKotlinCollectionRandom()) + newRow
                    writer.writeRow(rowWithRandom)
                }
                // Bonus ticket for each $20
                val ticketsBonus = total.toBigDecimal().div(20.toBigDecimal());
                for (i in 1..ticketsBonus.toInt()) {
                    ticketCount++
                    val rowWithRandomBonus = listOf(randomStringByKotlinCollectionRandom()) + newRow
                    writer.writeRow(rowWithRandomBonus)
                }
            } else {
                if (rowCount == 0) {
                    val rowWithRandomHeader = listOf(randomStringByKotlinCollectionRandom()) + newRow
                    writer.writeRow(rowWithRandomHeader)
                }
            }
            rowCount++;
        }
        println("""Total orders found $orderCount. Tickets found $ticketCount""")
    }

    writer.close()
}
