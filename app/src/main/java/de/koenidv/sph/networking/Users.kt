package de.koenidv.sph.networking

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.os.bundleOf
import com.androidnetworking.AndroidNetworking
import com.androidnetworking.error.ANError
import com.androidnetworking.interfaces.JSONObjectRequestListener
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.database.CoursesDb
import de.koenidv.sph.database.UsersDb
import de.koenidv.sph.debugging.DebugLog
import de.koenidv.sph.debugging.Debugger
import de.koenidv.sph.objects.User
import org.json.JSONObject
import java.util.*

//  Created by koenidv on 31.01.2021.
class Users {

    /**
     * Load users, filter for teachers and save to db
     * This will use sph's message recipient search function
     * to get every recipient for each character.
     * This might include other students, but fortunately we should
     * be able to just filter for type=lul
     */
    fun fetch(callback: (success: Int) -> Unit) {
        // Log fetching users
        DebugLog("Users", "Fetching users")

        // We need to make sure that we have an access token
        TokenManager.getToken { success, _ ->
            // If getting a token failed, call onComplete
            // with the error and return
            if (success != NetworkManager.SUCCESS) {
                callback(success)
                return@getToken
            }

            DebugLog("Users", "Fetching users - Success status " + success.toString())

            val users = mutableListOf<User>()
            val favoriteTeachers = CoursesDb.getFavoriteTeacherIds()

            // Iterate through every single char
            var char = 'a'
            var completed = 0

            // Lambda function to execute for each completed network request
            val onDone: (Int, List<User>) -> Unit = { mSuccess, mUsers ->
                DebugLog("Users", "Fetching users - mSuccess status" + mSuccess.toString())
                if (mSuccess == NetworkManager.SUCCESS) {

                    // Add found users to the list
                    for (user in mUsers) {
                        // If this user is not yet in the list, add it
                        if (users.indexOfFirst { it.userId == user.userId } == -1) {
                            users.add(user)
                        }
                    }

                    // We could check for char = z here, but that'd break if one
                    // request takes longer than the previous
                    completed++
                    if (completed >= 26) {
                        // If this was the last request, save the user list
                        // and call back success
                        UsersDb.save(users)
                        callback(NetworkManager.SUCCESS)

                        // Log success
                            DebugLog("Users", "Loaded users",
                                    bundleOf("usersCount" to users.size),
                                    type = Debugger.LOG_TYPE_SUCCESS)
                    }
                } else {
                    callback(mSuccess)
                }
            }

            while (char <= 'z') {
                // Use empty list, we want all found users, known or not
                loadUsersForQuery(char.toString(), listOf(), favoriteTeachers, onDone)
                // Use the next char
                char++
            }
            //StKl:03.12.2021:Das callback hier eingefuegt, da kein ordentiches Ende ueber den fetch Einstieg gefunden wurde
            //ToDo - Chech correct position of this callback
            DebugLog("Users", "Users - After while - My callback is comming")
            callback(success)
        }
        DebugLog("Users", "User fetch - Token finishes")
        //StKl:10.12.2021:Das callback hier am Ende von try eingefuegt
        DebugLog("Onboarding", "BlueScreen? Users Fetch")
        callback(NetworkManager.SUCCESS)//Returns unchanged success
    }

