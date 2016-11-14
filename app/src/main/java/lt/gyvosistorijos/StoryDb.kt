package lt.gyvosistorijos

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.*

class StoryDb(context: Context) :
        SQLiteOpenHelper(context, "story.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(Story.CREATE_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE " + Story.TABLE_NAME)
        onCreate(db)
    }


    fun insert(story: Story) {
        writableDatabase.insert(Story.TABLE_NAME, null,
                Story.FACTORY.marshal(story).asContentValues())
    }

    fun getAll(): List<Story> {
        val result = ArrayList<Story>()
        readableDatabase.rawQuery(Story.SELECT_ALL, null).use {
            while (it.moveToNext()) {
                result.add(Story.MAPPER.map(it))
            }
        }
        return result
    }
}
