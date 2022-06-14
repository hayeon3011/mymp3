package com.example.mymp3

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Parcelable
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mymp3.databinding.ItemRecyclerBinding
import java.text.SimpleDateFormat

//1. 매개변수(컨텍스트,컬렉션프레임워크)
//3. 상술처리한다. RecyclerView.Adapter<MusicRecyclerAdapter.CustomViewHolder>()내부클래스에 홀더를 만들었다는 것.
class MusicRecyclerAdapter(val context: Context, val musicList: MutableList<Music>?):
    RecyclerView.Adapter<MusicRecyclerAdapter.CustomViewHolder>() {
    //이미지사이즈 정의
    var ALBUM_IMAGE_SIZE = 50

    //dbHelper = DBHelper(context, "musicDB",1) 만들어서 실행하면 , 데이터베이스 파일이 있으면 또 만들어지지 않는다.
    //만들어진 데이터베이스 객체만 전달한다.
    val dbHelper = DBHelper(context, "musicDB",1)


    //4.2
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomViewHolder {
        val binding =
            ItemRecyclerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomViewHolder(binding)
    }

    //4.3
    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        //홀더를 커스텀뷰홀더로 바꿔서 바인딩하겠다.
        val binding = (holder as CustomViewHolder).binding
        val music = musicList?.get(holder.adapterPosition)    //포지션에 문제가 생기면 , valmusic = musicList?.get(holder.adapterPosition)으로 처리해라.
        //===========================화면셋팅=======================================
        binding.tvArtist.text = music?.artist    // 가수이름
        binding.tvTitle.text = music?.title  // 노래제목
        binding.tvDuration.text = SimpleDateFormat("mm:ss").format(music?.duration)  //경과시간
        val bitmap: Bitmap? = music?.getAlbumImage(context, ALBUM_IMAGE_SIZE)
        if (bitmap != null) {
            binding.ivAlbumArt.setImageBitmap(bitmap)
        } else {
            //앨범이미지가 없을경우 디폴트 이미지 listcover를 붙여넣는다.
            binding.ivAlbumArt.setImageResource(R.drawable.listcover)
        }
        //좋아하는 음악체크 : 0-> nolove , 1 -> love
        when (music?.love) {
            0 -> {
                holder.binding.ivLove.setImageResource(R.drawable.nolove)
            }
            1 -> {
                holder.binding.ivLove.setImageResource(R.drawable.love)
            }
        }
        //항목클릭시 재생하면으로 넘어감. position을 가지고 넘어감
        //item_recycler.xml 의 ConstraintLayout 자체가 binding.root임
        //***여기 부분이 중요하다 ****
        //해당되는 아이템뷰를 클릭을 하면 -> PlaymusicActivity에다가 musicList를 Parcelable을 통해서 변환된 ArrayList와 해당뷰 위치값을 전송함.
        //전체를 선택하면 playList , position을 확인할 수 있도록
        binding.root.setOnClickListener {
            //액티비티로 음악정보를 넘겨서 음악을 재생해주는 액티비티 설계
            //musicList 를 인텐트로 전달하기 위해 Parcelable ArrayList 에 저장하는 것
            val playList: ArrayList<Parcelable>? = musicList as ArrayList<Parcelable>
            //화면이동
            val intent = Intent(binding.root.context, PlaymusicActivity::class.java)
            intent.putExtra("playList", playList)
            //위에서 음악 리스트 가져오면서 음악 순서(position)도 같이 가져옴
            intent.putExtra("position", holder.adapterPosition)
            binding.root.context.startActivity(intent)
        }

        //ivLove 선택시 ivLove의 값을 바꿔줌.
        binding.ivLove.setOnClickListener {
            var updateFlag = false
            if(music?.love == 0) {
                binding.ivLove.setImageResource(R.drawable.love)
                music?.love = 1
            }else{
                binding.ivLove.setImageResource(R.drawable.nolove)
                music?.love = 0
            }
            if(music != null){
                updateFlag = dbHelper.updateLove(music)
                if(updateFlag == false){
                    Log.d("sophia", "하트 update 실패${music.toString()}")
                }
                //데이터를 무효화영역처리를 통해 화면을 다시 재생시킴(데이터를 다시 재배치)
                notifyDataSetChanged()
            }
        }

    }


    // 1. mjusicList null이 아니면 size를 주고, null이면 0을 줘라.
    override fun getItemCount(): Int {
        //null이 아닐떄에는 0을 주겠다.
        return musicList?.size?:0
    }

    //2. 뷰홀더 내부선언(바인딩)
    //내부클래스를 만든다. item_recycler.xml
    class CustomViewHolder(val binding: ItemRecyclerBinding) : RecyclerView.ViewHolder(binding.root)
}