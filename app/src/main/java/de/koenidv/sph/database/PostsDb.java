package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.Post;

public class PostsDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static PostsDb instance;

    private PostsDb() {
    }

    public static PostsDb getInstance() {
        if (PostsDb.instance == null) {
            PostsDb.instance = new PostsDb();
        }
        return PostsDb.instance;
    }

    public void save(@NotNull List<Post> posts) {
        for (Post post : posts) {
            save(post);
        }
    }

    public void save(@NotNull Post post) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        ContentValues cv = new ContentValues();

        // Put values into ContentValues
        cv.put("post_id", post.getPostId());
        cv.put("id_course", post.getId_course());
        cv.put("date", post.getDate().getTime() / 1000);
        cv.put("title", post.getTitle());
        if (post.getDescription() != null) cv.put("description", post.getDescription());
        cv.put("unread", post.getUnread());

        // Add or update post in db
        Cursor cursor = db.rawQuery("SELECT * FROM posts WHERE post_id = '" + post.getPostId() + "'", null);
        if (cursor.getCount() == 0) {
            db.insert("posts", null, cv);
        } else {
            db.update("posts", cv, "post_id = '" + post.getPostId() + "'", null);
        }
        cursor.close();
        db.close();
    }

    public List<Post> getAll() {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posts";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }


    public List<Post> getByCourseId(String course_id) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posts WHERE id_course = '" + course_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }

    public List<Post> getByIsUnread() {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM posts WHERE unread = 1";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get post objects and return them
        return getWithCursor(cursor, db);
    }

    private List<Post> getWithCursor(Cursor cursor, SQLiteDatabase db) {
        List<Post> returnList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {
                String postId = cursor.getString(0);
                String id_course = cursor.getString(1);
                Date date = new Date(cursor.getInt(2) * 1000L);
                String title = cursor.getString(3);
                String description = cursor.getString(4);
                boolean unread = cursor.getInt(5) == 1;

                Post newPost = new Post(postId, id_course, date, title, description, unread);

                returnList.add(newPost);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }

    /**
     * Clears all posts from posts db
     */
    public void clear() {
        SQLiteDatabase db = dbhelper.getWritableDatabase();
        db.delete("posts", null, null);
    }

}
