package com.manuel.red.contract_list

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.manuel.red.R
import com.manuel.red.databinding.FragmentContractListBinding
import com.manuel.red.models.PackageService
import com.manuel.red.models.PackageServiceContract
import com.manuel.red.models.RequestedContract
import com.manuel.red.package_service.MainAux
import com.manuel.red.requested_contract.RequestedContractActivity
import com.manuel.red.utils.Constants
import java.util.*

class ContractListFragment : BottomSheetDialogFragment(), OnContractListListener {
    private var binding: FragmentContractListBinding? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>
    private lateinit var packageServiceContractListAdapter: PackageServiceContractListAdapter
    private var totalPrice = 0
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = FragmentContractListBinding.inflate(LayoutInflater.from(activity))
        binding?.let { view ->
            val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
            bottomSheetDialog.setContentView(view.root)
            bottomSheetBehavior = BottomSheetBehavior.from(view.root.parent as View)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            setupRecyclerView()
            setupButtons()
            getPackageServices()
            configAnalytics()
            return bottomSheetDialog
        }
        return super.onCreateDialog(savedInstanceState)
    }

    private fun setupRecyclerView() {
        binding?.let { view ->
            packageServiceContractListAdapter =
                PackageServiceContractListAdapter(mutableListOf(), this)
            view.recyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = this@ContractListFragment.packageServiceContractListAdapter
            }
        }
    }

    private fun setupButtons() {
        binding?.let { view ->
            view.ibCancel.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
            }
            view.efab.setOnClickListener {
                requestContractTransaction()
            }
        }
    }

    private fun getPackageServices() {
        (activity as? MainAux)?.getPackagesServicesContractList()?.forEach { packageService ->
            packageServiceContractListAdapter.add(packageService)
        }
    }

    private fun configAnalytics() {
        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.METHOD, "check_contract_status")
        }
    }

    private fun requestContractTransaction() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.let { myUser ->
            enableUI(false)
            val packageServices = hashMapOf<String, PackageServiceContract>()
            packageServiceContractListAdapter.getPackagesServices().forEach { packageService ->
                packageServices[packageService.id!!] = PackageServiceContract(
                    packageService.id!!,
                    packageService.name!!,
                    packageService.newAvailable
                )
            }
            val requestedContract = RequestedContract(
                userId = myUser.uid,
                packagesServices = packageServices,
                totalPrice = totalPrice,
                status = 1,
                requested = Date().time
            )
            val db = FirebaseFirestore.getInstance()
            val requestDoc = db.collection(Constants.COLL_CONTRACTS_REQUESTED).document()
            val packageServicesRef = db.collection(Constants.COLL_PACKAGE_SERVICE)
            db.runBatch { batch ->
                batch.set(requestDoc, requestedContract)
                requestedContract.packagesServices.forEach { entry ->
                    batch.update(
                        packageServicesRef.document(entry.key), Constants.PROP_AVAILABLE,
                        FieldValue.increment(-entry.value.available.toLong())
                    )
                }
            }.addOnSuccessListener {
                dismiss()
                (activity as? MainAux)?.clearContractList()
                startActivity(Intent(context, RequestedContractActivity::class.java))
                Toast.makeText(
                    activity,
                    getString(R.string.requested_contracts_successfully),
                    Toast.LENGTH_SHORT
                ).show()
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_PAYMENT_INFO) {
                    val services = mutableListOf<Bundle>()
                    requestedContract.packagesServices.forEach { entry ->
                        if (entry.value.available >= 3) {
                            val bundle = Bundle()
                            bundle.putString("id_package_service", entry.key)
                            services.add(bundle)
                        }
                    }
                    param(FirebaseAnalytics.Param.QUANTITY, services.toTypedArray())
                }
                firebaseAnalytics.setUserProperty(
                    Constants.USER_PROP_DISCOUNT,
                    if (packageServices.size > 0) {
                        "with_discount"
                    } else {
                        "without_discount"
                    }
                )
            }.addOnFailureListener {
                Snackbar.make(
                    binding!!.root,
                    getString(R.string.error_requesting_contracts),
                    Snackbar.LENGTH_SHORT
                ).show()
            }.addOnCompleteListener {
                enableUI(true)
            }
        }
    }

    private fun enableUI(enable: Boolean) {
        binding?.let { view ->
            view.ibCancel.isEnabled = enable
            view.efab.isEnabled = enable
        }
    }

    override fun onDestroyView() {
        (activity as? MainAux)?.updateTotal()
        super.onDestroyView()
        binding = null
    }

    override fun setAmount(packageService: PackageService) {
        packageServiceContractListAdapter.update(packageService)
    }

    override fun showTotal(total: Int) {
        totalPrice = total
        binding?.let { view ->
            view.tvTotal.text =
                getString(R.string.package_service_full_contract_list, total)
        }
    }
}