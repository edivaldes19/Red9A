package com.manuel.red.requested_contract

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
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
    private lateinit var listenerRegistration: ListenerRegistration
    private var requestedContractList: MutableList<RequestedContract> = mutableListOf()
    private val errorSnack: Snackbar by lazy {
        Snackbar.make(binding.root, "", Snackbar.LENGTH_SHORT).setTextColor(Color.YELLOW)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestedContractBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        checkIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        configFirestoreRealtime()
    }

    override fun onPause() {
        super.onPause()
        listenerRegistration.remove()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_just_search, menu)
        val menuItem = menu?.findItem(R.id.action_search)
        val searchView = menuItem?.actionView as SearchView
        searchView.queryHint = getString(R.string.write_here_to_search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val temporaryList: MutableList<RequestedContract> = ArrayList()
                for (requestedContract in requestedContractList) {
                    if (newText!! in requestedContract.id) {
                        temporaryList.add(requestedContract)
                    }
                }
                requestedContractAdapter.updateList(temporaryList)
                if (temporaryList.isNullOrEmpty()) {
                    binding.tvWithoutResults.visibility = View.VISIBLE
                } else {
                    binding.tvWithoutResults.visibility = View.GONE
                }
                return false
            }
        })
        return super.onCreateOptionsMenu(menu)
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

    override fun onDeleteRequestedContract(requestedContract: RequestedContract) {
        requestedContractSelected = requestedContract
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.remove_contract))
            .setMessage(getString(R.string.are_you_sure_to_take_this_action))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                val db = FirebaseFirestore.getInstance()
                val requestedContractRef = db.collection(Constants.COLL_CONTRACTS_REQUESTED)
                requestedContractRef.document(requestedContract.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "${getString(R.string.contract)}: ${requestedContract.id} eliminado.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {
                        errorSnack.apply {
                            setText(getString(R.string.failed_to_remove_package))
                            show()
                        }
                    }
            }.setNegativeButton(getString(R.string.cancel), null).show()
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
        requestedContractAdapter = RequestedContractAdapter(requestedContractList, this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RequestedContractActivity)
            adapter = this@RequestedContractActivity.requestedContractAdapter
        }
    }

    private fun configFirestoreRealtime() {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val db = FirebaseFirestore.getInstance()
            val requestedContractRef = db.collection(Constants.COLL_CONTRACTS_REQUESTED)
                .whereEqualTo(Constants.PROP_USER_ID, user.uid)
                .orderBy(Constants.PROP_REQUESTED, Query.Direction.DESCENDING)
            listenerRegistration = requestedContractRef.addSnapshotListener { snapshots, error ->
                if (error != null) {
                    errorSnack.apply {
                        setText(getString(R.string.failed_to_query_the_data))
                        show()
                    }
                    return@addSnapshotListener
                }
                for (snapshot in snapshots!!.documentChanges) {
                    val requestedContract =
                        snapshot.document.toObject(RequestedContract::class.java)
                    requestedContract.id = snapshot.document.id
                    if (snapshot.type == DocumentChange.Type.ADDED) {
                        requestedContractAdapter.add(requestedContract)
                    } else if (snapshot.type == DocumentChange.Type.REMOVED) {
                        requestedContractAdapter.delete(requestedContract)
                    }
                }
            }
        }
    }
}