package com.example.mymp3

//import androidx.appcompat.widget.SearchView
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mymp3.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    //데이터베이스 객체화에 들어가는 db이름과 버전 상수화
    companion object {
        val DB_NAME = "musicDB"
        val VERSION = 1
    }

    lateinit var binding: ActivityMainBinding

    //승인받아야 할 항목 퍼미션 요청
    val permission = arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)

    //승인요청횟수
    val REQUEST_READ = 20

    // 데이터베이스 객체화
    val dbHelper: DBHelper by lazy { DBHelper(this, DB_NAME, VERSION) }

    var musicList: MutableList<Music>? = mutableListOf<Music>() // 테이블에 자료가 있음

    //리사이클러어뎁터 객체참조변수 선언
    lateinit var musicRecyclerAdapter: MusicRecyclerAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //승인이 되었으면 음악파일을 가져오는 것이고, 승인이 안되었으면 재요청한다.
        if (isPermitted() == true) {
            //승인이 되었으면, 외부파일을 가져와서, 컬렉션프레임워크 저장하고, 어뎁터를 불러버리면 된다.
            startProcess()
        } else {
            //승인요청 다시함.  android.Manifest.permission.READ_EXTERNAL_STORAGE
            //요청이 승인이 되면 콜백함수 onRequestPermissionsResult로 승인결과값을 알려준다.
            ActivityCompat.requestPermissions(this, permission, REQUEST_READ)
        }
    }

    //승인요청했을때 승인결과에 대한 콜백함수
    override fun onRequestPermissionsResult(
        requestCode: Int,   //여기로 리턴값이 온다.
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //승인됐으니 실행하면 외부파일을 가져와서, 컬렉션프레임워크 저장하고, 어뎁터를 불러버리면 된다.
                startProcess()
            } else {
                Toast.makeText(this, "권한요청 승인해야만 앱이 실행가능함", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }


    //외부파일 읽기 승인요청 함수:퍼미션 승인 후 실행
    fun isPermitted(): Boolean {
        //승인 요청 확인
        if (ContextCompat.checkSelfPermission(
                this,
                permission[0]
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        } else {
            return true
        }
    }

    //전체 플레이뮤직리스크를 가져와서, db에 없으면 외부장치에서 가져와서 db에 , null이 아니면 바로 제공한다.
    private fun startProcess() {

        //데이터베이스 기능 설정
        //music 테이블에서 자료 가져올 때 자료가 있으면 리사이클러뷰 보여주고
        // 자료가 없으면 getMusicList() 불러서 정보가져오고 -> music 테이블에 모두 저장 후 리사이클러뷰에 보여준다.

        musicList = dbHelper.selectMusicAll()

        //music테이블에 자료가 없으면 DB에서 가져오기 실행.
        if (musicList == null) {
            //getMusicList (외부장치에서 음악정보를 가져오는 기능담당)
            musicList = getMusicList()
            // musicList의 모든 정보를 데이터베이스 music 테이블에 저장

            if (musicList != null) {
                for (i in 0..(musicList!!.size - 1)) {
                    val music = musicList!!.get(i)
                    if (dbHelper.insertMusic(music) == false) {
                        Log.d("sophia", "삽입오류 발생 ${music.toString()}")
                    }
                }
            }
            Log.d("sophia", "테이블에 있어서 내용을 가져와서 보여줌")
        }


        //1. 음악정보를 가져와야된다. db에 있으면 getMusicList를 불러오면 안되므로 주석처리한다.
//        var musicList: MutableList<Music>? = getMusicList() //주석처리진행
        //2. 데이타베이스 저장한다.(중복반드시 점검할 것: id primary key)

        //3. 리사이클러뷰 보여줌
        musicRecyclerAdapter = MusicRecyclerAdapter(this, musicList)
        //어뎁터 만들고 MutableList 제공
        binding.recyclerView.adapter = musicRecyclerAdapter
        //4. 화면에 출력한다.
        binding.recyclerView.layoutManager = LinearLayoutManager(this)

        //데코할 곳
    }


    //외부파일에서 음악정보 가져오기
    @JvmName("getMusicList1")
    private fun getMusicList(): MutableList<Music>? {
        //1.외부파일에 있는 음악정보주소
        val listUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        //2. 요청해야될 음악정보 컬럼들
        val proj = arrayOf(
            MediaStore.Audio.Media._ID,         //음악id
            MediaStore.Audio.Media.TITLE,       //음악타이틀
            MediaStore.Audio.Media.ARTIST,      //가수명
            MediaStore.Audio.Media.ALBUM_ID,    //앨범이미지
            MediaStore.Audio.Media.DURATION    //곡길이
        )
        //3. 컨텐츠 리졸버 쿼리에 Uri, 요청음악정보칼럼 요구하고 결과값을 cusor 반환받는다.
        //contentResolver.query(listUri, projection, where, 내용, oredrby acending인지 decending인지)
        val cursor = contentResolver.query(listUri, proj, null, null, null)
        // Music: mp3 정보 5가지 기억, mp3파일 경로, mp3이미지 경로, 이미지경로를 통해 원하는 사이즈 비트맵 변경
        val musicList: MutableList<Music>? = mutableListOf<Music>()
        //음악이 null이 아닐때
        while (cursor!!.moveToNext()) {
            val id = cursor.getString(0)
            val title = cursor.getString(1).replace("'", "")
            val artist = cursor.getString(2).replace("'", "")
            val albumId = cursor.getString(3)
            val duration = cursor.getLong(4)

            val music = Music(id, title, artist, albumId, duration, 0)
            musicList?.add(music)
        }
        cursor?.close()
        return musicList
    }

    //옵션메뉴를 누르면
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        //메뉴에서 서치항목뷰를 찾음.
        val searchMenu = menu?.findItem(R.id.search)
        //서치뷰기능을 줌.
        val searchView = searchMenu?.actionView as SearchView

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            //글자를 작성하고, 찾기 버튼을 누르면 이 onQueryTextSubmit 콜백함수가 작동이 된다.
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.d("sophia","검색명령: ${query}")
                return true
            }

            //글자가 바뀔때마다 onQueryTextChange 불러지는 콜백함수
            //isNullOrBlank() 함수 : 공간에 공백을 남기면 true가 된다. ex) " " -> true
            //!query.isNullOrBlank() => true의 낫이므로 false.
            //찾는글자가 있으면 찾는 글자만 검색을 해서 가져오고, 그렇지 않으면 모두 다 가져온다.
            override fun onQueryTextChange(query: String?): Boolean {
                if(!query.isNullOrBlank()){
                    musicList?.clear()
                    dbHelper.searchMusic(query)?.let { musicList?.addAll(it) }
                    musicRecyclerAdapter.notifyDataSetChanged()

                }else{
                    musicList?.clear()
                    dbHelper.selectMusicAll()?.let { musicList?.addAll(it) }
                    musicRecyclerAdapter.notifyDataSetChanged()
                }
                return true
            }
        })
        return super.onCreateOptionsMenu(menu)
    }

    //옵션메뉴 2개를 선택시
    @SuppressLint("ResourceAsColor")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.itemLove -> {
                musicList?.clear()
                dbHelper.selectLove()?.let { musicList?.addAll(it) }
                musicRecyclerAdapter.notifyDataSetChanged()
            }
            R.id.itemAll->{
                musicList?.clear()
                dbHelper.selectMusicAll()?.let { musicList?.addAll(it) }
                musicRecyclerAdapter.notifyDataSetChanged()

            }
        }
        return super.onOptionsItemSelected(item)
    }
}



