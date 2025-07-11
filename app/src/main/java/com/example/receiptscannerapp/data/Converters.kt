package com.example.receiptscannerapp.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromItemList(items: List<String>): String {
        return gson.toJson(items)
    }

    @TypeConverter
    fun toItemList(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type)
    }

    @TypeConverter
    fun fromFloatList(embedding: List<Float>): String {
        return gson.toJson(embedding)
    }

    @TypeConverter
    fun toFloatList(json: String): List<Float> {
        val type = object : TypeToken<List<Float>>() {}.type
        return gson.fromJson(json, type)
    }
}
