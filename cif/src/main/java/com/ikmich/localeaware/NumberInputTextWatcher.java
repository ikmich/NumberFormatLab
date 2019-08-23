package com.ikmich.localeaware;

import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Pattern;

public class NumberInputTextWatcher implements TextWatcher {
    private boolean hasDecimalSeparator;
    private int startLen = 0;
    private static final String DIGITS = "-0123456789";

    private EditText et;
    private Locale mLocale;
    private String mCurrencyString = "";
    private boolean shouldFormatText = true;
    private String unformattedValue = "";
    private char prevChar;

    public NumberInputTextWatcher(EditText editText, @NonNull Locale locale) {
        et = editText;
        mLocale = locale;
    }

    public void shouldFormatText(boolean b) {
        shouldFormatText = b;
    }

    /**
     * Sets the currency string to be prefixed to the number.
     *
     * @param currencyString
     */
    public void setCurrencyString(String currencyString) {
        mCurrencyString = currencyString == null ? "" : currencyString.trim();
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    private String filter(String input) {
        if (input == null) {
            input = "";
        }

        // Remove currency string
        String quote = Pattern.quote(String.format("%s", mCurrencyString));
        input = input.replaceAll(quote, "");

        // Remove negative sign not at the start of number
        input = input.replaceAll("(?<=.)-+", "");

        // Reduce multiple trailing decimals to one
        input = input.replaceAll(
                String.format("\\%s+$", getDecimalChar()), String.valueOf(getDecimalChar()));

        // Ends in decimal
        boolean b1 = Pattern.compile(String.format("\\%s$", getDecimalChar()))
                .matcher(input).find();

        // Ends in zero after decimal
        boolean b2 = Pattern.compile(String.format("\\%s\\d*0+$", getDecimalChar()))
                .matcher(input).find();

        if (b1 || b2) {
            return mCurrencyString + input;
        }

        // Remove disallowed items
        StringBuilder sb = new StringBuilder();
        // List<Character> allowedChars = Arrays.asList(getDecimalChar());
        for (char c : input.toCharArray()) {
            if (/*allowedChars.contains(c) || DIGITS.indexOf(c) > -1*/ getAcceptedInputs().indexOf(c) > -1) {
                sb.append(c);
            }
        }
        input = sb.toString();
        unformattedValue = input;

        if (shouldFormatText) {
            input = format(input);
        }

        return mCurrencyString + input;
    }

    private String format(String input) {
        try {
            NumberFormat nf = NumberFormat.getInstance(mLocale);
            nf.setMaximumFractionDigits(5);
            Number number = nf.parse(input);
            input = nf.format(number);
        } catch (ParseException | ClassCastException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return input;
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
        String value = s.toString();
        boolean isDelete = value.length() < startLen;

        if (!isDelete && value.length() == 0) {
            return;
        }

        if (!isDelete) {
            char inputChar = value.charAt(start);
            boolean notAllowedHere = inputChar == getDecimalChar()
                    && (hasDecimalSeparator || (getGroupingChar() == prevChar));
            if (notAllowedHere) {
                // Prevent repeated decimal separator character.
                value = removeCharAt(value, start);
                if (start > 0) {
                    // Adjust the 'start' pointer, since an item has been removed from the input
                    start--;
                }
            }
        }

        String filtered = filter(value);

        et.removeTextChangedListener(this);
        et.setText(filtered);

        Log.d("dibug", "start: " + start);

        int cur;

        int diff = filtered.length() - value.length();
        // int selPos = start + diff;
        // int gap = isDelete ? 0 : 1;
        // if (start < filtered.length()) {
        //     et.setSelection(selPos + gap);
        // } else {
        //     et.setSelection(selPos);
        // }

        cur = start + diff;
        if (!isDelete) {
            cur++;
        }

        if (cur < 0) {
            cur = 0;
        }

        et.setSelection(cur);

        if (et.getSelectionEnd() == 0 && hasCurrencyString()) {
            cur = et.getSelectionEnd() + mCurrencyString.length();
            et.setSelection(cur);
        }

        et.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private String removeCharAt(String s, int index) {
        StringBuilder sb = new StringBuilder(s);
        return sb.deleteCharAt(index).toString();
    }

    public String getUnformattedValue() {
        return unformattedValue;
    }

    private boolean hasCurrencyString() {
        return !TextUtils.isEmpty(mCurrencyString);
    }

    public String getAcceptedInputs() {
        return NumberInputTextWatcher.DIGITS + getDecimalChar() + getGroupingChar();
    }

    public char getDecimalChar() {
        return DecimalFormatSymbols.getInstance(mLocale).getDecimalSeparator();
    }

    public char getGroupingChar() {
        return DecimalFormatSymbols.getInstance(mLocale).getGroupingSeparator();
    }
}
