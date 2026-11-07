package com.github.yournamehere

import android.annotation.SuppressLint
import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.aliucord.Utils
import com.aliucord.annotations.AliucordPlugin
import com.aliucord.entities.Plugin
import com.aliucord.patcher.after
import com.discord.utilities.rest.RestAPI
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemMessage
import com.discord.widgets.chat.list.entries.ChatListEntry
import com.discord.widgets.chat.list.entries.MessageEntry

@AliucordPlugin(
    requiresRestart = false,
)
@Suppress("unused")
class MyFirstKotlinPlugin : Plugin() {
    // Change this to any emoji you like
    private val emoji = "❤️"

    @SuppressLint("ClickableViewAccessibility")
    override fun start(context: Context) {
        patcher.after<WidgetChatListAdapterItemMessage>(
            "onConfigure",
            Int::class.java,
            ChatListEntry::class.java,
        ) { param ->
            val entry = param.args[1] as? MessageEntry ?: return@after
            val message = entry.message
            if (message.isLoading) return@after

            val channelId = message.channelId
            val messageId = message.id
            val itemView = (param.thisObject as RecyclerView.ViewHolder).itemView

            val detector = GestureDetector(
                itemView.context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDown(e: MotionEvent) = true

                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        react(channelId, messageId)
                        return true
                    }
                },
            )

            itemView.setOnTouchListener { _, event ->
                detector.onTouchEvent(event)
                false // don't consume, so scrolling and long-press still work
            }
        }
    }

    private fun react(channelId: Long, messageId: Long) {
        Utils.threadPool.execute {
            try {
                RestAPI.api.addReaction(channelId, messageId, emoji).subscribe()
            } catch (e: Throwable) {
                logger.error("Failed to add reaction", e)
            }
        }
    }

    override fun stop(context: Context) {
        patcher.unpatchAll()
    }
}
