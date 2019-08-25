package com.ikmich.numberformat;

import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.EditText;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;
import java.util.regex.Pattern;

@SuppressWarnings("JavaDoc")
public class NumberFormatterTextWatcher implements TextWatcher {
    private static final String DIGITS = "-0123456789";

    private EditText editText;
    private Locale locale;
    private String currencyString = "";
    private boolean shouldFormatText = true;

    private int prevLen = 0;
    private char prevChar;
    private boolean hasDecimalSeparator;
    private int numFractionDigits;

    private InputListener inputListener;

    public NumberFormatterTextWatcher(EditText editText, @NonNull Locale locale) {
        this.editText = editText;
        this.locale = locale;
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
        this.currencyString = currencyString == null ? "" : currencyString.trim();
    }

    private String filterInput(String input) {
        if (input == null) {
            input = "";
        }

        // Remove currency string
        input = input.replaceAll(Pattern.quote(
                String.format("%s", currencyString)), "");

        // Remove negative sign preceded by something else
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
            return currencyString + input;
        }

        // Remove disallowed items
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (getAcceptedInputs().indexOf(c) > -1) {
                sb.append(c);
            }
        }
        input = sb.toString();
        String unformattedValue = stripGroupingChar(input);

        numFractionDigits = getNumCharsAfterDecimal(input);
        String formattedValue = format(unformattedValue);

        if (inputListener != null) {
            inputListener.onChange(unformattedValue, formattedValue);
        }

        if (shouldFormatText) {
            input = formattedValue;
        }

        return currencyString + input;
    }

    private String stripGroupingChar(String input) {
        return input.replaceAll(
                Pattern.quote(String.format("%s", getGroupingChar())), "");
    }

    private String format(String input) {
        try {
            NumberFormat nf = NumberFormat.getInstance(locale);
            nf.setMaximumFractionDigits(numFractionDigits);
            Number number = nf.parse(input);
            input = nf.format(number);
        } catch (ParseException | ClassCastException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return input;
    }

    private int getNumCharsAfterDecimal(String input) {
        if (hasDecimal(input)) {
            int decimalIndex = input.indexOf(getDecimalChar());
            return decimalIndex < input.length() - 1
                    ? input.substring(decimalIndex + 1).length()
                    : 0;
        }
        return 0;
    }

    private boolean hasDecimal(String input) {
        if (TextUtils.isEmpty(input))
            return false;
        return input.indexOf(getDecimalChar()) > -1;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        hasDecimalSeparator = s.toString().indexOf(getDecimalChar()) > -1;
        prevLen = s.length();

        if (start > 0) {
            if (prevLen == start) {
                // typing, not first character.
                prevChar = s.charAt(start - 1);
            } else {
                // deleting
                prevChar = s.charAt(start);
            }
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String value = s.toString();
        boolean isDelete = value.length() < prevLen;

        if (!isDelete && value.length() == 0) {
            return;
        }

        if (!isDelete) {
            char inputChar = value.charAt(start);
            boolean notAllowedHere = (inputChar == getDecimalChar()
                    && (hasDecimalSeparator || (getGroupingChar() == prevChar)))
                    || inputChar == getGroupingChar();
            if (notAllowedHere) {
                // Prevent repeated decimal separator character.
                value = removeCharAt(value, start);
                if (start > 0) {
                    // Adjust 'start' pointer since an item has been removed
                    start--;
                }
            }
        } else {
            if (start > 0) {
                if (prevChar == getGroupingChar()) {
                    // Grouping character deleted. Also delete the number preceding it.
                    value = removeCharAt(value, start - 1);
                    // Adjust 'start' pointer since an item has been removed
                    start--;
                }
            }
        }

        String filtered = filterInput(value);

        editText.removeTextChangedListener(this);
        editText.setText(filtered);

        int diff = filtered.length() - value.length();
        if (diff < 0)
            diff = 0;
        int cursorPos = start + diff;
        if (!isDelete)
            cursorPos++;

        if (cursorPos < 0)
            cursorPos = 0;

        editText.setSelection(cursorPos);

        if (editText.getSelectionEnd() == 0 && hasCurrencyString()) {
            cursorPos = editText.getSelectionEnd() + currencyString.length();
            editText.setSelection(cursorPos);
        }

        editText.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private String removeCharAt(String s, int index) {
        StringBuilder sb = new StringBuilder(s);
        return sb.deleteCharAt(index).toString();
    }

    private boolean hasCurrencyString() {
        return !TextUtils.isEmpty(currencyString);
    }

    public String getAcceptedInputs() {
        return NumberFormatterTextWatcher.DIGITS + getDecimalChar() + getGroupingChar();
    }

    public char getDecimalChar() {
        return DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
    }

    public char getGroupingChar() {
        return DecimalFormatSymbols.getInstance(locale).getGroupingSeparator();
    }

    public void setInputListener(@NonNull InputListener inputListener) {
        this.inputListener = inputListener;
    }

    public interface InputListener {
        void onChange(String unformattedValue, String formattedValue);
    }
}
