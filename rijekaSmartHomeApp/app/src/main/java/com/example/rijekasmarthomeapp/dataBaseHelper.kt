package com.example.rijekasmarthomeapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class dataBaseHelpter (context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    val LOG_TABLE: String = "power_on_logs"
    val ID_COLUMN: String = "id"
    val DEVICE_NAME_COLUMN: String = "device_name"
    val TEMP_VALUE: String = "temperature"
    val TIME: String = "time"

    private val SQL_CREATE_QUERIES =
        "CREATE TABLE $LOG_TABLE (" +
                "$ID_COLUMN INTEGER PRIMARY KEY," +
                "$DEVICE_NAME_COLUMN TEXT," +
                "$TEMP_VALUE TEXT," +
                "$TIME INTEGER)"

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(SQL_CREATE_QUERIES)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        if (db != null) {
            db.execSQL("DROP TABLE IF EXISTS contacts")
        };
        onCreate(db);
    }

    fun insert(deviceName: String, tempValue: String, timeValue: Long): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put("device_name", deviceName)
        contentValues.put("temperature", tempValue)
        contentValues.put("time", timeValue)
        db.insert("power_on_logs", null, contentValues)
        return true
    }

    fun getData(deviceName: String): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $LOG_TABLE WHERE $DEVICE_NAME_COLUMN LIKE '$deviceName'", null)
    }

    fun getAvgTempData(deviceName: String): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT avg(NULLIF($TEMP_VALUE, '')) FROM $LOG_TABLE WHERE $DEVICE_NAME_COLUMN LIKE '$deviceName'", null)
    }

    fun getAllData(): Cursor {
       val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $LOG_TABLE", null)
    }

    fun deleteAllData() {
        val db = this.writableDatabase
        db.execSQL("DELETE FROM $LOG_TABLE")
    }

    companion object {
        const val DATABASE_NAME: String = "smartHomeDB"
        const val DATABASE_VERSION: Int = 1
    }
}
