package snow.player.ui.widget;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DimenRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import snow.player.ui.R;
import snow.player.ui.equalizer.EqualizerViewModel;
import snow.player.ui.util.Preconditions;

public class BandChartView extends View {
    private EqualizerViewModel mEqualizerViewModel;
    private int mMinBandLevel;
    private int mBandRange;
    private short[] mAllBandLevel;

    private boolean mInitialized;

    private Paint mPaint;
    private Path mLinePath;
    private CornerPathEffect mCornerPathEffect;
    private DashPathEffect mDashPathEffect;

    private Rect mContentRect;
    private Rect mHintTextRect;
    private String mHintText;

    private int lineColor;
    private int lineDisableColor;
    private int lineWidth;
    private int hintTextSize;
    private int hintTextColor;
    private int gridLineWidth;
    private int gridLineColor;

    private int mBandSpace;

    public BandChartView(Context context) {
        this(context, null);
    }

    public BandChartView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BandChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initView(context, attrs);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public BandChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initView(context, attrs);
    }

    private void initView(Context context, @Nullable AttributeSet attrs) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            // 因为 DashPathEffect 在低于 API 28 的版本中不支持硬件加速，因此需要关闭硬件加速才能生效
            setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        mLinePath = new Path();

        mContentRect = new Rect();
        mHintTextRect = new Rect();

        mHintText = getContext().getString(R.string.snow_ui_band_chart_hint);

        Resources res = context.getResources();
        lineColor = getThemeColor(context.getTheme(), R.attr.colorAccent, Color.parseColor("#FF7043"));
        lineDisableColor = Color.parseColor("#E0E0E0");
        lineWidth = res.getDimensionPixelOffset(R.dimen.snow_ui_band_chart_default_line_width);
        hintTextSize = res.getDimensionPixelOffset(R.dimen.snow_ui_band_chart_default_hint_text_size);
        hintTextColor = Color.parseColor("#FF7043");
        gridLineWidth = res.getDimensionPixelOffset(R.dimen.snow_ui_band_chart_default_grid_line_width);
        gridLineColor = Color.parseColor("#E0E0E0");

        int dashLength = res.getDimensionPixelOffset(R.dimen.snow_ui_band_chart_dash_length);
        mDashPathEffect = new DashPathEffect(new float[]{dashLength, dashLength}, 0);

        getAttrsValue(context, attrs);
    }

    private void getAttrsValue(Context context, @Nullable AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.BandChartView);

        lineColor = typedArray.getColor(R.styleable.BandChartView_lineColor, lineColor);
        lineDisableColor = typedArray.getColor(R.styleable.BandChartView_lineDisableColor, lineDisableColor);
        lineWidth = typedArray.getDimensionPixelOffset(R.styleable.BandChartView_lineWidth, lineWidth);
        hintTextSize = typedArray.getDimensionPixelOffset(R.styleable.BandChartView_hintTextSize, hintTextSize);
        hintTextColor = typedArray.getColor(R.styleable.BandChartView_hintTextColor, hintTextColor);
        gridLineWidth = typedArray.getDimensionPixelOffset(R.styleable.BandChartView_gridLineWidth, gridLineWidth);
        gridLineColor = typedArray.getColor(R.styleable.BandChartView_gridLineColor, gridLineColor);

        typedArray.recycle();
    }

    private int getThemeColor(Resources.Theme theme, @AttrRes int attrId, int defColor) {
        TypedValue outValue = new TypedValue();
        theme.resolveAttribute(attrId, outValue, true);
        return outValue.data != 0 ? outValue.data : defColor;
    }

    public void init(@NonNull EqualizerViewModel viewModel) {
        Preconditions.checkNotNull(viewModel);

        if (mInitialized) {
            return;
        }

        mInitialized = true;
        mEqualizerViewModel = viewModel;

        short[] bandLevelRange = mEqualizerViewModel.getEqualizerBandLevelRange();

        mMinBandLevel = bandLevelRange[0];
        mBandRange = bandLevelRange[1] - mMinBandLevel;

        mAllBandLevel = new short[mEqualizerViewModel.getEqualizerNumberOfBands()];
        updateAllBandLevelData();
        invalidate();
    }

    private void updateAllBandLevelData() {
        int numberOfBands = mEqualizerViewModel.getEqualizerNumberOfBands();

        for (int band = 0; band < numberOfBands; band++) {
            mAllBandLevel[band] = mEqualizerViewModel.getEqualizerBandLevel((short) band);
        }
    }

    public void notifyEqualizerSettingChanged() {
        if (!mInitialized) {
            return;
        }

        updateAllBandLevelData();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        updateContentRect();

        if (!mInitialized) {
            drawHintText(canvas);
            return;
        }

        drawBackgroundGrid(canvas);
        drawBandLine(canvas);
    }

    private void updateContentRect() {
        mContentRect.top = getPaddingTop();
        mContentRect.left = getPaddingLeft();
        mContentRect.right = getWidth() - getPaddingRight();
        mContentRect.bottom = getHeight() - getPaddingBottom();
    }

    private void drawHintText(Canvas canvas) {
        mPaint.setTextSize(hintTextSize);
        mPaint.getTextBounds(mHintText, 0, mHintText.length(), mHintTextRect);

        Gravity.apply(Gravity.CENTER, mHintTextRect.width(), mHintTextRect.height(), mContentRect, mHintTextRect);

        mPaint.setColor(hintTextColor);
        mPaint.setStyle(Paint.Style.FILL);
        canvas.drawText(mHintText, mHintTextRect.left, mHintTextRect.top, mPaint);
    }

    private void drawBackgroundGrid(Canvas canvas) {
        int halfGridLineWith = gridLineWidth / 2;

        // 绘制外框
        mPaint.setPathEffect(null);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(gridLineWidth);
        mPaint.setColor(gridLineColor);
        canvas.drawRect(halfGridLineWith, halfGridLineWith, getWidth() - halfGridLineWith, getHeight() - halfGridLineWith, mPaint);

        // 绘制垂直线条
        int space = mContentRect.width() / (mAllBandLevel.length - 1);
        int lineCount = mAllBandLevel.length - 2;
        for (int i = 1; i <= lineCount; i++) {
            canvas.drawLine(space * i, 0, space * i, getHeight(), mPaint);
        }

        // 绘制中央水平线条
        int centerY = getHeight() / 2;
        mPaint.setPathEffect(mDashPathEffect);
        canvas.drawLine(0, centerY, getWidth(), centerY, mPaint);
    }

    private void drawBandLine(Canvas canvas) {
        mLinePath.rewind();

        int numberOfBands = mAllBandLevel.length;
        int space = mContentRect.width() / (numberOfBands - 1);
        if (space != mBandSpace) {
            mBandSpace = space;
            mCornerPathEffect = new CornerPathEffect(Math.round(space / 2.0));
        }

        mLinePath.moveTo(mContentRect.left, getBandLevelY(0));

        for (int band = 1; band < numberOfBands; band++) {
            float x = band * space;
            float y = getBandLevelY(band);

            mLinePath.lineTo(x, y);
        }

        mPaint.setPathEffect(mCornerPathEffect);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(lineWidth);
        mPaint.setColor(isEnabled() ? lineColor : lineDisableColor);
        canvas.drawPath(mLinePath, mPaint);
    }

    private int getBandLevelY(int band) {
        int height = mContentRect.height();

        double percent = (mAllBandLevel[band] - mMinBandLevel) * 1.0 / mBandRange;

        int result = height - (int) Math.round(height * percent);
        float halfLineWith = (float) (lineWidth / 2.0);

        return (int) Math.min(Math.max(halfLineWith, result), mContentRect.height() - lineWidth);
    }

    public void setLineColor(@ColorInt int color) {
        this.lineColor = color;
        invalidate();
    }

    public void setLineColorRes(@ColorRes int resId) {
        setLineColor(getResources().getColor(resId));
    }

    @ColorInt
    public int getLineColor() {
        return lineColor;
    }

    public void setLineDisableColor(@ColorInt int lineDisableColor) {
        this.lineDisableColor = lineDisableColor;
        invalidate();
    }

    public void setLineDisableColorRes(@ColorRes int resId) {
        setLineDisableColor(getResources().getColor(resId));
    }

    @ColorInt
    public int getLineDisableColor() {
        return lineDisableColor;
    }

    public void setLineWidth(int lineWidth) {
        this.lineWidth = lineWidth;
        invalidate();
    }

    public void setLineWidthRes(@DimenRes int resId) {
        setLineWidth(getResources().getDimensionPixelOffset(resId));
    }

    public int getLineWidth() {
        return lineWidth;
    }

    public void setHintTextSize(int hintTextSize) {
        this.hintTextSize = hintTextSize;
        invalidate();
    }

    public void setHintTextSizeRes(@DimenRes int resId) {
        setHintTextSize(getResources().getDimensionPixelOffset(resId));
    }

    public int getHintTextSize() {
        return hintTextSize;
    }

    public void setHintTextColor(@ColorInt int hintTextColor) {
        this.hintTextColor = hintTextColor;
        invalidate();
    }

    public void setHintTextColorRes(@ColorRes int resId) {
        setHintTextColor(getResources().getColor(resId));
    }

    public int getHintTextColor() {
        return hintTextColor;
    }

    public void setGridLineWidth(int gridLineWidth) {
        this.gridLineWidth = gridLineWidth;
        invalidate();
    }

    public void setGridLineWidthRes(@DimenRes int resId) {
        setGridLineWidth(getResources().getDimensionPixelOffset(resId));
    }

    public int getGridLineWidth() {
        return gridLineWidth;
    }

    public void setGridLineColor(@ColorInt int gridLineColor) {
        this.gridLineColor = gridLineColor;
        invalidate();
    }

    public void setGridLineColorRes(@ColorRes int resId) {
        setGridLineColor(getResources().getColor(resId));
    }

    @ColorInt
    public int getGridLineColor() {
        return gridLineColor;
    }
}
