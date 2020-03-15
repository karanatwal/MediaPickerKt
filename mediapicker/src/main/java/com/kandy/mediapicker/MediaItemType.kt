package com.kandy.mediapicker

import android.annotation.SuppressLint
import android.os.Parcel
import android.os.Parcelable
import androidx.annotation.IntDef

@SuppressLint("ParcelCreator")

data class MediaItemType(
    @ItemType val type: Int,
    val itemLabel: String = "",
    val itemIcon: Int = 0,
    val hasBackground: Boolean = true,
    @ShapeType val backgroundType: Int = TYPE_CIRCLE,
    val itemBackgroundColor: Int = 0
) : Parcelable {
    constructor(source: Parcel) : this(
        source.readInt(),
        source.readString()!!,
        source.readInt(),
        1 == source.readInt(),
        source.readInt(),
        source.readInt()
    )

    override fun describeContents() = 0

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeInt(type)
        writeString(itemLabel)
        writeInt(itemIcon)
        writeInt((if (hasBackground) 1 else 0))
        writeInt(backgroundType)
        writeInt(itemBackgroundColor)
    }

    companion object {
        @IntDef(TYPE_CIRCLE, TYPE_SQUARE, TYPE_ROUNDED_SQUARE)
        @Retention(AnnotationRetention.SOURCE)
        annotation class ShapeType

        const val TYPE_CIRCLE = 0

        const val TYPE_SQUARE = 1

        const val TYPE_ROUNDED_SQUARE = 2

        @IntDef(
            ITEM_CAMERA,
            ITEM_GALLERY,
            ITEM_VIDEO,
            ITEM_VIDEO_GALLERY,
            ITEM_FILES,
            ITEM_GALLERY_MULTIPLE
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class ItemType

        const val ITEM_CAMERA = 10

        const val ITEM_GALLERY = 11

        const val ITEM_VIDEO = 12

        const val ITEM_VIDEO_GALLERY = 13

        const val ITEM_FILES = 14

        const val ITEM_GALLERY_MULTIPLE = 15

        @JvmField
        val CREATOR: Parcelable.Creator<MediaItemType> = object : Parcelable.Creator<MediaItemType> {
            override fun createFromParcel(source: Parcel): MediaItemType = MediaItemType(source)
            override fun newArray(size: Int): Array<MediaItemType?> = arrayOfNulls(size)
        }
    }
}