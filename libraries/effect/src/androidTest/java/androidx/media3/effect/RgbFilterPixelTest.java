/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.media3.effect;

import static androidx.media3.common.util.Assertions.checkNotNull;
import static androidx.media3.effect.BitmapTestUtil.MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE;
import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static com.google.common.truth.Truth.assertThat;

import android.content.Context;
import android.graphics.Bitmap;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.util.Pair;
import androidx.media3.common.FrameProcessingException;
import androidx.media3.common.util.GlUtil;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.IOException;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Pixel tests for {@link RgbFilter}.
 *
 * <p>Expected images are taken from an emulator, so tests on different emulators or physical
 * devices may fail. To test on other devices, please increase the {@link
 * BitmapTestUtil#MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE} and/or inspect the saved output bitmaps
 * as recommended in {@link GlEffectsFrameProcessorPixelTest}.
 */
@RunWith(AndroidJUnit4.class)
public final class RgbFilterPixelTest {
  public static final String ORIGINAL_PNG_ASSET_PATH =
      "media/bitmap/sample_mp4_first_frame/original.png";
  public static final String GRAYSCALE_PNG_ASSET_PATH =
      "media/bitmap/sample_mp4_first_frame/grayscale.png";
  public static final String INVERT_PNG_ASSET_PATH =
      "media/bitmap/sample_mp4_first_frame/invert.png";

  private final Context context = getApplicationContext();

  private @MonotonicNonNull EGLDisplay eglDisplay;
  private @MonotonicNonNull EGLContext eglContext;
  private @MonotonicNonNull SingleFrameGlTextureProcessor rgbMatrixProcessor;
  private @MonotonicNonNull EGLSurface placeholderEglSurface;
  private int inputTexId;
  private int outputTexId;
  private int inputWidth;
  private int inputHeight;

  @Before
  public void createGlObjects() throws IOException, GlUtil.GlException {
    eglDisplay = GlUtil.createEglDisplay();
    eglContext = GlUtil.createEglContext(eglDisplay);
    Bitmap inputBitmap = BitmapTestUtil.readBitmap(ORIGINAL_PNG_ASSET_PATH);
    inputWidth = inputBitmap.getWidth();
    inputHeight = inputBitmap.getHeight();
    placeholderEglSurface = GlUtil.createPlaceholderEglSurface(eglDisplay);
    GlUtil.focusEglSurface(eglDisplay, eglContext, placeholderEglSurface, inputWidth, inputHeight);
    inputTexId = BitmapTestUtil.createGlTextureFromBitmap(inputBitmap);

    outputTexId =
        GlUtil.createTexture(inputWidth, inputHeight, /* useHighPrecisionColorComponents= */ false);
    int frameBuffer = GlUtil.createFboForTexture(outputTexId);
    GlUtil.focusFramebuffer(
        checkNotNull(eglDisplay),
        checkNotNull(eglContext),
        checkNotNull(placeholderEglSurface),
        frameBuffer,
        inputWidth,
        inputHeight);
  }

  @After
  public void release() throws GlUtil.GlException, FrameProcessingException {
    if (rgbMatrixProcessor != null) {
      rgbMatrixProcessor.release();
    }
    GlUtil.destroyEglContext(eglDisplay, eglContext);
  }

  @Test
  public void drawFrame_grayscale_producesGrayscaleImage() throws Exception {
    String testId = "drawFrame_grayscale";
    RgbMatrix grayscaleMatrix = RgbFilter.createGrayscaleFilter();
    rgbMatrixProcessor = new RgbMatrixProcessor(context, grayscaleMatrix, /* useHdr= */ false);
    Pair<Integer, Integer> outputSize = rgbMatrixProcessor.configure(inputWidth, inputHeight);
    Bitmap expectedBitmap = BitmapTestUtil.readBitmap(GRAYSCALE_PNG_ASSET_PATH);

    rgbMatrixProcessor.drawFrame(inputTexId, /* presentationTimeUs= */ 0);
    Bitmap actualBitmap =
        BitmapTestUtil.createArgb8888BitmapFromCurrentGlFramebuffer(
            outputSize.first, outputSize.second);

    BitmapTestUtil.maybeSaveTestBitmapToCacheDirectory(
        testId, /* bitmapLabel= */ "actual", actualBitmap);
    float averagePixelAbsoluteDifference =
        BitmapTestUtil.getAveragePixelAbsoluteDifferenceArgb8888(
            expectedBitmap, actualBitmap, testId);
    assertThat(averagePixelAbsoluteDifference).isAtMost(MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE);
  }

  @Test
  public void drawFrame_inverted_producesInvertedFrame() throws Exception {
    String testId = "drawFrame_inverted";
    RgbMatrix invertedMatrix = RgbFilter.createInvertedFilter();
    rgbMatrixProcessor = new RgbMatrixProcessor(context, invertedMatrix, /* useHdr= */ false);
    Pair<Integer, Integer> outputSize = rgbMatrixProcessor.configure(inputWidth, inputHeight);
    Bitmap expectedBitmap = BitmapTestUtil.readBitmap(INVERT_PNG_ASSET_PATH);

    rgbMatrixProcessor.drawFrame(inputTexId, /* presentationTimeUs= */ 0);
    Bitmap actualBitmap =
        BitmapTestUtil.createArgb8888BitmapFromCurrentGlFramebuffer(
            outputSize.first, outputSize.second);

    BitmapTestUtil.maybeSaveTestBitmapToCacheDirectory(
        testId, /* bitmapLabel= */ "actual", actualBitmap);
    float averagePixelAbsoluteDifference =
        BitmapTestUtil.getAveragePixelAbsoluteDifferenceArgb8888(
            expectedBitmap, actualBitmap, testId);
    assertThat(averagePixelAbsoluteDifference).isAtMost(MAXIMUM_AVERAGE_PIXEL_ABSOLUTE_DIFFERENCE);
  }
}
