package com.manuel.red.offers_and_promotions

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.manuel.red.R
import com.manuel.red.databinding.FragmentOffersAndPromotionsBinding
import com.manuel.red.package_service.MainAux
import com.manuel.red.utils.Constants

class OffersAndPromotionsFragment : Fragment() {
    private var binding: FragmentOffersAndPromotionsBinding? = null
    private var mainTitle: String = ""
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOffersAndPromotionsBinding.inflate(inflater, container, false)
        binding?.let { view ->
            return view.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configRemoteConfig()
        configActionBar()
    }

    private fun configActionBar() {
        (activity as? AppCompatActivity)?.let { activity ->
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mainTitle = activity.supportActionBar?.title.toString().trim()
            activity.supportActionBar?.title = getString(R.string.offers_and_promotions)
            setHasOptionsMenu(true)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun configRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val percentage = remoteConfig.getLong(Constants.PROP_PERCENTAGE)
                val imagePath = remoteConfig.getString(Constants.PROP_IMAGE_PATH)
                val message = remoteConfig.getString(Constants.PROP_MESSAGE)
                binding?.let { view ->
                    view.tvMessage.text = message
                    view.tvPercentage.text = "${getString(R.string.discount_from)} $percentage%"
                    Glide.with(this).load(imagePath).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .placeholder(R.drawable.ic_cloud_download).error(R.drawable.ic_store)
                        .centerCrop().into(view.imgOffersAndPromotions)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.showButton(true)
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        (activity as? AppCompatActivity)?.let { activity ->
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            activity.supportActionBar?.title = mainTitle
            setHasOptionsMenu(false)
        }
        super.onDestroy()
    }
}