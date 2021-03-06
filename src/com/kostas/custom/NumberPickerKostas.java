/*
 * Copyright (c) 2010, Jeffrey F. Cole
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions are met:
 * 
 * 	Redistributions of source code must retain the above copyright notice, this
 * 	list of conditions and the following disclaimer.
 * 
 * 	Redistributions in binary form must reproduce the above copyright notice, 
 * 	this list of conditions and the following disclaimer in the documentation 
 * 	and/or other materials provided with the distribution.
 * 
 * 	Neither the name of the technologichron.net nor the names of its contributors 
 * 	may be used to endorse or promote products derived from this software 
 * 	without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE 
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF 
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS 
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN 
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) 
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.kostas.custom;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.os.Handler;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.*;
import android.widget.*;
import com.kostas.onlineHelp.R;

/**
 * A simple layout group that provides a numeric text area with two buttons to
 * increment or decrement the value in the text area. Holding either button
 * will auto increment the value up or down appropriately. 
 * 
 * @author Jeffrey F. Cole
 *
 */
public class NumberPickerKostas extends LinearLayout {

    private int minValue;
    private int maxValue;
    private int step;
	
	private final int TEXT_SIZE = 14;
	
	public Integer value;

    Button decrement;
    Button increment;

	public EditText valueText;
    public TextView descriptionText;
	
	private Handler repeatUpdateHandler = new Handler();
	
	private boolean autoIncrement = false;
	private boolean autoDecrement = false;

	/**
	 * This little guy handles the auto part of the auto incrementing feature.
	 * In doing so it instantiates itself. There has to be a pattern name for
	 * that...
	 *
	 * @author Jeffrey F. Cole
	 *
	 */
	class RepetetiveUpdater implements Runnable {
		public void run() {
			if( autoIncrement ){
				increment();
				repeatUpdateHandler.postDelayed( new RepetetiveUpdater(), 50 );
			} else if( autoDecrement ){
				decrement();
				repeatUpdateHandler.postDelayed( new RepetetiveUpdater(), 50 );
			}
		}
	}

    public NumberPickerKostas( Context context, AttributeSet attributeSet ) {
        this(context, attributeSet, R.attr.kostasPickerViewStyle);
    }
	
	public NumberPickerKostas( Context context, AttributeSet attributeSet, int defStyle ) {
		super(context, attributeSet, defStyle);



        // load the styled attributes and set their properties
        TypedArray attributes = context.obtainStyledAttributes(attributeSet, R.styleable.NumberPickerKostas, defStyle, 0);

        setMaxValue(attributes.getInteger(R.styleable.NumberPickerKostas_maxValue, 300));
        setMinValue(attributes.getInteger(R.styleable.NumberPickerKostas_minValue, 10));
        setStep(attributes.getInteger(R.styleable.NumberPickerKostas_step, 10));

		
		this.setLayoutParams( new LayoutParams( LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT ) );
		LayoutParams elementButtonParams = new LayoutParams( 0, LayoutParams.MATCH_PARENT , 1);
        elementButtonParams.leftMargin = 8;
        elementButtonParams.rightMargin = 2;
        LayoutParams elementTextParams = new LayoutParams( 0, LayoutParams.MATCH_PARENT , 2);
//        elementButtonParams.topMargin = ELEMENT_HEIGHT/4;
		
		// init the individual elements
		initDecrementButton( context );

        initDescriptionText( context, attributes.getString(R.styleable.NumberPickerKostas_textName) );
		initIncrementButton( context );

        initValueEditText( context );
            addView( descriptionText, elementTextParams);
            addView( valueText, elementTextParams );

			addView( decrement, elementButtonParams );
			addView( increment, elementButtonParams );
//		}
	}
	
