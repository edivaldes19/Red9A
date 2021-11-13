package com.manuel.red.offers_and_promotions

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEach
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.ktx.Firebase
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.manuel.red.R
import com.manuel.red.databinding.FragmentOffersAndPromotionsBinding
import com.manuel.red.package_service.OnMethodsToMainActivity
import com.manuel.red.utils.Constants

class OffersAndPromotionsFragment : Fragment() {
    private var binding: FragmentOffersAndPromotionsBinding? = null
    private var mainTitle = ""
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
        setupRemoteConfig()
        setupActionBar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? OnMethodsToMainActivity)?.showButton(true)
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as? AppCompatActivity)?.let { activity ->
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            activity.supportActionBar?.title = mainTitle
            setHasOptionsMenu(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.forEach { item ->
            item.isVisible = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setupRemoteConfig() {
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
                        .into(view.imgOffersAndPromotions)
                }
            }
        }
    }

    private fun setupActionBar() {
        (activity as? AppCompatActivity)?.let { activity ->
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mainTitle = activity.supportActionBar?.title.toString()
            activity.supportActionBar?.title = getString(R.string.offers_and_promotions)
            setHasOptionsMenu(true)
        }
    }
}