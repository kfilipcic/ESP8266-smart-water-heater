package com.example.rijekasmarthomeapp

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class dataBaseHelpter (context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    val RULES_TABLE: String = "device_rules"
    val ID_COLUMN: String = "id"
    val DEVICE_NAME_COLUMN: String = "device_name"
    override fun onCreate(db: SQLiteDatabase?) {
        TODO("Not yet implemented")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        TODO("Not yet implemented")
    }
    companion object {
        val DATABASE_NAME: String = "smartHomeDB"
        val DATABASE_VERSION: Int = 1
    }
    /*val
    private static final String DATABASE_NAME = "studentdb";
    private static final int DATABASE_VERSION = 1;
    private static final String STUDENT_TABLE = "student";
    private static final String ID_COLUMN = "id";               // Primarni kljuc
    private static final String NAME_COLUMN = "name";
    private static final String SURNAME_COLUMN = "surname";
    private static final String DATE_COLUMN = "dateofbirth";
    private static final String RMA_COLUMN = "rmamark";*/
}
