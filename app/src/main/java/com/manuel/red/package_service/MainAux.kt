package com.manuel.red.package_service

import com.google.firebase.auth.FirebaseUser
import com.manuel.red.models.PackageService

interface MainAux {
    fun addPackageServiceToContractList(packageService: PackageService)
    fun clearContractList()
    fun getPackagesServicesContractList(): MutableList<PackageService>
    fun getPackageServiceSelected(): PackageService?
    fun showButton(isVisible: Boolean)
    fun updateTotal()
    fun updateTitle(user: FirebaseUser)
}