package com.notereader.app.rendering.markdown

import android.content.Context
import android.text.Spanned
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.image.coil.CoilImagesPlugin

object MarkwonProvider {
    @Volatile
    private var instance: Markwon? = null

    fun get(context: Context): Markwon {
        return instance ?: synchronized(this) {
            instance ?: Markwon.builder(context.applicationContext)
                .usePlugin(TablePlugin.create(context.applicationContext))
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TaskListPlugin.create(context.applicationContext))
                .usePlugin(CoilImagesPlugin.create(context.applicationContext))
                .build().also { instance = it }
        }
    }

    fun renderMarkdown(context: Context, markdown: String): Spanned {
        return get(context).render(get(context).parse(markdown))
    }
}
