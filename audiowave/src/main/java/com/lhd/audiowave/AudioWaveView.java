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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;

import androidx.annotation.ColorRes;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AudioWaveView extends View {

    private static Map<String, SoundFile> mapCache = new HashMap<>();

    public static boolean ENABLE_LOG = true;

    private RectF rectOverlayCenter = new RectF();
    private RectF rectOverlayLeft = new RectF();
    private RectF rectOverlayRight = new RectF();
    private RectF rectView = new RectF();
    private RectF rectWave = new RectF();
    private RectF rectBackground = new RectF();
    private Rect rectTimeLine = new Rect();
    private RectF rectThumbProgress = new RectF();
    private RectF rectThumbLeft = new RectF();
    private RectF rectThumbRight = new RectF();
    private Rect rectAnchorLeft = new Rect();
    private Rect rectAnchorRight = new Rect();
    private Rect rectTextValue = new Rect();

    private Paint paintDefault = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintOverlayPick = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintOverlayRemove = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintWave = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintTimeLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintTimeLineIndicator = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintTimeLineIndicatorSub = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintCenterProgress = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintEditThumb = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintTextValue = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean isShowTimeLineIndicator = true;  //Có hiển thị các đường kẻ giá trị timeline hay không
    private float waveLinePadding = 0f; //Khoảng cách giữa cách đường sóng
    private float waveLineMaxHeight = 0f; // Độ cao lớn nhất của đường sóng
    private float waveLineWidth = 0f; //Chiều rộng của đường sóng
    private float audioBarHeight = 0f; //Độ cao của thanh audio
    private float timeLineIndicatorHeight = 0f; //Độ cao của đường kẻ giá trị timeline
    private float numberSubTimelineIndicator = 3; //Số lượng các đường kẻ timeline phụ (nhỏ) ở giữa các đường kẻ chính

    private PointF pointDown = new PointF();
    private boolean isScrolling = false;
    private float lastDistanceX = 0f;

    private Align editThumbAlign; //Căn lề để tính vị trí cho đường kẻ thumb edit (top,center,bottom)
    private float editThumbWidth;
    private float editThumbHeight;

    private float waveViewCurrentWidth; //Chiều rộng hiện tại của thanh bar chứa đầy đủ đường sóng
    private int touchSlop;
    private boolean isShowRandomPreview = true;
    private float waveZoomLevel = 1f;
    private float maxWaveZoomLevel = 5f;
    private float minWaveZoomLevel = 0.5f;
    private float defaultMinWaveZoomLevel = 0.5f;
    private int textTimeLineDefaultWidth = 0;
    private float textTimeLinePadding = 0;
    private float textValuePadding = 0f;
    private boolean isTextValuePullTogether = true; //Text giá trị của thumb edit có đẩy nhau khi ở gần nhau được hay không
    private boolean isAutoAdjustZoomLevel = false; //Tự động chỉnh kích cỡ zoom level sau khi load audio path

    private ProgressMode thumbProgressMode = ProgressMode.FLEXIBLE;
    private float thumbProgressStaticPosition = 0f;
    private float centerProgressHeight;
    private boolean isThumbProgressVisible = true;
    private boolean isThumbEditVisible = true;
    private boolean isSetThumbProgressToZeroAfterInit = false;

    private ScaleGestureDetector scaleGestureDetector;
    private IAudioListener audioListener;
    private IInteractedListener interactedListener;

    private float duration = 100f;
    private float progress = 0f;
    private float leftProgress = 0f;
    private float rightProgress = 0f;
    private float minRangeProgress = 0f;
    private float thumbTouchExpandSize = 0f;
    private float minSpaceBetweenText = 0f;
    private boolean isFixedThumbProgressByThumbEdit = true;
    private ModeEdit modeEdit = ModeEdit.NONE;

    private float anchorImageWidth = 0f;
    private float anchorImageHeight = 0f;
    private Align leftAnchorAlignVertical = Align.CENTER;
    private Align rightAnchorAlignVertical = Align.CENTER;
    private Align leftAnchorAlignHorizontal = Align.CENTER;
    private Align rightAnchorAlignHorizontal = Align.CENTER;
    private Drawable leftAnchorImage;
    private Drawable rightAnchorImage;
    private TextValuePosition textValuePosition;

    private float thumbEditCircleSize = 0f;
    private boolean isThumbEditCircleVisible = false;

    private boolean isMovingThumb;
    private int lastFocusThumbIndex = ThumbIndex.THUMB_NONE;
    private int thumbIndex = ThumbIndex.THUMB_NONE;
    private boolean isFlinging = false;
    private boolean isSendFlingEndEvent = true;

    private GestureDetector gestureDetector;
    private Scroller scroller;

    private boolean isCancel;
    private CacheMode cacheMode = CacheMode.SINGLE;

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
                if (mHeightsAtThisZoomLevel == null || mHeightsAtThisZoomLevel.length == 0)
                    return false;
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
                    validateEditThumbByProgress();
                    validateThumbProgressWithProgress();
                    invalidate();
                    if (interactedListener != null) {
                        interactedListener.onAudioBarScaling();
                        interactedListener.onRangerChanging(leftProgress, rightProgress, AdjustMode.SCALE);
                    }
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
                invalidate();
            }
        });

        scroller = new Scroller(context, new LinearInterpolator());
        gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
                float minX = 0f;
                float maxX = (int) waveViewCurrentWidth - getWidth();
                if (thumbProgressMode == ProgressMode.STATIC) {
                    if (modeEdit == ModeEdit.TRIM) {
                        minX = rectThumbLeft.centerX();
                        maxX = rectThumbRight.centerX() - getThumbProgressStaticPosition() * 2;
                    } else if (modeEdit == ModeEdit.CUT_OUT) {
                        if (progress < leftProgress) {
                            minX = 0f;
                            maxX = rectThumbLeft.centerX() - getThumbProgressStaticPosition() * 2;
                        } else if (progress > rightProgress) {
                            minX = rectThumbRight.centerX();
                            maxX = (int) waveViewCurrentWidth - getWidth();
                        }
                    }
                    minX -= getThumbProgressStaticPosition();
                    maxX += getThumbProgressStaticPosition();
                }
                if (modeEdit != ModeEdit.NONE) {
                    if (modeEdit == ModeEdit.TRIM && (progress < leftProgress || progress > rightProgress)) {
                        scrollStaticProgressIfNotInRange(velocityX);
                    } else if (modeEdit == ModeEdit.CUT_OUT && progress > leftProgress && progress < rightProgress) {
                        scrollStaticProgressIfNotInRange(velocityX);
                    } else {
                        isFlinging = true;
                        isSendFlingEndEvent = false;
                        if (interactedListener != null) {
                            interactedListener.onStartFling();
                        }
                        scroller.fling(getScrollX(), getScrollY(), (int) (-velocityX), 0, (int) minX, (int) maxX, getScrollY(), getScrollY());
                        invalidate();
                    }
                } else {
                    isFlinging = true;
                    isSendFlingEndEvent = false;
                    if (interactedListener != null) {
                        interactedListener.onStartFling();
                    }
                    eLog("Max: ", maxX, "--- MIn: ", minX);
                    scroller.fling(getScrollX(), getScrollY(), (int) (-velocityX), 0, (int) minX, (int) maxX, getScrollY(), getScrollY());
                    invalidate();
                }
                return true;
            }

        });

        if (attrs != null) {
            TypedArray ta = context.obtainStyledAttributes(attrs, R.styleable.AudioWaveView);
            paintBackground.setColor(ta.getColor(R.styleable.AudioWaveView_awv_background_color, Color.TRANSPARENT));
            audioBarHeight = ta.getDimension(R.styleable.AudioWaveView_awv_bar_audio_height, 0f);

            int overlayColorPick = ta.getColor(R.styleable.AudioWaveView_awv_overlay_color_pick, getAppColor(R.color.color_transparent));
            paintOverlayPick.setColor(overlayColorPick);
            int overlayColorRemove = ta.getColor(R.styleable.AudioWaveView_awv_overlay_color_remove, getAppColor(R.color.color_overlay_color));
            paintOverlayRemove.setColor(overlayColorRemove);

            paintWave.setColor(ta.getColor(R.styleable.AudioWaveView_awv_wave_color, Color.BLACK));
            waveLineWidth = ta.getDimension(R.styleable.AudioWaveView_awv_wave_line_size, dpToPixel(0.5f));
            adjustWaveByZoomLevel();
            paintWave.setStrokeCap(Paint.Cap.ROUND);
            waveLinePadding = ta.getDimension(R.styleable.AudioWaveView_awv_wave_line_padding, waveLineWidth / 10f);
            waveLineMaxHeight = ta.getDimension(R.styleable.AudioWaveView_awv_wave_line_max_height, 0f);
            defaultMinWaveZoomLevel = ta.getFloat(R.styleable.AudioWaveView_awv_wave_zoom_min_level, 0.5f);
            minWaveZoomLevel = defaultMinWaveZoomLevel;
            maxWaveZoomLevel = ta.getFloat(R.styleable.AudioWaveView_awv_wave_zoom_max_level, 5f);
            isAutoAdjustZoomLevel = ta.getBoolean(R.styleable.AudioWaveView_awv_wave_zoom_level_auto, false);

            textTimeLinePadding = ta.getDimension(R.styleable.AudioWaveView_awv_text_timeline_padding_with_bar, 0f);
            paintTimeLine.setColor(ta.getColor(R.styleable.AudioWaveView_awv_text_timeline_color, getAppColor(R.color.text_timeline_color)));
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
            isSetThumbProgressToZeroAfterInit = ta.getBoolean(R.styleable.AudioWaveView_awv_thumb_progress_to_zero_after_initializing, false);

            modeEdit = ModeEdit.NONE;
            int modeInt = ta.getInt(R.styleable.AudioWaveView_awv_mode_edit, ModeEdit.NONE.mode);
            if (modeInt == ModeEdit.TRIM.mode) {
                modeEdit = ModeEdit.TRIM;
            } else if (modeInt == ModeEdit.CUT_OUT.mode) {
                modeEdit = ModeEdit.CUT_OUT;
            }

            isShowRandomPreview = ta.getBoolean(R.styleable.AudioWaveView_awv_show_random_preview, true);

            duration = ta.getFloat(R.styleable.AudioWaveView_awv_duration, 100f);
            progress = ta.getFloat(R.styleable.AudioWaveView_awv_progress, 0f);
            leftProgress = ta.getFloat(R.styleable.AudioWaveView_awv_min_progress, 0f);
            rightProgress = ta.getFloat(R.styleable.AudioWaveView_awv_max_progress, duration);
            minRangeProgress = ta.getFloat(R.styleable.AudioWaveView_awv_min_range_progress, 0f);

            int thumbProgressModeInt = ta.getInt(R.styleable.AudioWaveView_awv_thumb_progress_mode, ProgressMode.FLEXIBLE.mode);
            if (thumbProgressModeInt == ProgressMode.STATIC.mode) {
                thumbProgressMode = ProgressMode.STATIC;
            } else {
                thumbProgressMode = ProgressMode.FLEXIBLE;
            }
            try {
                thumbProgressStaticPosition = ta.getDimension(R.styleable.AudioWaveView_awv_thumb_progress_static_position, 0f);
            } catch (RuntimeException e) {
                thumbProgressStaticPosition = ta.getInt(R.styleable.AudioWaveView_awv_thumb_progress_static_position, 0);
            }

            thumbTouchExpandSize = ta.getDimension(R.styleable.AudioWaveView_awv_thumb_touch_expand_size, 0f);
            isFixedThumbProgressByThumbEdit = ta.getBoolean(R.styleable.AudioWaveView_awv_fixed_thumb_progress_by_thumb_edit, true);
            isThumbEditVisible = ta.getBoolean(R.styleable.AudioWaveView_awv_thumb_edit_visible, true);
            paintEditThumb.setColor(ta.getColor(R.styleable.AudioWaveView_awv_thumb_edit_background, getAppColor(R.color.color_center_progress_color)));
            editThumbHeight = ta.getDimension(R.styleable.AudioWaveView_awv_thumb_edit_height, -1f);
            editThumbWidth = ta.getDimension(R.styleable.AudioWaveView_awv_thumb_edit_width, dpToPixel(1));
            int editThumbAlignValue = ta.getInt(R.styleable.AudioWaveView_awv_thumb_edit_align, Align.CENTER.value);
            editThumbAlign = Align.CENTER;
            if (editThumbAlignValue == Align.TOP.value) {
                editThumbAlign = Align.TOP;
            } else if (editThumbAlignValue == Align.BOTTOM.value) {
                editThumbAlign = Align.BOTTOM;
            }

            int leftAnchorAlignVerticalInt = ta.getInt(R.styleable.AudioWaveView_awv_thumb_edit_left_anchor_align_vertical, Align.CENTER.value);
            leftAnchorAlignVertical = getAlignVertical(leftAnchorAlignVerticalInt);
            int rightAnchorAlignVerticalInt = ta.getInt(R.styleable.AudioWaveView_awv_thumb_edit_right_anchor_align_vertical, leftAnchorAlignVerticalInt);
            rightAnchorAlignVertical = getAlignVertical(rightAnchorAlignVerticalInt);
            int leftAnchorAlignHorizontalInt = ta.getInt(R.styleable.AudioWaveView_awv_thumb_edit_left_anchor_align_horizontal, Align.CENTER.value);
            leftAnchorAlignHorizontal = getAlignHorizontal(leftAnchorAlignHorizontalInt);
            int rightAnchorAlignHorizontalInt = ta.getInt(R.styleable.AudioWaveView_awv_thumb_edit_right_anchor_align_horizontal, leftAnchorAlignHorizontalInt);
            rightAnchorAlignHorizontal = getAlignHorizontal(rightAnchorAlignHorizontalInt);
            anchorImageHeight = ta.getDimension(R.styleable.AudioWaveView_awv_thumb_edit_anchor_height, dpToPixel(16));
            anchorImageWidth = ta.getDimension(R.styleable.AudioWaveView_awv_thumb_edit_anchor_width, dpToPixel(24));
            leftAnchorImage = ta.getDrawable(R.styleable.AudioWaveView_awv_thumb_edit_left_anchor_image);
            rightAnchorImage = ta.getDrawable(R.styleable.AudioWaveView_awv_thumb_edit_right_anchor_image);

            isThumbEditCircleVisible = ta.getBoolean(R.styleable.AudioWaveView_awv_thumb_edit_circle_visible, false);
            thumbEditCircleSize = ta.getDimension(R.styleable.AudioWaveView_awv_thumb_edit_circle_radius, 0f);

            paintTextValue.setTextAlign(Paint.Align.CENTER);
            paintTextValue.setColor(ta.getColor(R.styleable.AudioWaveView_awv_thumb_edit_text_value_color, getAppColor(R.color.text_thumb_cut_text_value_color)));
            paintTextValue.setTextSize(ta.getDimension(R.styleable.AudioWaveView_awv_thumb_edit_text_value_size, dpToPixel(9)));
            textValuePadding = ta.getDimension(R.styleable.AudioWaveView_awv_thumb_edit_text_value_padding, dpToPixel(1));
            int fontIdTextValue = ta.getResourceId(R.styleable.AudioWaveView_awv_thumb_edit_text_value_font, -1);
            if (fontIdTextValue != -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    paintTextValue.setTypeface(getResources().getFont(fontIdTextValue));
                } else
                    paintTextValue.setTypeface(ResourcesCompat.getFont(context, fontIdTextValue));
            }
            int textValuePositionValue = ta.getInt(R.styleable.AudioWaveView_awv_thumb_edit_text_value_position, TextValuePosition.NONE.position);
            if (textValuePositionValue == TextValuePosition.BOTTOM_OF_ANCHOR.position) {
                textValuePosition = TextValuePosition.BOTTOM_OF_ANCHOR;
            } else if (textValuePositionValue == TextValuePosition.TOP_OF_ANCHOR.position) {
                textValuePosition = TextValuePosition.TOP_OF_ANCHOR;
            } else {
                textValuePosition = TextValuePosition.NONE;
            }
            isTextValuePullTogether = ta.getBoolean(R.styleable.AudioWaveView_awv_thumb_edit_text_value_pull_together, true);
            minSpaceBetweenText = ta.getDimension(R.styleable.AudioWaveView_awv_thumb_min_space_between_text, dpToPixel(0f));

            int cacheModeInt = ta.getInt(R.styleable.AudioWaveView_awv_cache_mode, CacheMode.SINGLE.value);

            if (cacheModeInt == CacheMode.NONE.value) {
                cacheMode = CacheMode.NONE;
            } else if (cacheModeInt == CacheMode.MULTIPLE.value) {
                cacheMode = CacheMode.MULTIPLE;
            } else {
                cacheMode = CacheMode.SINGLE;
            }

            ta.recycle();
        }
    }

    private Align getAlignVertical(int alignInt) {
        if (alignInt == Align.TOP.value) {
            return Align.TOP;
        } else if (alignInt == Align.BOTTOM.value) {
            return Align.BOTTOM;
        } else {
            return Align.CENTER;
        }
    }

    private Align getAlignHorizontal(int alignInt) {
        if (alignInt == Align.LEFT.value) {
            return Align.LEFT;
        } else if (alignInt == Align.RIGHT.value) {
            return Align.RIGHT;
        } else {
            return Align.CENTER;
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
        rectBackground.set(rectWave);

        if (waveLineMaxHeight == 0f) {
            waveLineMaxHeight = rectWave.height() - paintWave.getStrokeWidth(); //Phải trừ đi stroke width vì khi có stroke width thì đường vẽ bị to ra 1 nửa strokeWidth mỗi bên, trên, dưới
        } else {
            waveLineMaxHeight -= paintWave.getStrokeWidth();
        }
        if (centerProgressHeight == -1f) {
            centerProgressHeight = rectWave.height();
        }
        rectThumbProgress.top = rectWave.centerY() - centerProgressHeight / 2f + paintCenterProgress.getStrokeWidth() / 2f;
        rectThumbProgress.bottom = rectThumbProgress.top + centerProgressHeight - paintCenterProgress.getStrokeWidth() / 2f;
        validateThumbProgressWithProgress();
        calculateCurrentWidthView();
        configureEditThumb();
        validateEditThumbByProgress();
        configureAnchorImageVertical();
        configureAnchorImageHorizontal();
        configureOverlay();
        validateOverlayByProgress();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    /**
     * Tính toán vị trí ban đầu của thumb Edit (top, bottom)
     */
    private void configureEditThumb() {
        if (editThumbHeight == -1) {
            editThumbHeight = rectWave.height();
        }
        if (editThumbAlign == Align.TOP) {
            rectThumbLeft.top = rectWave.top;
            rectThumbLeft.bottom = (rectThumbLeft.top + editThumbHeight);
        } else if (editThumbAlign == Align.BOTTOM) {
            rectThumbLeft.bottom = rectWave.bottom;
            rectThumbLeft.top = (rectThumbLeft.bottom - editThumbHeight);
        } else {
            rectThumbLeft.top = (rectWave.centerY() - editThumbHeight / 2f);
            rectThumbLeft.bottom = (rectWave.centerY() + editThumbHeight / 2f);
        }
        rectThumbRight.top = rectThumbLeft.top;
        rectThumbRight.bottom = rectThumbLeft.bottom;
    }

    /**
     * Tính toán vị trí ban đầu của anchor image (top,bottom)
     */
    private void configureAnchorImageVertical() {
        configureAnchorImageVertical(rectAnchorLeft, rectThumbLeft, leftAnchorAlignVertical);
        configureAnchorImageVertical(rectAnchorRight, rectThumbRight, rightAnchorAlignVertical);
    }

    private void configureAnchorImageVertical(Rect anchorRect, RectF rectThumb, Align align) {
        if (align == Align.TOP) {
            anchorRect.top = (int) rectThumb.top;
        } else if (align == Align.BOTTOM) {
            anchorRect.top = (int) (rectThumb.bottom - anchorImageHeight);
        } else { //Center
            anchorRect.top = (int) (rectThumb.centerY() - anchorImageHeight / 2f);
        }
        anchorRect.bottom = (int) (anchorRect.top + anchorImageHeight);
    }

    /**
     * Tính toán vị trí của anchor image (left,right)
     */

    private void configureAnchorImageHorizontal() {
        configureAnchorImageHorizontal(rectAnchorLeft, rectThumbLeft, leftAnchorAlignHorizontal);
        configureAnchorImageHorizontal(rectAnchorRight, rectThumbRight, rightAnchorAlignHorizontal);
    }

    private void configureAnchorImageHorizontal(Rect anchorRect, RectF rectThumb, Align align) {
        if (align == Align.LEFT) {
            anchorRect.left = (int) (rectThumb.left - anchorImageWidth);
        } else if (align == Align.RIGHT) {
            anchorRect.left = (int) (rectThumb.right);
        } else { //Center
            anchorRect.left = (int) (rectThumb.centerX() - anchorImageWidth / 2f);
        }
        anchorRect.right = (int) (anchorRect.left + anchorImageWidth);
    }

    private void validateEditThumbByProgress() {
        validateThumbLeftWithProgress();
        validateThumbRightWithProgress();
    }

    private float getThumbProgressStaticPosition() {
        float progressPosition = thumbProgressStaticPosition;
        if (thumbProgressStaticPosition == ProgressPosition.LEFT.value) {
            progressPosition = 0f;
        } else if (thumbProgressStaticPosition == ProgressPosition.CENTER.value) {
            progressPosition = rectView.width() / 2f;
        }
        return progressPosition;
    }

    private void validateThumbProgressWithProgress() {
        if (thumbProgressMode == ProgressMode.STATIC) {
            if (thumbProgressStaticPosition == ProgressPosition.LEFT.value) {
                rectThumbProgress.left = rectView.left - paintCenterProgress.getStrokeWidth() / 2f + getScrollX();
            } else if (thumbProgressStaticPosition == ProgressPosition.CENTER.value) {
                rectThumbProgress.left = rectView.centerX() - paintCenterProgress.getStrokeWidth() / 2f + getScrollX();
            } else {
                rectThumbProgress.left = thumbProgressStaticPosition + getScrollX();
            }
        } else {
            rectThumbProgress.left = convertProgressToPosition(progress);
        }
    }

    private void validateThumbLeftWithProgress() {
        validateEditThumbByProgress(rectThumbLeft, leftProgress);
    }

    private void validateThumbRightWithProgress() {
        validateEditThumbByProgress(rectThumbRight, rightProgress);
    }

    private void validateEditThumbByProgress(RectF thumbRect, Float progress) {
        thumbRect.left = (int) (convertProgressToPosition(progress) - editThumbWidth / 2f);
        thumbRect.right = (int) (thumbRect.left + editThumbWidth);
    }

    private void loadTextTimelineSizeDefault(String defaultTimeLine) {
        paintTimeLine.getTextBounds(defaultTimeLine, 0, defaultTimeLine.length(), rectTimeLine);
        textTimeLineDefaultWidth = rectTimeLine.width();
    }

    /**
     * Tính toán ví trí lớp mờ (overlay)
     */

    private void configureOverlay() {
        rectOverlayCenter.top = rectWave.top;
        rectOverlayCenter.bottom = rectWave.bottom;
        rectOverlayLeft.top = rectOverlayCenter.top;
        rectOverlayLeft.bottom = rectOverlayCenter.bottom;
        rectOverlayRight.top = rectOverlayCenter.top;
        rectOverlayRight.bottom = rectOverlayCenter.bottom;
    }

    private void validateOverlayByProgress() {
        rectOverlayLeft.left = rectWave.left;
        rectOverlayLeft.right = rectThumbLeft.left;

        rectOverlayRight.left = rectThumbRight.right;
        rectOverlayRight.right = rectWave.right;
        if (rectOverlayRight.right < rectView.right) {
            rectOverlayRight.right = rectView.right;
        }

        rectOverlayCenter.left = rectThumbLeft.right;
        rectOverlayCenter.right = rectThumbRight.left;
    }


    /**
     * Sound File
     */
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

    public void cancelLoading() {
        isCancel = true;
    }

    public void setAudioPath(final String path) {
        Exception exceptionError = null;
        minWaveZoomLevel = defaultMinWaveZoomLevel;
        try {
            if (cacheMode == CacheMode.NONE || mapCache.get(path) == null) {
                mSoundFile = SoundFile.create(path, new SoundFile.ProgressListener() {
                    @Override
                    public boolean reportProgress(double fractionComplete) {
                        if (audioListener != null) {
                            audioListener.onLoadingAudio((int) (fractionComplete * 100), false);
                        }
                        eLog("Progress: ", (int) (fractionComplete * 100));
                        return !isCancel;
                    }
                });
                if (audioListener != null && !isCancel) {
                    audioListener.onLoadingAudio(100, true);
                }
                if (cacheMode == CacheMode.SINGLE) {
                    mapCache.clear();
                }
                mapCache.put(path, mSoundFile);
            } else {
                mSoundFile = mapCache.get(path);
            }
            if (isCancel)
                return;
            loadViewWithCurrentSoundFile();
        } catch (IOException e) {
            exceptionError = e;
            eLog("Loi doc ghi voi file: ", path);
        } catch (SoundFile.InvalidInputException e) {
            exceptionError = e;
            eLog("Loi doc ghi voi file: ", path);
        } catch (AudioWaveViewException e) {
            exceptionError = e;
            eLog("Audio Path is not exist or file is invalid. Path: " + path);
        }
        if (exceptionError != null) {
            if (audioListener != null) {
                audioListener.onLoadingAudioError(exceptionError);
            }
        }
        isCancel = false;
    }

    private void loadViewWithCurrentSoundFile() {
        mSampleRate = mSoundFile.getSampleRate();
        mSamplesPerFrame = mSoundFile.getSamplesPerFrame();
        duration = mSoundFile.getDuration();
        progress = 0f;
        if (modeEdit == ModeEdit.CUT_OUT) {
            leftProgress = duration / 2f - minRangeProgress / 2f;
            rightProgress = duration / 2f + minRangeProgress / 2f;
        } else {
            leftProgress = 0f;
            rightProgress = duration;
        }
        computeDoublesForAllZoomLevels();
        computeIntsForThisZoomLevel();
        if (isAutoAdjustZoomLevel) {
            int waveSize = mHeightsAtThisZoomLevel.length;
            float widthByWave = waveSize * waveLineWidth + (waveSize - 1) * waveLinePadding;
            float zoomLevel = widthByWave / rectView.width();
            if (widthByWave > 1f) {
                if (minWaveZoomLevel > 1f / zoomLevel)
                    minWaveZoomLevel = 1f / zoomLevel;
                AudioWaveView.this.waveZoomLevel = 1f / zoomLevel;
            }
        }
        calculateCurrentWidthView();
        validateEditThumbByProgress();
        if (thumbProgressMode == ProgressMode.STATIC && isSetThumbProgressToZeroAfterInit) {
            setCenterProgress(0f, ProgressAdjustMode.NONE);
        }
        if (audioListener != null) {
            audioListener.onLoadingAudioComplete();
        }
        postInvalidate();
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
            if (isShowRandomPreview && !listPreviewWave.isEmpty()) {
                waveViewCurrentWidth = (listPreviewWave.size() * waveLineWidth + (listPreviewWave.size() - 1) * waveLinePadding) * waveZoomLevel;
            } else {
                waveViewCurrentWidth = getWidth();
            }
        } else {
            waveViewCurrentWidth = (mHeightsAtThisZoomLevel.length * waveLineWidth + (mHeightsAtThisZoomLevel.length - 1) * waveLinePadding) * waveZoomLevel;
        }
        rectWave.right = rectView.left + waveViewCurrentWidth;
        rectBackground.right = rectView.right;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.drawRect(rectBackground, paintBackground);
        canvas.drawRect(rectWave, paintBackground);

        if (hasSoundFile() && mHeightsAtThisZoomLevel != null && mHeightsAtThisZoomLevel.length > 0) {
            float centerY = rectWave.centerY();
            float offsetIncreaseValue = (waveLineWidth + waveLinePadding) * waveZoomLevel;
            int startIndex = (int) (getScrollX() / offsetIncreaseValue);
            if (startIndex < 0)
                startIndex = 0;
            float offset = 0f + (paintWave.getStrokeWidth() / 2f) * waveZoomLevel + startIndex * offsetIncreaseValue;
            for (int i = startIndex; i < mHeightsAtThisZoomLevel.length; i++) {
                int value = mHeightsAtThisZoomLevel[i];
                canvas.drawLine(offset, centerY - value, offset, centerY + value, paintWave);
                offset += offsetIncreaseValue;
                if (offset > getScrollX() + rectView.width()) {
                    break;
                }
            }
        } else if (isShowRandomPreview) {
            drawRandomPreview(canvas);
        }
        drawOverlay(canvas);
        drawTimeLineAndIndicator(canvas);
        if (isThumbProgressVisible)
            drawCenterProgress(canvas);
        if (isThumbEditVisible && (modeEdit == ModeEdit.TRIM || modeEdit == ModeEdit.CUT_OUT)) {
            drawThumbCut(canvas);
            configureAnchorImageHorizontal();
            if (isThumbEditCircleVisible)
                drawThumbEditCircle(canvas);
            drawAnchorImage(canvas);
            drawTextValue(canvas);
        }
        if (isFlinging) {
            if (scroller.computeScrollOffset()) {
                scrollTo(scroller.getCurrX(), getScrollY());
                if (thumbProgressMode == ProgressMode.STATIC) {
                    float audioProgress = getCurrentStaticProgress();
                    interactedListener.onProgressThumbChanging(audioProgress, ProgressAdjustMode.ADJUST_BY_MOVING);
                }
            }
            float flingDistanceLess = Math.abs(scroller.getFinalX() - scroller.getCurrX());
//            eLog("Fling: ", isFlinging, " ---- Fling offset: ", scroller.computeScrollOffset(), "----- Finish: ", scroller.isFinished(), "----- CurrX: ", scroller.getCurrX());
//            eLog("CurrX: ", scroller.getCurrX(), " ----- FinalX: ", scroller.getFinalX(), " ---- Lest: ", scrollDistanceLess);
//            eLog("========================================");
            if (scroller.isFinished() || !scroller.computeScrollOffset() || flingDistanceLess <= 10) {
                isFlinging = false;
                isSendFlingEndEvent = true;
                scroller.forceFinished(true);
                if (interactedListener != null) {
                    if (thumbProgressMode == ProgressMode.STATIC) {
                        progress = getCurrentStaticProgress();
                        interactedListener.onProgressThumbChanging(progress, ProgressAdjustMode.ADJUST_BY_MOVING);
                    }
                    interactedListener.onStopFling(false);
                    invalidate();
                }
            }
        } else if (!isSendFlingEndEvent) {
            isSendFlingEndEvent = true;
            isFlinging = false;
            scroller.forceFinished(true);
            if (interactedListener != null) {
                if (thumbProgressMode == ProgressMode.STATIC) {
                    progress = getCurrentStaticProgress();
                    interactedListener.onProgressThumbChanging(progress, ProgressAdjustMode.ADJUST_BY_MOVING);
                }
                interactedListener.onStopFling(false);
            }
            invalidate();
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
        if (thumbProgressMode == ProgressMode.STATIC) {
            validateThumbProgressWithProgress();
        }
        canvas.drawLine(rectThumbProgress.left, rectThumbProgress.top, rectThumbProgress.left, rectThumbProgress.bottom, paintCenterProgress);
    }

    private void drawThumbCut(Canvas canvas) {
        canvas.drawRect(rectThumbLeft, paintEditThumb);
        canvas.drawRect(rectThumbRight, paintEditThumb);
    }

    /**
     * Draw thumb edit circle
     */

    private void drawThumbEditCircle(Canvas canvas) {
        float leftOfLeftCircle = rectThumbLeft.centerX() - thumbEditCircleSize / 2f;
        float rightOfLeftCircle = rectThumbLeft.centerX() + thumbEditCircleSize / 2f;
        float leftOfRightCircle = rectThumbRight.centerX() - thumbEditCircleSize / 2f;
        float rightOfRightCircle = rectThumbRight.centerX() + thumbEditCircleSize / 2f;
        float topOfTopCircle = rectThumbLeft.top - thumbEditCircleSize / 2f;
        float bottomOfTopCircle = rectThumbLeft.top + thumbEditCircleSize / 2f;
        float topOfBottomCircle = rectThumbLeft.bottom - thumbEditCircleSize / 2f;
        float bottomOfBottomCircle = rectThumbLeft.bottom + thumbEditCircleSize / 2f;

        canvas.drawOval(new RectF(leftOfLeftCircle, topOfTopCircle, rightOfLeftCircle, bottomOfTopCircle), paintEditThumb);
        canvas.drawOval(new RectF(leftOfLeftCircle, topOfBottomCircle, rightOfLeftCircle, bottomOfBottomCircle), paintEditThumb);
        canvas.drawOval(new RectF(leftOfRightCircle, topOfTopCircle, rightOfRightCircle, bottomOfTopCircle), paintEditThumb);
        canvas.drawOval(new RectF(leftOfRightCircle, topOfBottomCircle, rightOfRightCircle, bottomOfBottomCircle), paintEditThumb);

    }

    /**
     * Draw anchor image
     */

    private void drawAnchorImage(Canvas canvas) {
        if (leftAnchorImage != null) {
            leftAnchorImage.setBounds(rectAnchorLeft);
            leftAnchorImage.draw(canvas);
        }
        if (rightAnchorImage != null) {
            rightAnchorImage.setBounds(rectAnchorRight);
            rightAnchorImage.draw(canvas);
        }
    }

    /**
     * Draw overlay
     */
    private void drawOverlay(Canvas canvas) {
        if (modeEdit == ModeEdit.TRIM && isThumbEditVisible) {
            validateOverlayByProgress();
            canvas.drawRect(rectOverlayLeft, paintOverlayRemove);
            canvas.drawRect(rectOverlayRight, paintOverlayRemove);
            canvas.drawRect(rectOverlayCenter, paintOverlayPick);
        } else if (modeEdit == ModeEdit.CUT_OUT && isThumbEditVisible) {
            validateOverlayByProgress();
            canvas.drawRect(rectOverlayCenter, paintOverlayRemove);
            canvas.drawRect(rectOverlayLeft, paintOverlayPick);
            canvas.drawRect(rectOverlayRight, paintOverlayPick);
        }
    }

    /**
     * draw Text Value At Thumb
     */
    private float yText = 0f;

    private void drawTextValue(Canvas canvas) {
        if (textValuePosition != TextValuePosition.NONE) {
            paintTextValue.getTextBounds("0", 0, 1, rectTextValue);
            if (textValuePosition == TextValuePosition.BOTTOM_OF_ANCHOR) {
                yText = rectAnchorLeft.bottom + textValuePadding + rectTextValue.height();
            } else {
                yText = rectAnchorLeft.top - textValuePadding - rectTextValue.height();
            }
            String textLeft = convertTimeToTimeFormat(leftProgress);
            String textRight = convertTimeToTimeFormat(rightProgress);
            float textLeftWidth = paintTextValue.measureText(textLeft);
            float textRightWidth = paintTextValue.measureText(textLeft);
            float maxTextLeftX = rectAnchorRight.centerX() - textRightWidth / 2f - textLeftWidth / 2f;
            float minTextRightX = rectAnchorLeft.centerX() + textRightWidth / 2f + textLeftWidth / 2f;
            if (rectAnchorLeft.centerX() > rectView.left + waveViewCurrentWidth - textRightWidth - textLeftWidth / 2f - minSpaceBetweenText) {
                maxTextLeftX = rectView.left + waveViewCurrentWidth - textRightWidth - textLeftWidth / 2f - minSpaceBetweenText;
                minTextRightX = rectView.left + waveViewCurrentWidth - textRightWidth / 2f;
            } else if (rectAnchorRight.centerX() < rectView.left + textLeftWidth + textRightWidth / 2f + minSpaceBetweenText) {
                minTextRightX = rectView.left + textRightWidth / 2f + textLeftWidth + minSpaceBetweenText;
                maxTextLeftX = rectView.left + textLeftWidth / 2f;
            }
            drawTextValue(canvas, textLeft, rectAnchorLeft, rectView.left + textLeftWidth / 2f, maxTextLeftX);
            drawTextValue(canvas, textRight, rectAnchorRight, minTextRightX, rectView.left + waveViewCurrentWidth - textRightWidth / 2f);
        }
    }

    private void drawTextValue(Canvas canvas, String text, Rect rectAnchor, float minX, float maxX) {
        float xText = rectAnchor.centerX();
        if (xText < minX) {
            xText = minX;
        } else if (xText > maxX)
            xText = maxX;
        canvas.drawText(text, xText, yText, paintTextValue);
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

    public float convertProgressToPosition(float progress) {
        return progress / duration * rectWave.width() + rectWave.left;
    }

    public float convertPositionToProgress(float pixel) {
        return pixel / rectWave.width() * duration;
    }

    public float convertProgressPixelSize(float progress) {
        return progress / duration * rectWave.width();
    }

    //Lấy ra progress hiện tại khi ở chế độ static progress
    private float getCurrentStaticProgress() {
        return 1f * (getScrollX() + getThumbProgressStaticPosition()) / rectWave.width() * duration;
    }

    private float convertStaticPositionToProgress(float pixel) {
        return 1f * (getScrollX() + pixel) / rectWave.width() * duration;
    }

    private String convertTimeToTimeFormat(float time) {
        int t = (int) time / 1000;
        int second = t % 60;
        int minute = t / 60;
        DecimalFormat format = new DecimalFormat("00");
        return format.format(minute) + ":" + format.format(second);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        scaleGestureDetector.onTouchEvent(event);
        if (gestureDetector.onTouchEvent(event)) {
            return true;
        }
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                eLog("Action Down ", "---- Fling: ", isFlinging);
                if (!isSendFlingEndEvent)
                    stopFling();
                if (interactedListener != null) {
                    float touchProgress = convertPositionToProgress(event.getX() + getScrollX());
                    if (thumbProgressMode == ProgressMode.STATIC) {
                        touchProgress = convertStaticPositionToProgress(event.getX());
                    }
                    interactedListener.onTouchDownAudioBar(touchProgress, rectWave.contains(event.getX(), event.getY()));
                }
                pointDown.set(event.getX(), event.getY());
                if ((modeEdit == ModeEdit.CUT_OUT || modeEdit == ModeEdit.TRIM) && isThumbEditVisible) {
                    thumbIndex = getThumbFocus();
                }
                return true;
            case MotionEvent.ACTION_MOVE:
                if (isFlinging)
                    return true;
                eLog("ACtion Move ", "---- Fling: ", isFlinging);
                float disX = event.getX() - pointDown.x;
                float disY = event.getY() - pointDown.y;
                lastDistanceX = disX;
                if (isScrolling) {
                    if (thumbIndex == ThumbIndex.THUMB_NONE) {
                        scroll((int) disX);
                        eLog("Scroll: ", getScrollX());
                        if (interactedListener != null) {
                            progress = getCurrentStaticProgress();
                            interactedListener.onProgressThumbChanging(progress, ProgressAdjustMode.ADJUST_BY_MOVING);
                        }
                    } else {
                        moveThumb(disX);
                        //eLog("Progress: ", getCurrentStaticProgress(), "---- Right: ", rightProgress);
                        invalidate();
                    }
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
                eLog("ActionUp ---- SCroll: ", isScrolling, " --- FLing: ", isFlinging);
                if (isScrolling) {
                    scrollStaticProgressIfNotInRange(lastDistanceX);
                    isScrolling = false;
                    if (isMovingThumb) {
                        isMovingThumb = false;
                        thumbIndex = ThumbIndex.THUMB_NONE;
                    }
                } else {
                    if (interactedListener != null) {
                        float touchProgress = convertPositionToProgress(event.getX() + getScrollX());
                        if (thumbProgressMode == ProgressMode.STATIC) {
                            touchProgress = convertStaticPositionToProgress(event.getX());
                        }
                        interactedListener.onClickAudioBar(touchProgress, rectWave.contains(event.getX(), event.getY()));
                    }
                }
                if (interactedListener != null) {
                    float touchProgress = convertPositionToProgress(event.getX() + getScrollX());
                    if (thumbProgressMode == ProgressMode.STATIC) {
                        touchProgress = convertStaticPositionToProgress(event.getX());
                    }
                    interactedListener.onTouchReleaseAudioBar(touchProgress, rectWave.contains(event.getX(), event.getY()));
                }
                break;
            default:
                eLog("Other Action: ", event.getActionMasked());
                break;
        }
        return super.onTouchEvent(event);
    }

    private void scrollStaticProgressIfNotInRange(float vectorX) {
        if (isFixedThumbProgressByThumbEdit && thumbProgressMode == ProgressMode.STATIC) {
            if (modeEdit == ModeEdit.TRIM) {
                if (progress < leftProgress || progress > rightProgress) {
                    if (thumbIndex == ThumbIndex.THUMB_LEFT) {
                        if (progress < leftProgress) {
                            setCenterProgress(leftProgress, ProgressAdjustMode.ADJUST_BY_EDIT_THUMB);
                        }
                    } else if (thumbIndex == ThumbIndex.THUMB_RIGHT) {
                        if (progress > rightProgress) {
                            setCenterProgress(rightProgress, ProgressAdjustMode.ADJUST_BY_EDIT_THUMB);
                        }
                    } else {
                        if (progress > rightProgress)
                            setCenterProgress(rightProgress, ProgressAdjustMode.ADJUST_BY_MOVING);
                        else
                            setCenterProgress(leftProgress, ProgressAdjustMode.ADJUST_BY_MOVING);
                    }
                }
            } else if (modeEdit == ModeEdit.CUT_OUT) { //Mode Cut  out
                if (progress > leftProgress && progress < rightProgress) {
                    if (thumbIndex == ThumbIndex.THUMB_LEFT) {
                        if (progress > leftProgress) {
                            setCenterProgress(leftProgress, ProgressAdjustMode.ADJUST_BY_EDIT_THUMB);
                        }
                    } else if (thumbIndex == ThumbIndex.THUMB_RIGHT) {
                        if (progress < rightProgress) {
                            setCenterProgress(rightProgress, ProgressAdjustMode.ADJUST_BY_EDIT_THUMB);
                        }
                    } else {
                        if (vectorX > 0) {
                            setCenterProgress(leftProgress, ProgressAdjustMode.ADJUST_BY_MOVING);
                        } else {
                            setCenterProgress(rightProgress, ProgressAdjustMode.ADJUST_BY_MOVING);
                        }
                    }
                }
            }
        }
    }

    public void scroll(int disX) {
        float maxLeft = 0f;
        float maxRight = rectView.left + waveViewCurrentWidth - getWidth();
        if (thumbProgressMode == ProgressMode.STATIC) {
            maxLeft = 0f - getThumbProgressStaticPosition();
            maxRight += getThumbProgressStaticPosition();
        }
        if (getScrollX() - disX < maxLeft || (waveViewCurrentWidth <= getWidth() && thumbProgressMode != ProgressMode.STATIC)) {
            scrollTo((int) maxLeft, 0);
        } else if (getScrollX() - disX > maxRight) {
            scrollTo((int) (maxRight), 0);
        } else
            scrollBy(-disX, 0);

    }

    private void moveThumb(float distance) {
        int disMove = (int) distance;
        isMovingThumb = true;
        lastFocusThumbIndex = thumbIndex;
        float minCutProgress = 0f;
        if (duration > this.minRangeProgress) {
            minCutProgress = this.minRangeProgress;
        }
        float minBetween = 0f;
        if (duration > minCutProgress) {
            minBetween = convertProgressPixelSize(minCutProgress);
        }
        RectF thumbRect;
        if (thumbIndex == ThumbIndex.THUMB_LEFT) {
            thumbRect = rectThumbLeft;
            float minLeft = rectWave.left - editThumbWidth / 2f;
            float maxLeftOfThumbRight = rectWave.right - editThumbWidth / 2f;
            if (modeEdit == ModeEdit.CUT_OUT && (maxLeftOfThumbRight - rectThumbRight.left) < minBetween)
                minLeft = rectWave.left + (minBetween - (maxLeftOfThumbRight - rectThumbRight.left)) - editThumbWidth / 2f;
            float maxLeft = rectThumbRight.left - minBetween;
            if (modeEdit == ModeEdit.CUT_OUT) maxLeft = rectThumbRight.left - editThumbWidth / 2f;
            adjustMove(thumbRect, disMove, minLeft, maxLeft);
            leftProgress = convertPositionToProgress(thumbRect.right);

            if (leftProgress > rightProgress - minCutProgress && modeEdit == ModeEdit.TRIM)
                leftProgress = rightProgress - minCutProgress;
        } else if (thumbIndex == ThumbIndex.THUMB_RIGHT) {
            thumbRect = rectThumbRight;
            float minLeft =
                    rectThumbLeft.right + minBetween - editThumbWidth / 2f;//Bỏ đi giới hạn ở giữa
            if (modeEdit == ModeEdit.CUT_OUT) minLeft = rectThumbLeft.right - editThumbWidth / 2f;
            //(minCutProgress + minProgress).ToDimensionPosition()
            float minLeftOfThumbLeft = rectWave.left - editThumbWidth / 2f;
            float maxLeft = rectWave.right - editThumbWidth / 2f; //=rectView.left
            if (modeEdit == ModeEdit.CUT_OUT && (rectThumbLeft.left - minLeftOfThumbLeft) < minBetween)
                maxLeft = rectWave.right - (minBetween - (rectThumbLeft.left - minLeftOfThumbLeft)) - editThumbWidth / 2f;
            adjustMove(thumbRect, disMove, minLeft, maxLeft);
            rightProgress = convertPositionToProgress(thumbRect.left);
            if (rightProgress < leftProgress + minCutProgress && modeEdit == ModeEdit.TRIM)
                rightProgress = leftProgress + minCutProgress;
        }
        if (leftProgress < 0)
            leftProgress = 0f;
        if (rightProgress > duration)
            rightProgress = duration;
        fixProgressCenterWhenMoveThumb();
        if (interactedListener != null) {
            interactedListener.onRangerChanging(leftProgress, rightProgress, AdjustMode.MOVE);
        }
        invalidate();
    }

    private void adjustMove(RectF thumbRect, int disMove, float minLeft, float maxLeft) {
        if (thumbRect.left + disMove < minLeft) {
            thumbRect.left = minLeft;
        } else if (thumbRect.left + disMove > maxLeft) {
            thumbRect.left = maxLeft;
        } else {
            thumbRect.left += disMove;
        }
        thumbRect.right = thumbRect.left + editThumbWidth;
    }

    private void fixProgressCenterWhenMoveThumb() {
        if (isFixedThumbProgressByThumbEdit && isThumbEditVisible && modeEdit != ModeEdit.NONE) {
            if (modeEdit == ModeEdit.TRIM) {
                if (thumbProgressMode == ProgressMode.FLEXIBLE) {
                    if (progress < leftProgress) {
                        progress = leftProgress;
                    } else if (progress > rightProgress)
                        progress = rightProgress;
                    validateThumbProgressWithProgress();
                } else {

                }
            } else { //Trim
                if (progress > leftProgress && progress < rightProgress) {
                    if (thumbProgressMode == ProgressMode.FLEXIBLE) {
                        progress = rightProgress;
                        validateThumbProgressWithProgress();
                    }
                }
            }

        } else {

        }
    }

    private int getThumbFocus() {
        boolean isFocusThumbLeft = false;
        boolean isFocusThumbRight = false;
        RectF extraTouchAnchorLeft = new RectF(rectAnchorLeft.left - thumbTouchExpandSize, rectAnchorLeft.top - thumbTouchExpandSize, rectAnchorLeft.right + thumbTouchExpandSize, rectAnchorLeft.bottom + thumbTouchExpandSize);
        RectF extraTouchAnchorRight = new RectF(rectAnchorRight.left - thumbTouchExpandSize, rectAnchorRight.top - thumbTouchExpandSize, rectAnchorRight.right + thumbTouchExpandSize, rectAnchorRight.bottom + thumbTouchExpandSize);
        float realDownX = pointDown.x + getScrollX();
        if ((int) realDownX >= rectThumbLeft.left - thumbTouchExpandSize && (int) realDownX <= rectThumbLeft.right + thumbTouchExpandSize
                || extraTouchAnchorLeft.contains(realDownX, pointDown.y)) {
            isFocusThumbLeft = true;
        }
        if ((int) realDownX >= rectThumbRight.left - thumbTouchExpandSize && (int) realDownX <= rectThumbRight.right + thumbTouchExpandSize
                || extraTouchAnchorRight.contains(realDownX, pointDown.y)) {
            isFocusThumbRight = true;
        }
        if (isFocusThumbLeft && isFocusThumbRight) {
            if (Math.abs(realDownX - rectThumbLeft.centerX()) > Math.abs(rectThumbRight.centerX() - realDownX)) {
                return ThumbIndex.THUMB_RIGHT;
            } else
                return ThumbIndex.THUMB_LEFT;
        } else if (isFocusThumbLeft)
            return ThumbIndex.THUMB_LEFT;
        else if (isFocusThumbRight)
            return ThumbIndex.THUMB_RIGHT;
        else
            return ThumbIndex.THUMB_NONE;
    }

    public interface IAudioListener {
        void onLoadingAudio(int progress, boolean prepareView);

        void onLoadingAudioComplete();

        void onLoadingAudioError(Exception exceptionError);
    }

    public interface IInteractedListener {
        void onTouchDownAudioBar(float touchProgress, boolean touchInBar);

        void onClickAudioBar(float touchProgress, boolean touchInBar);

        void onTouchReleaseAudioBar(float touchProgress, boolean touchInBar);

        void onAudioBarScaling();

        void onRangerChanging(float minProgress, float maxProgress, AdjustMode adjustMode);

        void onStopFling(boolean isForcedStop);

        void onStartFling();

        void onProgressThumbChanging(float progress, ProgressAdjustMode progressAdjustMode);
    }

    public static class ThumbIndex {
        public static int THUMB_NONE = -1;
        public static int THUMB_LEFT = 0;
        public static int THUMB_RIGHT = 1;
    }

    public enum AdjustMode {
        NONE, MOVE, SCALE
    }

    public enum ProgressAdjustMode {
        NONE, ADJUST_BY_MOVING, ADJUST_BY_EDIT_THUMB
    }

    public enum ModeEdit {
        NONE(0), CUT_OUT(2), TRIM(1);
        public int mode;

        ModeEdit(int mode) {
            this.mode = mode;
        }
    }

    public enum ProgressMode {
        FLEXIBLE(0), STATIC(1);
        public int mode;

        ProgressMode(int mode) {
            this.mode = mode;
        }
    }

    public enum ProgressPosition {
        LEFT(-10), CENTER(-11);
        public int value;

        ProgressPosition(int value) {
            this.value = value;
        }
    }

    public enum Align {
        TOP(0), CENTER(2), BOTTOM(1), LEFT(3), RIGHT(4);
        public int value;

        Align(int mode) {
            this.value = mode;
        }
    }

    public enum TextValuePosition {
        NONE(-1), TOP_OF_ANCHOR(1), BOTTOM_OF_ANCHOR(0);
        public int position;

        TextValuePosition(int position) {
            this.position = position;
        }
    }

    public enum CacheMode {
        NONE(0), SINGLE(1), MULTIPLE(2);
        public int value;

        CacheMode(int value) {
            this.value = value;
        }
    }

    public static void eLog(Object... message) {
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
        setCenterProgress(progress);
        postInvalidate();
    }

    public void setProgress(float progress, boolean scrollToShowCenterProgress) {
        setCenterProgress(progress);
        if (thumbProgressMode == ProgressMode.FLEXIBLE) {
            if (scrollToShowCenterProgress && waveViewCurrentWidth > rectView.width()) {
                float freezeScrollPosition = rectView.left + rectView.width() / 2f;
                if (rectThumbProgress.left > rectView.left + rectView.width() / 2f) {
                    if (rectThumbProgress.left > rectView.left + waveViewCurrentWidth - rectView.width()) {
                        scrollTo((int) (rectView.left + waveViewCurrentWidth - rectView.width()), getScrollY());
                    } else
                        scrollTo((int) (rectThumbProgress.left - freezeScrollPosition), getScrollY());
                } else {
                    scrollTo(0, getScrollY());
                }
            }
        } else {
            float position = convertProgressToPosition(this.progress);
            scrollTo((int) (position - getThumbProgressStaticPosition()), getScrollY());
            if (interactedListener != null) {
                interactedListener.onProgressThumbChanging(getCurrentStaticProgress(), ProgressAdjustMode.NONE);
            }
        }
        postInvalidate();
    }

    private void setCenterProgress(float progress) {
        setCenterProgress(progress, ProgressAdjustMode.NONE);
    }

    private void setCenterProgress(float progress, ProgressAdjustMode progressAdjustMode) {
        this.progress = validateProgress(progress, 0f, duration);
        validateThumbProgressWithProgress();
        if (thumbProgressMode == ProgressMode.STATIC) {
            float position = convertProgressToPosition(this.progress);
            scrollTo((int) (position - getThumbProgressStaticPosition()), getScrollY());
            if (interactedListener != null) {
                interactedListener.onProgressThumbChanging(getCurrentStaticProgress(), progressAdjustMode);
            }
        }
    }

    public void setMinProgress(float progress) {
        leftProgress = validateProgress(progress, 0f, duration);
        validateEditThumbByProgress();
        postInvalidate();
    }

    public void setMaxProgress(float progress) {
        rightProgress = validateProgress(progress, 0f, duration);
        validateEditThumbByProgress();
        postInvalidate();
    }

    public boolean isCancel() {
        return isCancel;
    }

    private float validateProgress(float progress, float minProgress, float maxProgress) {
        if (progress < minProgress)
            return minProgress;
        else if (progress > maxProgress)
            return maxProgress;
        return progress;
    }

    public void setRangeProgress(float minProgress, float maxProgress) {
        if (duration < 0)
            return;
        float min = minProgress;
        float max = maxProgress;
        if (min > duration || min < 0) {
            min = 0f;
        }
        if (max > duration || max < min) {
            max = duration;
        }
        if (isFixedThumbProgressByThumbEdit) {
            if (modeEdit == ModeEdit.TRIM) {
                if (!(progress > min && progress < max)) {
                    progress = min;
                }
            } else if (modeEdit == ModeEdit.CUT_OUT) {
                if (progress > min && progress < max) {
                    progress = 0f;
                }
            }
            validateThumbProgressWithProgress();
        }

        leftProgress = min;
        rightProgress = max;
        validateEditThumbByProgress();
        if (interactedListener != null) {
            interactedListener.onRangerChanging(leftProgress, rightProgress, AdjustMode.NONE);
        }
        postInvalidate();
    }

    public void setModeEdit(ModeEdit modeEdit) {
        this.modeEdit = modeEdit;
        fixProgressCenterWhenMoveThumb();
        postInvalidate();
    }

    public ModeEdit getModeEdit() {
        return modeEdit;
    }

    public ProgressMode getThumbProgressMode() {
        return thumbProgressMode;
    }

    public void setThumbProgressMode(ProgressMode thumbProgressMode) {
        this.thumbProgressMode = thumbProgressMode;
        setProgress(0f, true);
    }

    public float getDuration() {
        return duration;
    }

    public float getProgress() {
        return progress;
    }

    public float getMinProgress() {
        return leftProgress;
    }

    public float getMaxProgress() {
        return rightProgress;
    }

    public float getMinRangeProgress() {
        return minRangeProgress;
    }

    public boolean isFlinging() {
        return isFlinging;
    }

    public int getThumbIndex() {
        return thumbIndex;
    }

    public void clearCache() {
        mapCache.clear();
    }

    public void setTextValuePullTogether(boolean textValuePullTogether) {
        isTextValuePullTogether = textValuePullTogether;
        postInvalidate();
    }

    public void stopFling() {
        if (isFlinging) {
            isFlinging = false;
            isSendFlingEndEvent = true;
            if (!scroller.isFinished()) {
                scroller.forceFinished(true);
            }
            if (interactedListener != null) {
                if (thumbProgressMode == ProgressMode.STATIC) {
                    progress = getCurrentStaticProgress();
                    interactedListener.onProgressThumbChanging(progress, ProgressAdjustMode.ADJUST_BY_MOVING);
                }
                interactedListener.onStopFling(true);
            }
            invalidate();
        }
    }

    public void setLeftAnchorAlignImage(Drawable drawable, Align anchorAlignHorizontal, Align anchorAlignVertical) {
        this.leftAnchorImage = drawable;
        leftAnchorAlignHorizontal = anchorAlignHorizontal;
        leftAnchorAlignVertical = anchorAlignVertical;
        configureAnchorImageHorizontal(rectAnchorLeft, rectThumbLeft, leftAnchorAlignHorizontal);
        configureAnchorImageVertical(rectAnchorLeft, rectThumbLeft, leftAnchorAlignVertical);
        postInvalidate();
    }

    public void setRightAnchorAlignImage(Drawable drawable, Align anchorAlignHorizontal, Align anchorAlignVertical) {
        this.rightAnchorImage = drawable;
        rightAnchorAlignHorizontal = anchorAlignHorizontal;
        rightAnchorAlignVertical = anchorAlignVertical;
        configureAnchorImageHorizontal(rectAnchorRight, rectThumbRight, rightAnchorAlignHorizontal);
        configureAnchorImageVertical(rectAnchorRight, rectThumbRight, rightAnchorAlignVertical);
        postInvalidate();
    }

}
