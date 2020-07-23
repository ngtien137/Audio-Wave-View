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

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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

    private Paint paintDefault = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintOverlay = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintBackground = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintWave = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint paintTimeLine = new Paint(Paint.ANTI_ALIAS_FLAG);

    private float waveLinePadding = 0f;
    private float waveLineMaxHeight = 0f;
    private float waveLineWidth = 0f;
    private float audioBarHeight = 0f;

    private PointF pointDown = new PointF();
    private boolean isScrolling = false;

    private float waveViewCurrentWidth;
    private int touchSlop;
    private boolean isShowRandomPreview = true;
    private float waveZoomLevel = 1f;
    private final float maxWaveZoomLevel = 5f;
    private final float minWaveZoomLevel = 0.5f;
    private int textTimeLineDefaultWidth = 0;
    private float textTimeLinePadding = 0;


    private ScaleGestureDetector scaleGestureDetector;
    private IAudioListener audioListener;
    private IInteractedListener interactedListener;

    private float duration = 0f;
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

            int overlayColor = ta.getColor(R.styleable.AudioWaveView_awv_background_color, Color.parseColor("#40000000"));
            paintOverlay.setColor(overlayColor);

            paintWave.setColor(ta.getColor(R.styleable.AudioWaveView_awv_wave_color, Color.BLACK));
            waveLineWidth = ta.getDimension(R.styleable.AudioWaveView_awv_wave_line_size, dpToPixel(2));
            adjustWaveByZoomLevel();
            paintWave.setStrokeCap(Paint.Cap.ROUND);
            waveLinePadding = ta.getDimension(R.styleable.AudioWaveView_awv_wave_line_padding, waveLineWidth / 10f);
            waveLineMaxHeight = ta.getDimension(R.styleable.AudioWaveView_awv_wave_line_max_height, 0f);

            textTimeLinePadding = ta.getDimension(R.styleable.AudioWaveView_awv_text_timeline_padding_with_bar, 0f);
            paintTimeLine.setColor(ta.getColor(R.styleable.AudioWaveView_awv_text_timeline_color, Color.BLACK));
            paintTimeLine.setTextSize(ta.getDimension(R.styleable.AudioWaveView_awv_text_timeline_size, dpToPixel(9)));
            paintTimeLine.setTextAlign(Paint.Align.CENTER);

            modeEdit = ModeEdit.NONE;
            int modeInt = ta.getInt(R.styleable.AudioWaveView_awv_mode_edit, ModeEdit.NONE.mode);
            if (modeInt == ModeEdit.CUT.mode) {
                modeEdit = ModeEdit.CUT;
            } else if (modeInt == ModeEdit.TRIM.mode) {
                modeEdit = ModeEdit.TRIM;
            }

            int fontId = ta.getResourceId(R.styleable.AudioWaveView_awv_text_timeline_font, -1);
            if (fontId != -1) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    paintTimeLine.setTypeface(getResources().getFont(fontId));
                } else
                    paintTimeLine.setTypeface(ResourcesCompat.getFont(context, fontId));
            }

            isShowRandomPreview = ta.getBoolean(R.styleable.AudioWaveView_awv_show_random_preview, true);
            ta.recycle();
        }
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
        String defaultTimeLine = "00:00";
        paintTimeLine.getTextBounds(defaultTimeLine, 0, defaultTimeLine.length(), rectTimeLine);
        textTimeLineDefaultWidth = rectTimeLine.width();
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

        calculateCurrentWidthView();
        super.onSizeChanged(w, h, oldw, oldh);
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
            leftProgress = 0f;
            rightProgress = duration;
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
            float centerY = rectWave.top + rectWave.height() / 2f;
            float offset = 0f + (paintWave.getStrokeWidth() / 2f) * waveZoomLevel;
            for (int value : mHeightsAtThisZoomLevel) {
                canvas.drawLine(offset, centerY - value, offset, centerY + value, paintWave);
                offset += (waveLineWidth + waveLinePadding) * waveZoomLevel;
            }
        } else if (isShowRandomPreview) {
            drawRandomPreview(canvas);
        }

        drawText(canvas);
    }

    private void drawText(Canvas canvas) {
        float yText = rectWave.top + textTimeLinePadding;
        float spaceBetweenTwoTimeLine = textTimeLineDefaultWidth * 3f / 2;
        eLog("sPACE: ",spaceBetweenTwoTimeLine);
        int countText = (int) (waveViewCurrentWidth / spaceBetweenTwoTimeLine);
        canvas.drawText("00:00", textTimeLineDefaultWidth / 2f, yText, paintTimeLine);
        if (duration>0) {
            float offset = spaceBetweenTwoTimeLine / 2f;
            for (int i = 1; i < countText; i++) {
                float progressTime = convertPositionToProgress(offset);
                String sTime = convertTimeToTimeFormat(progressTime);
                canvas.drawText(sTime, textTimeLineDefaultWidth / 2f, yText, paintTimeLine);
                offset += spaceBetweenTwoTimeLine / 2f;
            }
        }
    }

    private float convertPositionToProgress(float pixel) {
        return pixel / rectWave.width() * duration;
    }

    private String convertTimeToTimeFormat(float time) {
        int t = (int) time;
        int second = t % 60;
        int minute = t / 60;
        DecimalFormat format = new DecimalFormat("00");
        return format.format(minute) + ":" + format.format(second);
    }

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
}
