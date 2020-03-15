package com.kandy.mediapicker


import android.Manifest
import android.app.Activity.RESULT_OK
import android.content.ClipData
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.dialog_picker.view.*
import java.io.IOException

class MediaPicker : BottomSheetDialogFragment() {
    var activity: AppCompatActivity? = null
    var fragment: Fragment? = null
    private var uri: Uri? = null
    private var fileName = ""
    private var onPickerCloseListener: OnPickerCloseListener? = null
    private var onPickerErrorListener: OnPickerErrorListener? = null

    private var dialogTitle = ""
    private var dialogTitleId = 0
    private var dialogTitleSize = 0F
    private var dialogTitleColor = 0

    @ListType
    private var dialogListType = TYPE_LIST
    private var dialogGridSpan = 3
    private var dialogItems = ArrayList<MediaItemType>()

    @DialogStyle
    private var dialogStyle = DIALOG_STANDARD

    companion object {
        private const val ARG_TITLE = "title"
        private const val ARG_TITLE_ID = "titleId"
        private const val ARG_TITLE_SIZE = "titleSize"
        private const val ARG_TITLE_COLOR = "titleColor"
        private const val ARG_LIST_TYPE = "list"
        private const val ARG_GRID_SPAN = "gridSpan"
        private const val ARG_ITEMS = "items"

        const val REQUEST_PERMISSION_CAMERA = 1001
        const val REQUEST_PERMISSION_GALLERY = 1002
        const val REQUEST_PERMISSION_VIDEO = 1003
        const val REQUEST_PERMISSION_VGALLERY = 1004
        const val REQUEST_PERMISSION_FILE = 1005
        const val REQUEST_TAKE_PHOTO = 1101
        const val REQUEST_PICK_PHOTO = 1102
        const val REQUEST_PICK_PHOTO_MULTIPLE = 11021
        const val REQUEST_VIDEO = 1103
        const val REQUEST_PICK_FILE = 1104

        private fun newInstance(
            activity: AppCompatActivity?,
            fragment: Fragment?,
            dialogTitle: String,
            dialogTitleId: Int,
            dialogTitleSize: Float,
            dialogTitleColor: Int,
            dialogListType: Int,
            dialogGridSpan: Int,
            dialogMediaItems: ArrayList<MediaItemType>
        ): MediaPicker {

            val args = Bundle()

            args.putString(ARG_TITLE, dialogTitle)
            args.putInt(ARG_TITLE_ID, dialogTitleId)
            args.putFloat(ARG_TITLE_SIZE, dialogTitleSize)
            args.putInt(ARG_TITLE_COLOR, dialogTitleColor)
            args.putInt(ARG_LIST_TYPE, dialogListType)
            args.putInt(ARG_GRID_SPAN, dialogGridSpan)
            args.putParcelableArrayList(ARG_ITEMS, dialogMediaItems)

            val dialog = MediaPicker()
            dialog.arguments = args
            dialog.activity = activity
            dialog.fragment = fragment

            return dialog
        }

        const val TYPE_LIST = 0
        const val TYPE_GRID = 1

        @IntDef(TYPE_LIST, TYPE_GRID)
        @Retention(AnnotationRetention.SOURCE)
        annotation class ListType


        @IntDef(DIALOG_STANDARD, DIALOG_MATERIAL)
        @Retention(AnnotationRetention.SOURCE)
        annotation class DialogStyle

        const val DIALOG_STANDARD = 10
        const val DIALOG_MATERIAL = 20
    }

    class Builder {
        private var activity: AppCompatActivity? = null
        private var fragment: Fragment? = null

        private var dialogTitle = ""
        private var dialogTitleId = 0
        private var dialogTitleSize = 0F
        private var dialogTitleColor = 0

        @ListType
        private var dialogListType = TYPE_LIST
        private var dialogGridSpan = 3
        private var dialogItems = ArrayList<MediaItemType>()

        @DialogStyle
        private var dialogStl = DIALOG_STANDARD

        constructor(activity: AppCompatActivity) {
            this.activity = activity
        }

        constructor(fragment: Fragment) {
            this.fragment = fragment
        }

        fun setTitle(title: String): Builder {
            dialogTitle = title
            return this
        }

        fun setTitle(title: Int): Builder {
            dialogTitleId = title
            return this
        }

        fun setTitleTextSize(textSize: Float): Builder {
            dialogTitleSize = textSize
            return this
        }

