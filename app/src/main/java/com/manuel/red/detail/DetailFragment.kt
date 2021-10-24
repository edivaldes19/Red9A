package com.manuel.red.detail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.text.HtmlCompat
import androidx.fragment.app.Fragment
import com.google.firebase.storage.FirebaseStorage
import com.manuel.red.R
import com.manuel.red.databinding.FragmentDetailBinding
import com.manuel.red.models.PackageService
import com.manuel.red.package_service.MainAux
import com.manuel.red.utils.Constants

class DetailFragment : Fragment() {
    private var binding: FragmentDetailBinding? = null
    private var packageService: PackageService? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDetailBinding.inflate(inflater, container, false)
        binding?.let { view ->
            return view.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getPackageService()
        setupButtons()
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.showButton(true)
        super.onDestroyView()
        binding = null
    }

    @SuppressLint("SetTextI18n")
    private fun getPackageService() {
        packageService = (activity as? MainAux)?.getPackageServiceSelected()
        packageService?.let { packageService1 ->
            binding?.let { binding ->
                binding.tvName.text = packageService1.name
                binding.tvDescription.text =
                    "${getString(R.string.description)}: ${packageService1.description}"
                binding.tvPrice.text = "${getString(R.string.price)}: $${packageService1.price} MXN"
                binding.tvSpeed.text = "${getString(R.string.speed)}: ${packageService1.speed} Mbps"
                binding.tvLimit.text = "${getString(R.string.limit)}: ${packageService1.limit} Gbps"
                binding.tvValidity.text =
                    "${getString(R.string.validity)}: ${packageService1.validity} ${getString(R.string.months)}"
                binding.tvAvailable.text =
                    getString(R.string.detail_available, packageService1.available)
                setNewAvailable(packageService1)
                context?.let { context ->
                    val packageServiceRef =
                        FirebaseStorage.getInstance().reference.child(packageService1.administratorId)
                            .child(Constants.PATH_PACKAGE_SERVICE_IMAGES)
                            .child(packageService1.id!!)
                    packageServiceRef.listAll().addOnSuccessListener { imgList ->
                        val detailAdapter = DetailAdapter(imgList.items, context)
                        binding.vpPackageService.apply {
                            adapter = detailAdapter
                        }
                    }
                }
            }
        }
    }

    private fun setNewAvailable(packageService: PackageService) {
        binding?.let { view ->
            view.etNewAvailable.setText(packageService.newAvailable.toString())
            val newAvailableStr = getString(
                R.string.detail_total_price,
                packageService.totalPrice(),
                packageService.newAvailable,
                packageService.price
            )
            view.tvTotalPrice.text =
                HtmlCompat.fromHtml(newAvailableStr, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    }

    private fun setupButtons() {
        packageService?.let { packageService ->
            binding?.let { binding ->
                binding.fabSub.setOnClickListener {
                    if (packageService.newAvailable > 1) {
                        packageService.newAvailable--
                        setNewAvailable(packageService)
                        binding.fabSub.isEnabled = true
                        binding.fabSum.isEnabled = true
                    } else if (packageService.newAvailable == 1) {
                        binding.fabSub.isEnabled = false
                        binding.fabSum.isEnabled = true
                    }
                }
                binding.fabSum.setOnClickListener {
                    if (packageService.newAvailable < 5) {
                        packageService.newAvailable++
                        setNewAvailable(packageService)
                        binding.fabSum.isEnabled = true
                        binding.fabSub.isEnabled = true
                    } else if (packageService.newAvailable == 5) {
                        binding.fabSum.isEnabled = false
                        binding.fabSub.isEnabled = true
                    }
                }
                binding.efab.setOnClickListener {
                    packageService.newAvailable = binding.etNewAvailable.text.toString().toInt()
                    addToContractList(packageService)
                }
            }
        }
    }

    private fun addToContractList(packageService: PackageService) {
        (activity as? MainAux)?.let { mainAux ->
            mainAux.addPackageServiceToContractList(packageService)
            activity?.onBackPressed()
        }
    }
}