	private void initIncrementButton( Context context){
        increment = new Button( context );
		increment.setTextSize( TEXT_SIZE+6 );
		increment.setText( "+" );
        increment.setTextColor(getResources().getColor(R.color.white_back));
        increment.setTypeface(Typeface.DEFAULT_BOLD);
        increment.setBackgroundDrawable(getResources().getDrawable(R.drawable.plus_minus_selector));

                // Increment once for a click
                        increment.setOnClickListener(new OnClickListener() {
                            public void onClick(View v) {
                                increment();
                            }
                        });
		
		// Auto increment for a long click
		increment.setOnLongClickListener( 
				new OnLongClickListener(){
					public boolean onLongClick(View arg0) {
						autoIncrement = true;
						repeatUpdateHandler.post( new RepetetiveUpdater() );
						return false;
					}
				}
		);
		
		// When the button is released, if we're auto incrementing, stop
		increment.setOnTouchListener( new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if( event.getAction() == MotionEvent.ACTION_UP && autoIncrement ){
					autoIncrement = false;
				}
				return false;
			}
		});
	}


    private void initDescriptionText( Context context, String text){


        descriptionText = new TextView( context );
        descriptionText.setTextSize(TEXT_SIZE);
        descriptionText.setTextColor(getResources().getColor(R.color.white_back));
        descriptionText.setGravity(Gravity.CENTER);
        descriptionText.setTypeface(Typeface.DEFAULT_BOLD);

        descriptionText.setBackgroundDrawable(getResources().getDrawable(R.color.interval_green));

        descriptionText.setText( text );
    }

	private void initValueEditText( Context context){
		
		value = new Integer( minValue );
		
		valueText = new EditText( context );
		valueText.setTextSize( TEXT_SIZE );
        valueText.setFocusable(false);
        valueText.setTypeface(Typeface.DEFAULT_BOLD);
//        valueText.setText("NO");

        valueText.setTextColor(getResources().getColor(R.color.interval_green));
        valueText.setGravity(Gravity.CENTER);
        valueText.setBackgroundDrawable(getResources().getDrawable(R.color.white_back));
		
		// Since we're a number that gets affected by the button, we need to be
		// ready to change the numeric value with a simple ++/--, so whenever
		// the value is changed with a keyboard, convert that text value to a
		// number. We can set the text area to only allow numeric input, but 
		// even so, a carriage return can get hacked through. To prevent this
		// little quirk from causing a crash, store the value of the internal
		// number before attempting to parse the changed value in the text area
		// so we can revert to that in case the text change causes an invalid
		// number
		valueText.setOnKeyListener(new OnKeyListener() {
            public boolean onKey(View v, int arg1, KeyEvent event) {
                int backupValue = value;
                try {
                    value = Integer.parseInt(((EditText) v).getText().toString());
                } catch (NumberFormatException nfe) {
                    value = backupValue;
                }
                return false;
            }
        });
		
		// Highlight the number when we get focus
		valueText.setOnFocusChangeListener(new OnFocusChangeListener() {
			public void onFocusChange(View v, boolean hasFocus) {
				if( hasFocus ){
					((EditText)v).selectAll();
				}else{
                    if (getValue()<10) setValue(10);
                }
			}
		});
		valueText.setGravity( Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL );
		valueText.setText( value==0? "NO" : value.toString() );
        if (value==0) disableButtonColor(true);

		valueText.setInputType( InputType.TYPE_CLASS_NUMBER );
	}
	
	private void initDecrementButton( Context context){
//		decrement = new ImageButton( context );
//		decrement.setTextSize( TEXT_SIZE );
        decrement = new Button( context );
        decrement.setTextSize( TEXT_SIZE+6 );
        decrement.setTextColor(getResources().getColor(R.color.white_back));
        decrement.setTypeface(Typeface.DEFAULT_BOLD);

		decrement.setText( "-" );

//        decrement.setBackgroundDrawable(getResources().getDrawable(R.drawable.interval_minus_selector));
        decrement.setBackgroundDrawable(getResources().getDrawable(R.drawable.plus_minus_selector));


        // Decrement once for a click
		decrement.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
            	decrement();
            }
        });
		

		// Auto Decrement for a long click
		decrement.setOnLongClickListener( 
				new OnLongClickListener(){
					public boolean onLongClick(View arg0) {
						autoDecrement = true;
						repeatUpdateHandler.post( new RepetetiveUpdater() );
						return false;
					}
				}
		);
		
		// When the button is released, if we're auto decrementing, stop
		decrement.setOnTouchListener( new OnTouchListener() {
			public boolean onTouch(View v, MotionEvent event) {
				if( event.getAction() == MotionEvent.ACTION_UP && autoDecrement ){
					autoDecrement = false;
				}
				return false;
			}
		});
	}
	
	public void increment(){
		if( value < maxValue ){



            if (value<minValue+step){
                disableButtonColor(false);
            }

			value += step;
			valueText.setText( value.toString() );

		}
	}

	public void decrement(){
		if( value > minValue ){
			value -= step;



                valueText.setText(value==0? "NO" : value.toString());
                if (value==0) disableButtonColor(true);

		}
	}
	
	public int getValue(){
		return value;
	}
	
	public void setValue( int value ){
		if( value > maxValue ) value = maxValue;
		if( value >= minValue ){
			this.value = value;
			valueText.setText(this.value.toString());
		}


	}


    public int getMinValue() {
        return minValue;
    }

    public void setMinValue(int minValue) {
        this.minValue = minValue;
    }

    public int getMaxValue() {
        return maxValue;
    }

    public void setMaxValue(int maxValue) {
        this.maxValue = maxValue;
    }

    public int getStep() {
        return step;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public void disableButtonColor(boolean disable){
        if (disable){
            descriptionText.setBackgroundDrawable(getResources().getDrawable(R.color.secondary_grey));
            increment.setBackgroundDrawable(getResources().getDrawable(R.color.secondary_grey));
            decrement.setBackgroundDrawable(getResources().getDrawable(R.color.secondary_grey));
            valueText.setTextColor(getResources().getColor(R.color.secondary_grey));
        }else{
            descriptionText.setBackgroundDrawable(getResources().getDrawable(R.color.interval_green));
            increment.setBackgroundDrawable(getResources().getDrawable(R.drawable.plus_minus_selector));
            decrement.setBackgroundDrawable(getResources().getDrawable(R.drawable.plus_minus_selector));
            valueText.setTextColor(getResources().getColor(R.color.interval_green));
        }
    }
}
