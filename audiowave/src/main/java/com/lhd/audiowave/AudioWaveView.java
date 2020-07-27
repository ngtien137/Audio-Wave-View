package com.lhd.audiowave;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AudioWaveView extends View {

    public static boolean ENABLE_LOG = true;

    private RectF rectOverlayCenter = new RectF();
    private RectF rectOverlayLeft = new RectF();
    private RectF rectOverlayRight = new RectF();
    private RectF rectView = new RectF();
    private RectF rectWave = new RectF();
    private Rect rectTimeLine = new Rect();
    private RectF rectThumbProgress = new RectF();
    private Rect rectThumbLeft = new Rect();
    private Rect rectThumbRight = new Rect();

    private Paint paintDefault = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintOverlay = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintWave = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintTimeLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintTimeLineIndicator = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintTimeLineIndicatorSub = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintCenterProgress = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintEditThumb = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean isShowTimeLineIndicator = true;
    private float waveLinePadding = 0f;
    private float waveLineMaxHeight = 0f;
    private float waveLineWidth = 0f;
    private float audioBarHeight = 0f;
    private float timeLineIndicatorHeight = 0f;
    private float numberSubTimelineIndicator = 3;

    private PointF pointDown = new PointF();
    private boolean isScrolling = false;

    private ThumbAlign editThumbAlign;
    private float editThumbWidth;
    private float editThumbHeight;

    private float waveViewCurrentWidth;
    private int touchSlop;
    private boolean isShowRandomPreview = true;
    private float waveZoomLevel = 1f;
    private final float maxWaveZoomLevel = 5f;
    private final float minWaveZoomLevel = 0.5f;
    private int textTimeLineDefaultWidth = 0;
    private float textTimeLinePadding = 0;

    private float centerProgressHeight;
    private boolean isThumbProgressVisible = true;
    private boolean isThumbEditVisible = true;


    private ScaleGestureDetector scaleGestureDetector;
    private IAudioListener audioListener;
    private IInteractedListener interactedListener;

    private float duration = 100f;
    private float progress = 0f;
    private float leftProgress = 0f;
    private float rightProgress = 0f;
    private float minRangeProgress = 0f;
    private ModeEdit modeEdit = ModeEdit.NONE;

    public AudioWaveView(Context context) {
        super(context);
        initView(context, null);
    }

    public AudioWaveView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context, attrs);
    }

    public AudioWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AudioWaveView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    private float currentScaleSpanX = 0f;

    public void initView(Context context, @Nullable AttributeSet attrs) {
        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureDetector.OnScaleGestureListener() {

            @Override
            public boolean onScale(ScaleGestureDetector scaleGestureDetector) {
                //eLog("Scaling: New: ", scaleGestureDetector.getCurrentSpanX(), "-- Old: ", currentScaleSpanX);
                float distanceSpan = scaleGestureDetector.getCurrentSpanX() - currentScaleSpanX;
                float adjustZoom = 0f;
                if (distanceSpan > touchSlop) {
                    adjustZoom = 0.1f;
                } else if (distanceSpan < -touchSlop) {
                    adjustZoom = -0.1f;
                }
                if (adjustZoom != 0) {
                    adjustZoomLevel(adjustZoom);
                    currentScaleSpanX = scaleGestureDetector.getCurrentSpanX();
                    adjustWaveByZoomLevel();
                    calculateCurrentWidthView();
                    if (interactedListener != null)
                        interactedListener.onAudioBarScaling();
                    invalidate();
                }
                return true;
            }

            @Override
            public boolean onScaleBegin(ScaleGestureDetector scaleGestureDetector) {
                //eLog("Scale Begin ", scaleGestureDetector.getCurrentSpanX());
                currentScaleSpanX = scaleGestureDetector.getCurrentSpanX();
                return true;
            }

            @Override
            public void onScaleEnd(ScaleGestureDetector scaleGestureDetector) {
                //eLog("Scale End");
            }
        });
        if (attrs != null) {
            float density = getResources().getDisplayMetrics().density;
            eLog("Density: ", density);
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AudioWaveView);
            paintBackground.setColor(ta.getColor(R.styleable.AudioWaveView_awv_background_color, Color.TRANSPARENT));
            audioBarHeight = ta.getDimension(R.styleable.AudioWaveView_awv_bar_audio_height, 0f);

            int overlayColor = ta.getColor(R.styleable.AudioWaveView_awv_background_color, getAppColor(R.color.color_background_color));
            paintOverlay.setColor(overlayColor);

            paintWave.setColor(ta.getColor(R.styleable.AudioWaveView_awv_wave_color, Color.BLACK));
            waveLineWidth = ta.getDimension(R.styleable.AudioWaveView_awv_wave_line_size, dpToPixel(0.5f));
            adjustWaveByZoomLevel();
            paintWave.setStrokeCap(Paint.Cap.ROUND);
            waveLinePadding = ta.getDimension(R.styleable.AudioWaveView_awv_wave_line_padding, waveLineWidth / 10f);
            waveLineMaxHeight = ta.getDimension(R.styleable.AudioWaveView_awv_wave_line_max_height, 0f);

            textTimeLinePadding = ta.getDimension(R.styleable.AudioWaveView_awv_text_timeline_padding_with_bar, 0f);
            paintTimeLine.setColor(ta.getColor(R.styleable.AudioWaveView_awv_text_timeline_color, Color.parseColor("#45000000")));
            paintTimeLine.setTextSize(ta.getDimension(R.styleable.AudioWaveView_awv_text_timeline_size, dpToPixel(9)));
            paintTimeLine.setTextAlign(Paint.Align.CENTER);
            int fontId = ta.getResourceId(R.styleable.AudioWaveView_awv_text_timeline_font, -1);
            if (fontId != -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    paintTimeLine.setTypeface(getResources().getFont(fontId));
                } else
                    paintTimeLine.setTypeface(ResourcesCompat.getFont(context, fontId));
            }

            isShowTimeLineIndicator = ta.getBoolean(R.styleable.AudioWaveView_awv_show_timeline_indicator, true);
            float timeLineIndicatorWidth = ta.getDimension(R.styleable.AudioWaveView_awv_indicator_timeline_width, dpToPixel(0.5f));
            paintTimeLineIndicator.setColor(ta.getColor(R.styleable.AudioWaveView_awv_indicator_timeline_color, getAppColor(R.color.color_indicator_timeline_color)));
            paintTimeLineIndicator.setStrokeWidth(timeLineIndicatorWidth);
            timeLineIndicatorHeight = ta.getDimension(R.styleable.AudioWaveView_awv_indicator_timeline_height, dpToPixel(6));
            paintTimeLineIndicatorSub.setColor(ta.getColor(R.styleable.AudioWaveView_awv_indicator_timeline_sub_indicator_color, getAppColor(R.color.color_indicator_timeline_sub_indicator_color)));
            paintTimeLineIndicatorSub.setStrokeWidth(timeLineIndicatorWidth);
            numberSubTimelineIndicator = ta.getInt(R.styleable.AudioWaveView_awv_indicator_timeline_sub_indicator_count, 3);

            paintCenterProgress.setColor(ta.getColor(R.styleable.AudioWaveView_awv_thumb_progress_color, getAppColor(R.color.color_center_progress_color)));
            paintCenterProgress.setStrokeWidth(ta.getDimension(R.styleable.AudioWaveView_awv_thumb_progress_size, dpToPixel(1)));
            centerProgressHeight = ta.getDimension(R.styleable.AudioWaveView_awv_thumb_progress_height, -1f);
            isThumbProgressVisible = ta.getBoolean(R.styleable.AudioWaveView_awv_thumb_progress_visible, true);

            modeEdit = ModeEdit.NONE;
            int modeInt = ta.getInt(R.styleable.AudioWaveView_awv_mode_edit, ModeEdit.NONE.mode);
            if (modeInt == ModeEdit.CUT.mode) {
                modeEdit = ModeEdit.CUT;
            } else if (modeInt == ModeEdit.TRIM.mode) {
                modeEdit = ModeEdit.TRIM;
            }

            isShowRandomPreview = ta.getBoolean(R.styleable.AudioWaveView_awv_show_random_preview, true);

            duration = ta.getFloat(R.styleable.AudioWaveView_awv_duration, 100f);
            progress = ta.getFloat(R.styleable.AudioWaveView_awv_progress, 0f);
            leftProgress = ta.getFloat(R.styleable.AudioWaveView_awv_min_progress, 0f);
            rightProgress = ta.getFloat(R.styleable.AudioWaveView_awv_min_progress, duration);
            minRangeProgress = ta.getFloat(R.styleable.AudioWaveView_awv_min_range_progress, 0f);

            isThumbEditVisible = ta.getBoolean(R.styleable.AudioWaveView_awv_thumb_edit_visible, true);
            paintEditThumb.setColor(ta.getColor(R.styleable.AudioWaveView_awv_thumb_edit_background, getAppColor(R.color.color_center_progress_color)));
            editThumbHeight = ta.getDimension(R.styleable.AudioWaveView_awv_thumb_edit_height, -1f);
            editThumbWidth = ta.getDimension(R.styleable.AudioWaveView_awv_thumb_edit_width, dpToPixel(1));
            int editThumbAlignValue = ta.getInt(R.styleable.AudioWaveView_awv_thumb_edit_align, ThumbAlign.CENTER.value);
            editThumbAlign = ThumbAlign.CENTER;
            if (editThumbAlignValue == ThumbAlign.TOP.value) {
                editThumbAlign = ThumbAlign.TOP;
            } else if (editThumbAlignValue == ThumbAlign.BOTTOM.value) {
                editThumbAlign = ThumbAlign.BOTTOM;
            }
            ta.recycle();
        }
    }

    private int getAppColor(@ColorRes int colorRes) {
        return ContextCompat.getColor(getContext(), colorRes);
    }

    private void adjustZoomLevel(float value) {
        if (waveZoomLevel + value > maxWaveZoomLevel)
            waveZoomLevel = maxWaveZoomLevel;
        else if (waveZoomLevel + value < minWaveZoomLevel)
            waveZoomLevel = minWaveZoomLevel;
        else waveZoomLevel += value;
    }

    private void adjustWaveByZoomLevel() {
        paintWave.setStrokeWidth(waveLineWidth * waveZoomLevel);
    }

    private float dpToPixel(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        rectView.set(getPaddingLeft(), getPaddingTop(), w - getPaddingRight(), h - getPaddingBottom());
        if (audioBarHeight == 0f) {
            audioBarHeight = rectView.height();
        }
        //text
        loadTextTimelineSizeDefault("00:00");
        //
        rectWave.set(rectView.left, rectView.centerY() - audioBarHeight / 2f, rectView.right, rectView.centerY() + audioBarHeight / 2f);

        if (waveLineMaxHeight == 0f) {
            waveLineMaxHeight = rectWave.height() - paintWave.getStrokeWidth(); //Phải trừ đi stroke width vì khi có stroke width thì đường vẽ bị to ra 1 nửa strokeWidth mỗi bên, trên, dưới
        } else {
            waveLineMaxHeight -= paintWave.getStrokeWidth();
        }

        rectOverlayCenter.top = rectView.top;
        rectOverlayCenter.bottom = rectView.bottom;

        rectOverlayLeft.top = rectView.top;
        rectOverlayLeft.bottom = rectView.bottom;

        rectOverlayRight.top = rectView.top;
        rectOverlayRight.bottom = rectView.bottom;

        if (centerProgressHeight == -1f) {
            centerProgressHeight = rectWave.height();
        }
        rectThumbProgress.top = rectWave.centerY() - centerProgressHeight / 2f - paintCenterProgress.getStrokeWidth() / 2f;
        rectThumbProgress.bottom = rectThumbProgress.top + centerProgressHeight;
        validateRectWithProgress();
        calculateCurrentWidthView();
        configureEditThumb();
        validateEditThumbByProgress();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void configureEditThumb() {
        if (editThumbHeight == -1) {
            editThumbHeight = rectWave.height();
        }
        if (editThumbAlign == ThumbAlign.TOP) {
            rectThumbLeft.top = (int) rectWave.top;
            rectThumbLeft.bottom = (int) (rectThumbLeft.top + editThumbHeight);
        } else if (editThumbAlign == ThumbAlign.BOTTOM) {
            rectThumbLeft.bottom = (int) rectWave.bottom;
            rectThumbLeft.top = (int) (rectThumbLeft.bottom - editThumbHeight);
        } else {
            rectThumbLeft.top = (int) (rectWave.centerY() - editThumbHeight / 2f);
            rectThumbLeft.bottom = (int) (rectWave.centerY() - editThumbHeight / 2f);
        }
        rectThumbRight.top = rectThumbLeft.top;
        rectThumbRight.bottom = rectThumbLeft.bottom;
    }

    private void validateEditThumbByProgress() {
        validateThumbLeftWithProgress();
        validateThumbRightWithProgress();
    }

    private void loadTextTimelineSizeDefault(String defaultTimeLine) {
        paintTimeLine.getTextBounds(defaultTimeLine, 0, defaultTimeLine.length(), rectTimeLine);
        textTimeLineDefaultWidth = rectTimeLine.width();
    }

    //Sound file
    private SoundFile mSoundFile;
    private int mSampleRate;
    private int mSamplesPerFrame;

    private int mNumZoomLevels;
    private int[] mLenByZoomLevel;
    private double[][] mValuesByZoomLevel;
    private double[] mZoomFactorByZoomLevel;
    private int mZoomLevel;
    private int[] mHeightsAtThisZoomLevel;

    private boolean mInitialized;

    /**
     * Loading sample data of audio
     */
    private void computeDoublesForAllZoomLevels() {
        int numFrames = mSoundFile.getNumFrames();
        int[] frameGains = mSoundFile.getFrameGains();
        double[] smoothedGains = new double[numFrames];
        if (numFrames == 1) {
            smoothedGains[0] = frameGains[0];
        } else if (numFrames == 2) {
            smoothedGains[0] = frameGains[0];
            smoothedGains[1] = frameGains[1];
        } else if (numFrames > 2) {
            smoothedGains[0] = (double) (
                    (frameGains[0] / 2.0) +
                            (frameGains[1] / 2.0));
            for (int i = 1; i < numFrames - 1; i++) {
                smoothedGains[i] = (double) (
                        (frameGains[i - 1] / 3.0) +
                                (frameGains[i] / 3.0) +
                                (frameGains[i + 1] / 3.0));
            }
            smoothedGains[numFrames - 1] = (double) (
                    (frameGains[numFrames - 2] / 2.0) +
                            (frameGains[numFrames - 1] / 2.0));
        }

        // Make sure the range is no more than 0 - 255
        double maxGain = 1.0;
        for (int i = 0; i < numFrames; i++) {
            if (smoothedGains[i] > maxGain) {
                maxGain = smoothedGains[i];
            }
        }
        double scaleFactor = 1.0;
        if (maxGain > 255.0) {
            scaleFactor = 255 / maxGain;
        }

        // Build histogram of 256 bins and figure out the new scaled max
        maxGain = 0;
        int gainHist[] = new int[256];
        for (int i = 0; i < numFrames; i++) {
            int smoothedGain = (int) (smoothedGains[i] * scaleFactor);
            if (smoothedGain < 0)
                smoothedGain = 0;
            if (smoothedGain > 255)
                smoothedGain = 255;

            if (smoothedGain > maxGain)
                maxGain = smoothedGain;

            gainHist[smoothedGain]++;
        }

        // Re-calibrate the min to be 5%
        double minGain = 0;
        int sum = 0;
        while (minGain < 255 && sum < numFrames / 20) {
            sum += gainHist[(int) minGain];
            minGain++;
        }

        // Re-calibrate the max to be 99%
        sum = 0;
        while (maxGain > 2 && sum < numFrames / 100) {
            sum += gainHist[(int) maxGain];
            maxGain--;
        }

        // Compute the heights
        double[] heights = new double[numFrames];
        double range = maxGain - minGain;
        for (int i = 0; i < numFrames; i++) {
            double value = (smoothedGains[i] * scaleFactor - minGain) / range;
            if (value < 0.0)
                value = 0.0;
            if (value > 1.0)
                value = 1.0;
            heights[i] = value * value;
        }

        mNumZoomLevels = 5;
        mLenByZoomLevel = new int[5];
        mZoomFactorByZoomLevel = new double[5];
        mValuesByZoomLevel = new double[5][];

        // Level 0 is doubled, with interpolated values
        mLenByZoomLevel[0] = numFrames * 2;
        mZoomFactorByZoomLevel[0] = 2.0;
        mValuesByZoomLevel[0] = new double[mLenByZoomLevel[0]];
        if (numFrames > 0) {
            mValuesByZoomLevel[0][0] = 0.5 * heights[0];
            mValuesByZoomLevel[0][1] = heights[0];
        }
        for (int i = 1; i < numFrames; i++) {
            mValuesByZoomLevel[0][2 * i] = 0.5 * (heights[i - 1] + heights[i]);
            mValuesByZoomLevel[0][2 * i + 1] = heights[i];
        }

        // Level 1 is normal
        mLenByZoomLevel[1] = numFrames;
        mValuesByZoomLevel[1] = new double[mLenByZoomLevel[1]];
        mZoomFactorByZoomLevel[1] = 1.0;
        for (int i = 0; i < mLenByZoomLevel[1]; i++) {
            mValuesByZoomLevel[1][i] = heights[i];
        }

        // 3 more levels are each halved
        for (int j = 2; j < 5; j++) {
            mLenByZoomLevel[j] = mLenByZoomLevel[j - 1] / 2;
            mValuesByZoomLevel[j] = new double[mLenByZoomLevel[j]];
            mZoomFactorByZoomLevel[j] = mZoomFactorByZoomLevel[j - 1] / 2.0;
            for (int i = 0; i < mLenByZoomLevel[j]; i++) {
                mValuesByZoomLevel[j][i] =
                        0.5 * (mValuesByZoomLevel[j - 1][2 * i] +
                                mValuesByZoomLevel[j - 1][2 * i + 1]);
            }
        }

        if (numFrames > 5000) {
            mZoomLevel = 3;
        } else if (numFrames > 1000) {
            mZoomLevel = 2;
        } else if (numFrames > 300) {
            mZoomLevel = 1;
        } else {
            mZoomLevel = 0;
        }

        mInitialized = true;
    }


    public boolean hasSoundFile() {
        return mSoundFile != null;
    }

    public void setAudioPath(String path) {
        try {
            mSoundFile = SoundFile.create(path, new SoundFile.ProgressListener() {
                @Override
                public boolean reportProgress(double fractionComplete) {
                    if (audioListener != null) {
                        audioListener.onLoadingAudio((int) (fractionComplete * 100), false);
                    }
                    eLog("Progress: ", (int) (fractionComplete * 100));
                    return true;
                }
            });
            if (audioListener != null) {
                audioListener.onLoadingAudio(100, true);
            }
            mSampleRate = mSoundFile.getSampleRate();
            mSamplesPerFrame = mSoundFile.getSamplesPerFrame();
            duration = mSoundFile.getDuration();
            progress = 0f;
            if (modeEdit == ModeEdit.TRIM) {
                leftProgress = duration / 2f - minRangeProgress / 2f;
                rightProgress = duration / 2f + minRangeProgress / 2f;
            } else {
                leftProgress = 0f;
                rightProgress = duration;
            }
            validateEditThumbByProgress();
            computeDoublesForAllZoomLevels();
            computeIntsForThisZoomLevel();
            calculateCurrentWidthView();
            postInvalidate();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (SoundFile.InvalidInputException e) {
            e.printStackTrace();
        }
    }

    private void computeIntsForThisZoomLevel() {
        int halfHeight = (int) (waveLineMaxHeight / 2);
        mHeightsAtThisZoomLevel = new int[mLenByZoomLevel[mZoomLevel]];
        for (int i = 0; i < mLenByZoomLevel[mZoomLevel]; i++) {
            mHeightsAtThisZoomLevel[i] =
                    (int) ((mValuesByZoomLevel[mZoomLevel][i] * halfHeight));
        }
    }

    private void calculateCurrentWidthView() {
        if (mHeightsAtThisZoomLevel == null || mHeightsAtThisZoomLevel.length == 0) {
            waveViewCurrentWidth = getWidth();
        } else {
            waveViewCurrentWidth = (mHeightsAtThisZoomLevel.length * waveLineWidth + (mHeightsAtThisZoomLevel.length - 1) * waveLinePadding) * waveZoomLevel;
        }
        rectWave.right = rectView.left + waveViewCurrentWidth;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(rectWave, paintBackground);

        if (hasSoundFile()) {
            float centerY = rectWave.centerY();
            float offset = 0f + (paintWave.getStrokeWidth() / 2f) * waveZoomLevel;
            for (int value : mHeightsAtThisZoomLevel) {
                canvas.drawLine(offset, centerY - value, offset, centerY + value, paintWave);
                offset += (waveLineWidth + waveLinePadding) * waveZoomLevel;
            }
        } else if (isShowRandomPreview) {
            drawRandomPreview(canvas);
        }

        drawTimeLineAndIndicator(canvas);
        if (isThumbProgressVisible)
            drawCenterProgress(canvas);
        if (isThumbEditVisible) {
            drawThumbCut(canvas);
        }
    }

    /**
     * Draw TimeLine and the line value indicator
     */
    private void drawTimeLineAndIndicator(Canvas canvas) {
        loadTextTimelineSizeDefault("00:00");
        float yText = rectWave.top - textTimeLinePadding;
        float spaceBetweenTwoTimeLine = textTimeLineDefaultWidth * 2f;
        int countText = (int) (waveViewCurrentWidth / spaceBetweenTwoTimeLine);
        float offset = textTimeLineDefaultWidth / 2f;
        canvas.drawText("00:00", offset, yText, paintTimeLine);
        drawTimeLineIndicator(canvas, offset, timeLineIndicatorHeight, paintTimeLineIndicator);
        drawTimeLineIndicator(canvas, spaceBetweenTwoTimeLine / 2f, timeLineIndicatorHeight / 2f, paintTimeLineIndicatorSub);
        drawTimeLineIndicator(canvas, offset + spaceBetweenTwoTimeLine / 2f, timeLineIndicatorHeight / 2f, paintTimeLineIndicatorSub);
        if (duration > 0) {
            offset = spaceBetweenTwoTimeLine;
            for (int i = 0; i < countText; i++) {
                float progressTime = convertPositionToProgress(offset);
                String sTime = convertTimeToTimeFormat(progressTime);
                boolean drawThisWave = false;
                if (i == countText - 1) {
                    float fixOffset = offset - spaceBetweenTwoTimeLine;
                    if (rectWave.right - offset >= spaceBetweenTwoTimeLine) {
                        drawThisWave = true;
                    }
                    //Không vẽ time line ở đây vì quá gần với timeline cuối
                    //Nhưng vẫn vẽ các timeline phụ
                    drawSubTimelineIndicator(canvas, fixOffset, (rectWave.right - textTimeLineDefaultWidth / 2f) - fixOffset, spaceBetweenTwoTimeLine);
                } else {
                    drawThisWave = true;
                }
                if (drawThisWave) {
                    canvas.drawText(sTime, offset, yText, paintTimeLine);
                    drawTimeLineIndicator(canvas, offset, timeLineIndicatorHeight, paintTimeLineIndicator);
                    if (i < countText - 2) {
                        drawSubTimelineIndicator(canvas, offset, spaceBetweenTwoTimeLine, spaceBetweenTwoTimeLine);
                    }
                }
                offset += spaceBetweenTwoTimeLine;
            }

            //Vẽ timeline cuối cùng với giá trị là duration của audio
            String lastTime = convertTimeToTimeFormat(duration);
            loadTextTimelineSizeDefault(lastTime);
            offset = rectWave.right - rectTimeLine.width() / 2f;
            canvas.drawText(lastTime, offset, yText, paintTimeLine);
            drawTimeLineIndicator(canvas, offset, timeLineIndicatorHeight, paintTimeLineIndicator);
        }
    }

    /**
     * Draw large line value indicator
     */
    private void drawTimeLineIndicator(Canvas canvas, float xIndicator, float height, Paint paintIndicator) {
        if (isShowTimeLineIndicator) {
            canvas.drawLine(xIndicator, rectWave.top, xIndicator, rectWave.top + height, paintIndicator);
            canvas.drawLine(xIndicator, rectWave.bottom - height, xIndicator, rectWave.bottom, paintIndicator);
        }
    }

    /**
     * Draw small line value indicator
     */
    private void drawSubTimelineIndicator(Canvas canvas, float startX, float widthForSubIndicator, float minWidthForFullSub) {
        if (isShowTimeLineIndicator && numberSubTimelineIndicator > 0) {
            if (widthForSubIndicator >= minWidthForFullSub) {
                float spaceBetween = widthForSubIndicator / (numberSubTimelineIndicator + 1);
                float offset = startX + spaceBetween;
                for (int i = 0; i < numberSubTimelineIndicator; i++) {
                    drawTimeLineIndicator(canvas, offset, timeLineIndicatorHeight / 2f, paintTimeLineIndicatorSub);
                    offset += spaceBetween;
                }
            } else {
                float spaceBetween = widthForSubIndicator / 2;
                float offset = startX + spaceBetween;
                for (int i = 0; i < 2; i++) {
                    drawTimeLineIndicator(canvas, offset, timeLineIndicatorHeight / 2f, paintTimeLineIndicatorSub);
                    offset += spaceBetween;
                }
            }
        }
    }

    /**
     * Draw Thumb
     */
    private void drawCenterProgress(Canvas canvas) {
        canvas.drawLine(rectThumbProgress.left, rectThumbProgress.top, rectThumbProgress.left, rectThumbProgress.bottom, paintCenterProgress);
    }

    private void drawThumbCut(Canvas canvas) {
        canvas.drawRect(rectThumbLeft, paintEditThumb);
        canvas.drawRect(rectThumbRight, paintEditThumb);
    }

    /////

    private float convertProgressToPosition(float progress) {
        return progress / duration * rectWave.width() + rectWave.left;
    }

    private float convertPositionToProgress(float pixel) {
        return pixel / rectWave.width() * duration;
    }

    private String convertTimeToTimeFormat(float time) {
        int t = (int) time / 1000;
        int second = t % 60;
        int minute = t / 60;
        DecimalFormat format = new DecimalFormat("00");
        return format.format(minute) + ":" + format.format(second);
    }

    private void validateRectWithProgress() {
        rectThumbProgress.left = convertProgressToPosition(progress);
    }

    private void validateThumbLeftWithProgress() {  
        validateEditThumbByProgress(rectThumbLeft, leftProgress);
    }

    private void validateThumbRightWithProgress() { 
        validateEditThumbByProgress(rectThumbRight, rightProgress);
    }

    private void validateEditThumbByProgress(Rect thumbRect, Float progress) {
        thumbRect.left = (int) (convertProgressToPosition(progress) - editThumbWidth / 2f);
        thumbRect.right = (int) (thumbRect.left + editThumbWidth);
    }

    /**
     * Draw preview wave
     */
    private List<Float> listPreviewWave = new ArrayList<>(); //List fake for show random preview

    private void drawRandomPreview(Canvas canvas) {
        boolean addToListPreview = true;
        if (!listPreviewWave.isEmpty()) {
            addToListPreview = false;
        }
        int demoListSize = (int) (getWidth() / ((waveLineWidth + waveLinePadding)));
        float centerY = getHeight() / 2f;
        float offset = 0f + (paintWave.getStrokeWidth() / 2f) * waveZoomLevel;
        for (int i = 0; i < demoListSize; i++) {
            float randomPercent = ((new Random()).nextInt(10) / 10f);
            float randomHeight = waveLineMaxHeight / 2f * randomPercent;
            if (addToListPreview) {
                listPreviewWave.add(randomHeight);
            } else {
                randomHeight = listPreviewWave.get(i);
            }
            canvas.drawLine(offset, centerY - randomHeight, offset, centerY + randomHeight, paintWave);
            offset += (waveLineWidth + waveLinePadding) * waveZoomLevel;
        }
        waveViewCurrentWidth = offset - (waveLineWidth / 2f + waveLinePadding) * waveZoomLevel;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (interactedListener != null) {
                    interactedListener.onTouchDownAudioBar();
                }
                pointDown.set(event.getX(), event.getY());
                return true;
            case MotionEvent.ACTION_MOVE:
                float disX = event.getX() - pointDown.x;
                float disY = event.getY() - pointDown.y;
                if (isScrolling) {
                    scroll((int) disX);
                    pointDown.set(event.getX(), event.getY());
                    return true;
                } else {
                    if (Math.abs(disX) >= touchSlop) {
                        isScrolling = true;
                        return true;
                    }
                }
                return false;
            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                if (isScrolling) {
                    isScrolling = false;
                }
                if (interactedListener != null) {
                    interactedListener.onTouchReleaseAudioBar();
                }
                break;
            default:

                break;
        }
        return super.onTouchEvent(event);
    }

    public void scroll(int disX) {
        if (getScrollX() - disX < 0 || waveViewCurrentWidth <= getWidth()) {
            scrollTo(0, 0);
        } else if (getScrollX() - disX > waveViewCurrentWidth - getWidth()) {
            scrollTo((int) (waveViewCurrentWidth - getWidth()), 0);
        } else
            scrollBy(-disX, 0);

    }

    public interface IAudioListener {
        void onLoadingAudio(int progress, boolean prepareView);
    }

    public interface IInteractedListener {
        void onTouchDownAudioBar();

        void onTouchReleaseAudioBar();

        void onAudioBarScaling();
    }

    public enum ModeEdit {
        NONE(0), TRIM(2), CUT(1);
        public int mode;

        ModeEdit(int mode) {
            this.mode = mode;
        }
    }

    public enum ThumbAlign {
        TOP(0), CENTER(2), BOTTOM(1);
        public int value;

        ThumbAlign(int mode) {
            this.value = mode;
        }
    }

    public void eLog(Object... message) {
        if (ENABLE_LOG) {
            StringBuilder mes = new StringBuilder();
            for (Object sMes : message
            ) {
                String m = "null";
                if (sMes != null)
                    m = sMes.toString();
                mes.append(m);
            }
            Log.e("AudioWaveViewLog", mes.toString());
        }
    }

    public void setAudioListener(IAudioListener audioListener) {
        this.audioListener = audioListener;
    }

    public void setInteractedListener(IInteractedListener interactedListener) {
        this.interactedListener = interactedListener;
    }

    public void setProgress(float progress) {
        this.progress = validateProgress(progress, 0f, duration);
        postInvalidate();
    }

    public void setMinProgress(float progress) {
        leftProgress = validateProgress(progress, 0f, duration);
        postInvalidate();
    }

    public void setMaxProgress(float progress) {
        rightProgress = validateProgress(progress, 0f, duration);
        postInvalidate();
    }

    private float validateProgress(float progress, float minProgress, float maxProgress) {
        if (progress < minProgress)
            return minProgress;
        else if (progress > maxProgress)
            return maxProgress;
        return progress;
    }
}