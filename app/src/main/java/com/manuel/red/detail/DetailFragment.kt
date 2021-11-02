package com.manuel.red.detail

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.forEachIndexed
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
    private var mainTitle = ""
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
        setupActionBar()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (activity as? MainAux)?.showButton(true)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.forEachIndexed { _, item ->
            item.isVisible = false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getPackageService() {
        packageService = (activity as? MainAux)?.getPackageServiceSelected()
        packageService?.let { pack ->
            binding?.let { binding ->
                binding.tvName.text = pack.name
                binding.tvDescription.text =
                    "${getString(R.string.description)}: ${pack.description}"
                binding.tvPrice.text = "${getString(R.string.price)}: $${pack.price} MXN"
                binding.tvSpeed.text = "${getString(R.string.speed)}: ${pack.speed} Mbps"
                binding.tvLimit.text = "${getString(R.string.limit)}: ${pack.limit} Gbps"
                binding.tvValidity.text =
                    "${getString(R.string.validity)}: ${pack.validity} ${getString(R.string.months)}"
                binding.tvAvailable.text =
                    getString(R.string.detail_available, pack.available)
                setNewAvailable(pack)
                context?.let { context ->
                    val packageServiceRef =
                        FirebaseStorage.getInstance().reference.child(pack.administratorId)
                            .child(Constants.PATH_PACKAGE_SERVICE_IMAGES)
                            .child(pack.id!!)
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

    private fun setupActionBar() {
        (activity as? AppCompatActivity)?.let { activity ->
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            mainTitle = activity.supportActionBar?.title.toString()
            activity.supportActionBar?.title = packageService?.name
            setHasOptionsMenu(true)
        }
    }
}