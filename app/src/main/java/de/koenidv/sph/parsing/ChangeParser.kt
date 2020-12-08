package de.koenidv.sph.parsing

import android.annotation.SuppressLint
import android.util.Log
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.objects.Change
import java.text.SimpleDateFormat
import java.util.*

//  Created by koenidv on 08.12.2020.
class ChangeParser {

    // todo documentation
    @Suppress("LocalVariableName")
    @SuppressLint("DefaultLocale")
    fun parseChangesFromRaw(rawResponse: String): List<Change>? {
        val changes = mutableListOf<Change>()

        // todo check if response is valid
        // we could check the week type (a/b) here: <span class="badge woche">
        // todo get last refresh


        // Remove stuff that we don't need
        var rawContent = rawResponse.substring(rawResponse.indexOf("<div class=\"panel panel-primary\""))
        rawContent = rawContent.substring(0, rawContent.indexOf("<link"))
        // Remove newlines, we don't need them (But also isn't necessary atm and makes debugging horrible)
        // rawContent = rawContent.replace("\n", "").replace("\t", "")

        // For remembering where we left off :)
        var rawToday: String
        var rawChange: String
        var rawCell: String
        var dayInContent = 0
        var cellInRow: Int

        // For getting the date
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMAN)

        // We'll need those below to construct our Change object
        var date: Date
        var internalId: String
        var id_course_external : String? = ""
        var id_course_external_before: String?
        var lessons: List<Int>
        var type: String
        var className: String?
        var className_before: String?
        var id_teacher: String? = null
        var id_subsTeacher: String?
        var room: String?
        var description: String?

        // Extract every changes table, i.e. every available day
        while (rawContent.contains("<table class=\"table")) {
            // Get today's table only
            rawToday = rawContent.substring(rawContent.indexOf("<table class=\"table"), rawContent.indexOf("<div class=\"fixed-table-footer"))
            // Remove the extracted table from rawContent
            rawContent = rawContent.substring(rawContent.indexOf("<div class=\"fixed-table-footer") + 30)
            // Extract only the table's body
            // We don't need <thead> as long as the table order remains the same
            // This might vary for different schools
            rawToday = rawToday.substring(rawToday.indexOf("<tbody>") + 7, rawToday.indexOf("</tbody>"))

            // Parse date
            val dateIndex = ordinalIndexOf(rawContent, "Vertretungen am ", dayInContent) + 16
            date = dateFormat.parse(rawContent.substring(dateIndex, dateIndex + 10))!!

            // We are left with a table, each row contains these columns:
            // title (not needed), lessons (11 11 - 12), classname (Q34), old classname (Q34, mostly empty),
            // substitute teacher id (Bar, mostly empty), teacher id (Bar), type (EVA), course (M-GK-3),
            // old course (M-GK-3, mostly empty), room (M119), description

            // Get the change's data for every table row
            while (rawToday.contains("<tr")) {
                // Extract change from today's changes
                rawChange = rawToday.substring(rawToday.indexOf("<tr>") + 4, rawToday.indexOf("</tr>"))
                rawToday = rawToday.substring(rawToday.indexOf("</tr>") + 5)

                // Process every cell
                cellInRow = 0
                while (rawChange.contains("<td")) {
                    // Extract cell from table row
                    rawCell = rawChange.substring(rawChange.indexOf("<td"))
                    rawCell = rawCell.substring(rawCell.indexOf(">") + 1, rawCell.indexOf("</td>"))

                    when (cellInRow) {
                        0 -> {
                        } // Ignore first cell
                        1 -> { // Affected lessons
                            if (rawCell.contains(" - ")) {
                                // Get start and end lesson and put everything in between in a list
                                val fromLesson = Integer.getInteger(rawCell.substring(0, rawCell.indexOf(" ")))!!
                                val toLesson = Integer.getInteger(rawCell.substring(0, rawCell.indexOf(" ")))!!
                                lessons = (fromLesson..toLesson).toList()
                            } else {
                                lessons = listOf(Integer.getInteger(rawCell))
                            }
                        }
                        2 -> className = rawCell
                        3 -> className_before = rawCell
                        4 -> id_subsTeacher = rawCell.toLowerCase()
                        5 -> id_teacher = rawCell.toLowerCase()
                        6 -> type = rawCell.toUpperCase()
                        7 -> id_course_external = rawCell.toUpperCase()
                        8 -> id_course_external_before = rawCell.toUpperCase()
                        9 -> room = rawCell.toUpperCase()
                        10 -> description = rawCell
                    }
                    // Next cell
                    cellInRow++
                }


                /*

                TODO Do not use change ids with course and teacher, change might not have one
                 Use rolling change ids instead, guess course ids (from timetable) and search for favorite course

                if ()

                // Create Change with the extracted values and add it to the list
                if (id_teacher != null) {
                    internalId = IdParser().getCourseId(id_course_external, TYPE_GMB, id_teacher)
                } else {
                    // todo find course by external id. Must have seen it before, maybe full timetable
                    internalId = ""
                }

                    changes.add(Change(
                            changeId = IdParser().getChangeId(internalId, date, changes),

                    ))

                Log.d(SphPlanner.TAG, rawChange)*/
            }


            // This will crash if no course is specified
            // Might happen before holidays


            Log.d(SphPlanner.TAG, rawToday)
            // Next day
            dayInContent++
        }

        return changes
    }

    /**
     * Helper to get the nth index of a String in a String
     * @param str String to check
     * @param substr String to look for
     * @param n Ordinal of occurance (0 -> First)
     * @return Index of the nth occurence of a String or -1 if none is found
     */
    fun ordinalIndexOf(str: String, substr: String?, n: Int): Int {
        var n = n
        var pos = -1
        do {
            pos = str.indexOf(substr!!, pos + 1)
        } while (n-- > 0 && pos != -1)
        return pos
    }

}