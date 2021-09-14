package cat.copernic.jmendezv

import com.google.gson.GsonBuilder
import java.nio.file.Files
import java.nio.file.Paths
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class CalendariEscolar(val dataIniciStr: String, val dataFinalStr: String,
val fitxerNotLectius: String, val horariJson: String, val output: String) {
    private val NO_LECTIUS = mutableListOf<LocalDate>()
    private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val dataInici: LocalDate = LocalDate.parse(dataIniciStr, formatter)
    val dataFinal: LocalDate = LocalDate.parse(dataFinalStr, formatter)

    init {
        carregaNoLectius()
        val schedule = parseHorari()
        calculaDatesInicialsFinal(schedule)
        writeSchedule(schedule, output)
    }

    // En principi es el mateix per tots els cicles
    private fun carregaNoLectius() {
        val path = Paths.get(fitxerNotLectius)
        if (!Files.exists(path)) {
            throw IllegalArgumentException("Missing fitxer $fitxerNotLectius!!!")
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
        while(dataActual.noHiHaDocencia()) dataActual = dataActual.plusDays(1)
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
        } while(dataRealInici.isBefore(dataFinal))

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
                // pair.first = data final, pair.second les hores reals que ha sumat, poden ser més de les necessàries
                if (triple.third > hours) {
//                    println("s'han adjudicat ${pair.second - hours} hores de mes" )
                }
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

    fun writeSchedule(schedule: Schedule, output: String): Unit {
        val buffer = StringBuilder()

        schedule.scheduleEntries.forEach { entry ->
            buffer.append(entry.toString()).append("\n")
            entry.ufs.forEach { uf ->
                buffer.append(uf.toString()).append("\n")
                buffer.append("De ${uf.dataInici.diaDeLaSetmana()} ${formatter.format(uf.dataInici)} fins ${uf.dataFinal.diaDeLaSetmana()} ${
                    formatter.format(uf.dataFinal)
                }").append("\n")
                if (uf.dataFinal.isAfter(dataFinal)) {
                    val periode = dataFinal.until(uf.dataFinal)
                    buffer.append("*** ${entry.cycle}: la ${uf.name} de ${uf.module} acaba ${periode.days} dies després del final de les classes lectives ${
                        formatter.format(dataFinal)
                    } ***").append("\n")
                }
            }
            buffer.append("******\n")
        }
        Files.deleteIfExists(Paths.get(output))
        val path = Files.createFile(Paths.get(output))
        Files.write(path, buffer.toString().toByteArray())
    }

}

fun main(args: Array<String>) {
    CalendariEscolar(
        "20/09/2021",
        "07/06/2022",
        "no_lectius.dat",
        "horari_pep.json",
        "horari_pep.txt")
    println("Horari generat")

}