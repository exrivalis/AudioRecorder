package com.alterpat.voicerecorder.db

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.sql.Timestamp

@Entity(tableName = "audioRecords")
data class AudioRecord (
    var filename: String,
    var filePath: String,
    var date: Long,
    var duration: String){

    @PrimaryKey(autoGenerate = true)
    var id: Int = 0
    @Ignore
    var isChecked: Boolean = false
}