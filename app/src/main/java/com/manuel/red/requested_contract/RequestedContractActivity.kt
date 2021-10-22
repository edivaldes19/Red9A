package com.manuel.red.requested_contract

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.manuel.red.R
import com.manuel.red.chat.ChatFragment
import com.manuel.red.contract_status.ContractStatusFragment
import com.manuel.red.databinding.ActivityRequestedContractBinding
import com.manuel.red.models.RequestedContract
import com.manuel.red.utils.Constants

class RequestedContractActivity : AppCompatActivity(), OnRequestedContractListener,
    RequestedContractAux {
    private lateinit var binding: ActivityRequestedContractBinding
    private lateinit var requestedContractAdapter: RequestedContractAdapter
    private lateinit var requestedContractSelected: RequestedContract
    private val errorSnack: Snackbar by lazy {
        Snackbar.make(binding.root, "", Snackbar.LENGTH_SHORT).setTextColor(Color.RED)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestedContractBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        setupFirestore()
        checkIntent(intent)
    }

    override fun onRequestedContractStatus(requestedContract: RequestedContract) {
        requestedContractSelected = requestedContract
        val fragment = ContractStatusFragment()
        supportFragmentManager.beginTransaction().add(R.id.containerMain, fragment)
            .addToBackStack(null).commit()
    }

    override fun onStartChat(requestedContract: RequestedContract) {
        requestedContractSelected = requestedContract
        val fragment = ChatFragment()
        supportFragmentManager.beginTransaction().add(R.id.containerMain, fragment)
            .addToBackStack(null).commit()
    }

    override fun getRequestedContractSelected(): RequestedContract = requestedContractSelected
    private fun checkIntent(intent: Intent?) {
        intent?.let { intent1 ->
            val actionIntent = intent1.getIntExtra(Constants.ACTION_INTENT, 0)
            if (actionIntent == 1) {
                val id = intent.getStringExtra(Constants.PROP_ID) ?: ""
                val status = intent.getIntExtra(Constants.PROP_STATUS, 0)
                requestedContractSelected = RequestedContract(id = id, status = status)
                val fragment = ContractStatusFragment()
                supportFragmentManager.beginTransaction().add(R.id.containerMain, fragment)
                    .addToBackStack(null).commit()
            }
        }
    }

    private fun setupRecyclerView() {
        requestedContractAdapter = RequestedContractAdapter(mutableListOf(), this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RequestedContractActivity)
            adapter = this@RequestedContractActivity.requestedContractAdapter
        }
    }

    private fun setupFirestore() {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val db = FirebaseFirestore.getInstance()
            db.collection(Constants.COLL_CONTRACTS_REQUESTED)
                .whereEqualTo(Constants.PROP_USER_ID, user.uid)
                .orderBy(Constants.PROP_DATE, Query.Direction.DESCENDING).get()
                .addOnSuccessListener { querySnapshot ->
                    for (document in querySnapshot) {
                        val requestedContract = document.toObject(RequestedContract::class.java)
                        requestedContract.id = document.id
                        requestedContractAdapter.add(requestedContract)
                    }
                }.addOnFailureListener {
                    errorSnack.apply {
                        setText(getString(R.string.failed_to_query_the_data))
                        show()
                    }
                }
        }
    }
}