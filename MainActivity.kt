package com.aysegulyilmaz.kotlinartbook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.recyclerview.widget.LinearLayoutManager
import com.aysegulyilmaz.kotlinartbook.databinding.ActivityMainBinding

//MainActivity de sadece id ve isim çekiyoruz

class MainActivity : AppCompatActivity() {
    private lateinit var binding : ActivityMainBinding
    private lateinit var artList: ArrayList<Arts>
    private lateinit var artAdapter : ArtAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        artList = ArrayList<Arts>()
        artAdapter = ArtAdapter(artList)

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = artAdapter

        try{
            val database = this.openOrCreateDatabase("Arts",MODE_PRIVATE,null)
            val cursor = database.rawQuery("SELECT * FROM arts",null)
            val artName = cursor.getColumnIndex("artname")
            val idIndex = cursor.getColumnIndex("id")

            while(cursor.moveToNext()){
                val name = cursor.getString(artName)
                val id = cursor.getInt(idIndex)
                //bunun için recyclerviewa kolyaca katramk için direkt art classı oluşturup recycler viewda onu çağırabiliriz
                val art = Arts(name,id)
                //bunu arrayliste koyup sonra recyclerviewda göstericez en üste arraylist oluşturduk
                artList.add(art) // oluşturduğumuz artları ekledik
            }
            //art adaptere haber ver veri setimiz değişti
            artAdapter.notifyDataSetChanged()
            cursor.close()
            //bunu hallettikten sonra kullanıcıya göstermek için bir recycler view adapter yazıyoruz



        }catch(e:Exception){
            e.printStackTrace()
        }
    }
    //açıldığında ne olacak

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        //xml le kodu bağlamak için infalter kullnıcaz
        val menuInflater = menuInflater
        menuInflater.inflate(R.menu.art_menu,menu)
        return super.onCreateOptionsMenu(menu)
    }
    //tıklanınca ne olacak
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == R.id.add_art_item){
            val intent = Intent(this@MainActivity,ArtActivity::class.java)
            intent.putExtra("info","new")//mainactivityde yeni bir veri eklemek istiyoruz
            startActivity(intent)
        }
        return super.onOptionsItemSelected(item)
    }
}