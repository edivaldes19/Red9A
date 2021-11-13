package com.manuel.red.requested_contract

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Bundle
import android.provider.Settings
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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.manuel.red.R
import com.manuel.red.chat.ChatFragment
import com.manuel.red.contract_status.ContractStatusFragment
import com.manuel.red.databinding.ActivityRequestedContractBinding
import com.manuel.red.models.RequestedContract
import com.manuel.red.utils.ConnectionReceiver
import com.manuel.red.utils.Constants

class RequestedContractActivity : AppCompatActivity(), OnRequestedContractListener,
    OnRequestedContractSelected, ConnectionReceiver.ReceiverListener {
    private lateinit var binding: ActivityRequestedContractBinding
    private lateinit var contractAdapter: RequestedContractAdapter
    private lateinit var requestedContractSelected: RequestedContract
    private lateinit var listenerRegistration: ListenerRegistration
    private var requestedContractList = mutableListOf<RequestedContract>()
    private val snackBar: Snackbar by lazy {
        Snackbar.make(binding.root, "", Snackbar.LENGTH_SHORT).setTextColor(Color.YELLOW)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRequestedContractBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupRecyclerView()
        checkIntent(intent)
        checkInternetConnection()
    }

    override fun onResume() {
        super.onResume()
        setupFirestoreInRealtime()
    }

    override fun onPause() {
        super.onPause()
        listenerRegistration.remove()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_just_search, menu)
        val menuItem = menu?.findItem(R.id.action_search)
        val searchView = menuItem?.actionView as SearchView
        searchView.queryHint = getString(R.string.search_by_id)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = mutableListOf<RequestedContract>()
                for (requestedContract in requestedContractList) {
                    if (newText!!.lowercase() in requestedContract.id.lowercase()) {
                        filteredList.add(requestedContract)
                    }
                }
                contractAdapter.updateList(filteredList)
                binding.tvWithoutResults.visibility = if (filteredList.isNullOrEmpty()) {
                    View.VISIBLE
                } else {
                    View.GONE
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
        MaterialAlertDialogBuilder(this).setTitle(getString(R.string.delete_contract))
            .setMessage("${getString(R.string.contract_id_text_only)}: ${requestedContract.id}")
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                val db = Firebase.firestore
                val requestedContractRef = db.collection(Constants.COLL_CONTRACTS_REQUESTED)
                requestedContractRef.document(requestedContract.id).delete()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "${getString(R.string.contract)}: ${requestedContract.id} ${getString(R.string.removed)}.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    .addOnFailureListener {
                        snackBar.apply {
                            setText(getString(R.string.failed_to_remove_package))
                            show()
                        }
                    }
            }.setNegativeButton(getString(R.string.cancel), null).show()
    }

    override fun getRequestedContractSelected() = requestedContractSelected
    override fun onNetworkChange(isConnected: Boolean) = showNetworkErrorSnackBar(isConnected)
    private fun checkInternetConnection() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(Constants.ACTION_INTENT)
        registerReceiver(ConnectionReceiver(), intentFilter)
        ConnectionReceiver.receiverListener = this
        val manager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting
        showNetworkErrorSnackBar(isConnected)
    }

    private fun showNetworkErrorSnackBar(isConnected: Boolean) {
        if (!isConnected) {
            Snackbar.make(
                binding.root,
                getString(R.string.no_network_connection),
                Snackbar.LENGTH_INDEFINITE
            ).setTextColor(Color.WHITE)
                .setAction(getString(R.string.go_to_settings)) { startActivity(Intent(Settings.ACTION_WIFI_SETTINGS)) }
                .show()
        }
    }

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
        contractAdapter = RequestedContractAdapter(requestedContractList, this)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@RequestedContractActivity)
            adapter = this@RequestedContractActivity.contractAdapter
        }
    }

    private fun setupFirestoreInRealtime() {
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val db = Firebase.firestore
            val requestedContractRef = db.collection(Constants.COLL_CONTRACTS_REQUESTED)
                .whereEqualTo(Constants.PROP_USER_ID, user.uid)
                .orderBy(Constants.PROP_REQUESTED, Query.Direction.DESCENDING)
            listenerRegistration =
                requestedContractRef.addSnapshotListener { querySnapshot, error ->
                    if (error != null) {
                        snackBar.apply {
                            setText(getString(R.string.failed_to_query_the_data))
                            show()
                        }
                        return@addSnapshotListener
                    }
                    for (documentChange in querySnapshot!!.documentChanges) {
                        val requestedContract =
                            documentChange.document.toObject(RequestedContract::class.java)
                        requestedContract.id = documentChange.document.id
                        when (documentChange.type) {
                            DocumentChange.Type.ADDED -> contractAdapter.add(requestedContract)
                            DocumentChange.Type.MODIFIED -> contractAdapter.update(requestedContract)
                            DocumentChange.Type.REMOVED -> contractAdapter.delete(requestedContract)
                        }
                    }
                }
        }
    }
}