    /**
     * Load all new users for a given query
     */
    fun loadUsersForQuery(query: String,
                          currentUsers: List<User> = UsersDb.all(),
                          favoriteTeachers: List<String> = CoursesDb.getFavoriteTeacherIds(),
                          callback: (Int, List<User>) -> Unit) {
        // Log fetching users
        DebugLog("Users", "Loading users for $query")

        TokenManager.getToken { success, _ ->
            if (success != NetworkManager.SUCCESS) {
                callback(success, listOf())
                return@getToken
            }
            DebugLog("Users", "LoadUserQuery - Step 1")
            // Not get the all recipients for the current character
            AndroidNetworking.post(SphPlanner.appContext().getString(R.string.url_messages))
                    .addBodyParameter("a", "searchRecipt")
                    .addBodyParameter("q", query)
                    .build()
                    .getAsJSONObject(object : JSONObjectRequestListener {
                        override fun onResponse(response: JSONObject) {
                            // Check if response contains an "items" array
                            if (response.isNull("items")) return

                            DebugLog("Users", "LoadUserQuery - Step 2")

                            // If response is valid, get the users array
                            var index = 0
                            val items = response.getJSONArray("items")
                            var currentItem: JSONObject
                            var text: String
                            var firstname: String?
                            var lastname: String?
                            var teacherId: String?
                            val returnUsers = mutableListOf<User>()

                            // Get every item from the array
                            while (index < response.getInt("total_count")) {
                                currentItem = items.getJSONObject(index)
                                DebugLog("Users", "LoadUserQuery - Step 3")
                                // If list doesn't contain this user yet
                                // Also, only get teachers at this time (type=lul)
                                if (currentUsers.indexOfFirst {
                                            it.userId == currentItem.getString("id")
                                        } == -1
                                        && currentItem.getString("type") == "lul") {
                                    // We'll only use names with "," and "(..)"
                                    // This way we'll ignore admin entries
                                    // However, this will not add anything if a school does not
                                    // show a teacher's first name or shorthand
                                    DebugLog("Users", "LoadUserQuery - Step 4")
                                    text = currentItem.getString("text")

                                    if (text.contains(",") &&
                                            text.contains("""\(.*\)""".toRegex())) {

                                        // Get the name and shorthand
                                        lastname = text.substringBefore(",")
                                        firstname = text
                                                .substringAfter(", ")
                                                .substringBefore(" (")
                                        teacherId = text
                                                .substringAfterLast("(")
                                                .substringBefore(")")
                                                .toLowerCase(Locale.ROOT)

                                        // Add it to the list
                                        returnUsers.add(User(
                                                userId = currentItem.getString("id"),
                                                teacherId = teacherId,
                                                firstname = firstname,
                                                lastname = lastname,
                                                type = currentItem.getString("type"),
                                                favoriteTeachers.contains(teacherId)
                                        ))
                                    }
                                }

                                index++ // Next user for this result
                            }

                            DebugLog("Users", "Returned user list index: " + index.toString())
                            DebugLog("Users", "Returned user list: $query")
                            // Call back with the found users
                            callback(NetworkManager.SUCCESS, returnUsers)


                        }

                        override fun onError(error: ANError) {
                            // Handle request errors

                            // Log error
                            DebugLog("Users", "Error loading users", error)
                            Log.d(SphPlanner.TAG, error.errorDetail)

                            when (error.errorDetail) {
                                "connectionError" -> {
                                    callback(NetworkManager.FAILED_NO_NETWORK, listOf())
                                }
                                "requestCancelledError" -> {
                                    callback(NetworkManager.FAILED_CANCELLED, listOf())
                                }
                                else -> {
                                    callback(NetworkManager.FAILED_UNKNOWN, listOf())
                                }
                            }
                        }
                    })
            DebugLog("Users", "Not get the all recipients for the current character")
        }
        DebugLog("Users", "Token Manager finishes")
    }

    /**
     * Add a teacher using their id and username (lastname, firstname (abbr))
     */
    fun addTeacherFromMessage(id: String, username: String) {
        val matches = Regex("""(.*),\s(.*)\s\((.*)\)""").find(username)?.groupValues

        if (matches != null && matches.size > 1) {
            UsersDb.save(listOf(User(
                    "l-$id",
                    matches[3],
                    matches[2],
                    matches[1],
                    "lul",
                    false // todo check if teacher should be pinned
            )))
        }
    }

    /**
     * Find a user's id by their username (lastname, firstname (abbr))
     */
    fun getTeacherUserId(username: String): String? {
        val matches = Regex("""(.*),\s(.*)\s\((.*)\)""").find(username)?.groupValues

        return if (matches != null && matches.size > 1) {
            UsersDb.getTeacherUserId(matches[2], matches[1], matches[3])
        } else {
            null
        }
    }


    /**
     * Send en email to an adress based upon the template in SharedPrefs
     */
    fun sendEmail(user: User) {
        // Get the template
        var template = SphPlanner.appContext()
                .getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)
                .getString("users_mail_template", "")!!

        // Replace placholders with data for firstname: fname(n) for first n chars
        template = Regex("""#fname\((\d)\)""").replace(template, transform = { match ->
            user.firstname.toString()
                    .take(match.groupValues[1].toInt())
                    .toLowerCase(Locale.ROOT)
        })
        // lastname: lname(n) for first n chars
        template = Regex("""#lname\((\d)\)""").replace(template, transform = { match ->
            user.lastname.toString()
                    .take(match.groupValues[1].toInt())
                    .toLowerCase(Locale.ROOT)
        })
        // Full names, abbreviation and id
        template = template.replace("#firstname", user.firstname.toString().toLowerCase(Locale.ROOT))
                .replace("#lastname", user.lastname.toString().toLowerCase(Locale.ROOT))
                .replace("#abbr", user.teacherId.toString().toLowerCase(Locale.ROOT))
                .replace("#id", user.userId.replace("l-", ""))

        // Now start an intent to send an email to that address
        val emailIntent = Intent.createChooser(Intent(Intent.ACTION_SENDTO)
                .apply {
                    data = Uri.parse("mailto:$template")
                },
                SphPlanner.appContext().getString(R.string.users_email_title)
                        .replace("%firstname", user.firstname.toString())
                        .replace("%lastname", user.lastname.toString()))
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

        SphPlanner.appContext().startActivity(emailIntent)
    }

}