package cat.copernic.jmendezv

import java.time.LocalDate
import com.google.gson.annotations.SerializedName
import kotlin.text.StringBuilder

data class Day(
    @SerializedName("monday")
    var monday: Int = 0,
    @SerializedName("tuesday")
    var tuesday: Int = 0,
    @SerializedName("wednesday")
    var wednesday: Int = 0,
    @SerializedName("thursday")
    var thursday: Int = 0,
    @SerializedName("friday")
    var friday: Int = 0) {

    override fun toString(): String {
        val buffer = StringBuilder()
        if (monday > 0) buffer.append("dilluns ${monday}h. ")
        if (tuesday > 0) buffer.append("dimarts ${tuesday}h. ")
        if (wednesday > 0) buffer.append("dimecres ${wednesday}h. ")
        if (thursday > 0) buffer.append("dijous ${thursday}h. ")
        if (friday > 0) buffer.append("divendres ${friday}h. ")
        return buffer.toString()
    }

}

data class UF(@SerializedName("module") var module: String,
              @SerializedName("name") var name: String = "",
              @SerializedName("description") var description: String = "",
              @SerializedName("hours") var hours: Int = 0) {
    var dataInici: LocalDate = LocalDate.now()
    var dataFinal: LocalDate = LocalDate.now()

    override fun toString(): String {
        val buffer = StringBuilder()
        buffer.append("${module} ${name} '${description}' ${hours}h.")
        return buffer.toString()
    }
}

data class ScheduleEntry(@SerializedName("cycle") var cycle: String = "",
                         @SerializedName("pack") var pack: String = "",
                         @SerializedName("days") var day: Day = Day(),
                         @SerializedName("ufs") val ufs: List<UF> = listOf()) {
    override fun toString(): String {
        val buffer = StringBuilder()
        buffer.append("${cycle} ${pack} ${day}\n${ufs} ")
        return buffer.toString()
    }
}

data class Schedule(@SerializedName("schedule_entry") val scheduleEntries: List<ScheduleEntry> = listOf())
