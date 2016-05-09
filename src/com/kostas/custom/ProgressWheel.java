package com.kostas.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.view.View;
import com.kostas.onlineHelp.R;


/**
 * An indicator of progress, similar to Android's ProgressBar.
 * Can be used in 'spin mode' or 'increment mode'
 */
public class ProgressWheel extends View {

    //Sizes (with defaults)
    //private int layout_side = 150;
    //private int layout_width = 0;
    private int fullRadius = 100;
    private int circleRadius = 80;
    private int barLength = 60;
    private int barWidth = 18;
    private int rimWidth = 20;
    private int textSize = 15;
    private float contourSize = 0;

    //Padding (with defaults)
//    private int paddingTop = 5;
//    private int paddingBottom = 5;
//    private int paddingLeft = 5;
//    private int paddingRight = 5;

    //Colors (with defaults)
    private int barColor = -4179669;//0xAA000000;
    //private int contourColor = 0xAA000000;
   // private int circleColor = 0x00000000;
    private int rimColor = -592395;//0xAADDDDDD;
    private int textColor = -13710223;// 0xFF000000;

    //Paints
    private Paint barPaint = new Paint();
//    private Paint circlePaint = new Paint();
    private Paint rimPaint = new Paint();
    private Paint textPaint = new Paint();
//    private Paint contourPaint = new Paint();

    //Rectangles
    @SuppressWarnings("unused")
    private RectF rectBounds = new RectF();
    private RectF circleBounds = new RectF();
//    private RectF circleOuterContour = new RectF();
//    private RectF circleInnerContour = new RectF();

    //Animation
    //The amount of pixels to move the bar by on each draw
    private int spinSpeed = 2;
    //The number of milliseconds to wait inbetween each draw
    private int delayMillis = 0;
    private Handler spinHandler = new Handler() {
        /**
         * This is the code that will increment the progress variable
         * and so spin the wheel
         */
        @Override
        public void handleMessage(Message msg) {
            invalidate();
            if (isSpinning) {
                progress += spinSpeed;
                if (progress > 360) {
                    progress = 0;
                }
                spinHandler.sendEmptyMessageDelayed(0, delayMillis);
            }
            //super.handleMessage(msg);
        }
    };
    int progress = 0;
    boolean isSpinning = false;

    //Other
    private String text = "";
    private String[] splitText;

    int startAngle = 0;
    private float multiplier = 1;

