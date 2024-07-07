package com.example.myapplication

import android.os.Parcel
import android.os.Parcelable

data class TransportData(
    val routeShortName: String,
    val routeLongName: String,
    val tripHeadSign: String,
    val stopName: String,
    val arrivalTime: String,
    val departureTime: String,
    val waitTime: Double
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(routeShortName)
        parcel.writeString(routeLongName)
        parcel.writeString(tripHeadSign)
        parcel.writeString(stopName)
        parcel.writeString(arrivalTime)
        parcel.writeString(departureTime)
        parcel.writeDouble(waitTime)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TransportData> {
        override fun createFromParcel(parcel: Parcel): TransportData {
            return TransportData(parcel)
        }

        override fun newArray(size: Int): Array<TransportData?> {
            return arrayOfNulls(size)
        }
    }
}
