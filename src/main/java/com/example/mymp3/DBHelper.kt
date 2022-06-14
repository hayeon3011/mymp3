package com.example.mymp3

import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

//SQLiteOpenHelper 상속받고 ,dbName 정할 것
//데이터베이스명은 DBHelper() 결정 됨
//데이터베이스 파일이 있으면 onCreate() 부르지 않음
class DBHelper(context: Context,dbName: String, version: Int): SQLiteOpenHelper(context,dbName,null,version) {
    companion object{
        val TABLE_NAME = "musicTBL"
    }
    //DBHelper 생성시 최초 한번만 실행 : DB 명 부여 시점
    //테이블 생성
    override fun onCreate(db: SQLiteDatabase?) {
        //테이블설계
        //db.execSQL() -> 테이블에 변화를 주면 사용 : Insert, update, delete , drop , create
        //db.rawQuery() -> 테이블 변화없이 정보를 가져와야 할 떄 사용 : select -> Cusor
        //id: String, title: String?, artist:String?, albumId:String?, duration:Long?, love:Int?
        val createQuery = "create table ${TABLE_NAME}(id TEXT primary key, title TEXT, artist Text, albumId TEXT, duration INTEGER, love INTEGER)".trimIndent()
        db?.execSQL(createQuery)
    }

    //DB 최초 생성 후 버전 변경시 실행
    //테이블 제거
    override fun onUpgrade(db: SQLiteDatabase?, newVersion: Int, oldVersion: Int) {
        //테이블제거
        val dropQuery = "drop table $TABLE_NAME".trimIndent()
        db?.execSQL(dropQuery)
        this.onCreate(db)
    }

    //음악삽입(한 곡 정보를 입력해 주는 것)
    fun insertMusic(music: Music): Boolean{
        var insertFlag = false
        val insertQuery = "insert into ${TABLE_NAME}(id, title, artist, albumId, duration, love) " +
                "values('${music.id}','${music.title}','${music.artist}','${music.albumId}',${music.duration},${music.love})".trimIndent()
        //db는 SQLiteDatabase 가져오는 방법 2가지 있는데 - writableDatabase & readableDatabase
        val db = this.writableDatabase
        try{
            db.execSQL(insertQuery)
            insertFlag = true
        }catch (e: SQLException){
            Log.d("sophia","${e.printStackTrace()}")
        }finally {
            db.close()
        }
        return insertFlag
    }

    //선택 (전체 음악정보 레코드를 가져오는 것)
    fun selectMusicAll(): MutableList<Music>?{
        var musicList: MutableList<Music>? = mutableListOf<Music>()
        var cursor: Cursor? = null

        val selectQuery = "select * from $TABLE_NAME".trimIndent()
        val db = this.readableDatabase

        try{
            cursor = db.rawQuery(selectQuery,null)
            if(cursor.count > 0){
                while (cursor.moveToNext()){
                    val id = cursor.getString(0)
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val albumId = cursor.getString(3)
                    val duration = cursor.getLong(4)
                    val love = cursor.getInt(5)
                    val music = Music(id,title, artist, albumId, duration, love)
                    musicList?.add(music)
                }
            }else{
                musicList = null
            }
        }catch (e:Exception){
            Log.d("sophia","${e.printStackTrace()}")
            musicList = null
        }finally {
            cursor?.close()
            db.close()
        }
        return musicList
    }

    //선택 : 조건에 맞는 선택(id를 주면 Music을 주는 것)
    fun selectMusic(id: String?): Music? {
        var music: Music? = null
        var cursor: Cursor? = null

        val selectQuery = "select * from $TABLE_NAME where id = '${id}'".trimIndent()
        val db = this.readableDatabase

        cursor = db.rawQuery(selectQuery, null)

        try {
            if(cursor.count > 0){
                if(cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val albumId = cursor.getString(3)
                    val duration = cursor.getLong(4)
                    val love = cursor.getInt(5)
                    music = Music(id, title, artist, albumId, duration, love)
                }
            }
        }catch (e: Exception){
            Log.d("sophia","${e.printStackTrace()}")
            music = null
        }finally {
            cursor.close()
            db.close()
        }
        return music
    }

    //노래 즐겨찾기 db구현 (좋아요 선택하면 저장하는 방식)
    fun updateLove(music: Music):Boolean{
        var updateLoveFlag = false
        //music id를 찾아서 love로선택하라.
        val updateQuery = "UPDATE ${TABLE_NAME} SET love = '${music.love}' WHERE id ='${music.id}'".trimIndent()
        val db = this.readableDatabase

        try{
            db.execSQL(updateQuery)
            updateLoveFlag = true
        }catch (e:SQLException){
            Log.d("sophia","${e.printStackTrace()}")
        }finally {
            db.close()
        }
        return updateLoveFlag
    }

    //좋아요 선택한 음악을 모두 찾아서 가져오는 것
    fun selectLove(): MutableList<Music>?{
        var musicList : MutableList<Music>? = mutableListOf()
        var cursor: Cursor? = null

        val searchQuery ="select * from ${TABLE_NAME} where love = 1".trimIndent()
        val db = this.readableDatabase

        cursor = db.rawQuery(searchQuery,null)

        try {
            if(cursor.count > 0){
                if(cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val albumId = cursor.getString(3)
                    val duration = cursor.getLong(4)
                    val love = cursor.getInt(5)

                    musicList?.add(Music(id, title, artist, albumId, duration, love))
                }
            }
        }catch (e: Exception){
            Log.d("sophia","${e.printStackTrace()}")
            musicList = null
        }finally {
            cursor?.close()
            db.close()
        }
        return musicList
    }

    //노래를 검색해서 해당되는 정보를 찾아서 리턴하는 함수
    fun searchMusic(query: String?): MutableList<Music>?{
        var musicList: MutableList<Music>?= mutableListOf()
        var cursor: Cursor? = null

        val searchQuery = "select * from ${TABLE_NAME} where artist love '${query}%' or title love '${query}%".trimIndent()
        val db = this.readableDatabase

        cursor = db.rawQuery(searchQuery, null)

        try {
            if(cursor.count > 0){
                if(cursor.moveToNext()) {
                    val id = cursor.getString(0)
                    val title = cursor.getString(1)
                    val artist = cursor.getString(2)
                    val albumId = cursor.getString(3)
                    val duration = cursor.getLong(4)
                    val love = cursor.getInt(5)

                    musicList?.add(Music(id, title, artist, albumId, duration, love))
                }
            }
        }catch (e: Exception){
            Log.d("sophia","${e.printStackTrace()}")
            musicList = null
        }finally {
            cursor?.close()
            db.close()
        }
        return musicList

    }

}
