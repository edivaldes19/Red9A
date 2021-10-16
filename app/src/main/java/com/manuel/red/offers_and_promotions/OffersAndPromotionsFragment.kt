package com.manuel.red.offers_and_promotions

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

class OffersAndPromotionsFragment : Fragment() {
    private var binding: FragmentOffersAndPromotionsBinding? = null
    private var mainTitle: String = ""
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentOffersAndPromotionsBinding.inflate(inflater, container, false)
        binding?.let { fragmentOffersAndPromotionsBinding ->
            return fragmentOffersAndPromotionsBinding.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        configRemoteConfig()
        configActionBar()
    }

    private fun configActionBar() {
        (activity as? AppCompatActivity)?.let { appCompatActivity ->
            appCompatActivity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mainTitle = appCompatActivity.supportActionBar?.title.toString()
            appCompatActivity.supportActionBar?.title = getString(R.string.offers_and_promotions)
            setHasOptionsMenu(true)
        }
    }

    private fun configRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val percentage = remoteConfig.getDouble("percentage")
                val photoUrl = remoteConfig.getString("imagePath")
                val message = remoteConfig.getString("message")
                binding?.let { fragmentOffersAndPromotionsBinding ->
                    fragmentOffersAndPromotionsBinding.tvMessage.text = message
                    fragmentOffersAndPromotionsBinding.tvPercentage.text = percentage.toString()
                    Glide.with(this).load(photoUrl).diskCacheStrategy(DiskCacheStrategy.NONE)
                        .placeholder(R.drawable.ic_error_outline).error(R.drawable.ic_local_offer)
                        .centerCrop()
                        .into(fragmentOffersAndPromotionsBinding.imgOffersAndPromotions)
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
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            it.supportActionBar?.title = mainTitle
            setHasOptionsMenu(false)
        }
        super.onDestroy()
    }
}