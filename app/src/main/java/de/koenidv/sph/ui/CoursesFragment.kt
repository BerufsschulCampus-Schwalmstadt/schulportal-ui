package de.koenidv.sph.ui

//  Created by koenidv on 12.12.2020.
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import de.koenidv.sph.R
import de.koenidv.sph.database.DatabaseHelper

class CoursesFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        val demoString = StringBuilder()
        val favoriteCourses = DatabaseHelper.getInstance().favoriteCourses

        favoriteCourses.forEach { demoString.append(it.toString()).append("\n\n") }
        view.findViewById<TextView>(R.id.text_dashboard).text = demoString.toString()

        return view
    }
}