package com.manuel.red.contract_status

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.forEachIndexed
import androidx.fragment.app.Fragment
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.manuel.red.R
import com.manuel.red.databinding.FragmentContractStatusBinding
import com.manuel.red.models.RequestedContract
import com.manuel.red.requested_contract.OnRequestedContractSelected
import com.manuel.red.utils.Constants

class ContractStatusFragment : Fragment() {
    private var binding: FragmentContractStatusBinding? = null
    private var requestedContract: RequestedContract? = null
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentContractStatusBinding.inflate(inflater, container, false)
        binding?.let { view ->
            return view.root
        }
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getRequestedContract()
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

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        (activity as? AppCompatActivity)?.let { activity ->
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            activity.supportActionBar?.title = getString(R.string.my_contracts)
            setHasOptionsMenu(false)
        }
    }

    private fun getRequestedContract() {
        requestedContract =
            (activity as? OnRequestedContractSelected)?.getRequestedContractSelected()
        requestedContract?.let { contract ->
            updateUI(contract)
            getRequestedContractInRealtime(contract.id)
            setupActionBar()
            setupAnalytics()
        }
    }

    private fun updateUI(requestedContract: RequestedContract) {
        binding?.let { view ->
            view.progressBar.progress = when (requestedContract.status) {
                1 -> 10
                2 -> 50
                3 -> 100
                else -> 0
            }
            view.cbOnHold.isChecked = requestedContract.status > 0
            view.cbActivated.isChecked = requestedContract.status > 1
            view.cbTimedOut.isChecked = requestedContract.status > 2
        }
    }

    private fun getRequestedContractInRealtime(requestedContractId: String) {
        val db = Firebase.firestore
        val reference =
            db.collection(Constants.COLL_CONTRACTS_REQUESTED).document(requestedContractId)
        reference.addSnapshotListener { documentSnapshot, error ->
            if (error != null) {
                Toast.makeText(
                    activity,
                    getString(R.string.error_when_consulting_this_contract),
                    Toast.LENGTH_SHORT
                ).show()
                return@addSnapshotListener
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                val requestedContract = documentSnapshot.toObject(RequestedContract::class.java)
                requestedContract?.let { requestedContract1 ->
                    requestedContract1.id = documentSnapshot.id
                    updateUI(requestedContract1)
                }
            }
        }
    }

    private fun setupActionBar() {
        (activity as? AppCompatActivity)?.let { activity ->
            activity.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            activity.supportActionBar?.title = getString(R.string.contract_status)
            setHasOptionsMenu(true)
        }
    }

    private fun setupAnalytics() {
        firebaseAnalytics = Firebase.analytics
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.METHOD, Constants.PROP_CHECK_CONTRACT_STATUS)
        }
    }
}