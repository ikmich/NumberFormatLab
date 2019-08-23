package com.ikmich.localeaware;

import android.support.annotation.NonNull;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;
import java.util.regex.Pattern;

public class NumberInput {

    private EditText et;
    private NumberInputTextWatcher textWatcher;
    private Builder mBuilder;
    private Locale mLocale;

    private NumberInput(EditText editText, @NonNull Locale locale) {
        et = editText;
        mLocale = locale;
        textWatcher = new NumberInputTextWatcher(et, locale);
    }

    /**
     * Called to setup the Locale-aware behaviour.
     *
     * @param clearField Whether to clear the EditText field's contents or not. Pass true
     *                   in the event of a configuration change. i.e. when
     *                   savedInstanceState != null
     */
    public void setup(final boolean clearField) {
        char groupingSeparator = DecimalFormatSymbols.getInstance(mLocale).getGroupingSeparator();
        Log.d("dibug", String.format("grouping separator: '%s'", groupingSeparator));

        et.post(new Runnable() {
            @Override
            public void run() {
                if (clearField) {
                    et.setText(null);
                }

                String contents = et.getText().toString();
                et.setText(String.format("%s%s", getCurrencyString(), contents));
                et.setInputType(
                        InputType.TYPE_CLASS_NUMBER
                                | InputType.TYPE_NUMBER_FLAG_DECIMAL
                                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
                et.setKeyListener(DigitsKeyListener.getInstance(textWatcher.getAcceptedInputs()));

                final Runnable selectionAction = new Runnable() {
                    @Override
                    public void run() {
                        int sel = et.getSelectionEnd();
                        boolean hasCurrencySymbol = Pattern.compile(String.format("^%s", getCurrencyString()))
                                .matcher(et.getText()).find();
                        if (hasCurrencySymbol) {
                            if (sel <= getCurrencyString().length()) {
                                et.setSelection(getCurrencyString().length());
                            }
                        }
                    }
                };

                et.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectionAction.run();
                    }
                });

                selectionAction.run();

                et.addTextChangedListener(textWatcher);
            }
        });
    }

    private void attachBuilder(@NonNull Builder builder) {
        mBuilder = builder;

        textWatcher.shouldFormatText(mBuilder.shouldFormatText);
        textWatcher.setCurrencyString(getCurrencyString());
    }

    private String getCurrencyString() {
        if (!mBuilder.shouldShowCurrency) {
            return "";
        }

        if (!TextUtils.isEmpty(mBuilder.mCurrencyString)) {
            return mBuilder.mCurrencyString;
        }

        return Currency.getInstance(mLocale).getSymbol();
    }

    public String getUnformattedValue() {
        return textWatcher.getUnformattedValue();
    }

    @SuppressWarnings({"unused", "WeakerAccess"})
    public static class Builder {
        private boolean shouldFormatText = true;
        private boolean shouldShowCurrency = false;
        private String mCurrencyString = "";
        private Locale mLocale;

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

        public NumberInput buildFor(EditText editText) {
            NumberInput inputFormatter = new NumberInput(editText, mLocale);
            inputFormatter.attachBuilder(this);
            return inputFormatter;
        }
    }
}
