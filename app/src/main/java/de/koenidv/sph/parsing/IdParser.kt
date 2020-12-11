package de.koenidv.sph.parsing

import android.annotation.SuppressLint
import de.koenidv.sph.objects.Change
import java.text.SimpleDateFormat
import java.util.*

const val TYPE_UNKNOWN = -1 // Unknown id type
const val TYPE_INTERNAL = 0 // Example: m_bar_1
const val TYPE_GMB = 1 // Example: M-GK-3
const val TYPE_SPH = 2 // Example: Q3Mbar01 - GYM
const val TYPE_NAMED = 3 // Example: Mathematik GK 13
const val TYPE_NUMBER = 4 // Example: 760

//  Created by koenidv on 06.12.2020.
class IdParser {

    /**
     * Parse a given external course id string to an internal course id
     * If there are two courses with the same subject and teacher, the internal id will be identical
     * todo: Different ids for courses with same subject and teacher
     * @param courseId External id string to parse to an internal id
     * @param courseIdType The id's type (must be one of TYPE_INTERNAL, TYPE_GMB, TYPE_SPH, TYPE_NAMED, TYPE_NUMBER): Nullable
     * @param teacherId The course teacher's id
     * @return Parsed internal id
     */
    fun getCourseId(courseId: String, courseIdType: Int?, teacherId: String): String {
        // Get id type estimate if no type is passed
        val idType = courseIdType ?: getCourseIdType(courseId)

        when (idType) {
            TYPE_INTERNAL -> return courseId
            TYPE_GMB -> {
                val classType: String
                if (courseId.contains("-"))
                    classType = courseId.substring(0, courseId.indexOf("-")).take(8)
                else
                    classType = courseId
                var index = 1
                // todo check for duplicate
                return classType + "_" + teacherId + "_" + index
            }
            else -> TODO("Parse IDs")
        }

    }

    /**
     * Estimates the type of an course id
     * @param courseId Id to check
     * @return An estimate of the id's type
     */
    fun getCourseIdType(courseId: String): Int {
        val patternNumber = "^\\d{1,4}\$".toRegex() // Number ID: 1-4 digits only
        val patternNamed = "^.*[^-0-9]\\s+.*\$".toRegex() // Named ID: Containing whitespace not following on digit or hyphen
        val patternSph = "^.*\\d\\s-\\s.*\$".toRegex() // SPH ID: Ending with at least one digit followed by " - " and non-digits
        val patternGmb = "^\\w{1,8}-\\w{1,2}-\\d{1,2}\$".toRegex() // GMB ID: 3 hyphen-separated sets: 1-6 chars, 1-2 chars, 1-2 digits
        val patternInternal = "^\\w{1,8}_\\w{1,4}_\\d{1,2}\$".toRegex() // Internal ID: 2 underscore-separated sets of 1-6 word characters and 1-2 digits in the end

        return when {
            patternNumber.matches(courseId) -> TYPE_NUMBER
            patternNamed.matches(courseId) -> TYPE_NAMED
            patternSph.matches(courseId) -> TYPE_SPH
            patternGmb.matches(courseId) -> TYPE_GMB
            patternInternal.matches(courseId) -> TYPE_INTERNAL
            else -> TYPE_UNKNOWN // Id does not match any type
        }
    }

    // todo documentation
    @SuppressLint("SimpleDateFormat")
    fun getChangeId(internalCourseId: String, date: Date, allChanges: List<Change>): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd")
        var index = 1

        // todo will not work, use allChanges.none { ... }
        while (allChanges.contains(internalCourseId + "_change" + formatter.format(date) + "_$index"))
            index++

        return internalCourseId + "_change" + formatter.format(date) + "_$index"
    }

}