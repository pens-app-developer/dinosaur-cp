package com.p_en_s.dinosaurhuntergaogaobeam.ui.home

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat.getDrawable
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.findNavController
import com.p_en_s.dinosaurhuntergaogaobeam.MainActivity
import com.p_en_s.dinosaurhuntergaogaobeam.OnBackPressedListener
import com.p_en_s.dinosaurhuntergaogaobeam.R
import kotlinx.android.synthetic.main.fragment_home.*


class HomeFragment : Fragment(), OnBackPressedListener {

    private var thisActivity: FragmentActivity = FragmentActivity()
    private var clearStageId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        thisActivity = this.requireActivity()

        // 保存データを取得
        val dataStore: SharedPreferences = thisActivity.getSharedPreferences(
            "DataStore",
            Context.MODE_PRIVATE
        )
        // クリア済みのステージIDを取得
        clearStageId = dataStore.getInt("clearStageId", 0)


        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onBackPressed() {
        // タイトルに戻る
        val intent = Intent(thisActivity.application, MainActivity::class.java)
        intent.putExtra("fromStage", 1)
        startActivity(intent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // クリック時の動作設定
        buttonReturn.setOnClickListener {
            onBackPressed()
        }

        var stageId = 0
        while (true){
            stageId++
            // アイテム用のIDを取得
            val itemResourcesId = resources.getIdentifier(
                "nav_gallery_${stageId}",
                "id",
                thisActivity.packageName
            )
            // 画像IDを取得
            val imageResourcesId = resources.getIdentifier(
                "character_${stageId}_1",
                "drawable",
                thisActivity.packageName
            )
            // テキストIDを取得
            val textResourcesId = resources.getIdentifier(
                "stage_${stageId}",
                "string",
                thisActivity.packageName
            )
            // 画面遷移用のIDを取得
            val actionResourcesId = resources.getIdentifier(
                "action_nav_home_to_nav_gallery_${stageId}",
                "id",
                thisActivity.packageName
            )
            // 画像IDが取得できたか判定
            if (itemResourcesId > 0) {
                // LinearLayoutオブジェクトを作成する。
                val linearLayout = LinearLayout(view.context)
                linearLayout.id = View.generateViewId()
                linearLayout.orientation = LinearLayout.HORIZONTAL
                linearLayout.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                val linearLayoutLayoutParams = linearLayout.layoutParams
                val linearLayoutMarginLayoutParams = linearLayoutLayoutParams as ViewGroup.MarginLayoutParams
                linearLayoutMarginLayoutParams.setMargins(0, 8, 0, 0)
                linearLayout.layoutParams = linearLayoutMarginLayoutParams
                // LinearLayoutを追加
                layoutStageList.addView(linearLayout)

                // ImageViewオブジェクトを作成する。
                val imageView = ImageView(view.context)
                // LayoutParamsオブジェクトを設定
                imageView.layoutParams = LinearLayout.LayoutParams(
                    convertDp2Px(60f).toInt(),
                    convertDp2Px(60f).toInt()
                )

                // TextViewオブジェクトを作成する。
                val textView = TextView(view.context)
                textView.textSize = 20f
                textView.gravity = Gravity.START + Gravity.CENTER_VERTICAL
                // LayoutParamsオブジェクトを設定
                textView.layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    convertDp2Px(60f).toInt()
                )
                val textViewLayoutParams = textView.layoutParams
                val textViewMarginLayoutParams = textViewLayoutParams as ViewGroup.MarginLayoutParams
                textViewMarginLayoutParams.setMargins(8, 0, 0, 0)
                textView.layoutParams = textViewMarginLayoutParams

                // クリア済みのステージか判定
                if (clearStageId >= stageId) {
                    linearLayout.foreground = getDrawable(thisActivity, R.drawable.background_collection_item_list)
                    // 画像を設定
                    imageView.setImageResource(imageResourcesId)
                    // テキストを設定
                    textView.setText(textResourcesId)
                    // クリック時の動作設定
                    linearLayout.setOnClickListener {
                        findNavController().navigate(actionResourcesId)
                    }
                } else {
                    // 画像を設定
                    imageView.setImageResource(R.drawable.icon_question)
                    // テキストを設定
                    textView.setText(R.string.stage_close)
                }
                // 画像をレイアウトに追加
                linearLayout.addView(imageView)
                // テキストをレイアウトに追加
                linearLayout.addView(textView)
            } else {
                // 取得できなかったらループを終了
                break
            }
        }
    }

    private fun convertDp2Px(dp: Float): Float {
        val metrics: DisplayMetrics = resources.displayMetrics
        return dp * metrics.density
    }
}