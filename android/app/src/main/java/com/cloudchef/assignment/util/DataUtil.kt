package com.cloudchef.assignment.util

import com.cloudchef.assignment.model.DataModel
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken

class DataUtil {
    companion object {
        fun parseMessage(message: String?): ArrayList<DataModel> {
            val typeToken = object : TypeToken<ArrayList<DataModel>>() {}.type
            try {
                val parseResult = Gson().fromJson<ArrayList<DataModel>>(message, typeToken)
                parseResult.sort()
                return parseResult
            } catch (e: JsonSyntaxException) {
                return ArrayList()
            }
        }
    }
}