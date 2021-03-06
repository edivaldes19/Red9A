package com.manuel.red.contract_list

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.manuel.red.R
import com.manuel.red.databinding.ItemPackageServiceContractListBinding
import com.manuel.red.models.PackageService

class PackageServiceContractListAdapter(
    private val packageServiceList: MutableList<PackageService>,
    private val listener: OnContractListListener
) : RecyclerView.Adapter<PackageServiceContractListAdapter.ViewHolder>() {
    private lateinit var context: Context
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(context)
            .inflate(R.layout.item_package_service_contract_list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val packageService = packageServiceList[position]
        holder.setListener(packageService)
        holder.binding.tvName.text = packageService.name
        holder.binding.tvAmount.text = packageService.newAvailable.toString()
        Glide.with(context).load(packageService.imagePath).diskCacheStrategy(DiskCacheStrategy.ALL)
            .placeholder(R.drawable.ic_cloud_download).error(R.drawable.ic_broken_image)
            .into(holder.binding.imgPackageService)
    }

    override fun getItemCount() = packageServiceList.size
    fun add(packageService: PackageService) {
        if (!packageServiceList.contains(packageService)) {
            packageServiceList.add(packageService)
            notifyItemInserted(packageServiceList.size - 1)
            calcTotal()
        } else {
            update(packageService)
        }
    }

    fun update(packageService: PackageService) {
        val index = packageServiceList.indexOf(packageService)
        if (index != -1) {
            packageServiceList[index] = packageService
            notifyItemChanged(index)
            calcTotal()
        }
    }

    private fun calcTotal() {
        var result = 0
        for (packageService in packageServiceList) {
            result += packageService.totalPrice()
        }
        listener.showTotal(result)
    }

    fun getPackagesServices(): List<PackageService> = packageServiceList
    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemPackageServiceContractListBinding.bind(view)
        fun setListener(packageService: PackageService) {
            binding.fabSub.setOnClickListener {
                if (packageService.newAvailable > 1) {
                    packageService.newAvailable--
                    listener.setAmount(packageService)
                    binding.fabSub.isEnabled = true
                    binding.fabSum.isEnabled = true
                } else if (packageService.newAvailable == 1) {
                    binding.fabSub.isEnabled = false
                    binding.fabSum.isEnabled = true
                }
            }
            binding.fabSum.setOnClickListener {
                if (packageService.newAvailable < 5) {
                    packageService.newAvailable++
                    listener.setAmount(packageService)
                    binding.fabSum.isEnabled = true
                    binding.fabSub.isEnabled = true
                } else if (packageService.newAvailable == 5) {
                    binding.fabSum.isEnabled = false
                    binding.fabSub.isEnabled = true
                }
            }
        }
    }
}