    public float getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }

    /**
     * The constructor for the ProgressWheel
     *
     * @param context
     * @param attrs
     */
    public ProgressWheel(Context context, AttributeSet attrs) {
        super(context, attrs);

        parseAttributes(context.obtainStyledAttributes(attrs,
                R.styleable.ProgressWheel));
    }

    /*
     * When this is called, make the view square.
     * From: http://www.jayway.com/2012/12/12/creating-custom-android-views-part-4-measuring-and-how-to-force-a-view-to-be-square/
     *
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // The first thing that happen is that we call the superclass
        // implementation of onMeasure. The reason for that is that measuring
        // can be quite a complex process and calling the super method is a
        // convenient way to get most of this complexity handled.
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // We can’t use getWidth() or getHight() here. During the measuring
        // pass the view has not gotten its final size yet (this happens first
        // at the start of the layout pass) so we have to use getMeasuredWidth()
        // and getMeasuredHeight().
        //int size = 0;
        //int width = (int) (getMeasuredWidth()*multiplier);
        //int height = (int) (getMeasuredHeight()*multiplier);
        //int widthWithoutPadding = width;// - paddingLeft - paddingRight;
        //int heigthWithoutPadding = height;// - paddingTop - paddingBottom;

        // Finally we have some simple logic that calculates the size of the view
        // and calls setMeasuredDimension() to set that size.
        // Before we compare the width and height of the view, we remove the padding,
        // and when we set the dimension we add it back again. Now the actual content
        // of the view will be square, but, depending on the padding, the total dimensions
        // of the view might not be.
//        if (widthWithoutPadding > heigthWithoutPadding) {
//            size = heigthWithoutPadding;
//        } else {
//            size = widthWithoutPadding;
//        }

        // If you override onMeasure() you have to call setMeasuredDimension().
        // This is how you report back the measured size.  If you don’t call
        // setMeasuredDimension() the parent will throw an exception and your
        // application will crash.
        // We are calling the onMeasure() method of the superclass so we don’t
        // actually need to call setMeasuredDimension() since that takes care
        // of that. However, the purpose with overriding onMeasure() was to
        // change the default behaviour and to do that we need to call
        // setMeasuredDimension() with our own values.
        //setMeasuredDimension(size + paddingLeft + paddingRight, size + paddingTop + paddingBottom);
    }

    /**
     * Use onSizeChanged instead of onAttachedToWindow to get the dimensions of the view,
     * because this method is called after measuring the dimensions of MATCH_PARENT & WRAP_CONTENT.
     * Use this dimensions to setup the bounds and paints.
     */
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        // Share the dimensions
        //layout_width = w;
        //layout_side = h;

        setupBounds();
        setupPaints();
        invalidate();
    }

    /**
     * Set the properties of the paints we're using to
     * draw the progress wheel
     */
    private void setupPaints() {
        barPaint.setColor(barColor);
        barPaint.setAntiAlias(true);
        barPaint.setStyle(Style.STROKE);
        barPaint.setStrokeWidth(barWidth);

        rimPaint.setColor(rimColor);
        rimPaint.setAntiAlias(true);
        rimPaint.setStyle(Style.STROKE);
        rimPaint.setStrokeWidth(rimWidth);

//        circlePaint.setColor(circleColor);
//        circlePaint.setAntiAlias(true);
//        circlePaint.setStyle(Style.FILL);

        textPaint.setColor(textColor);
        textPaint.setStyle(Style.FILL);
        textPaint.setAntiAlias(true);
        textPaint.setTextSize(textSize);


//        contourPaint.setColor(contourColor);
//        contourPaint.setAntiAlias(true);
//        contourPaint.setStyle(Style.STROKE);
//        contourPaint.setStrokeWidth(contourSize);
    }

    /**
     * Set the bounds of the component
     */
    private void setupBounds() {
        // Width should equal to Height, find the min value to steup the circle
        //int minValue = layout_side;

        // Calc the Offset if needed
        //int xOffset = layout_width - minValue;
        //int offset = layout_side - minValue;

        // Add the offset
        //int padding =  (offset / 2);
        //int paddingLeftRight =  (xOffset / 2);

        int width = getWidth(); //this.getLayoutParams().width;
        int height = getHeight(); //this.getLayoutParams().height;

        rectBounds = new RectF(0,
                0,
                width ,
                height);

        circleBounds = new RectF(barWidth,
                 barWidth,
                width - barWidth,
                height - barWidth);
//        circleInnerContour = new RectF(circleBounds.left + (rimWidth / 2.0f) + (contourSize / 2.0f), circleBounds.top + (rimWidth / 2.0f) + (contourSize / 2.0f), circleBounds.right - (rimWidth / 2.0f) - (contourSize / 2.0f), circleBounds.bottom - (rimWidth / 2.0f) - (contourSize / 2.0f));
//        circleOuterContour = new RectF(circleBounds.left - (rimWidth / 2.0f) - (contourSize / 2.0f), circleBounds.top - (rimWidth / 2.0f) - (contourSize / 2.0f), circleBounds.right + (rimWidth / 2.0f) + (contourSize / 2.0f), circleBounds.bottom + (rimWidth / 2.0f) + (contourSize / 2.0f));

        fullRadius = (width - barWidth) / 2;
        circleRadius = (fullRadius - barWidth) + 1;
    }

    /**
     * Parse the attributes passed to the view from the XML
     *
     * @param a the attributes to parse
     */
    private void parseAttributes(TypedArray a) {
        barWidth = (int) a.getDimension(R.styleable.ProgressWheel_barWidth,
                barWidth);

        rimWidth = (int) a.getDimension(R.styleable.ProgressWheel_rimWidth,
                rimWidth);


        textSize = (int) a.getDimension(R.styleable.ProgressWheel_textSize,
                textSize)-2;


        // Recycle
        a.recycle();
    }

    //----------------------------------

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //Draw the inner circle
//        canvas.drawArc(circleBounds, 360, 360, false, circlePaint);
        //Draw the rim
        canvas.drawArc(circleBounds, 360, 360, false, rimPaint);
        //Draw the bar
        if (isSpinning) {
            canvas.drawArc(circleBounds, progress - 90, barLength, false,
                    barPaint);
        } else {
            canvas.drawArc(circleBounds, startAngle - 90, progress, false, barPaint);
        }

        if(splitText!=null){

            //Draw the text (attempts to center it horizontally and vertically)
            float textHeight = textPaint.descent() - textPaint.ascent()/16;
            float verticalTextOffset = (textHeight / 2) /*- textPaint.descent()/2*/;

            Paint blackPaint = new Paint();
            blackPaint.setColor(0xFF000000);
            blackPaint.setTextSize((int) 3*textSize / 5);
            Typeface tf=Typeface.createFromAsset(getContext().getAssets(),"fonts/OpenSans-Semibold.ttf");

            textPaint.setTypeface(tf);
            blackPaint.setTypeface(tf);

            for (String s : splitText) {
                float horizontalTextOffset = textPaint.measureText(s) / 2;

                    canvas.drawText(s, getWidth() / 2 - (7*horizontalTextOffset/8),
                            getHeight() / 2 - 3*verticalTextOffset/5, textPaint);
            }
        }
    }

    /**
     * Set the progress to a specific value
     */
    public void setProgress(int i) {
        isSpinning = false;
        progress = i;
        spinHandler.sendEmptyMessage(0);
    }

    public int getProgress(){
      return progress;
    }


    //----------------------------------

    /**
     * Set the text in the progress bar
     * Doesn't invalidate the view
     *
     * @param text the text to show ('\n' constitutes a new line)
     */
    public void setText(String text) {
        this.text = text;
        splitText = this.text.split("\n");
    }

    public int getTextSize() {
        return textSize;
    }

    public void setTextSize(int textSize) {
        this.textSize = textSize;
    }

}