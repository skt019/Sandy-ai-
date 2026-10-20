package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UploadedFileDao {
    @Query("SELECT * FROM uploaded_files ORDER BY uploadedAt DESC")
    fun getAllFilesFlow(): Flow<List<UploadedFileEntity>>

    @Query("SELECT * FROM uploaded_files ORDER BY uploadedAt DESC")
    suspend fun getAllFiles(): List<UploadedFileEntity>

    @Query("SELECT * FROM uploaded_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Long): UploadedFileEntity?

    @Query("SELECT * FROM uploaded_files WHERE fileName LIKE '%' || :query || '%' OR extractedText LIKE '%' || :query || '%' OR summary LIKE '%' || :query || '%' ORDER BY uploadedAt DESC")
    suspend fun searchFiles(query: String): List<UploadedFileEntity>

    @Query("SELECT * FROM uploaded_files ORDER BY uploadedAt DESC LIMIT 1")
    suspend fun getLatestFile(): UploadedFileEntity?

    @Query("SELECT * FROM uploaded_files WHERE fileType = :type ORDER BY uploadedAt DESC LIMIT 1")
    suspend fun getLatestFileByType(type: String): UploadedFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: UploadedFileEntity): Long

    @Update
    suspend fun updateFile(file: UploadedFileEntity)

    @Delete
    suspend fun deleteFile(file: UploadedFileEntity)

    @Query("DELETE FROM uploaded_files WHERE id = :id")
    suspend fun deleteFileById(id: Long)

    @Query("DELETE FROM uploaded_files")
    suspend fun clearAllFiles()
}
