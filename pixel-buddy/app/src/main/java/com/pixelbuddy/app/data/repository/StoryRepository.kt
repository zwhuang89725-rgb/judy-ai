package com.pixelbuddy.app.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pixelbuddy.app.domain.model.Story
import com.pixelbuddy.app.domain.model.StoryCategory
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StoryRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()
    private var cachedStories: List<Story>? = null

    /**
     * 获取全部故事（从 assets/stories.json 加载，内存缓存）。
     */
    fun getAllStories(): List<Story> {
        cachedStories?.let { return it }

        return try {
            val json = context.assets.open("stories.json")
                .bufferedReader()
                .use { it.readText() }

            val type = object : TypeToken<List<Story>>() {}.type
            val stories: List<Story> = gson.fromJson(json, type)
            cachedStories = stories
            stories
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * 按分类筛选。
     */
    fun getByCategory(category: StoryCategory): List<Story> =
        getAllStories().filter { it.category == category }

    /**
     * 按年龄段筛选。
     */
    fun getByAge(ageRange: String): List<Story> =
        getAllStories().filter { it.ageRange == ageRange }

    /**
     * 按标题搜索。
     */
    fun search(query: String): List<Story> =
        getAllStories().filter {
            it.title.contains(query, ignoreCase = true)
        }

    /**
     * 按 ID 获取单篇故事。
     */
    fun getById(id: String): Story? =
        getAllStories().find { it.id == id }

    /**
     * 获取所有分类。
     */
    fun getCategories(): List<StoryCategory> =
        StoryCategory.entries.toList()
}
