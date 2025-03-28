import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import com.github.doyaaaaaken.kotlincsv.dsl.csvWriter


fun main(args: Array<String>) {
    readValidCsv()
}

private fun readValidCsv() {
    val writer = csvWriter().openAndGetRawWriter("halloween-food-220241023.csv")

    val validCategories = listOf("Halloween");
    val validProducts = listOf("Freaky food pack",)
    var rowCount = 0
    var orderCount = 0
    var ticketCount = 0

    csvReader {
        quoteChar = '"'
    }.open("/Users/jrobens/DocumentsLocal/Kegworth-pc/20241023-halloween/items-2024-08-01-2024-11-02.csv") {
        readAllAsSequence().forEach { row ->
            // row.map { println(it) }
            // No class on the dicso option.
            val categoryName = row[3]
            val productName = row[4]
            val modifiers = row[8]
            var childClass = ""
            var childName = ""
            val classRegex = Regex("Class:([^,]*)")
            childClass = classRegex.find(modifiers)?.groups?.get(1)?.value ?: ""

            val yearGroupYoung = "Years K-2"
            val yearGroupOld = "Years 3-6"
            var childYearGroup = "";

            // when in?
            if ("K" in childClass) {
                childYearGroup = yearGroupYoung
            }
            if ("1" in childClass) {
                childYearGroup = yearGroupYoung
            }
            if ("2" in childClass) {
                childYearGroup = yearGroupYoung
            }

            if ("3" in childClass) {
                childYearGroup = yearGroupOld
            }
            if ("4" in childClass) {
                childYearGroup = yearGroupOld
            }
            if ("5" in childClass) {
                childYearGroup = yearGroupOld
            }
            if ("6" in childClass) {
                childYearGroup = yearGroupOld
            }


            val nameRegex = Regex("Name:([^,]*)")
            childName = nameRegex.find(modifiers)?.groups?.get(1)?.value ?: ""

            val newRow: MutableList<String> = mutableListOf(
                childName,
                childClass,
                childYearGroup,
                productName,
                row.get(0),
                row.get(9),
                row.get(23),
                //  row.get(14),
                row.get(22),
            )

            println("$categoryName - $productName")
            val total = row.get(9).replace("$", "")
            // $5 a ticket
            // $8 for a food pack
            orderCount++
            if (isNumericToX(total) && validProducts.contains(productName) && validCategories.contains(categoryName)) {
                val tickets = total.toBigDecimal().div(8.toBigDecimal());
                for (i in 1..tickets.toInt()) {
                    ticketCount++
                    writer.writeRow(newRow)
                }
            } else {
                if (rowCount == 0) {
                    newRow[0] = "Name"
                    newRow[1] = "Class"
                    newRow[2] = "Year Group"
                    writer.writeRow(newRow)
                }
            }
            rowCount++
        }
        println("""Total orders found $orderCount. Tickets found $ticketCount""")
    }

    writer.close()
}
