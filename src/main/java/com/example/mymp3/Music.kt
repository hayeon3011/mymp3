package com.example.mymp3

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Parcel
import android.os.ParcelFileDescriptor
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import java.io.IOException

//액티비티와 액티비티 사이의 객체를 전달하기 위해서는 번들, Serializable, Parcelable 3가지 방법이 있다.
@Parcelize
class Music(var id:String, var title:String?, var artist: String?, var albumId: String?, var duration: Long?, var love: Int? ):Parcelable{
    //PlayMusicActivity 인텐트로 객체를 전송하기 위해 Serializable -> Parcelable 사용(속도초리,용량처리 개선)

    //companion object: 정적멤버함수
    companion object: Parceler<Music> {

        //Music에 있는 (규격화 내용쓰기)
        override fun Music.write(parcel: Parcel, flags: Int) {
            parcel.writeString(id)
            parcel.writeString(title)
            parcel.writeString(artist)
            parcel.writeString(albumId)
            parcel.writeLong(duration!!)
            parcel.writeInt(love!!)
        }

        //Music var id:String, var title:String?, var artist: String?, var albumId: String?, var duration: Long?, var love: Int? ) parcel로 받아서 줌.
        override fun create(parcel: Parcel): Music {
            return Music(parcel)
        }
    }

    //생성자의 일반객체내용을 전송하기 위해서, this()롤 해서 parcel의 형식으로 바꿔줌(규격화된 내용 읽기)
    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readString(),
        parcel.readLong(),
        parcel.readInt()
    )

    //앨범의 uri 가져오는 방법:컨텐츠리졸버를 이용해서 앨범정보를 가져오기 위해서 해당되는 경로 uri 열기(이미지정보)
    fun getAlbumUri(): Uri {
        return Uri.parse("content://media/external/audio/albumart/" + albumId)
    }

    //음악정보를 가져오기 위한 경로 uri 열기
    fun getMusicUri(): Uri {
        return Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    }

    //해당되는음악에 내가 원하는 사이즈로 비트맵 만들기
    fun getAlbumImage(context: Context, albumImageSize: Int): Bitmap?{
        val contentResolver : ContentResolver = context.getContentResolver()
        //앨범경로
        val uri = getAlbumUri()
        //앨범에 대한 정보를 저장하기 위함
        val options = BitmapFactory.Options()


        if(uri != null) {
            var parcelFileDescriptor: ParcelFileDescriptor? = null

            try {
                //외부파일에 있는 이미지파일을 가져오기 위한 스트림
                parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")

                var bitmap = BitmapFactory.decodeFileDescriptor(parcelFileDescriptor!!.fileDescriptor, null, options)

                //비트맵을 가져와서 사이즈 결정(원본이미지 사이즈가 내가 원하는 사이즈하고 맞지 않을경우 원하는 사이즈로 맞춤)
                if (bitmap != null) {
                    if (options.outHeight !== albumImageSize || options.outWidth !== albumImageSize) {
                        val tempBitmap = Bitmap.createScaledBitmap(bitmap, albumImageSize, albumImageSize, true)
                        bitmap.recycle()
                        bitmap = tempBitmap
                    }
                }
                return bitmap
            } catch (e: Exception) {
                Log.d("sophia", "getAlbumImage()${e.toString()}")
            } finally {
                try {
                    parcelFileDescriptor?.close()
                } catch (e: IOException) {
                    Log.d("sophia", "parcelFileDescriptor?.close()${e.toString()}")
                }
            }
        }// end of if(uri != null)
        return null
    }
}
