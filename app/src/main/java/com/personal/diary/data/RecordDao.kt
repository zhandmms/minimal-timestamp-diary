package com.personal.diary.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    /** 首页按日期取当天的记录，按时间升序（HH:mm 定长字符串可直接字典序排序）。 */
    @Query("SELECT * FROM record WHERE dateString = :date ORDER BY timeString ASC, id ASC")
    fun observeByDate(date: String): Flow<List<Record>>

    /** 日历页：当前浏览月份一次性查询有记录的日期集合，禁止逐日查询。 */
    @Query("SELECT DISTINCT dateString FROM record WHERE dateString LIKE :monthPrefix")
    fun observeMonthDates(monthPrefix: String): Flow<List<String>>

    @Insert
    suspend fun insert(record: Record)

    @Query("DELETE FROM record WHERE id = :id")
    suspend fun deleteById(id: Long)
}
