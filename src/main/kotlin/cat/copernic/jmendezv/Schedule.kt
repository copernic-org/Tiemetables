package cat.copernic.jmendezv

import java.time.LocalDate
import com.google.gson.annotations.SerializedName

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
    var friday: Int = 0)

data class UF(@SerializedName("module") var module: String,
              @SerializedName("name") var name: String = "",
              @SerializedName("description") var description: String = "",
              @SerializedName("hours") var hours: Int = 0) {
    var dataInici: LocalDate = LocalDate.now()
    var dataFinal: LocalDate = LocalDate.now()
}

data class ScheduleEntry(@SerializedName("cycle") var cycle: String = "",
                         @SerializedName("pack") var pack: String = "",
                         @SerializedName("days") var day: Day = Day(),
                         @SerializedName("ufs") val ufs: List<UF> = listOf())


data class Schedule(@SerializedName("schedule_entry") val scheduleEntries: List<ScheduleEntry> = listOf())
