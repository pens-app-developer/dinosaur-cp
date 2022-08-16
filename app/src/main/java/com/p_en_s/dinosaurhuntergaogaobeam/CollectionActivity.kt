package com.p_en_s.dinosaurhuntergaogaobeam

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.navigation.NavigationView
import kotlinx.android.synthetic.main.content_collection.*


class CollectionActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    private var clearStageId: Int = 0

    private var arrayMediaPlayer: Array<MediaPlayer?>? = null
    private var threadSoundLoop: Thread? = null

    private lateinit var adView: AdView

    private val adSize: AdSize
        get() {
            val display = windowManager.defaultDisplay
            val outMetrics = DisplayMetrics()
            display.getMetrics(outMetrics)

            val density = outMetrics.density

            var adWidthPixels = adViewContainer.width.toFloat()
            if (adWidthPixels == 0f) {
                adWidthPixels = outMetrics.widthPixels.toFloat()
            }

            val adWidth = (adWidthPixels / density).toInt()
            return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth)
        }

    private fun loadBanner() {
        adView.adUnitId = getString(R.string.ad_unit_id_banner)
        adView.adSize = adSize
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_collection)

        // バナー広告の設定
        adView = AdView(this)
        adViewContainer.addView(adView)
        loadBanner()

        // 保存データを取得
        val dataStore: SharedPreferences = getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        // クリア済みのステージIDを取得
        clearStageId = dataStore.getInt("clearStageId", 0)

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        // ナビケーション内のアイテムを設定
        setNavItem(navView)
        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onResume() {
        super.onResume()
        // BGM再生
        playMediaPlayer()
    }

    override fun onPause() {
        super.onPause()
        // BGM削除
        releaseMediaPlayer()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.collection, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    override fun onBackPressed() {
        val pFragment = this.supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val fragments = pFragment!!.childFragmentManager.fragments
        if (fragments.size == 1  && fragments[0] is OnBackPressedListener) {
            (fragments[0] as OnBackPressedListener).onBackPressed()
        } else {
            super.onBackPressed()
        }
    }

    private fun setNavItem(navView: NavigationView) {
        var stageId = 0
        while (true){
            stageId++
            // アイテム用のIDを取得
            val itemResourcesId = resources.getIdentifier(
                "nav_gallery_${stageId}",
                "id",
                packageName
            )
            // 画像IDを取得
            val imageResourcesId = resources.getIdentifier(
                "character_${stageId}_1",
                "drawable",
                packageName
            )
            // テキストIDを取得
            val textResourcesId = resources.getIdentifier(
                "stage_${stageId}",
                "string",
                packageName
            )
            // アイテム用のIDが取得できたか判定
            if (itemResourcesId > 0) {
                // Itemを追加
                navView.menu.add(0, itemResourcesId, 0, "")
                val itemView = navView.menu.findItem(itemResourcesId)
                itemView.isVisible = true
                // クリア済みのステージか判定
                if (clearStageId >= stageId) {
                    itemView.isEnabled = true
                    itemView.setIcon(imageResourcesId)
                    itemView.setTitle(textResourcesId)
                } else {
                    itemView.isEnabled = false
                    itemView.setIcon(R.drawable.icon_question)
                    itemView.setTitle(R.string.stage_close)
                }
            } else {
                // 取得できなかったらループを終了
                break
            }
        }
        return
    }

    private fun playMediaPlayer() {
        //MediaPlayerの設定
        arrayMediaPlayer = arrayOf(
            MediaPlayer.create(this, R.raw.bgm_title),
            MediaPlayer.create(this, R.raw.bgm_title)
        )

        //初回再生時のノイズ除去処理
        for (i in arrayMediaPlayer!!.indices) {
            arrayMediaPlayer!![i]?.setVolume(0f, 0f)
            arrayMediaPlayer!![i]?.isLooping = false
            arrayMediaPlayer!![i]?.seekTo(arrayMediaPlayer!![i]?.duration!! - 200 * (i + 1))
            arrayMediaPlayer!![i]?.setOnCompletionListener { mediaPlayer ->
                mediaPlayer.setVolume(0.3f, 0.3f)
                mediaPlayer.seekTo(0)
                mediaPlayer.setOnCompletionListener { mediaPlayer2 -> mediaPlayer2.seekTo(0) }
                if (threadSoundLoop == null) {
                    threadSoundLoop = Thread(
                        RunnableSoundLoop(
                            arrayMediaPlayer!!,
                            System.currentTimeMillis()
                        )
                    )
                    threadSoundLoop!!.isDaemon = true
                    threadSoundLoop!!.start()
                    mediaPlayer.start()
                }
            }
            arrayMediaPlayer!![i]?.start()
        }
    }

    private fun releaseMediaPlayer() {
        for (i in arrayMediaPlayer!!.indices) {
            if (arrayMediaPlayer!![i] != null) {
                arrayMediaPlayer!![i]?.stop()
                arrayMediaPlayer!![i]?.release()
                arrayMediaPlayer!![i] = null
            }
        }
        threadSoundLoop = null
    }
}