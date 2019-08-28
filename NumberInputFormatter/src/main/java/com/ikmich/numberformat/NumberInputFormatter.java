package com.ikmich.numberformat;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;

import java.util.Currency;
import java.util.Locale;
import java.util.regex.Pattern;

public class NumberInputFormatter {

    private EditText editText;
    private NumberFormatterTextWatcher textWatcher;
    private Builder builder;
    private Locale locale;

    private NumberInputFormatter(EditText editText, @NonNull Locale locale, @NonNull Builder builder) {
        this.editText = editText;
        this.locale = locale;
        this.builder = builder;

        textWatcher = new NumberFormatterTextWatcher(this.editText, locale);
        textWatcher.shouldFormatText(builder.shouldFormatText);
        textWatcher.setCurrencyString(getCurrencyString());
        textWatcher.setMaxDecimalChars(builder.maxDecimalChars);
    }

    /**
     * Sets up the Locale-aware number formatting behaviour.
     *
     * @param clearField Whether to clear the EditText field's contents or not. Pass `true`
     *                   in the event of a configuration change. i.e. when
     *                   savedInstanceState != null
     */
    public void setup(final boolean clearField) {
        editText.post(new Runnable() {
            @SuppressLint("ClickableViewAccessibility")
            @Override
            public void run() {
                if (clearField) {
                    editText.setText(null);
                }

                String contents = editText.getText().toString();
                editText.setText(String.format("%s%s", getCurrencyString(), contents));
                editText.setInputType(
                        InputType.TYPE_CLASS_NUMBER
                                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                editText.setKeyListener(
                        DigitsKeyListener.getInstance(textWatcher.getAcceptedInputs()));

                final Runnable cursorCheck = new Runnable() {
                    @Override
                    public void run() {
                        int sel = editText.getSelectionEnd();
                        boolean hasCurrencySymbol = Pattern.compile("^" + Pattern.quote(getCurrencyString()))
                                .matcher(editText.getText()).find();
                        if (hasCurrencySymbol) {
                            if (sel <= getCurrencyString().length()) {
                                editText.setSelection(getCurrencyString().length());
                            }
                        }
                    }
                };

                editText.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                            case MotionEvent.ACTION_UP:
                                v.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        cursorCheck.run();
                                    }
                                });
                                break;
                        }
                        return false;
                    }
                });

                cursorCheck.run();

                editText.addTextChangedListener(textWatcher);
            }
        });
    }

    private String getCurrencyString() {
        if (!builder.shouldShowCurrency) {
            return "";
        }

        if (!TextUtils.isEmpty(builder.mCurrencyString)) {
            return builder.mCurrencyString;
        }

        return Currency.getInstance(locale).getSymbol();
    }

    public void setInputListener(@NonNull NumberFormatterTextWatcher.InputListener inputListener) {
        textWatcher.setInputListener(inputListener);
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class Builder {
        private boolean shouldFormatText = true;
        private boolean shouldShowCurrency = false;
        private String mCurrencyString = "";
        private Locale mLocale;
        private int maxDecimalChars = -1;

        public Builder() {
            this(Locale.getDefault());
        }

        public Builder(@NonNull Locale locale) {
            this.mLocale = locale;
        }

        public Builder formatInput(boolean b) {
            this.shouldFormatText = b;
            return this;
        }

        public Builder showCurrency(boolean b) {
            return showCurrency(b, "");
        }

        public Builder showCurrency(boolean b, String currencyString) {
            this.shouldShowCurrency = b;
            this.mCurrencyString = currencyString;
            return this;
        }

        public Builder setMaxDecimalChars(int maxDecimalChars) {
            this.maxDecimalChars = maxDecimalChars;
            return this;
        }

        public NumberInputFormatter buildFor(EditText editText) {
            return new NumberInputFormatter(editText, mLocale, this);
        }
    }
}
