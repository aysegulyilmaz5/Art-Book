package com.aysegulyilmaz.kotlinartbook

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.appsearch.SetSchemaRequest.READ_EXTERNAL_STORAGE
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.aysegulyilmaz.kotlinartbook.databinding.ActivityArtBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_art.*
import java.io.ByteArrayOutputStream

class ArtActivity : AppCompatActivity() {

    private lateinit var binding :ActivityArtBinding
    private lateinit var activityResultLauncher : ActivityResultLauncher<Intent>
    private lateinit var activityPermissionLauncher : ActivityResultLauncher<String>
    var selectedBitmap : Bitmap? = null //nullable değer veridk
    private lateinit var database : SQLiteDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArtBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
        registerLauncher()

        val intent = intent
        val info = intent.getStringExtra("info")
        if(info.equals("new")){
            binding.artnameText.setText("")
            binding.artistnameText.setText("")
            binding.yearText.setText("")
            binding.button.visibility = View.VISIBLE
        }else{
            binding.button.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)
            val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ?" , arrayOf(selectedId.toString()))
            val artname = cursor.getColumnIndex("artname")
            val artistname = cursor.getColumnIndex("artistname")
            val year = cursor.getColumnIndex("year")
            val image = cursor.getColumnIndex("image")

            while(cursor.moveToNext()){
                binding.artnameText.setText(cursor.getString(artname))
                binding.artistnameText.setText(cursor.getString(artistname))
                binding.yearText.setText(cursor.getString(year))

                val byteArray = cursor.getBlob(image)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                binding.imageView.setImageBitmap(bitmap)
            }
            cursor.close()

        }




    }

    fun save(view:View){
        val artname = binding.artnameText.text.toString()
        val artistname = binding.artistnameText.text.toString()
        val year = binding.yearText.text.toString()
        if(selectedBitmap != null){
            val smallBitmap = makeSmallerbitmap(selectedBitmap!!,300)
            //görseli database'e kaydetmek için veriye çevirmemiz lazım
            val outputStream = ByteArrayOutputStream()
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            try{
                //val database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY,artname VARCHAR,artistname VARCHAR,year VARCHAR,image BLOB)")
                val sqlString = "INSERT INTO arts (artname,artistname,year,image)VALUES(?,?,?,?)" // Değer vericem ama ne vereceğimi bilmiyorum bu yüzden ? koyduk
                val statement = database.compileStatement(sqlString)//çalıştırmadan önce ne ne ile bağlanacak kontrol ediyor
                statement.bindString(1,artname) //bind bağla demek biz ilk soru işareti ile artname'i bağladık normalde indexler 0 dan başlıyor ama burda 1 den başlıyor
                statement.bindString(2,artistname)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()

            }catch (e:java.lang.Exception){
                e.printStackTrace()
            }
            //1.
            val intent = Intent(this@ArtActivity,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)

            //2.finish()//main activitye geri dönmek için finish çalıştırabiliriz
        }



    }
    //bitmapin boyutunu küçültüyoruz
    private fun makeSmallerbitmap(image:Bitmap,maximumSize:Int): Bitmap{
        var width = image.width
        var height = image.height

        var bitmapRatio: Double = width.toDouble() /height.toDouble()

        if(bitmapRatio >1){
            //landscape
            //yatay görsel demek
            width = maximumSize
            val scaledheight = width / bitmapRatio
            height = scaledheight.toInt()

        }else{
            //portrate
            //dikey görsel demek
            height = maximumSize
            val scaledWidth = height * bitmapRatio
            width = scaledWidth.toInt()

        }
        return Bitmap.createScaledBitmap(image,width,height,true)

    }

    fun selectImage(view:View){
        //izin verilmediyse eğer
        println("ok")
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED ){
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)){
            //rationale
                Snackbar.make(view,"Permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give Permission",View.OnClickListener {
                    //request permission
                    activityPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)

                }).show()

            }
            else{
                //request permission
                activityPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }

        }
        //izin verilirse direkt galeriye gidiyoruz
        else{
            val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            //intent
            activityResultLauncher.launch(intentToGallery)

        }

    }

    private fun registerLauncher(){
        activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {result ->
                if(result.resultCode == RESULT_OK){
                    val intentFromResult =result.data
                    if(intentFromResult != null){
                        val imageData = intentFromResult.data //URIyı veriyor
                       // binding.imageView.setImageURI(imageData)//veriyi alıp bu şekilde direkt image viewda gösterebiliriz
                        //fakat veriyi alıp direkt üstüne koymak yerine bitmap yapıp veriyi küçültüp veritabanına kaydedicez
                        if(imageData !=null) {
                            try {
                                //kullanıcı API 28 ve sonrasını mı kullanıyor kontrol etmemiz lazım
                                if (Build.VERSION.SDK_INT >= 28) {

                                    val source = ImageDecoder.createSource(
                                        this@ArtActivity.contentResolver,
                                        imageData
                                    )
                                    selectedBitmap =
                                        ImageDecoder.decodeBitmap(source) //Bu şekilde bitmapi kaynağın içerisine koyuyoruz
                                    binding.imageView.setImageBitmap(selectedBitmap)
                                } else {
                                    selectedBitmap = MediaStore.Images.Media.getBitmap(
                                        contentResolver,
                                        imageData
                                    )
                                    binding.imageView.setImageBitmap(selectedBitmap)
                                }

                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                }

            }

        activityPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result ->
            if(result){
                //permission granted
                val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                activityResultLauncher.launch(intentToGallery)
            }else{
                //permission denied
                Toast.makeText(this@ArtActivity,"Permission needed!",Toast.LENGTH_LONG).show()
            }

        }
    }
}