package com.simbastudio.imtokrus.activity


import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.fragment.app.Fragment
import com.simbastudio.imtokrus.R
import com.simbastudio.imtokrus.databinding.ActivityMainBinding
import com.simbastudio.imtokrus.fragment.UserFragment
import com.simbastudio.imtokrus.fragments.HomeFragment
import com.simbastudio.imtokrus.fragments.NotificationFragment
import com.simbastudio.imtokrus.fragments.SearchFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        moveToFragment(HomeFragment())

        bottomMenuOnClick()
    }

    /////////Логика взаимодействия с навигационным нижним меню//////////////////////////////////////////////////////////
    private fun bottomMenuOnClick() = with(binding){
        bNavView.setOnNavigationItemSelectedListener { item ->

            //Добавляю слушатель нажатий на нижнее меню приложения
            when(item.itemId){
                R.id.id_new_content -> {
                    item.isChecked = false
                    startActivity(Intent(this@MainActivity, CreateActivity::class.java))
                }

                R.id.id_my_profile -> {
                    moveToFragment(UserFragment())
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.id_search -> {
                    moveToFragment(SearchFragment())
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.id_my_favorite -> {
                    moveToFragment(NotificationFragment())
                    return@setOnNavigationItemSelectedListener true
                }

                R.id.id_home -> {
                    moveToFragment(HomeFragment())
                    return@setOnNavigationItemSelectedListener true
                }
            }
            false
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun moveToFragment(fragment: Fragment){
        val fragmentTrans = supportFragmentManager.beginTransaction()
        fragmentTrans.replace(R.id.placeHolder, fragment)
        fragmentTrans.commit()
    }
}