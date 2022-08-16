package com.p_en_s.dinosaurhuntergaogaobeam

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.view.Window
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.*
import kotlinx.android.synthetic.main.activity_stage.*
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random


class StageActivity : AppCompatActivity() {

    private var bossStage: Int = 0
    private var maxStage: Int = 0
    private var stageId: Int = 0
    private var counter: Int = 0
    private var targetCount: Int = 0
    private var timeLimit: Int = 0
    private var mTimerTask: MainTimerTask = MainTimerTask()
    private var mTimer: Timer? = null
    private var arrayImageCharacterId: ArrayList<Int> = arrayListOf()
    private var nowImageCharacterIndex: Int = 0

    private var onTapDisabled: Boolean = true

    private var mSoundPool: SoundPool? = null

    private var mSoundResIdTap: Int? = 0
    private var mSoundResIdTapRandom: Int? = 0
    private var mSoundResIdStart: Int? = 0
    private var mSoundResIdSuccess: Int? = 0
    private var mSoundResIdFailure: Int? = 0

    private var arrayMediaPlayer: Array<MediaPlayer?>? = null
    private var threadSoundLoop: Thread? = null

    private lateinit var mInterstitialAd: InterstitialAd

    private var isViewAd: Boolean = false

    private var attackStartImageCenterX = 0
    private var attackStartImageCenterY = 0

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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stage)

        // バナー広告の設定
        adView = AdView(this)
        adViewContainer.addView(adView)
        loadBanner()

        // 全画面広告の設定
        mInterstitialAd = InterstitialAd(this)
        mInterstitialAd.adUnitId = getString(R.string.ad_unit_id_interstitial)
        mInterstitialAd.loadAd(AdRequest.Builder().build())
        mInterstitialAd.adListener = object : AdListener() {
            override fun onAdClosed() {
                mInterstitialAd.loadAd(AdRequest.Builder().build())
            }
        }

        // ステージデータをリソースから取得
        bossStage = resources.getInteger(R.integer.boss_stage)
        maxStage = resources.getInteger(R.integer.max_stage)
        val targetCounts: IntArray = resources.getIntArray(R.array.target_counts)
        val timeLimits: IntArray = resources.getIntArray(R.array.time_limits)

        // ステージIDを設定
        stageId = intent.getIntExtra("stageId", 1)
        // 目標タップ回数を設定
        targetCount = targetCounts[stageId - 1]
        progressBarTapCounter.max = targetCount
        // 制限時間を設定
        timeLimit = timeLimits[stageId - 1]
        progressBarTimeCounter.max = timeLimit

        // キャラクターの各画像IDを取得して格納
        var imageNum = 0
        while (true){
            imageNum++
            // 画像IDを取得
            val resourcesId = resources.getIdentifier(
                "character_${stageId}_${imageNum}",
                "drawable",
                packageName
            )
            // 画像IDが取得できたか判定
            if (resourcesId > 0) {
                // 取得出来ていたらキャラクターの画像IDに追加
                arrayImageCharacterId.add(resourcesId)
            } else {
                // 取得できなかったらループを終了
                break
            }
        }

        // 背景画像を取得して設定
        imageStageBg.setImageResource(resources.getIdentifier(
            "stage_bg_${stageId}",
            "drawable",
            packageName
        ))
        // 成功画像を取得して設定
        imageCharacterSuccess.setImageResource(resources.getIdentifier(
            "character_${stageId}_success",
            "drawable",
            packageName
        ))
        // 失敗画像を設定
        imageCharacterFailure.setImageResource(arrayImageCharacterId[0])

        // タップエリアを押したときの動作
        findViewById<View>(R.id.buttonTapCharacter).setOnTouchListener { v, event ->
            // 押した対象がタップエリアでイベントがシングルタップの押すかマルチタップの押すだった場合に処理
            if (v.id == R.id.buttonTapCharacter && (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN)) {
                // キャラクタータップ時の動作
                onCharacterTap(event)
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()

        // 表示を初期化
        nowImageCharacterIndex = 0
        imageCharacter.setImageResource(arrayImageCharacterId[0])
        imageCharacter.visibility = View.VISIBLE
        imageCharacterSuccess.visibility = View.GONE
        imageCharacterFailure.visibility = View.GONE
        imageTextReadyGoReady.visibility = View.GONE
        imageTextReadyGoReady.scaleX = 1.0f
        imageTextReadyGoReady.scaleY = 1.0f
        imageTextReadyGoReady.alpha = 1.0f
        imageTextReadyGoGo.visibility = View.GONE
        imageTextReadyGoGo.scaleX = 1.0f
        imageTextReadyGoGo.scaleY = 1.0f
        imageTextReadyGoGo.alpha = 1.0f
        layoutResultText.visibility = View.GONE
        layoutResultText.scaleX = 4.0f
        layoutResultText.scaleY = 4.0f
        layoutResultText.alpha = 0.0f
        layoutDescription.visibility = View.GONE
        layoutDescription.alpha = 0.0f
        buttonResult.visibility = View.GONE
        buttonResult.alpha = 0.0f
        buttonReturn.visibility = View.GONE
        buttonReturn.alpha = 0.0f
        layoutBestScore.visibility = View.VISIBLE
        layoutScore.alpha = 0.0f
        layoutScore.visibility = View.GONE

        // タップ進行度を初期化
        counter = 0
        progressBarTapCounter.progress = targetCount

        // 制限時間を初期化
        progressBarTimeCounter.progress = 0

        // BGM再生
        if (bossStage > stageId) {
            playMediaPlayer(R.raw.bgm_stage)
        } else {
            playMediaPlayer(R.raw.bgm_stage_boss)
        }

        // 効果音のロード
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        mSoundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(20)
            .build()
        mSoundResIdTap = mSoundPool?.load(this, R.raw.se_tap, 1)
        mSoundResIdTapRandom = mSoundPool?.load(this, R.raw.se_tap_random, 1)
        mSoundResIdStart = mSoundPool?.load(this, R.raw.se_start, 1)
        mSoundResIdSuccess = mSoundPool?.load(this, R.raw.se_success, 1)
        mSoundResIdFailure = mSoundPool?.load(this, R.raw.se_failure, 1)
        // タイマーの設定
        mTimerTask = MainTimerTask()
        mTimerTask.setActivity(this)
        mTimerTask.setTimeLimit(timeLimit)
        mTimer = Timer(true)

        // 開始アニメーション処理
        val animatorReady = imageTextReadyGoReady.animate()
        animatorReady.duration = 2000
        animatorReady.scaleX(4.0f).scaleY(4.0f).alpha(0.0f)
        animatorReady.setListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator?) {
                imageTextReadyGoReady.visibility = View.GONE
                val animatorGo = imageTextReadyGoGo.animate()
                animatorGo.duration = 1500
                animatorGo.scaleX(4.0f).scaleY(4.0f).alpha(0.0f)
                animatorGo.setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator?) {
                        imageTextReadyGoGo.visibility = View.GONE
                    }
                    override fun onAnimationStart(p0: Animator?) {
                        imageTextReadyGoGo.visibility = View.VISIBLE
                        // タイマーの開始
                        mTimer?.scheduleAtFixedRate(mTimerTask, 0, 10)
                        // 開始音を鳴らす
                        mSoundPool?.play(mSoundResIdStart!!, 1.0f, 1.0f, 0, 0, 1.0f)
                    }
                })
                animatorGo.start()
            }
            override fun onAnimationStart(p0: Animator?) {
                imageTextReadyGoReady.visibility = View.VISIBLE
            }
        })
        animatorReady.start()

    }

    override fun onPause() {
        super.onPause()
        // タップ無効化
        onTapDisabled = true
        // BGM削除
        releaseMediaPlayer()
        // 効果音の削除
        mSoundPool?.release()
        mSoundPool = null
        // タイマー削除
        mTimer?.cancel()
        mTimer = null
    }

    private class MainTimerTask: TimerTask() {
        private var mHandler = Handler()
        private var activity: StageActivity? = null
        private var timeCounter: Int = 0
        private var timeLimit: Int = 0

        override fun run() {
            mHandler.post{
                // 初回のみタップの無効化を解除
                if (timeCounter == 0) {
                    activity?.onTapDisabled = false
                }
                // 経過時間を表示
                activity?.progressBarTimeCounter?.progress = timeCounter

                if (timeCounter == timeLimit) {
                    activity?.setFailure()
                }
                // 経過時間を更新
                timeCounter++
            }
        }

        fun setActivity(StageActivity: StageActivity) {
            activity = StageActivity
        }
        fun setTimeLimit(int: Int) {
            timeLimit = int
        }
        fun getTimeCounter(): Int {
            return timeCounter
        }
    }

    override fun onBackPressed() {
        // BGM削除
        releaseMediaPlayer()
        // 効果音の削除
        mSoundPool?.release()
        mSoundPool = null
        // タイマー削除
        mTimer?.cancel()
        mTimer = null
        // タイトルに戻る
        val intent = Intent(application, MainActivity::class.java)
        intent.putExtra("fromStage", 1)
        startActivity(intent)
        finish()
    }

    private fun onCharacterTap(event: MotionEvent) {
        // タップ動作無効化中の場合は何もしない
        if (onTapDisabled) {
            return
        }

        // 制限時間後には処理しない
        if (timeLimit <= mTimerTask.getTimeCounter()) {
            return
        }
        // タップ音を鳴らす
        mSoundPool?.play(mSoundResIdTap!!, 1.0f, 1.0f, 0, 0, 1.0f)
        // 規定数以下の場合にのみ実行
        if (targetCount > counter) {
            // 残り回数が10回以上あるときはランダムで音を鳴らす
            if (targetCount - counter > 10) {
                val random = Random.nextInt(100, 10000) % 100
                if (random < 5) {
                    mSoundPool?.play(mSoundResIdTapRandom!!, 1.0f, 1.0f, 0, 0, 1.0f)
                }
            }

            // カウンターを1進めて表示を更新
            counter++
            progressBarTapCounter.progress = targetCount - counter

            setTapEffectAnimator(event)

            if (arrayImageCharacterId.size > nowImageCharacterIndex + 1) {
                val checkCountTarget = (targetCount / arrayImageCharacterId.size) * (nowImageCharacterIndex + 1)
                if (checkCountTarget == counter) {
                    nowImageCharacterIndex++
                    // 次の画像に差し替える
                    imageCharacter.setImageResource(arrayImageCharacterId[nowImageCharacterIndex])
                }
            }

            // 規定数まで来たら実行
            if (targetCount == counter) {
                setSuccess()
            }
        }
    }

    private fun onSuccessTap() {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            // タップを無効化
            onTapDisabled = true

            if (isViewAd) {
                if (mInterstitialAd.isLoaded) {
                    mInterstitialAd.adListener = object : AdListener() {
                        override fun onAdClosed() {
                            // 次のステージへ
                            val nextStageId = stageId + 1
                            val intent = Intent(application, StageActivity::class.java)
                            intent.putExtra("stageId", nextStageId)
                            startActivity(intent)
                            // BGM削除
                            releaseMediaPlayer()
                            // 効果音の削除
                            mSoundPool?.release()
                            mSoundPool = null
                            finish()
                        }
                    }
                    mInterstitialAd.show()
                    return
                }
            }

            // 次のステージへ
            val nextStageId = stageId + 1
            val intent = Intent(application, StageActivity::class.java)
            intent.putExtra("stageId", nextStageId)
            startActivity(intent)
            // BGM削除
            releaseMediaPlayer()
            // 効果音の削除
            mSoundPool?.release()
            mSoundPool = null
            finish()
        }
    }

    private fun onFailureTap() {
        // タップ動作無効化されていなければ処理
        if (!onTapDisabled) {
            // タップを無効化
            onTapDisabled = true

            // もう一度同じステージを始める
            val intent = Intent(application, StageActivity::class.java)
            intent.putExtra("stageId", stageId)
            startActivity(intent)
            // BGM削除
            releaseMediaPlayer()
            // 効果音の削除
            mSoundPool?.release()
            mSoundPool = null
            finish()
        }
    }

    private fun playMediaPlayer(bgmId: Int) {
        // MediaPlayerの設定
        arrayMediaPlayer = arrayOf(
            MediaPlayer.create(this, bgmId),
            MediaPlayer.create(this, bgmId)
        )

        // 初回再生時のノイズ除去処理
        for (i in arrayMediaPlayer!!.indices) {
            arrayMediaPlayer!![i]?.setVolume(0f, 0f)
            arrayMediaPlayer!![i]?.isLooping = false
            arrayMediaPlayer!![i]?.seekTo(arrayMediaPlayer!![i]?.duration!! - 200 * (i + 1))
            arrayMediaPlayer!![i]?.setOnCompletionListener { mediaPlayer ->
                mediaPlayer.setVolume(1f, 1f)
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

    private fun setMediaVolumeDown() {
        for (i in arrayMediaPlayer!!.indices) {
            if (arrayMediaPlayer!![i] != null) {
                arrayMediaPlayer!![i]?.setVolume(0.4f, 0.4f)
            }
        }
    }

    private fun setSuccess() {
        // タイマー削除
        mTimer?.cancel()
        mTimer = null

        // BGMを小さくする
        setMediaVolumeDown()
        // 成功音を鳴らす
        mSoundPool?.play(mSoundResIdSuccess!!, 1.0f, 1.0f, 0, 0, 1.0f)
        // キャラクターを成功画像に変更する
        imageCharacter.visibility = View.GONE
        imageCharacterSuccess.visibility = View.VISIBLE

        // 保存データを取得
        val dataStore: SharedPreferences = getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        // クリア済みステージ数を取得
        val clearStageId = dataStore.getInt("clearStageId", 0)
        // 現在のステージがクリア済みステージよりも大きい場合
        if (stageId > clearStageId) {
            // クリア済みステージ数を更新
            val editor = dataStore.edit()
            editor.putInt("clearStageId", stageId)
            editor.apply()
        }

        // リザルトメッセージのアニメーション
        val animatorResult = AnimatorSet()
        animatorResult.duration = 1000
        animatorResult.playTogether(
            ObjectAnimator.ofFloat(layoutResultText, "alpha", 1f),
            ObjectAnimator.ofFloat(layoutResultText, "scaleX", 1f),
            ObjectAnimator.ofFloat(layoutResultText, "scaleY", 1f)
        )
        animatorResult.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(p0: Animator?) {
                val animatorResultButton = AnimatorSet()
                animatorResultButton.duration = 500
                // 次のステージが最後の場合は広告の表示がある
                if (stageId + 1 == maxStage) {
                    // 広告表示フラグを立てる
                    isViewAd = true
                    // 広告表示のメッセージあり版でレイアウトを調整
                    animatorResultButton.playTogether(
                        ObjectAnimator.ofFloat(layoutDescription, "alpha", 1f),
                        ObjectAnimator.ofFloat(buttonResult, "alpha", 1f),
                        ObjectAnimator.ofFloat(buttonReturn, "alpha", 1f),
                        ObjectAnimator.ofFloat(layoutScore, "alpha", 1f),
                        ObjectAnimator.ofFloat(textStageViewAd, "alpha", 1f)
                    )
                    textStageViewAd.visibility = View.VISIBLE
                    val lp = textStageViewAd.layoutParams
                    val mlp = lp as MarginLayoutParams
                    mlp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, 36)
                    textStageViewAd.layoutParams = mlp
                } else {
                    // 広告表示のメッセージなし版でレイアウトを調整
                    animatorResultButton.playTogether(
                        ObjectAnimator.ofFloat(layoutDescription, "alpha", 1f),
                        ObjectAnimator.ofFloat(buttonResult, "alpha", 1f),
                        ObjectAnimator.ofFloat(buttonReturn, "alpha", 1f),
                        ObjectAnimator.ofFloat(layoutScore, "alpha", 1f)
                    )
                    val lp = buttonResult.layoutParams
                    val mlp = lp as MarginLayoutParams
                    mlp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, 36)
                    buttonResult.layoutParams = mlp
                }
                animatorResultButton.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(p0: Animator?) {
                        // 次のステージへボタンを押したときの動作
                        buttonResult.setOnClickListener {
                            // 次のステージに移動
                            onSuccessTap()
                        }
                        // タイトルに戻るボタンを押したときの動作
                        buttonReturn.setOnClickListener {
                            // タイトル画面に移動
                            onBackPressed()
                        }
                    }
                    override fun onAnimationRepeat(p0: Animator?) {}
                    override fun onAnimationCancel(p0: Animator?) {}
                    override fun onAnimationStart(p0: Animator?) {}
                })
                textCollectionExplanation.setText(
                    resources.getIdentifier(
                        "collection_explanation_${stageId}_1",
                        "string",
                        packageName
                    )
                )
                textCollectionSource.setText(
                    resources.getIdentifier(
                        "collection_source_${stageId}",
                        "string",
                        packageName
                    )
                )
                layoutDescription.visibility = View.VISIBLE
                // リザルトボタンを成功時の画像にする
                buttonResult.setBackgroundResource(resources.getIdentifier(
                    "button_stage_result_success",
                    "drawable",
                    packageName
                ))
                // 最後のステージじゃなければ
                if (stageId < maxStage) {
                    // リザルトボタンを表示
                    buttonResult.visibility = View.VISIBLE
                }
                // 戻るボタンを表示
                buttonReturn.visibility = View.VISIBLE
                animatorResultButton.start()
            }
            override fun onAnimationRepeat(p0: Animator?) {}
            override fun onAnimationCancel(p0: Animator?) {}
            override fun onAnimationStart(p0: Animator?) {}
        })

        // 成功メッセージを3種類の中からランダムに設定する
        val resultTextType = (Random.nextInt(1, 300) % 3) + 1
        val resultExtTexts: IntArray = resources.getIntArray(R.array.result_success_ext_texts)
        val resultExtTextIndex = resultTextType - 1
        // キャラクター名の画像を設定
        imageResultCharacter.setImageResource(resources.getIdentifier(
            "text_stage_character_${stageId}",
            "drawable",
            packageName
        ))
        // 接続詞の画像を設定
        imageResultExt.setImageResource(resources.getIdentifier(
            "text_stage_result_ext_${resultExtTexts[resultExtTextIndex]}",
            "drawable",
            packageName
        ))
        // 成功メッセージの画像を設定
        imageResultText.setImageResource(resources.getIdentifier(
            "text_stage_result_success_${resultTextType}",
            "drawable",
            packageName
        ))
        // リザルトメッセージのレイアウトを表示
        layoutResultText.visibility = View.VISIBLE
        // スコア表示の処理
        setScore()
        animatorResult.start()
    }

    private fun setFailure() {
        // タイマー削除
        mTimer?.cancel()
        mTimer = null

        // BGMを小さくする
        setMediaVolumeDown()
        // 失敗音を鳴らす
        mSoundPool?.play(mSoundResIdFailure!!, 1.0f, 1.0f, 0, 0, 1.0f)
        // キャラクターを失敗画像に変更する
        imageCharacter.visibility = View.GONE
        imageCharacterFailure.visibility = View.VISIBLE

        // リザルトメッセージのアニメーション
        val animatorResult = AnimatorSet()
        animatorResult.duration = 1000
        animatorResult.playTogether(
            ObjectAnimator.ofFloat(layoutResultText, "alpha", 1f),
            ObjectAnimator.ofFloat(layoutResultText, "scaleX", 1f),
            ObjectAnimator.ofFloat(layoutResultText, "scaleY", 1f)
        )
        animatorResult.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(p0: Animator) {
                val animatorResultButton = AnimatorSet()
                animatorResultButton.duration = 500
                animatorResultButton.playTogether(
                    ObjectAnimator.ofFloat(buttonResult, "alpha", 1f),
                    ObjectAnimator.ofFloat(buttonReturn, "alpha", 1f),
                    ObjectAnimator.ofFloat(layoutScore, "alpha", 1f)
                )

                val lp = buttonResult.layoutParams
                val mlp = lp as MarginLayoutParams
                mlp.setMargins(mlp.leftMargin, mlp.topMargin, mlp.rightMargin, 36)
                buttonResult.layoutParams = mlp

                animatorResultButton.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(p0: Animator) {
                        // もう一度チャレンジボタンを押したときの動作
                        buttonResult.setOnClickListener {
                            // 同じステージに移動
                            onFailureTap()
                        }
                        // タイトルに戻るボタンを押したときの動作
                        buttonReturn.setOnClickListener {
                            // タイトル画面に移動
                            onBackPressed()
                        }
                    }
                    override fun onAnimationRepeat(p0: Animator) {}
                    override fun onAnimationCancel(p0: Animator) {}
                    override fun onAnimationStart(p0: Animator) {}
                })
                // リザルトボタンを失敗時の画像にする
                buttonResult.setBackgroundResource(
                    resources.getIdentifier(
                        "button_stage_result_failure",
                        "drawable",
                        packageName
                    )
                )
                // リザルトボタンを表示
                buttonResult.visibility = View.VISIBLE
                // 戻るボタンを表示
                buttonReturn.visibility = View.VISIBLE
                animatorResultButton.start()
            }
            override fun onAnimationRepeat(p0: Animator) {}
            override fun onAnimationCancel(p0: Animator) {}
            override fun onAnimationStart(p0: Animator) {}
        })

        // 失敗メッセージを3種類の中からランダムに設定する
        val resultTextType = (Random.nextInt(1, 300) % 3) + 1
        val resultExtTexts: IntArray = resources.getIntArray(R.array.result_failure_ext_texts)
        val resultExtTextIndex = resultTextType - 1

        // キャラクター名の画像を設定
        imageResultCharacter.setImageResource(resources.getIdentifier(
            "text_stage_character_${stageId}",
            "drawable",
            packageName
        ))
        // 接続詞の画像を設定
        imageResultExt.setImageResource(resources.getIdentifier(
            "text_stage_result_ext_${resultExtTexts[resultExtTextIndex]}",
            "drawable",
            packageName
        ))
        // 失敗メッセージの画像を設定
        imageResultText.setImageResource(resources.getIdentifier(
            "text_stage_result_failure_${resultTextType}",
            "drawable",
            packageName
        ))
        // リザルトメッセージのレイアウトを表示
        layoutResultText.visibility = View.VISIBLE
        // スコア表示の処理
        setScore()
        animatorResult.start()
    }

    private fun setScore() {
        // 保存データを取得
        val dataStore: SharedPreferences = getSharedPreferences("DataStore", Context.MODE_PRIVATE)
        // 現在のステージの最高スコアを取得
        val bestScorePoint = dataStore.getInt("score_${stageId}_1", 0)
        // 今回のスコアを計算
        val nowScorePoint = ( timeLimit - mTimerTask.getTimeCounter() ) * targetCount / 10
        // テキストに変換して設定
        textBestScorePoint.text = bestScorePoint.toString()
        textNowScorePoint.text = nowScorePoint.toString()

        // 今回のスコアが最高スコアよりも大きければ
        if (bestScorePoint < nowScorePoint) {
            // 今回のスコアを最高スコアとして保存
            val editor = dataStore.edit()
            editor.putInt("score_${stageId}_1", nowScorePoint)
            editor.apply()

            // 新記録用のテキスト色に変更
            textNowScorePoint.setTextColor(ContextCompat.getColor(this, R.color.colorScorePointNewRecord))
            textNowScoreNewRecord.setTextColor(ContextCompat.getColor(this, R.color.colorScorePointNewRecord))
            // 新記録のメッセージを表示
            textNowScoreNewRecord.visibility = View.VISIBLE
        } else {
            // 新記録のメッセージを非表示
            textNowScoreNewRecord.visibility = View.GONE
        }
        // 最高スコアが0の場合
        if (bestScorePoint == 0) {
            // 最高スコアのレイアウトを非表示
            layoutBestScore.visibility = View.GONE
        }
        // スコアのレイアウトを表示
        layoutScore.visibility = View.VISIBLE
    }

    private fun setTapEffectAnimator(event: MotionEvent) {
        // タップした座標を取得
        val tapPointX = event.rawX.toInt()
        val tapPointY = event.rawY.toInt()

        val effectAttackWidth = 30
        val effectAttackHeight = 60
        val effectHitWidth = 30
        val effectHitHeight = 30
        // エフェクト用の画像を準備
        val imageEffectAttack = addInvisibleImageView(
            layoutTapEffect,
            "effect_attack",
            effectAttackWidth.toFloat(),
            effectAttackHeight.toFloat()
        )
        val imageEffectHit = addInvisibleImageView(
            layoutTapEffect,
            "effect_hit",
            effectHitWidth.toFloat(),
            effectHitHeight.toFloat()
        )

        // ヒット画像を表示させる位置を取得
        val marginX = tapPointX - convertDp2Px((effectHitWidth / 2).toFloat()).toInt()
        val marginY = tapPointY - convertDp2Px((effectHitHeight / 2).toFloat()).toInt() - getStatusBarHeight(this)
        // マージンを設定して位置調整
        val imageEffectHitLayoutParams = imageEffectHit.layoutParams
        val imageEffectHitMarginLayoutParams = imageEffectHitLayoutParams as MarginLayoutParams
        imageEffectHitMarginLayoutParams.setMargins(marginX, marginY, 0, 0)
        imageEffectHit.layoutParams = imageEffectHitMarginLayoutParams

        // ConstraintSetを生成してConstraintLayoutから制約を複製する
        val constraintSet = ConstraintSet()
        constraintSet.clone(layoutTapEffect)
        // imageEffectAttackをimageAttackStartPointの上揃えでセット
        constraintSet.connect(imageEffectAttack.id, ConstraintSet.TOP, imageAttackStartPoint.id, ConstraintSet.TOP)
        constraintSet.connect(imageEffectAttack.id, ConstraintSet.START, imageAttackStartPoint.id, ConstraintSet.START)
        constraintSet.connect(imageEffectAttack.id, ConstraintSet.END, imageAttackStartPoint.id, ConstraintSet.END)
        // imageEffectHitを左上にセット
        constraintSet.connect(imageEffectHit.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
        constraintSet.connect(imageEffectHit.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
        // 編集した制約をConstraintLayoutに反映させる
        constraintSet.applyTo(layoutTapEffect)

        // 攻撃始点の中心座標を取得
        if (attackStartImageCenterX == 0 ) {
            imageAttackStartPoint.rotation = 0f
            val location = IntArray(2)
            imageAttackStartPoint.getLocationOnScreen(location)
            attackStartImageCenterX = (location[0] + (imageAttackStartPoint.width / 2))
            attackStartImageCenterY = (location[1] + (imageAttackStartPoint.height / 2))
        }
        // 攻撃始点の中心座標からタップした座標までの角度を取得
        val radian = getRadian(
            attackStartImageCenterX.toDouble(),
            attackStartImageCenterY.toDouble(),
            tapPointX.toDouble(),
            tapPointY.toDouble()
        )
        val degree: Double = radian * 180.0 / Math.PI
        // 攻撃始点の中心座標と攻撃エフェクトの開始位置までの各軸の移動距離を取得
        val attackPointX = (cos(radian) * ((imageAttackStartPoint.height - convertDp2Px((effectAttackHeight / 2).toFloat())) / 2)).toInt()
        val attackPointY = (sin(radian) * ((imageAttackStartPoint.height - convertDp2Px((effectAttackHeight / 2).toFloat())) / 2)).toInt()
        // 攻撃エフェクトの開始位置からタップした座標までの各軸の移動距離を取得
        val moveToX = tapPointX - attackStartImageCenterX - attackPointX
        val moveToY = tapPointY - attackStartImageCenterY - attackPointY
        // 攻撃エフェクトの直線の移動距離を取得
        val distance = getDistance(
            (attackStartImageCenterX - attackPointX).toDouble(),
            (attackStartImageCenterY - attackPointY).toDouble(),
            tapPointX.toDouble(),
            tapPointY.toDouble()
        )

        // アニメーションの設定
        val animatorEffectAttack = AnimatorSet()
        // アニメーション時間は移動距離から設定
        animatorEffectAttack.duration = (distance * 0.2).toLong()
        // 攻撃エフェクトを動かすアニメーションを設定
        animatorEffectAttack.playTogether(
            ObjectAnimator.ofFloat(imageEffectAttack, "translationX", 0f, moveToX.toFloat()),
            ObjectAnimator.ofFloat(imageEffectAttack, "translationY", 0f, moveToY.toFloat())
        )
        // アニメーションのイベントリスナーを設定
        animatorEffectAttack.addListener(object : Animator.AnimatorListener {
            override fun onAnimationEnd(p0: Animator) {
                // 攻撃エフェクト画像を非表示
                imageEffectAttack.visibility = View.GONE
                // imageEffectAttackとimageAttackStartPoint関連を外す
                val constraintSetReset = ConstraintSet()
                constraintSetReset.clone(layoutTapEffect)
                constraintSetReset.connect(imageEffectAttack.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
                constraintSetReset.connect(imageEffectAttack.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
                constraintSetReset.connect(imageEffectAttack.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
                constraintSetReset.applyTo(layoutTapEffect)
                // 攻撃エフェクト画像を削除
                layoutTapEffect.removeView(imageEffectAttack)

                // アニメーションの設定
                val animatorEffectHit = AnimatorSet()
                // アニメーション時間
                animatorEffectHit.duration = 200
                // ヒットエフェクトを動かすアニメーションを設定
                animatorEffectHit.playTogether(
                    ObjectAnimator.ofFloat(imageEffectHit, "scaleX", 0.5f, 1.5f),
                    ObjectAnimator.ofFloat(imageEffectHit, "scaleY", 0.5f, 1.5f),
                    ObjectAnimator.ofFloat(imageEffectHit, "alpha", 1.0f, 0.0f)
                )
                // アニメーションのイベントリスナーを設定
                animatorEffectHit.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationEnd(p0: Animator) {
                        // ヒットエフェクト画像を削除
                        layoutTapEffect.removeView(imageEffectHit)
                    }
                    override fun onAnimationRepeat(p0: Animator) {}
                    override fun onAnimationCancel(p0: Animator) {}
                    override fun onAnimationStart(p0: Animator) {
                        // キャラクターを震えさせるアニメーションを設定
                        val objectAnimator = ObjectAnimator.ofFloat(
                            imageCharacter,
                            "translationX",
                            2f,
                            -2f
                        )
                        objectAnimator.duration = 5
                        objectAnimator.start()
                        // ヒットエフェクト画像を表示
                        imageEffectHit.visibility = View.VISIBLE
                    }
                })
                animatorEffectHit.start()
            }
            override fun onAnimationRepeat(p0: Animator) {}
            override fun onAnimationCancel(p0: Animator) {}
            override fun onAnimationStart(p0: Animator) {
                // 回転軸の基準を設定
                imageEffectAttack.pivotX = convertDp2Px((effectHitWidth / 2).toFloat())
                imageEffectAttack.pivotY = (imageAttackStartPoint.height / 2).toFloat()
                // タップした方向に向けて回転させる
                imageEffectAttack.rotation = degree.toFloat() + 90f
                imageAttackStartPoint.rotation = degree.toFloat() + 90f
                // 攻撃エフェクト画像を表示
                imageEffectAttack.visibility = View.VISIBLE
            }
        })
        // アニメーションスタート
        animatorEffectAttack.start()
    }

    private fun getDistance(x: Double, y: Double, x2: Double, y2: Double): Int {
        val distance = sqrt((x2 - x) * (x2 - x) + (y2 - y) * (y2 - y))
        return distance.toInt()
    }

    private fun getRadian(x: Double, y: Double, x2: Double, y2: Double): Double {
        return atan2(y2 - y, x2 - x)
    }

    fun convertDp2Px(dp: Float): Float {
        val metrics: DisplayMetrics = resources.displayMetrics
        return dp * metrics.density
    }

    private fun getStatusBarHeight(activity: Activity): Int {
        val rect = Rect()
        val window: Window = activity.window
        window.decorView.getWindowVisibleDisplayFrame(rect)
        return rect.top
    }

    private fun addInvisibleImageView(constraintLayout: ConstraintLayout, drawableName: String, width: Float, height: Float): ImageView {

        // ImageViewオブジェクトを作成する。
        val imageView = ImageView(this)
        imageView.setImageResource(
            resources.getIdentifier(
                drawableName,
                "drawable",
                packageName
            )
        )
        // LayoutParamsオブジェクトを設定
        imageView.layoutParams = LinearLayout.LayoutParams(
            convertDp2Px(width).toInt(),
            convertDp2Px(height).toInt()
        )
        // 画像を非表示に設定
        imageView.visibility = View.INVISIBLE
        // 画像をレイアウトに追加
        constraintLayout.addView(imageView)
        // 画像にIDを設定
        imageView.id = View.generateViewId()
        // 作成したImageViewオブジェクトを返却
        return imageView
    }
}
