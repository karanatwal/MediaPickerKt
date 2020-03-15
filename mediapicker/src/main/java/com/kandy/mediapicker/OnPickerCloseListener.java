package com.kandy.mediapicker;

import android.net.Uri;

public interface OnPickerCloseListener {
    void onPickerClosed(@MediaItemType.Companion.ItemType int type, Uri uri);
}
