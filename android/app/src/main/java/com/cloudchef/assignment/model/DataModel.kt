package com.cloudchef.assignment.model

class DataModel (val timestamp: Long, val value: Double) : Comparable<DataModel> {
    override fun compareTo(other: DataModel): Int {
        return if (timestamp == other.timestamp) {
            0
        } else if (timestamp > other.timestamp) {
            1
        } else {
            -1
        }
    }
}