package com.phoenix.pillreminder.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.lang.Exception
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalDateTimeAdapter: TypeAdapter<LocalDateTime>() {
    @Throws(IOException::class)
    override fun write(out: JsonWriter?, value: LocalDateTime?) {
        if(value != null){
            out?.value(DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(value))
        } else{
            out?.nullValue()
        }
    }

    @Throws(IOException::class)
    override fun read(`in`: JsonReader?): LocalDateTime? {
        val value = `in`?.nextString()
        return try{
            LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (e: Exception){
            null
        }
    }

}