package com.manuel.red.chat

import com.manuel.red.models.Message

interface OnChatListener {
    fun deleteMessage(message: Message)
}