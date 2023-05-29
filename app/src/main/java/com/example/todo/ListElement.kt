package com.example.todo

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize


@Parcelize
class ListElement(var TODO: String, var groupType: String, var priority: Boolean,  var hour: Int, var minute: Int, var day: Int, var month: Int, var year: Int, var done: Boolean) : Parcelable
