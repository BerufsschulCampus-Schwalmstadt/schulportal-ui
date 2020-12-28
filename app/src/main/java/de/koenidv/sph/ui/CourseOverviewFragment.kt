package de.koenidv.sph.ui

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.Navigation
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import de.koenidv.sph.R
import de.koenidv.sph.SphPlanner
import de.koenidv.sph.adapters.AttachmentsAdapter
import de.koenidv.sph.adapters.PostsAdapter
import de.koenidv.sph.database.*
import de.koenidv.sph.networking.AttachmentManager
import de.koenidv.sph.objects.Course
import me.saket.bettermovementmethod.BetterLinkMovementMethod


// Created by koenidv on 18.12.2020.
class CourseOverviewFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_course_overview, container, false)
        val prefs: SharedPreferences = SphPlanner.applicationContext().getSharedPreferences("sharedPrefs", Context.MODE_PRIVATE)

        val pinsTitleText = view.findViewById<TextView>(R.id.pinsTitleTextView)
        val pinsRecycler = view.findViewById<RecyclerView>(R.id.pinsRecycler)
        val postsTitleText = view.findViewById<TextView>(R.id.postsTitleTextView)
        val postsRecycler = view.findViewById<RecyclerView>(R.id.postsRecycler)
        val postsLoading = view.findViewById<ProgressBar>(R.id.postsLoading)
        val loadMorePostsButton = view.findViewById<MaterialButton>(R.id.loadMorePostsButton)

        // Get passed course id argument
        val courseId = arguments?.getString("courseId") ?: ""
        val course: Course? = CoursesDb.getInstance().getByInternalId(courseId)

        // Set action bar title
        val appendix = if (course?.isLK == true) getString(R.string.course_appendix_lk) else ""
        (activity as AppCompatActivity).supportActionBar?.title = course?.fullname + appendix
        // Set open in browser url
        SphPlanner.openInBrowserUrl = getString(R.string.url_course_overview).replace("%numberid", course?.number_id.toString())

        // Get data
        val posts = PostsDb.getInstance().getByCourseId(courseId)
        val tasks = PostTasksDb.getInstance().getByCourseId(courseId)
        // Get file attachments..
        val attachments = FileAttachmentsDb.getInstance().getByCourseId(courseId).toMutableList()
        val pins = FileAttachmentsDb.getInstance().getPinsByCourseId(courseId).toMutableList()
        // ..and link attachments
        if (prefs.getBoolean("links_in_post_attachments", true))
            attachments.addAll(LinkAttachmentsDb.getInstance().getByCourseId(courseId))
        pins.addAll(LinkAttachmentsDb.getInstance().getPinsByCourseId(courseId))
        // We need to reorder pins as files and links should be mixed, according to their last use
        pins.sortByDescending { it.lastuse() }

        /*
         * Pinned attachments recycler
         */

        if (!pins.isNullOrEmpty()) {
            pinsRecycler.adapter = AttachmentsAdapter(pins,
                    AttachmentManager().onAttachmentClick(requireActivity()),
                    AttachmentManager().onAttachmentLongClick(requireActivity()))
            PagerSnapHelper().attachToRecyclerView(pinsRecycler)
        } else {
            // Hide recycler and title if there are no pinned attachments
            pinsTitleText.visibility = View.GONE
            pinsRecycler.visibility = View.GONE
        }


        /*
         * Posts recycler
         * Set up with first 2 posts only
         * Then load more on button click
         */

        if (posts.isNotEmpty()) {
            val postsToShow = posts.take(2).toMutableList()
            val taskstoShow = tasks.filter { it.id_post == postsToShow[0].postId || it.id_post == postsToShow.getOrNull(1)?.postId }.toMutableList()
            val filesToShow = attachments.filter { it.postId() == postsToShow[0].postId || it.postId() == postsToShow.getOrNull(1)?.postId }.toMutableList()

            // Movement method to open links in-app
            val movementMethod = BetterLinkMovementMethod.newInstance()
            movementMethod.setOnLinkClickListener { _, url ->
                if (prefs.getBoolean("open_links_inapp", true)) {
                    // Open WebViewFragment with respective url on click
                    Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                            .navigate(R.id.webviewFromPostsAction, bundleOf("url" to url))
                    true
                } else false
            }
            // todo on long click, attachment bottom sheet

            val postsAdapter = PostsAdapter(
                    postsToShow,
                    taskstoShow,
                    filesToShow,
                    movementMethod,
                    AttachmentManager().onAttachmentClick(requireActivity()),
                    AttachmentManager().onAttachmentLongClick(requireActivity())
            )
            postsRecycler.adapter = postsAdapter

            loadMorePostsButton.setOnClickListener {
                // Load all posts
                // Replace posts adapter data with all posts
                postsToShow.clear()
                postsToShow.addAll(posts)
                taskstoShow.clear()
                taskstoShow.addAll(tasks)
                filesToShow.clear()
                filesToShow.addAll(attachments)
                // Show loading symbol and hide button
                postsLoading.visibility = View.VISIBLE
                loadMorePostsButton.visibility = View.GONE
                // Update RecyclerView and hide ProgessBar
                // todo only notify for updated items
                postsAdapter.notifyDataSetChanged()
                postsLoading.visibility = View.GONE
                // Mark all posts as read
                PostsDb.getInstance().markAsRead(courseId)
            }

            // Mark displayed posts as read
            PostsDb.getInstance().markAsRead(postsToShow[0].postId, postsToShow.getOrNull(1)?.postId)

            // Hide load more button if there are no further posts
            if (posts.size == 2) {
                loadMorePostsButton.visibility = View.GONE
            }
        } else {
            postsTitleText.text = getString(R.string.posts_no_data)
            postsRecycler.visibility = View.GONE
            loadMorePostsButton.visibility = View.GONE
        }

        return view
    }
}