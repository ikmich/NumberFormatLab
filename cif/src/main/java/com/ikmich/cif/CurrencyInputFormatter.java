package com.ikmich.cif;

import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Locale;
import java.util.regex.Pattern;

// @SuppressWarnings({"unused", "WeakerAccess"})
public class CurrencyInputFormatter {

    private EditText mEditText;
    private InputWatcher inputWatcher;
    private Builder mBuilder;
    private Locale mLocale;
    private static final String DIGITS = "0123456789";

    private CurrencyInputFormatter(EditText editText, @NonNull Locale locale) {
        mEditText = editText;
        mLocale = locale;
        inputWatcher = new InputWatcher();
    }

    public void setup() {
        mEditText.setText(null);
        mEditText.setKeyListener(DigitsKeyListener.getInstance(getAcceptedInputs()));
        mEditText.addTextChangedListener(inputWatcher);
    }

    private void attachBuilder(Builder builder) {
        mBuilder = builder;
    }

    private class InputWatcher implements TextWatcher {
        private boolean hasDecimalSeparator;
        private int startLen = 0;
        private int len = 0;
        private char prevChar;
        private String str;
        private String formatted;

        InputWatcher() {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            hasDecimalSeparator = s.toString().indexOf(getDecimalChar()) > -1;
            startLen = s.length();

            if (start >= 0 && start < s.length()) {
                prevChar = s.charAt(start);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            len = s.length();
            str = s.toString();

            boolean deleting = len < startLen;

            if (!deleting) {
                char inputChar = s.charAt(start);
                if (hasDecimalSeparator && inputChar == getDecimalChar()) {
                    // Prevent repeat decimal separator character.
                    mEditText.removeTextChangedListener(this);
                    mEditText.setText(removeCharAt(str, start));
                    mEditText.setSelection(start);
                    mEditText.addTextChangedListener(this);
                }
            } else {
                if (prevChar == getDecimalChar()) {
                    return;
                }
            }

            if (mBuilder.mFormatText) {
                // Ends in decimal
                boolean b1 = Pattern.compile(String.format("\\%s$", getDecimalChar()))
                        .matcher(str).find();

                // Ends in zero after decimal
                boolean b2 = Pattern.compile(String.format("\\%s\\d*0+$", getDecimalChar()))
                        .matcher(str).find();

                boolean shouldFormat = !b1 && !b2;
                if (!shouldFormat) {
                    return;
                }
                formatted = formatCurrencyInput(str);

                mEditText.removeTextChangedListener(this);
                mEditText.setText(formatted);

                int diff = formatted.length() - len;
                int selPos = start + diff;
                int gap = deleting ? 0 : 1;

                if (start < s.length()) {
                    mEditText.setSelection(selPos + gap);
                } else {
                    mEditText.setSelection(selPos);
                }

                mEditText.addTextChangedListener(this);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    private String getAcceptedInputs() {
        return DIGITS + getDecimalChar();
    }

    private char getDecimalChar() {
        return DecimalFormatSymbols.getInstance().getDecimalSeparator();
    }

    private String removeCharAt(String s, int index) {
        StringBuilder sb = new StringBuilder(s);
        return sb.deleteCharAt(index).toString();
    }

    private String formatCurrencyInput(String input) {
        try {
            DecimalFormat df = (DecimalFormat) NumberFormat.getInstance(mLocale);
            df.setMaximumFractionDigits(9);
            Number number = df.parse(input);
            String formatted = df.format(number);
            return getCurrencySymbol() + formatted;
        } catch (ParseException | ClassCastException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private String getCurrencySymbol() {
        if (!mBuilder.mShowCurrency) {
            return "";
        }

        String currency = Currency.getInstance(mLocale).getCurrencyCode();
        if (!TextUtils.isEmpty(mBuilder.mCurrencySymbol)) {
            currency = mBuilder.mCurrencySymbol;
        }

        return currency;
    }

    public static class Builder {
        private boolean mFormatText = true;
        private boolean mShowCurrency = false;
        private String mCurrencySymbol = "";
        private Locale mLocale;

        public Builder() {
            this(Locale.getDefault());
        }

        public Builder(@NonNull Locale locale) {
            this.mLocale = locale;
        }

        public Builder dontFormat() {
            this.mFormatText = false;
            return this;
        }

        public Builder showCurrency(boolean b) {
            this.mShowCurrency = b;
            return this;
        }

        public Builder showCurrency(boolean b, String currencySymbol) {
            this.mShowCurrency = b;
            this.mCurrencySymbol = currencySymbol;
            return this;
        }

        public CurrencyInputFormatter buildFor(EditText editText) {
            CurrencyInputFormatter inputFormatter = new CurrencyInputFormatter(editText, mLocale);
            inputFormatter.attachBuilder(this);
            return inputFormatter;
        }
    }
}
