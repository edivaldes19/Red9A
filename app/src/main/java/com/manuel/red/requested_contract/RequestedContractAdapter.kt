package com.manuel.red.requested_contract

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.manuel.red.R
import com.manuel.red.databinding.ItemRequestedContractBinding
import com.manuel.red.models.RequestedContract
import com.manuel.red.utils.TimestampToText
import java.util.*

class RequestedContractAdapter(
    private val requestedContractList: MutableList<RequestedContract>,
    private val listener: OnRequestedContractListener
) : RecyclerView.Adapter<RequestedContractAdapter.ViewHolder>() {
    private lateinit var context: Context
    private val aValues: Array<String> by lazy {
        context.resources.getStringArray(R.array.status_value)
    }
    private val aKeys: Array<Int> by lazy {
        context.resources.getIntArray(R.array.status_key).toTypedArray()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val view =
            LayoutInflater.from(context).inflate(R.layout.item_requested_contract, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val contract = requestedContractList[position]
        holder.setListener(contract)
        holder.binding.tvId.text = context.getString(R.string.contract_id, contract.id)
        var names = ""
        contract.packagesServices.forEach { entry ->
            names += "${entry.value.name}(${entry.value.available}), "
        }
        holder.binding.tvPackageServiceNames.text = names.dropLast(2)
        holder.binding.tvTotalPrice.text =
            context.getString(R.string.package_service_full_contract_list, contract.totalPrice)
        val index = aKeys.indexOf(contract.status)
        val statusStr = if (index != -1) {
            aValues[index]
        } else {
            context.getString(R.string.unknown)
        }
        holder.binding.tvStatus.text =
            context.getString(R.string.order_status, statusStr.lowercase(Locale.getDefault()))
        val time = TimestampToText.getTimeAgo(contract.timestamp)
        holder.binding.tvDate.text = time
    }

    override fun getItemCount(): Int = requestedContractList.size
    fun add(requestedContract: RequestedContract) {
        requestedContractList.add(requestedContract)
        notifyItemInserted(requestedContractList.size - 1)
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemRequestedContractBinding.bind(view)
        fun setListener(requestedContract: RequestedContract) {
            binding.btnContractStatus.setOnClickListener {
                listener.onRequestedContractStatus(requestedContract)
            }
            binding.chpChat.setOnClickListener {
                listener.onStartChat(requestedContract)
            }
        }
    }
}