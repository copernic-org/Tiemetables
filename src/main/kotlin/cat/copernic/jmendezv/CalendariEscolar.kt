package cat.copernic.jmendezv

import com.google.gson.GsonBuilder
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.Period
import java.time.format.DateTimeFormatter

object CalendariEscolar {
    var dataIniciStr: String = ""
    var dataFinalStr: String = ""
    private val fitxerNoLectius: String = "no_lectius.dat"
    var horariJson: String = ""
    var output: String = ""
    private var dataInici: LocalDate = LocalDate.now()
    private var dataFinal: LocalDate = LocalDate.now()

    private val NO_LECTIUS = mutableListOf<LocalDate>()
    private val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    fun build(): Unit {
        if (dataIniciStr.isEmpty() or dataIniciStr.isBlank()) throw java.lang.IllegalArgumentException("Falta data inici curs")
        if (dataFinalStr.isEmpty() or dataFinalStr.isBlank()) throw java.lang.IllegalArgumentException("Falta data final curs")
        if (horariJson.isEmpty() or horariJson.isBlank()) throw java.lang.IllegalArgumentException("Falta JSON d'entrada")
        if (output.isEmpty() or output.isBlank()) output = horariJson.substring(0, horariJson.indexOf(".")).plus(".txt")
        dataInici = LocalDate.parse(dataIniciStr, formatter)
        dataFinal = LocalDate.parse(dataFinalStr, formatter)
        carregaNoLectius()
        val schedule = parseHorari()
        calculaDatesInicialsFinal(schedule)
        writeSchedule(schedule)
        println("$output creat correctament")
    }

    // En principi es el mateix per tots els cicles
    private fun carregaNoLectius() {
        val path = Paths.get(fitxerNoLectius)
        if (!Files.exists(path)) {
            throw IllegalArgumentException("Missing fitxer $fitxerNoLectius!!!")
        }
        Files.readAllLines(path).forEach {
            NO_LECTIUS += LocalDate.parse(it, formatter)
        }
    }

    private fun LocalDate.isWeekend() = this.dayOfWeek == DayOfWeek.SATURDAY || this.dayOfWeek == DayOfWeek.SUNDAY

    private fun LocalDate.noHiHaDocencia() = (this in NO_LECTIUS || this.isWeekend())

    private fun LocalDate.diaDeLaSetmana() = when (dayOfWeek) {
        DayOfWeek.MONDAY -> "dilluns"
        DayOfWeek.TUESDAY -> "dimarts"
        DayOfWeek.WEDNESDAY -> "dimecres"
        DayOfWeek.THURSDAY -> "dijous"
        DayOfWeek.FRIDAY -> "divendres"
        DayOfWeek.SATURDAY -> "dissabte"
        DayOfWeek.SUNDAY -> "diumenge"
    }