        fun setTitleTextColor(textColor: Int): Builder {
            dialogTitleColor = textColor
            return this
        }

        fun setListType(@ListType type: Int, gridSpan: Int = 3): Builder {
            dialogListType = type
            dialogGridSpan = gridSpan
            return this
        }

        fun setItems(mediaItems: ArrayList<MediaItemType>): Builder {
            mediaItems.forEachIndexed { i, itemModel ->
                mediaItems.forEachIndexed { j, itemModel2 ->
                    if (i != j && itemModel2.type == itemModel.type) {
                        throw IllegalStateException("You cannot have two similar item models in this list")
                    }
                }
            }
            dialogItems = mediaItems
            return this
        }

        fun setDialogStyle(@DialogStyle style: Int): Builder {
            dialogStl = style
            return this
        }

        fun create(): MediaPicker {
            val dialog = newInstance(
                activity,
                fragment,
                dialogTitle,
                dialogTitleId,
                dialogTitleSize,
                dialogTitleColor,
                dialogListType,
                dialogGridSpan,
                dialogItems
            )

            dialog.dialogStyle = dialogStl

            return dialog
        }
    }

    override fun getTheme() = if (dialogStyle == DIALOG_MATERIAL)
        R.style.BottomSheetDialogTheme
    else R.style.BottomSheetDialogThemeNormal

    override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(context!!, theme)


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(Lyt.dialog_picker, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getData()
        createTitle(view)
        createList(view)
    }

    private fun getData() {
        val args = arguments ?: return

        dialogTitle = args.getString(ARG_TITLE)!!
        dialogTitleId = args.getInt(ARG_TITLE_ID)
        dialogTitleSize = args.getFloat(ARG_TITLE_SIZE)
        dialogTitleColor = args.getInt(ARG_TITLE_COLOR)
        dialogListType = args.getInt(ARG_LIST_TYPE)
        dialogGridSpan = args.getInt(ARG_GRID_SPAN)
        dialogItems = args.getParcelableArrayList(ARG_ITEMS)!!
    }

    private fun createTitle(view: View) {
        if (dialogTitle == "" && dialogTitleId == 0) {
            view.pickerTitle.visibility = View.GONE
            return
        }

        if (dialogTitle == "") {
            view.pickerTitle set dialogTitleId
        } else {
            view.pickerTitle set dialogTitle
        }

        if (dialogTitleSize != 0F) {
            view.pickerTitle.textSize = dialogTitleSize
        }

        view.pickerTitle.setTextColor(
            if (dialogTitleColor == 0) Color.WHITE
            else dialogTitleColor
        )
    }

