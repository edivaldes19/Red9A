package com.manuel.red.requested_contract

import com.manuel.red.models.RequestedContract

interface OnRequestedContractListener {
    fun onRequestedContractStatus(requestedContract: RequestedContract)
    fun onStartChat(requestedContract: RequestedContract)
    fun onDeleteRequestedContract(requestedContract: RequestedContract)
}