    private fun LocalDate.cursActual() =
        if (month > Month.AUGUST) "${year}-${(year + 1).toString().substring(2, 4)}" else "${year - 1}-${
            year.toString().substring(2, 4)
        }"

    private fun isDiaDeClasse(
        data: LocalDate = dataInici,
        horesDl: Int = 0,
        horesDm: Int = 0,
        horesDx: Int = 0,
        horesDj: Int = 0,
        horesDv: Int = 0,
    ): Boolean {

        // Caps de setmana i dies no lectius
        if (data.noHiHaDocencia()) return false

        when (data.dayOfWeek) {
            DayOfWeek.MONDAY -> if (horesDl != 0) return true
            DayOfWeek.TUESDAY -> if (horesDm != 0) return true
            DayOfWeek.WEDNESDAY -> if (horesDx != 0) return true
            DayOfWeek.THURSDAY -> if (horesDj != 0) return true
            DayOfWeek.FRIDAY -> if (horesDv != 0) return true
        }
        return false
    }

    // Retrona un triple perque la data d'inici pot no ser lectiva per a aquesta uf
    private fun calculaIniciFinalUf(
        inici: LocalDate = dataInici,
        horesPerFer: Int,
        horesDl: Int = 0,
        horesDm: Int = 0,
        horesDx: Int = 0,
        horesDj: Int = 0,
        horesDv: Int = 0,
    ): Triple<LocalDate, LocalDate, Int> {
//        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        var dataActual = inici
        var totalHores = 0

        // busquen el primer dia de clase a partir de la data inici
        while (dataActual.noHiHaDocencia()) dataActual = dataActual.plusDays(1)
        var dataRealInici = dataActual

        do {
            when (dataRealInici.dayOfWeek) {
                DayOfWeek.MONDAY -> if (horesDl != 0) break
                DayOfWeek.TUESDAY -> if (horesDm != 0) break
                DayOfWeek.WEDNESDAY -> if (horesDx != 0) break
                DayOfWeek.THURSDAY -> if (horesDj != 0) break
                DayOfWeek.FRIDAY -> if (horesDv != 0) break
            }
            dataRealInici = dataRealInici.plusDays(1)
        } while (dataRealInici.isBefore(dataFinal))

        while (totalHores < horesPerFer) {

            while (dataActual.noHiHaDocencia())
                dataActual = dataActual.plusDays(1)

            when (dataActual.dayOfWeek) {
                DayOfWeek.MONDAY -> totalHores += horesDl
                DayOfWeek.TUESDAY -> totalHores += horesDm
                DayOfWeek.WEDNESDAY -> totalHores += horesDx
                DayOfWeek.THURSDAY -> totalHores += horesDj
                DayOfWeek.FRIDAY -> totalHores += horesDv
            }
            dataActual = dataActual.plusDays(1)

        }
        // data inici, data final, total hores acumulades
        return Triple(dataRealInici, dataActual.minusDays(1), totalHores)
    }

    private fun calculaDatesInicialsFinal(schedule: Schedule): Unit {

        schedule.scheduleEntries.forEach { entry ->
            var di: LocalDate = dataInici
            var df: LocalDate = dataInici
//            val cicle = entry.cycle
            val day = entry.day
//            val pack = entry.pack
            val ufs = entry.ufs
            var horesAcumulades = 0
            ufs.forEach { uf ->
                val hours = uf.hours - horesAcumulades
                val triple = calculaIniciFinalUf(di,
                    hours,
                    day.monday,
                    day.tuesday,
                    day.wednesday,
                    day.thursday,
                    day.friday)
                df = triple.second
                uf.dataInici = triple.first
                uf.dataFinal = df
                // s'han sumat les hores exactes, cal que el dia d'inici sigui el segunt disponible
                if (triple.third == hours) {
                    di = df.plusDays(1)
                    horesAcumulades = 0
                }
                // sobren hores, el dia final de la uf passada és el dia d'inici de la seguent
                // cal tenir en compte les hores de més
                else {
                    di = df
                    horesAcumulades = triple.third - hours
                }

                if (uf.controls > 0) {
                    val period = Period.between(uf.dataInici, uf.dataFinal)
                    val dies = period.months * 30 + period.days
                    val parts = dies / uf.controls
                    uf.llistaControls = mutableListOf()

                    for (i in 1..uf.controls) {
                        var dataControl = uf.dataInici.plusDays(parts.toLong() * i)
                        while (!isDiaDeClasse(
                                dataControl,
                                entry.day.monday,
                                entry.day.tuesday,
                                entry.day.wednesday,
                                entry.day.thursday,
                                entry.day.friday)
                        ) {
                            dataControl = dataControl.plusDays(1)
                        }
                        // si es l'ultim control un dia abans de l'ultim dia de classe
                        if (i == uf.controls) {
                            dataControl = uf.dataFinal.minusDays(1)
                            while (!isDiaDeClasse(
                                    dataControl,
                                    entry.day.monday,
                                    entry.day.tuesday,
                                    entry.day.wednesday,
                                    entry.day.thursday,
                                    entry.day.friday)
                            ) {
                                dataControl = dataControl.minusDays(1)
                            }
                        }
//                        if (dataControl.isAfter(uf.dataFinal)) {
//                            dataControl = uf.dataFinal
//                        }
                        uf.llistaControls.add(dataControl)
                    }
                }
            }
        }
    }

    private fun parseHorari(): Schedule {

        val path = Paths.get(horariJson)
        if (!Files.exists(path)) {
            throw IllegalArgumentException("Missing fitxer $horariJson!!!")
        }
        val gson = GsonBuilder().apply {
            setPrettyPrinting()
        }.create()
        val jsonString: String = String(Files.readAllBytes(path))
        // json as array should be:
//        val sType = object : TypeToken<List<Schedule>>() { }.type
//        val schedule = gson.fromJson<List<Schedule>>(jsonString, sType)
//        val sType = object : TypeToken<List<Schedule>>() { }.type
        return gson.fromJson(jsonString, Schedule::class.java)
    }

    private fun writeSchedule(schedule: Schedule): Unit {
        val buffer = StringBuilder()

        buffer.append("CURS ${LocalDate.now().cursActual()}\n").append("============\n\n")
        buffer.append("Docent: ${
            horariJson.substring(horariJson.indexOf("_") + 1, horariJson.indexOf(".")).capitalize()
        }\n\n")
        schedule.scheduleEntries.forEach { entry ->
            buffer.append(entry.toString()).append("\n\n")
            var i = 1
            var horesPack = 0
            entry.ufs.forEach { uf ->
                val period = Period.between(uf.dataInici, uf.dataFinal)
                horesPack += uf.hours
                buffer.append("${i++}. ").append(uf.toString()).append("\n")
                buffer.append("\tDe ${uf.dataInici.diaDeLaSetmana()}, ${formatter.format(uf.dataInici)} a ${uf.dataFinal.diaDeLaSetmana()}, ${
                    formatter.format(uf.dataFinal)
                }").append(""" (${period})""").append("\n\n")
                // dates controls
                if (uf.controls > 0) {
                    for ((index, control) in uf.llistaControls.withIndex()) {
                        buffer.append("\tControl ${index + 1}. ${control.diaDeLaSetmana()}, ${formatter.format(control)}\n")
                    }
                    buffer.append("\n")
                }
                // Ens passem de rosca
                if (uf.dataFinal.isAfter(dataFinal)) {
                    val periode = dataFinal.until(uf.dataFinal)
                    buffer.append("*** ${entry.cycle}: la ${uf.name} de ${uf.module} acaba ${periode.days} dies després del final de les classes lectives ${
                        formatter.format(dataFinal)
                    } ***").append("\n")
                }
            }
            buffer.append("*** Total ${horesPack} hores, de ${entry.ufs[0].dataInici.diaDeLaSetmana()}, ${
                formatter.format(entry.ufs[0].dataInici)
            } a ${entry.ufs[entry.ufs.size - 1].dataFinal.diaDeLaSetmana()}, ${formatter.format(entry.ufs[entry.ufs.size - 1].dataFinal)} (${
                Period.between(entry.ufs[0].dataInici,
                    entry.ufs[entry.ufs.size - 1].dataFinal)
            }) ***\n\n")
            buffer.append("******\n\n")
        }
        buffer.append("[EOF]\n")
        Files.deleteIfExists(Paths.get(output))
        val path = Files.createFile(Paths.get(output))
        Files.write(path, buffer.toString().toByteArray())
    }

}

// ***** DSL *****

// init is a function type with receiver
suspend fun nouCalendariEscolar(init: CalendariEscolar.() -> Unit): CalendariEscolar {
    val calendariEscolar = CalendariEscolar
    calendariEscolar.init()
    return calendariEscolar
}

// ***** DSL *****

// Dates are in format dd-MM-yyyy
fun main(args: Array<String>): Unit = runBlocking {
    launch {
        nouCalendariEscolar {
            dataIniciStr = "20-09-2021"
            dataFinalStr = "07-06-2022"
            horariJson = "horari_pep.json"
        }.build()
    }
    println("Please wait...")
}
