package com.personal.diary.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room 单表实体。字段严格固定：id（自增主键）、dateString、timeString、isAm、content。
 * 绝对不加额外字段。
 */
@Entity(tableName = "record")
data class Record(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val dateString: String,   // "yyyy-MM-dd"
    val timeString: String,   // "HH:mm"
    val isAm: Boolean,
    val content: String
)
