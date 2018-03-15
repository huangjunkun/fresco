/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.bitmaps;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.platform.PlatformDecoder;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Factory implementation for Honeycomb through Kitkat
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@ThreadSafe
public class HoneycombBitmapFactory extends PlatformBitmapFactory {

  private final EmptyJpegGenerator mJpegGenerator;
  private final PlatformDecoder mPurgeableDecoder;

  public HoneycombBitmapFactory(EmptyJpegGenerator jpegGenerator,
                         PlatformDecoder purgeableDecoder) {
    mJpegGenerator = jpegGenerator;
    mPurgeableDecoder = purgeableDecoder;
  }

  /**
   * Creates a bitmap of the specified width and height.
   *
   * @param width the width of the bitmap
   * @param height the height of the bitmap
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config}
   * used to create the decoded Bitmap
   * @return a reference to the bitmap
   * @throws TooManyBitmapsException if the pool is full
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
  @Override
  public CloseableReference<Bitmap> createBitmapInternal(
      int width,
      int height,
      Bitmap.Config bitmapConfig) {
    CloseableReference<PooledByteBuffer> jpgRef = mJpegGenerator.generate(
        (short) width,
        (short) height);
    try {
      EncodedImage encodedImage = new EncodedImage(jpgRef);
      encodedImage.setImageFormat(DefaultImageFormats.JPEG);
      try {
        CloseableReference<Bitmap> bitmapRef =
            mPurgeableDecoder.decodeJPEGFromEncodedImage(
                encodedImage, bitmapConfig, null, jpgRef.get().size());
        bitmapRef.get().setHasAlpha(true);
        bitmapRef.get().eraseColor(Color.TRANSPARENT);
        return bitmapRef;
      } finally {
        EncodedImage.closeSafely(encodedImage);
      }
    } finally {
      jpgRef.close();
    }
  }
}
