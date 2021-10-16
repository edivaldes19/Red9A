package com.manuel.red.package_service

import com.manuel.red.models.PackageService

interface OnPackageServiceListener {
    fun onClick(packageService: PackageService)
}