package com.manuel.red.package_service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.remoteconfig.ktx.remoteConfig
import com.google.firebase.remoteconfig.ktx.remoteConfigSettings
import com.manuel.red.R
import com.manuel.red.about.AboutActivity
import com.manuel.red.contract_list.ContractListFragment
import com.manuel.red.databinding.ActivityMainBinding
import com.manuel.red.detail.DetailFragment
import com.manuel.red.models.PackageService
import com.manuel.red.offers_and_promotions.OffersAndPromotionsFragment
import com.manuel.red.profile.ProfileFragment
import com.manuel.red.requested_contract.RequestedContractActivity
import com.manuel.red.settings.SettingsActivity
import com.manuel.red.utils.ConnectionReceiver
import com.manuel.red.utils.Constants
import java.security.MessageDigest
import java.util.*

class MainActivity : AppCompatActivity(), OnPackageServiceListener, MainAux,
    ConnectionReceiver.ReceiverListener {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var packageServiceAdapter: PackageServiceAdapter
    private lateinit var listenerRegistration: ListenerRegistration
    private var packageServiceSelected: PackageService? = null
    private val packageServiceContractList: MutableList<PackageService> = mutableListOf()
    private var packageServiceList: MutableList<PackageService> = mutableListOf()
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val errorSnack: Snackbar by lazy {
        Snackbar.make(binding.root, "", Snackbar.LENGTH_SHORT).setTextColor(Color.YELLOW)
    }
    private val resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            val response = IdpResponse.fromResultIntent(activityResult.data)
            if (activityResult.resultCode == RESULT_OK) {
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null) {
                    Toast.makeText(
                        this,
                        "${getString(R.string.welcome)} ${user.displayName}",
                        Toast.LENGTH_LONG
                    ).show()
                    val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                    val token = preferences.getString(Constants.PROP_TOKEN, null)
                    token?.let {
                        val tokenMap = hashMapOf(Pair(Constants.PROP_TOKEN, token))
                        val userMap = hashMapOf(
                            Constants.PROP_LAST_MODIFICATION to Date().time,
                            Constants.PROP_USERNAME to user.displayName.toString(),
                            Constants.PROP_PROFILE_PICTURE to user.photoUrl.toString()
                        )
                        val db = FirebaseFirestore.getInstance()
                        db.collection(Constants.COLL_USERS).document(user.uid).run {
                            set(userMap)
                            collection(Constants.COLL_TOKENS).add(tokenMap)
                                .addOnSuccessListener { Log.i("Registered Token", token) }
                                .addOnFailureListener { Log.i("Unregistered Token", token) }
                        }
                    }
                }
            } else {
                if (response == null) {
                    Toast.makeText(this, getString(R.string.see_you_soon), Toast.LENGTH_SHORT)
                        .show()
                    finish()
                } else {
                    response.error?.let { firebaseUiException ->
                        if (firebaseUiException.errorCode == ErrorCodes.NO_NETWORK) {
                            errorSnack.apply {
                                setText(getString(R.string.network_error_check_your_connection))
                                show()
                            }
                        } else {
                            errorSnack.apply {
                                setText("${getString(R.string.error_code)}: ${firebaseUiException.errorCode}")
                                show()
                            }
                        }
                    }
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Red_NoActionBar)
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        configRemoteConfig()
        configAuth()
        configRecyclerView()
        configButtons()
        configAnalytics()
        checkConnection()
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                preferences.edit {
                    putString(Constants.PROP_TOKEN, token).apply()
                }
                Log.i("Token obtained", token.toString())
            } else {
                Log.i("Token not obtained", task.exception.toString())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
        configFirestoreRealtime()
    }

    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
        listenerRegistration.remove()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val menuItem = menu?.findItem(R.id.action_search)
        val searchView = menuItem?.actionView as SearchView
        searchView.queryHint = getString(R.string.write_here_to_search)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                val temporaryList: MutableList<PackageService> = ArrayList()
                for (packageService in packageServiceList) {
                    if (newText!! in packageService.name.toString()) {
                        temporaryList.add(packageService)
                    }
                }
                packageServiceAdapter.updateList(temporaryList)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_offers_and_promotions -> {
                val fragment = OffersAndPromotionsFragment()
                supportFragmentManager.beginTransaction().add(R.id.containerMain, fragment)
                    .addToBackStack(null).commit()
                showButton(false)
            }
            R.id.action_profile -> {
                val fragment = ProfileFragment()
                supportFragmentManager.beginTransaction().add(R.id.containerMain, fragment)
                    .addToBackStack(null).commit()
                showButton(false)
            }
            R.id.action_requested_contract_history -> startActivity(
                Intent(
                    this,
                    RequestedContractActivity::class.java
                )
            )
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
            }
            R.id.action_about -> {
                startActivity(Intent(this, AboutActivity::class.java))
            }
            R.id.action_sign_off -> {
                MaterialAlertDialogBuilder(this).setTitle(getString(R.string.sign_off))
                    .setMessage(getString(R.string.are_you_sure_to_take_this_action))
                    .setPositiveButton(getString(R.string.yes)) { _, _ ->
                        AuthUI.getInstance().signOut(this).addOnSuccessListener {
                            Toast.makeText(
                                this,
                                getString(R.string.you_have_logged_out),
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                binding.nsvPackageServices.visibility = View.GONE
                                binding.llProgress.visibility = View.VISIBLE
                            } else {
                                errorSnack.apply {
                                    setText(getString(R.string.failed_to_log_out))
                                    show()
                                }
                            }
                        }
                    }.setNegativeButton(getString(R.string.cancel), null).show()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onClick(packageService: PackageService) {
        val index = packageServiceContractList.indexOf(packageService)
        packageServiceSelected = if (index != -1) {
            packageServiceContractList[index]
        } else {
            packageService
        }
        val fragment = DetailFragment()
        supportFragmentManager.beginTransaction().add(R.id.containerMain, fragment)
            .addToBackStack(null).commit()
        showButton(false)
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_ID, packageService.id!!)
            param(FirebaseAnalytics.Param.ITEM_NAME, packageService.name!!)
        }
    }

    override fun getPackagesServicesContractList(): MutableList<PackageService> =
        packageServiceContractList

    override fun getPackageServiceSelected(): PackageService? = packageServiceSelected
    override fun showButton(isVisible: Boolean) {
        binding.btnViewContractList.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    override fun addPackageServiceToContractList(packageService: PackageService) {
        val index = packageServiceContractList.indexOf(packageService)
        if (index != -1) {
            packageServiceContractList[index] = packageService
        } else {
            packageServiceContractList.add(packageService)
        }
        updateTotal()
    }

    override fun updateTotal() {
        var total = 0
        packageServiceContractList.forEach { packageService ->
            total += packageService.totalPrice()
        }
        if (total == 0) {
            binding.tvTotal.text = getString(R.string.empty_list)
            binding.btnViewContractList.isEnabled = false
        } else {
            binding.tvTotal.text = getString(R.string.package_service_full_contract_list, total)
            binding.btnViewContractList.isEnabled = true
        }
    }

    override fun clearContractList() {
        packageServiceContractList.clear()
    }

    override fun updateTitle(user: FirebaseUser) {
        supportActionBar?.title = user.displayName
    }

    override fun onNetworkChange(isConnected: Boolean) {
        showNetworkErrorToast(isConnected)
    }

    private fun checkConnection() {
        val intentFilter = IntentFilter()
        intentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE")
        registerReceiver(ConnectionReceiver(), intentFilter)
        ConnectionReceiver.receiverListener = this
        val manager =
            applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        val isConnected = networkInfo != null && networkInfo.isConnectedOrConnecting
        showNetworkErrorToast(isConnected)
    }

    private fun showNetworkErrorToast(connected: Boolean) {
        if (!connected) {
            Toast.makeText(
                this,
                getString(R.string.network_error_check_your_connection),
                Toast.LENGTH_LONG
            ).show()
            startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
        }
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun configRemoteConfig() {
        val remoteConfig = Firebase.remoteConfig
        val configSettings = remoteConfigSettings {
            minimumFetchIntervalInSeconds = 10
        }
        remoteConfig.setConfigSettingsAsync(configSettings)
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val isOfferAndPromotionDay =
                    remoteConfig.getBoolean(Constants.PROP_IS_OFFER_AND_PROMO_DAY)
                val offerAndPromotionCounter =
                    remoteConfig.getLong(Constants.PROP_OFFER_AND_PROMO_COUNTER)
                if (isOfferAndPromotionDay) {
                    val badge = BadgeDrawable.create(this)
                    BadgeUtils.attachBadgeDrawable(
                        badge,
                        binding.toolbar,
                        R.id.action_offers_and_promotions
                    )
                    badge.number = offerAndPromotionCounter.toInt()
                }
            }
        }
    }

    @SuppressLint("PackageManagerGetSignatures")
    private fun configAuth() {
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                updateTitle(auth.currentUser!!)
                binding.llProgress.visibility = View.GONE
                binding.nsvPackageServices.visibility = View.VISIBLE
            } else {
                val providers = arrayListOf(
                    AuthUI.IdpConfig.FacebookBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build(),
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.PhoneBuilder().build()
                )
                val loginView = AuthMethodPickerLayout.Builder(R.layout.view_login)
                    .setFacebookButtonId(R.id.btnFacebook).setGoogleButtonId(R.id.btnGoogle)
                    .setEmailButtonId(R.id.btnEmail).setPhoneButtonId(R.id.btnPhone)
                    .setTosAndPrivacyPolicyId(R.id.tvTermsAndConditions).build()
                resultLauncher.launch(
                    AuthUI.getInstance().createSignInIntentBuilder()
                        .setAvailableProviders(providers).setIsSmartLockEnabled(false)
                        .setTosAndPrivacyPolicyUrls(
                            Constants.TERMS_AND_CONDITIONS,
                            Constants.PRIVACY_POLICY
                        ).setAuthMethodPickerLayout(loginView).setTheme(R.style.LoginTheme).build()
                )
            }
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val info = packageManager.getPackageInfo(
                    packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                for (signature in info.signingInfo.apkContentsSigners) {
                    val md = MessageDigest.getInstance("SHA")
                    md.update(signature.toByteArray())
                    Log.d("API >= 28 KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT))
                }
            } else {
                val info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
                for (signature in info.signatures) {
                    val md = MessageDigest.getInstance("SHA")
                    md.update(signature.toByteArray())
                    Log.d("API < 28 KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun configRecyclerView() {
        packageServiceAdapter = PackageServiceAdapter(packageServiceList, this)
        binding.recyclerView.apply {
            layoutManager =
                GridLayoutManager(this@MainActivity, 2, GridLayoutManager.HORIZONTAL, false)
            adapter = this@MainActivity.packageServiceAdapter
        }
    }

    private fun configButtons() {
        binding.btnViewContractList.setOnClickListener {
            val fragment = ContractListFragment()
            fragment.show(
                supportFragmentManager.beginTransaction(),
                ContractListFragment::class.java.simpleName
            )
        }
    }

    private fun configAnalytics() {
        firebaseAnalytics = Firebase.analytics
    }

    private fun configFirestoreRealtime() {
        val db = FirebaseFirestore.getInstance()
        val packageServiceRef = db.collection(Constants.COLL_PACKAGE_SERVICE)
        listenerRegistration = packageServiceRef.addSnapshotListener { snapshots, error ->
            if (error != null) {
                errorSnack.apply {
                    setText(getString(R.string.failed_to_query_the_data))
                    show()
                }
                return@addSnapshotListener
            }
            for (snapshot in snapshots!!.documentChanges) {
                val packageService = snapshot.document.toObject(PackageService::class.java)
                packageService.id = snapshot.document.id
                when (snapshot.type) {
                    DocumentChange.Type.ADDED -> packageServiceAdapter.add(packageService)
                    DocumentChange.Type.MODIFIED -> packageServiceAdapter.update(packageService)
                    DocumentChange.Type.REMOVED -> packageServiceAdapter.delete(packageService)
                }
            }
        }
    }
}