package com.p_en_s.dinosaurhuntergaogaobeam.ui.gallery

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import com.p_en_s.dinosaurhuntergaogaobeam.R
import kotlinx.android.synthetic.main.fragment_gallery.*

class GalleryFragment: Fragment() {

    private val args: GalleryFragmentArgs by navArgs()
    private var stageId: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stageId = args.stageId

        imageCharacter.setImageResource(
            resources.getIdentifier(
                "character_${stageId}_1",
                "drawable",
                activity?.packageName
            )
        )
        textJapaneseName.setText(
            resources.getIdentifier(
                "collection_japanese_name_${stageId}",
                "string",
                activity?.packageName
            )
        )
        textScientificName.setText(
            resources.getIdentifier(
                "collection_scientific_name_${stageId}",
                "string",
                activity?.packageName
            )
        )
        textGeologicalTime.setText(
            resources.getIdentifier(
                "collection_geological_time_${stageId}",
                "string",
                activity?.packageName
            )
        )
        textLength.setText(
            resources.getIdentifier(
                "collection_length_${stageId}",
                "string",
                activity?.packageName
            )
        )
        textWeight.setText(
            resources.getIdentifier(
                "collection_weight_${stageId}",
                "string",
                activity?.packageName
            )
        )
        textExplanation1.setText(
            resources.getIdentifier(
                "collection_explanation_${stageId}_1",
                "string",
                activity?.packageName
            )
        )
        textExplanation2.setText(
            resources.getIdentifier(
                "collection_explanation_${stageId}_2",
                "string",
                activity?.packageName
            )
        )
        textSource.setText(
            resources.getIdentifier(
                "collection_source_${stageId}",
                "string",
                activity?.packageName
            )
        )


    }
}