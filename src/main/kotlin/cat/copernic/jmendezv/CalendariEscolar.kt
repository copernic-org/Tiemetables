package cat.copernic.jmendezv

import com.google.gson.GsonBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalUnit

//import kotlinx.serialization.json.*
//import kotlinx.serialization.*
//import kotlinx.serialization.protobuf.*

data class CalendariEscolar(val dataInici: LocalDate, val dataFinal: LocalDate) {
    val FESTIUS = mutableListOf<LocalDate>()

    init {
        carregaFestius()
    }

    private fun carregaFestius(festius: String = "no_lectius.dat") {
        val path = Paths.get(festius)
        if (!Files.exists(path)) {
            throw IllegalArgumentException("Missing fitxer $festius!!!")
        }
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        //var date = LocalDate.parse("31/12/2018", formatter)
        Files.readAllLines(path).forEach {
            FESTIUS += LocalDate.parse(it, formatter)
        }
    }

    fun LocalDate.isWeekend() = this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY

    fun calculaFinal(
        inici: LocalDate = dataInici,
        horesPerFer: Int,
        horesDl: Int = 0,
        horesDm: Int = 0,
        horesDx: Int = 0,
        horesDj: Int = 0,
        horesDv: Int = 0,
    ): Pair<LocalDate, Int> {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        var dataActual = inici
        var totalHores = 0
        while (totalHores < horesPerFer) {
            if (dataActual > dataFinal) {
//                throw IllegalArgumentException("Input error: data final: ${formatter.format(dataFinal)}, data actual: ${
//                    formatter.format(dataActual)
//                }")
            }
            if (dataActual in FESTIUS || dataActual.isWeekend()) {
                dataActual = dataActual.plusDays(1)
                continue
            }
            when (dataActual.dayOfWeek) {
                DayOfWeek.MONDAY -> totalHores += horesDl
                DayOfWeek.TUESDAY -> totalHores += horesDm
                DayOfWeek.WEDNESDAY -> totalHores += horesDx
                DayOfWeek.THURSDAY -> totalHores += horesDj
                DayOfWeek.FRIDAY -> totalHores += horesDv
            }
            dataActual = dataActual.plusDays(1)
        }
        return Pair(dataActual, totalHores)
    }

    fun calculaDatesInicialsFinal(schedule: Schedule): Unit {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        schedule.scheduleEntries.forEach { entry ->
            var di: LocalDate = dataInici
            var df: LocalDate = dataInici
//            val cicle = entry.cycle
            val day = entry.day
//            val pack = entry.pack
            val ufs = entry.ufs
            ufs.forEach { uf ->
                val hours = uf.hours
                    df = calculaFinal(di,
                        hours,
                        day.monday,
                        day.tuesday,
                        day.wednesday,
                        day.thursday,
                        day.friday).first
                    uf.dataInici = di
                    uf.dataFinal = df
                    di = df
//                println(uf)
//                println("${formatter.format(uf.dataInici)} ${formatter.format(uf.dataFinal)}")

            }
        }
        //return schedule
    }

    fun parseHorari(horari: String = "horari_pep.json"): Schedule {

        val path = Paths.get(horari)
        if (!Files.exists(path)) {
            throw IllegalArgumentException("Missing fitxer $horari!!!")
        }
        val gson = GsonBuilder().apply {
            setPrettyPrinting()
        }.create()
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        //var date = LocalDate.parse("31/12/2018", formatter)
        val jsonString: String = String(Files.readAllBytes(path))
        // json as array should be:
//        val sType = object : TypeToken<List<Schedule>>() { }.type
//        val schedule = gson.fromJson<List<Schedule>>(jsonString, sType)
//        val sType = object : TypeToken<List<Schedule>>() { }.type
        return gson.fromJson(jsonString, Schedule::class.java)
    }

    fun writeSchedule(schedule: Schedule, filename: String): Unit {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val buffer = StringBuilder()

        schedule.scheduleEntries.forEach { entry ->
//            println("$entry")
//            Files.write(path, entry.toString().toByteArray())
            buffer.append(entry.toString()).append("\n")
            entry.ufs.forEach { uf ->
//                println(uf)
//                Files.write(path, uf.toString().toByteArray())
                buffer.append(uf.toString()).append("\n")
//                println("${formatter.format(uf.dataInici)} - ${formatter.format(uf.dataFinal)}")
//                Files.write(path, "${formatter.format(uf.dataInici)} - ${formatter.format(uf.dataFinal)}".toByteArray())
                buffer.append("${formatter.format(uf.dataInici)} - ${formatter.format(uf.dataFinal)}").append("\n")
                if (uf.dataFinal.isAfter( dataFinal)) {
                    val periode = dataFinal.until(uf.dataFinal)
//                    println("*** ${entry.cycle}: la ${uf.name} de ${uf.module} acaba ${periode.days} dies després del final de les classes lectives ${formatter.format(dataFinal)} ***")
//                    Files.write(path, "*** ${entry.cycle}: la ${uf.name} de ${uf.module} acaba ${periode.days} dies després del final de les classes lectives ${formatter.format(dataFinal)} ***".toByteArray())
                    buffer.append("*** ${entry.cycle}: la ${uf.name} de ${uf.module} acaba ${periode.days} dies després del final de les classes lectives ${formatter.format(dataFinal)} ***").append("\n")
                }
            }
        }
        Files.deleteIfExists(Paths.get(filename))
        val path = Files.createFile(Paths.get(filename))
        Files.write(path, buffer.toString().toByteArray())
    }


}

fun main(args: Array<String>) {
    val calendariEscolar = CalendariEscolar(LocalDate.of(2021, 9, 20),
        LocalDate.of(2022, 6, 7))

    val schedule = calendariEscolar.parseHorari()
//    println(schedule)
    calendariEscolar.calculaDatesInicialsFinal(schedule)
    calendariEscolar.writeSchedule(schedule, "horari_pep.txt")

//    schedule[0].ufs[0].dataFinal = LocalDate.now()
//    println(schedule.scheduleEntries[0])
//    println(schedule.scheduleEntries[1])


//    val dataFinalM07UF1 = calendariEscolar.calculaFinal(LocalDate.of(2021, 9, 20),
//        99, horesDm = 2, horesDj = 3, horesDv = 2)
//    var formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
//    println("${formatter.format(dataFinalM07UF1.first)} ${dataFinalM07UF1.second}")


}