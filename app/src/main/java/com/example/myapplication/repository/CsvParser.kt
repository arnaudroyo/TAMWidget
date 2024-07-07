import com.example.myapplication.repository.*
import com.example.myapplication.repository.model.*
import java.io.File

object CsvParser {

    fun parseTrips(file: File): List<Trip> {
        return file.bufferedReader().useLines { lines ->
            lines.drop(1)
                .map { line ->
                    val tokens = line.split(",")
                    Trip(
                        tripId = tokens[2],
                        routeId = tokens[0],
                        serviceId = tokens[1],
                        tripHeadSign = tokens[3]
                    )
                }
                .toList()
        }
    }

    fun parseRoutes(file: File): List<Route> {
        return file.bufferedReader().useLines { lines ->
            lines.drop(1)
                .map { line ->
                    val tokens = line.split(",")
                    Route(
                        routeId = tokens[0],
                        routeShortName = tokens[2],
                        routeLongName = tokens[3]
                    )
                }
                .toList()
        }
    }

    fun parseStops(file: File): List<Stop> {
        return file.bufferedReader().useLines { lines ->
            lines.drop(1)
                .map { line ->
                    val tokens = line.split(",")
                    Stop(
                        stopId = tokens[0],
                        stopName = tokens[2]
                    )
                }
                .toList()
        }
    }

    fun parseStopTimes(file: File): List<StopTime> {
        return file.bufferedReader().useLines { lines ->
            lines.drop(1)
                .map { line ->
                    val tokens = line.split(",")
                    StopTime(
                        tripId = tokens[0],
                        arrivalTime = tokens[1],
                        departureTime = tokens[2],
                        stopId = tokens[3]
                    )
                }
                .toList()
        }
    }
    fun parseCalendar(file: File): List<CalendarEntry> {
        val calendarEntries = mutableListOf<CalendarEntry>()
        file.forEachLine { line ->
            if (line.isNotEmpty() && !line.startsWith("service_id")) { // Skip header and empty lines
                val tokens = line.split(",")
                if (tokens.size >= 10) { // Adjust this number based on the actual number of fields in your calendar.txt
                    val calendarEntry = CalendarEntry(
                        serviceId = tokens[0],
                        monday = tokens[1].toInt(),
                        tuesday = tokens[2].toInt(),
                        wednesday = tokens[3].toInt(),
                        thursday = tokens[4].toInt(),
                        friday = tokens[5].toInt(),
                        saturday = tokens[6].toInt(),
                        sunday = tokens[7].toInt(),
                        startDate = tokens[8],
                        endDate = tokens[9]
                    )
                    calendarEntries.add(calendarEntry)
                }
            }
        }
        return calendarEntries
    }

    fun parseCalendarDates(file: File): List<CalendarDateEntry> {
        val reader = file.bufferedReader()
        return reader.lineSequence()
            .drop(1) // Skip header
            .map { line ->
                val parts = line.split(",")
                CalendarDateEntry(parts[0], parts[1], parts[2].toInt())
            }
            .toList()
    }

}