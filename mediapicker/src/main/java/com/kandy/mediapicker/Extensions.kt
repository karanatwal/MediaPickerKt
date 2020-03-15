package com.kandy.mediapicker

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.annotation.LayoutRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatTextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.ByteArrayOutputStream

typealias Str = R.string
typealias Clr = R.color
typealias Lyt = R.layout
typealias Drw = R.drawable

internal fun Uri.realPath(context: Context): Uri {
    val result: String
    val cursor = context.contentResolver.query(this, null, null, null, null)

    if (cursor == null) {
        result = this.path!!
    } else {
        cursor.moveToFirst()
        val idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
        result = cursor.getString(idx)
        cursor.close()
    }
    return Uri.parse(result)
}

internal fun Bitmap.toUri(context: Context, title: String): Uri {
    val bytes = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 100, bytes)
    val path = MediaStore.Images.Media.insertImage(
        context.contentResolver, this,
        title, context.getString(Str.app_name)
    )
    return Uri.parse(path)
}

infix fun ViewGroup.inflate(@LayoutRes lyt: Int): View {
    return LayoutInflater.from(context).inflate(lyt, this, false)
}

internal fun <I> RecyclerView.init(
    items: List<I>,
    @LayoutRes itemLayout: Int,
    layoutManager: RecyclerView.LayoutManager = LinearLayoutManager(this.context),
    bind: (View, I, Int) -> Unit,
    itemClick: (I, Int) -> Unit = { _, _ -> }
) {

    adapter = BaseAdapter(items, itemLayout, bind, itemClick)
    this.layoutManager = layoutManager
}

infix fun Bitmap.rotate(degree: Int): Bitmap {
    val w = width
    val h = height

    val mtx = Matrix()
    mtx.postRotate(degree.toFloat())

    return Bitmap.createBitmap(this, 0, 0, w, h, mtx, true)
}


infix fun ImageView.set(@DrawableRes id: Int) {
    setImageResource(id)
}

infix fun ImageView.set(bitmap: Bitmap) {
    setImageBitmap(bitmap)
}

infix fun ImageView.set(drawable: Drawable) {
    setImageDrawable(drawable)
}

@RequiresApi(Build.VERSION_CODES.M)
infix fun ImageView.set(ic: Icon) {
    setImageIcon(ic)
}

infix fun ImageView.set(uri: Uri) {
    setImageURI(uri)
}

infix fun AppCompatTextView.set(@StringRes id: Int) {
    setText(id)
}

infix fun AppCompatTextView.set(text: String) {
    setText(text)
}