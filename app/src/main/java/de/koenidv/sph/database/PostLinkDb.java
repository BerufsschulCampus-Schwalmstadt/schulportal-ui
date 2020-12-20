package de.koenidv.sph.database;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.koenidv.sph.objects.Post;
import de.koenidv.sph.objects.PostLink;
import de.koenidv.sph.objects.PostTask;

public class PostLinkDb {

    private final DatabaseHelper dbhelper = DatabaseHelper.getInstance();

    private static PostLinkDb instance;

    private PostLinkDb() {
    }

    public static PostLinkDb getInstance() {
        if (PostLinkDb.instance == null) {
            PostLinkDb.instance = new PostLinkDb();
        }
        return PostLinkDb.instance;
    }

    public void save(List<PostLink> postlinks) {
        for (PostLink postlink : postlinks) {
            save(postlink);
        }
    }

    public void save(PostLink postlink) {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        ContentValues cv = new ContentValues();

        // Put values into ContentValues
        cv.put("id_course", postlink.getId_course());
        cv.put("id_post", postlink.getId_post());
        cv.put("name",postlink.getName());
        cv.put("date", postlink.getDate().getTime() / 1000);
        cv.put("url", postlink.getUrl().toString());

        // Add or update post in db
        Cursor cursor = db.rawQuery("SELECT * FROM postlink WHERE post_id = '" + postlink.getId_post() + "'AND url = '" + postlink.getUrl() +"'" , null);
        if (cursor.getCount() == 0) {
            db.insert("postlink", null, cv);
        } else {
            db.update("poslink", cv, "post_id = '" + postlink.getId_post() + "'AND url = '" + postlink.getUrl() +"'", null);
        }
        cursor.close();
        db.close();
    }


    public List<PostLink> getByCourseId(String course_id) throws MalformedURLException {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postlist WHERE id_course = '" + course_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }

    public List<PostLink> getByPostId(String post_id) throws MalformedURLException {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postlist WHERE id_post = '" + post_id + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }

    public List<PostLink> getByDate(String date) throws MalformedURLException {
        final SQLiteDatabase db = dbhelper.getReadableDatabase();
        // Query posts
        String queryString = "SELECT * FROM postlist WHERE date = '" + date + "'";
        Cursor cursor = db.rawQuery(queryString, null);
        // Get posts with the cursor
        return getWithCursor(cursor, db);
    }




    private List<PostLink> getWithCursor(Cursor cursor, SQLiteDatabase db) throws MalformedURLException {
        List<PostLink> returnList = new ArrayList<>();
        if (cursor.moveToFirst()) {
            do {

                String id_course = cursor.getString(0);
                String id_post = cursor.getString(1);
                String name = cursor.getString(2);
                Date date = new Date(cursor.getInt(4) * 1000L);
                URL url = new URL(cursor.getString(5));

                PostLink newPostLink = new PostLink(id_course, id_post, name, date, url);

                returnList.add(newPostLink);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return returnList;
    }
}
