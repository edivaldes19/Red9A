package com.manuel.red.package_service

import com.google.firebase.auth.FirebaseUser
import com.manuel.red.models.PackageService

interface OnMethodsToMainActivity {
    fun addPackageServiceToContractList(packageService: PackageService)
    fun clearContractList()
    fun getPackagesServicesContractList(): MutableList<PackageService>
    fun getPackageServiceSelected(): PackageService?
    fun showButton(isVisible: Boolean)
    fun updateTitle(user: FirebaseUser)
    fun updateTotal()
}