    private fun createList(view: View) {
        val viewItem =
            if (dialogListType == TYPE_LIST) Lyt.item_picker_list else Lyt.item_picker_grid
        val manager = if (dialogListType == TYPE_LIST)
            LinearLayoutManager(context) else GridLayoutManager(context, dialogGridSpan)

        view.pickerItems.init(
            dialogItems,
            viewItem,
            manager,
            { subView: View, mediaItem: MediaItemType, _: Int ->
                initIconBackground(mediaItem, subView.findViewById(R.id.icon))
                initIcon(mediaItem, subView.findViewById(R.id.icon))
                initLabel(mediaItem, subView.findViewById(R.id.label))
            },
            { mediaItem: MediaItemType, _: Int ->
                when (mediaItem.type) {
                    MediaItemType.ITEM_CAMERA -> {
                        if (checkSelfPermission(
                                context!!,
                                Manifest.permission.CAMERA
                            )
                            != PackageManager.PERMISSION_GRANTED
                            || checkSelfPermission(
                                context!!,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                            || checkSelfPermission(
                                context!!,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ),
                                REQUEST_PERMISSION_CAMERA
                            )
                        } else {
                            openCamera()
                        }
                    }
                    MediaItemType.ITEM_GALLERY -> {
                        if (checkSelfPermission(
                                context!!,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                            || checkSelfPermission(
                                context!!,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ),
                                REQUEST_PERMISSION_GALLERY
                            )
                        } else {
                            openGallery()
                        }
                    }
                    MediaItemType.ITEM_GALLERY_MULTIPLE -> {
                        if (checkSelfPermission(
                                context!!,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                            || checkSelfPermission(
                                context!!,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ),
                                REQUEST_PERMISSION_GALLERY
                            )
                        } else {
                            openGalleryMultiple()
                        }
                    }
                    MediaItemType.ITEM_VIDEO -> {
                        if (checkSelfPermission(
                                context!!,
                                Manifest.permission.CAMERA
                            )
                            != PackageManager.PERMISSION_GRANTED
                            || checkSelfPermission(
                                context!!,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                            || checkSelfPermission(
                                context!!,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.CAMERA,
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ),
                                REQUEST_PERMISSION_VIDEO
                            )
                        } else {
                            openVideoCamera()
                        }
                    }
                    MediaItemType.ITEM_VIDEO_GALLERY -> {
                        if (checkSelfPermission(
                                context!!,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                            || checkSelfPermission(
                                context!!,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ),
                                REQUEST_PERMISSION_VGALLERY
                            )
                        } else {
                            openVideoGallery()
                        }
                    }
                    MediaItemType.ITEM_FILES -> {
                        if (checkSelfPermission(
                                context!!,
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                            || checkSelfPermission(
                                context!!,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            )
                            != PackageManager.PERMISSION_GRANTED
                        ) {
                            requestPermissions(
                                arrayOf(
                                    Manifest.permission.READ_EXTERNAL_STORAGE,
                                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                                ),
                                REQUEST_PERMISSION_FILE
                            )
                        } else {
                            openFilePicker()
                        }
                    }
                }
            }
        )
    }

    private fun initIconBackground(mediaItem: MediaItemType, view: View) {
        if (mediaItem.hasBackground) {
            val color = if (mediaItem.itemBackgroundColor == 0)
                ContextCompat.getColor(view.context, Clr.colorAccent)
            else mediaItem.itemBackgroundColor

            val bg: Drawable?

            when (mediaItem.backgroundType) {
                MediaItemType.TYPE_SQUARE -> {
                    bg = ContextCompat.getDrawable(view.context, Drw.bg_square)
                    bg?.mutate()?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                }
                MediaItemType.TYPE_ROUNDED_SQUARE -> {
                    bg = ContextCompat.getDrawable(view.context, Drw.bg_rounded_square)
                    bg?.mutate()?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                }
                else -> {
                    bg = ContextCompat.getDrawable(view.context, Drw.bg_circle)
                    bg?.mutate()?.setColorFilter(color, PorterDuff.Mode.SRC_IN)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                view.background = bg
            } else {
                view.setBackgroundDrawable(bg)
            }
        }
    }

    private fun initIcon(mediaItem: MediaItemType, icon: AppCompatImageView) {
        if (mediaItem.itemIcon == 0) {
            icon set when (mediaItem.type) {
                MediaItemType.ITEM_GALLERY -> Drw.ic_image
                MediaItemType.ITEM_GALLERY_MULTIPLE -> Drw.ic_image
                MediaItemType.ITEM_VIDEO -> Drw.ic_videocam
                MediaItemType.ITEM_VIDEO_GALLERY -> Drw.ic_video_library
                MediaItemType.ITEM_FILES -> Drw.ic_file
                else -> Drw.ic_camera
            }
        } else {
            icon set mediaItem.itemIcon
        }
    }

    private fun initLabel(mediaItem: MediaItemType, label: AppCompatTextView) {
        if (mediaItem.itemLabel == "") {
            label set when (mediaItem.type) {
                MediaItemType.ITEM_GALLERY -> "Open Gallery"
                MediaItemType.ITEM_GALLERY_MULTIPLE -> "Open Gallery"
                MediaItemType.ITEM_VIDEO -> "Record Video"
                MediaItemType.ITEM_VIDEO_GALLERY -> "Choose Video"
                MediaItemType.ITEM_FILES -> "Select File"
                else -> "Take Photo"
            }
        } else {
            label set mediaItem.itemLabel
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        onPermissionsResult(requestCode, permissions, grantResults)
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    private fun onPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_PERMISSION_CAMERA -> if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            }
            REQUEST_PERMISSION_GALLERY -> if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                openGallery()
            }
            REQUEST_PERMISSION_VIDEO -> if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                openVideoCamera()
            }
            REQUEST_PERMISSION_VGALLERY -> if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                openVideoGallery()
            }
            REQUEST_PERMISSION_FILE -> if (grantResults.isNotEmpty() && grantResults.first() == PackageManager.PERMISSION_GRANTED) {
                openFilePicker()
            }
            else -> onPickerErrorListener?.onPickerError("Please allow Permissions in Settings to continue.")
        }
    }

    private fun openCamera() {
        fileName = (System.currentTimeMillis() / 1000).toString() + ".jpg"

        val contentValues = ContentValues()
        contentValues.put(MediaStore.Images.Media.TITLE, fileName)
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, getString(Str.app_name))
        uri = context!!.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )

        val takePhoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhoto.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        startActivityForResult(takePhoto, REQUEST_TAKE_PHOTO)
    }

    private fun openGallery() {
        val pickPhoto = Intent(
            Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(pickPhoto, REQUEST_PICK_PHOTO)
    }

    private fun openGalleryMultiple() {
        val pickPhoto = Intent(
            Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        pickPhoto.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(pickPhoto, REQUEST_PICK_PHOTO_MULTIPLE)
    }


    private fun openVideoCamera() {
        fileName = (System.currentTimeMillis() / 1000).toString() + ".mp4"

        val takeVideo = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        takeVideo.putExtra(
            MediaStore.EXTRA_OUTPUT,
            Environment.getExternalStorageDirectory().absolutePath + "/" + fileName
        )

        startActivityForResult(takeVideo, REQUEST_VIDEO)
    }

    private fun openVideoGallery() {
        val pickVideo =
            Intent(Intent.ACTION_PICK, android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickVideo, REQUEST_VIDEO)
    }

    private fun openFilePicker() {
        val pickFile = Intent(Intent.ACTION_GET_CONTENT)
        pickFile.type = "*/*"

        startActivityForResult(pickFile, REQUEST_PICK_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_TAKE_PHOTO -> takePhoto()
                REQUEST_PICK_PHOTO -> pickPhoto(data)
                REQUEST_PICK_PHOTO_MULTIPLE -> pickPhotoMultiple(data)
                REQUEST_VIDEO -> pickVideo(data)
                REQUEST_PICK_FILE -> pickFile(data)
            }
        }
    }

    private fun takePhoto() {
        val uri = this.uri ?: return

        var bitmap: Bitmap
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = false
            options.inPreferredConfig = Bitmap.Config.RGB_565
            options.inDither = true
            bitmap = BitmapFactory.decodeFile(uri.realPath(context!!).path, options)

            val exif = ExifInterface(uri.realPath(context!!).path!!)

            when (exif.getAttribute(ExifInterface.TAG_ORIENTATION)) {
                "6" -> bitmap = bitmap rotate 90
                "8" -> bitmap = bitmap rotate 270
                "3" -> bitmap = bitmap rotate 180
            }

            if (onPickerCloseListener != null) {
                onPickerCloseListener?.onPickerClosed(
                    MediaItemType.ITEM_CAMERA,
                    bitmap.toUri(context!!, fileName)
                )
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        dismiss()
    }


    private fun pickPhotoMultiple(data: Intent?) {

        if (data == null) {
            return
        }

        val uriList: ClipData = data.clipData ?: return

        if (uriList.itemCount > 0 && onPickerCloseListener != null) {
            var count = uriList.itemCount
            if (count > 6) count = 6
            for (i in 0 until count) {
                val uri = uriList.getItemAt(i).uri
                onPickerCloseListener?.onPickerClosed(MediaItemType.ITEM_GALLERY, uri)
            }

        }
        dismiss()
    }

    private fun pickPhoto(data: Intent?) {
        if (data == null) {
            return
        }

        val uri = data.data ?: return

        if (onPickerCloseListener != null) {
            onPickerCloseListener?.onPickerClosed(MediaItemType.ITEM_GALLERY, uri)
        }
        dismiss()
    }

    private fun pickVideo(data: Intent?) {
        if (data == null) {
            return
        }

        val uri = data.data ?: return

        if (onPickerCloseListener != null) {
            onPickerCloseListener?.onPickerClosed(MediaItemType.ITEM_VIDEO_GALLERY, uri)
        }
        dismiss()
    }

    private fun pickFile(data: Intent?) {
        if (data == null) {
            return
        }

        val uri = data.data ?: return

        if (onPickerCloseListener != null) {
            onPickerCloseListener?.onPickerClosed(MediaItemType.ITEM_FILES, uri)
        }
        dismiss()
    }

    fun setPickerCloseListener(onClose: (Int, Uri) -> Unit) {
        onPickerCloseListener = OnPickerCloseListener(onClose)
    }

    fun setPickerErrorListener(onError: (String) -> Unit) {
        onPickerErrorListener = OnPickerErrorListener(onError)
    }

    fun show() {
        if (activity == null && fragment?.fragmentManager != null) {
            show(fragment!!.fragmentManager!!, "")
        } else {
            show(activity?.supportFragmentManager!!, "")
        }
    }
}
