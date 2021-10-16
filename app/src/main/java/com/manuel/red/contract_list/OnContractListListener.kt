package com.manuel.red.contract_list

import com.manuel.red.models.PackageService

interface OnContractListListener {
    fun setAmount(packageService: PackageService)
    fun showTotal(total: Int)
}