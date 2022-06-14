package com.example.mymp3

import android.graphics.Bitmap
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.view.View
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import com.example.mymp3.databinding.ActivityPlaymusicBinding
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

class PlaymusicActivity : AppCompatActivity() {
    lateinit var binding: ActivityPlaymusicBinding

    //1. 뮤직플레이어 변수 선언
    private var mediaPlayer: MediaPlayer? = null

    var musicRecyclerAdapter:MusicRecyclerAdapter?=null

    //2. 음악정보객체변수
    private var music: Music? = null
    private var playList: ArrayList<Parcelable>?= null
    private var position: Int = 0
    private val PREVIOUS = 0
    private val NEXT = 1

    //3. 음악앨볌이미지사이즈
    private var ALBUM_IMAGE_SIZE = 150

    //4. 코루틴 스코프  런치
    private var playerJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlaymusicBinding.inflate(layoutInflater)
        setContentView(binding.root)


        //인텐트로 데이터를 전달해왔을 떄 (ArrayList<Parcel>, 위치)
        //music = intent.getSerializableExtra("music") as Music
        playList = intent.getParcelableArrayListExtra("playList")
        position = intent.getIntExtra("position", 0)
        Log.d("sophia", "playList?.get(position) ${playList?.get(0)}")
        music = playList?.get(position) as Music

        musicStart(music)

    }

    fun musicStart(music: Music?) {
        if (music != null) {
            //===========================화면뷰셋팅================================================
            binding.tvTitle.text = music?.title
            binding.tvArtist.text = music?.artist

            //좋아요 기능: 음악이 좋으면 꽉찬구름으로 변경
            if (music?.love == 1) {
                binding.ivLove.setImageResource(R.drawable.love)
            } else {
                binding.ivLove.setImageResource(R.drawable.nolove)
            }
            binding.tvDurationStart.text = "00:00"
            binding.tvDurationStop.text = SimpleDateFormat("mm:ss").format(music?.duration)

            //앨범이미지 가져옴.
            val bitmap: Bitmap? = music?.getAlbumImage(this, ALBUM_IMAGE_SIZE)
            if (bitmap != null) {
                binding.ivAlbumArt.setImageBitmap(bitmap)
            } else {
                binding.ivAlbumArt.setImageResource(R.drawable.albumcover)
            }
            //시크바의 최대값을 준다.
            binding.seekBar.max = music?.duration!!.toInt()
            //==================================================================================
            //음원실행 생성 및 재생
            mediaPlayer = MediaPlayer.create(this, music?.getMusicUri())


            //이 부분이 코루틴으로 돌리는 것
            //시크바 이벤트 설정으로 노래와 같이 동기화처리 ChangeListener: 움직이기만 하면 이벤트 발생
            binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                //시크바를 터치하고 이동할 때 발생되는 이벤트 (fromUser : 유저 터치 확인)
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,      //progress : 진행상태
                    fromUser: Boolean   //fromUser : 유저에 의해 이동했다면 true, 프로그램에 이동하면 false
                ) {
                    if (fromUser) {
                        mediaPlayer?.seekTo(progress)   //시크바를 움직이면 그 위치에 맞도록 노래도 따라 움직인다.
                    }
                }

                //시크바를 터치하는 순간 이벤트 발생
                override fun onStartTrackingTouch(p0: SeekBar?) {
                }
                //시크바 터치를 놓는순가 이벤트 발생
                override fun onStopTrackingTouch(p0: SeekBar?) {
                }

            })

        }
    }

    //클릭 이벤트를 구성함
    fun onClickView(view: View?) {
        when (view?.id) {
            //목록 이미지를 클릭시, 코루틴 취소, 음악객체 해제, 음악객체 = null
            R.id.ivList -> {
                mediaPlayer?.stop()
                playerJob?.cancel()
                finish()
            }

            //재생 이미지를 클릭시
            R.id.ivStart -> {
                if (mediaPlayer?.isPlaying == true) {
                    binding.ivStart.setImageResource(R.drawable.play)
                    binding.seekBar.progress = mediaPlayer?.currentPosition!!
                    mediaPlayer?.pause()
                } else {
                    mediaPlayer?.start()
                    binding.ivStart.setImageResource(R.drawable.pause)
                    // 음악시작 (시크바, 진행시간을 코루틴으로 진행)
                    val backgroundScope = CoroutineScope(Dispatchers.Default + Job())

                    playerJob = backgroundScope.launch {
                        //노래진행 사항을 시크바와 시작진행시간 값에 넣어주기
                        //사용자가 만든 스레드에서 화면에 뷰 값을 변경하게 되면 오류발생
                        //해결방법: 스레드 안에서 뷰 값을 변경하고 싶으면 runOnUiThread{}사용

                        while (mediaPlayer?.isPlaying == true) {
                            //노래 진행 위치를 시크바에 적용
                            // **중요**
                            runOnUiThread {
                                var currentPosition = mediaPlayer?.currentPosition!!
                                binding.seekBar.progress = currentPosition
                                binding.tvDurationStart.text =
                                    SimpleDateFormat("mm:ss").format(currentPosition)
                            }
                            try {
                                delay((500))
                            } catch (e: Exception) {
                                Log.d("sophia", "delay(500) ${e.toString()}")
                            }
                        }//end of while
                        Log.d("sophia", "currentPosition ${mediaPlayer!!.currentPosition}")
                        Log.d("sophia", "max ${binding.seekBar.max}")
                        runOnUiThread {
                            if (mediaPlayer!!.currentPosition == binding.seekBar.max - 1000) {
                                binding.seekBar.progress = 0
                                binding.tvDurationStart.text = "00:00"
                            }
                            binding.ivStart.setImageResource(R.drawable.play)
                        }
                    }

                }// end of backgroundScope.launch
            } // end of if(mediaPlayer?.isPlaying == true)

            //정지 이미지를 클릭시
            R.id.ivStop -> {
                mediaPlayer?.stop()
                playerJob?.cancel()
                //음악이 멈췄으면 음악을 다시 위치하는 것이다.
                mediaPlayer = MediaPlayer.create(this, music?.getMusicUri())
                binding.seekBar.progress = 0
                binding.tvDurationStart.text = "00:00"
                binding.ivStart.setImageResource(R.drawable.play)
            }

            // 이전 이미지 클릭시
            R.id.ivPrevious -> {
                //음악정지, 코루틴취소, 음악객체 해제, 음악객체 = null
                mediaPlayer?.stop()
                playerJob?.cancel()
                position = position -1
                if(position < 0){
                    position = playList!!.size-1
                }
                music = playList?.get(position) as Music
                musicStart(music)
            }

            // 다음 이미지 클릭시
            R.id.ivNext -> {
                //음악정지, 코루틴취소, 음악객체 해제, 음악객체 = null
                mediaPlayer?.stop()
                playerJob?.cancel()
                position = position +1
                if(position > playList!!.size-1){
                    position = 0
                }
                music = playList?.get(position) as Music
                musicStart(music)
            }
        }
    }

    override fun onStop() {
        mediaPlayer?.stop()
        playerJob?.cancel()
        super.onStop()
    }
}
