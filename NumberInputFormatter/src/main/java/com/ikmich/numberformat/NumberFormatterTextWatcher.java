package com.ikmich.numberformat;

import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import java.text.DecimalFormat;
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

    private int lengthBefore = 0;
    private char charBefore;
    private boolean hasDecimalSeparator;
    private int numFractionDigits;
    private int maxDecimalChars = -1;

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

    /**
     * Strips out multiple decimal characters if any, leaving the last occurring decimal character.
     *
     * @param input The number string
     * @return The filtered number string.
     */
    private String resolveDecimals(String input) {
        if (TextUtils.isEmpty(input))
            return input;

        char[] chars = input.toCharArray();
        int decimalIndex = -1;

        // Find the last occurring decimal
        for (int i = chars.length - 1; i >= 0; i--) {
            if (chars[i] == getDecimalChar()) {
                decimalIndex = i;
                break;
            }
        }

        if (decimalIndex > -1) {
            // Remove any decimals occurring before this index
            String left = input.substring(0, decimalIndex);
            String right = input.substring(decimalIndex);
            left = left.replaceAll(Pattern.quote(String.valueOf(getDecimalChar())), "");
            return left + right;
        }

        return input;
    }

    /**
     * Removes negative sign character that is not at the beginning of the number string
     *
     * @param input
     * @return
     */
    private String resolveNegativeSign(String input) {
        return input.replaceAll("(?<=.)-+", "");
    }

    private String removeCurrencyString(String input) {
        return input.replaceAll(Pattern.quote(currencyString), "");
    }

    /**
     * Strips out any characters that are not allowed for a number as dictated by the current
     * Locale.
     *
     * @param input The number string to be filtered.
     * @return The filtered number string with all disallowed characters removed.
     */
    private String removeDisallowedChars(String input) {
        StringBuilder sb = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (getAcceptedInputs().indexOf(c) > -1) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * Filters a number string to remove unwanted characters, and formats the output.
     *
     * @param input The number string to be filtered.
     * @return The formatted or unformatted number string, depending on whether
     * <em>shouldFormatCode</em> is true or false.
     */
    private String filterInput(String input) {
        if (input == null) {
            input = "";
        }

        input = removeCurrencyString(input);
        input = resolveNegativeSign(input);
        input = removeDisallowedChars(input);
        input = resolveDecimals(input);

        // Format the characteristic (the part before the decimal character)
        int decimalIndex = input.indexOf(getDecimalChar());
        if (decimalIndex > -1) {
            String left = input.substring(0, decimalIndex);
            String right = input.substring(decimalIndex);
            String formattedCharacteristic = format(left);
            input = formattedCharacteristic + right;
        }

        String unformattedValue = stripGroupingChar(input);

        numFractionDigits = getNumCharsAfterDecimal(input);
        String formattedValue = format(unformattedValue);

        if (inputListener != null) {
            inputListener.onChange(
                    String.format("%s%s", currencyString, unformattedValue),
                    String.format("%s%s", currencyString, formattedValue));
        }

        // Ends in decimal
        boolean b1 = Pattern.compile(String.format("\\%s$", getDecimalChar()))
                .matcher(input).find();

        // Ends in zero after decimal
        boolean b2 = Pattern.compile(String.format("\\%s\\d*0+$", getDecimalChar()))
                .matcher(input).find();

        if (b1 || b2) {
            return currencyString + input;
        }

        if (shouldFormatText) {
            input = formattedValue;
        }

        return currencyString + input;
    }

    private String stripGroupingChar(String input) {
        return input.replaceAll(Pattern.quote(String.valueOf(getGroupingChar())), "");
    }

    private String format(String input) {
        if (TextUtils.isEmpty(input))
            return input;

        try {
            DecimalFormat nf = (DecimalFormat) NumberFormat.getInstance(locale);
            if (isMaxDecimalCharsPropSet()) {
                nf.setMaximumFractionDigits(maxDecimalChars);
            } else {
                nf.setMaximumFractionDigits(numFractionDigits);
            }
            nf.setParseBigDecimal(true);
            Number number = nf.parse(input);
            input = nf.format(number);
        } catch (ParseException | ClassCastException e) {
            e.printStackTrace();
            Log.e("ERROR", e.getMessage(), e);
        }

        return input;
    }

    private int getNumCharsAfterDecimal(String input) {
        int decimalIndex = input.indexOf(getDecimalChar());
        if (decimalIndex > -1) {
            return decimalIndex < input.length() - 1
                    ? input.substring(decimalIndex + 1).length() : 0;
        }
        return 0;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        hasDecimalSeparator = s.toString().indexOf(getDecimalChar()) > -1;
        lengthBefore = s.length();

        if (start > 0) {
            if (lengthBefore == start) {
                // typing
                charBefore = s.charAt(start - 1);
            } else {
                // deleting
                charBefore = s.charAt(start);
            }
        }
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String value = s.toString();
        boolean isDelete = value.length() < lengthBefore;

        if (!isDelete && value.length() == 0) {
            return;
        }

        if (isDelete) {
            if (start > 0) {
                if (charBefore == getGroupingChar()) {
                    // Grouping character deleted. Also delete the number preceding it.
                    value = removeCharAt(value, start - 1);
                    // Adjust 'start' pointer since an item has been removed
                    start--;
                }
            }
        } else {
            char inputChar = value.charAt(start);
            // Remove character that is not allowed (e.g a repeating decimal;
            // decimal after grouping character, etc).
            boolean notAllowedHere = (inputChar == getDecimalChar()
                    && (hasDecimalSeparator || (getGroupingChar() == charBefore)))
                    || inputChar == getGroupingChar();

            if (notAllowedHere) {
                value = removeCharAt(value, start);
                if (start > 0) {
                    start--;
                }
            }

            /*
             If there's a maxDecimalChars set, check needs to be put in place to ensure
             that typing is disallowed after the maxDecimalChars value is reached.
             */
            int decimalIndex = value.indexOf(getDecimalChar());
            if (start > decimalIndex) {
                if (isMaxDecimalCharsPropSet() && getNumCharsAfterDecimal(value) > maxDecimalChars) {
                    int lastIndex = value.length() - 1;
                    value = removeCharAt(value, lastIndex);
                    if (start == lastIndex) {
                        start--;
                    }
                }
            }
        }

        String filtered = filterInput(value);

        editText.removeTextChangedListener(this);
        editText.setText(filtered);

        int diff = filtered.length() - value.length();

        int cursorPos = start + diff + count;
        if (cursorPos < 0)
            cursorPos = 0;

        if (hasCurrencyString() && cursorPos < currencyString.length())
            cursorPos = cursorPos + (currencyString.length() - cursorPos);

        editText.setSelection(cursorPos);

        editText.addTextChangedListener(this);
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private boolean isMaxDecimalCharsPropSet() {
        return maxDecimalChars > -1;
    }

    private String removeCharAt(String s, int index) {
        if (index < 0 || index >= s.length())
            return s;

        StringBuilder sb = new StringBuilder(s);
        return sb.deleteCharAt(index).toString();
    }

    private boolean hasCurrencyString() {
        return !TextUtils.isEmpty(currencyString);
    }

    /**
     * Returns a string of accepted characters in the number input field.
     *
     * @return
     */
    public String getAcceptedInputs() {
        return NumberFormatterTextWatcher.DIGITS + getDecimalChar() + getGroupingChar();
    }

    /**
     * Gets the decimal character used by the specified Locale.
     *
     * @return
     */
    public char getDecimalChar() {
        return DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
    }

    /**
     * Gets the grouping character used by the specified Locale.
     *
     * @return
     */
    public char getGroupingChar() {
        return DecimalFormatSymbols.getInstance(locale).getGroupingSeparator();
    }

    /**
     * Used to limit the number of decimal places that should be allowed.
     *
     * @param maxDecimalChars
     */
    public void setMaxDecimalChars(int maxDecimalChars) {
        this.maxDecimalChars = maxDecimalChars;
    }

    public void setInputListener(@NonNull InputListener inputListener) {
        this.inputListener = inputListener;
    }

    public interface InputListener {
        /**
         * Called when the input changes with a valid value
         *
         * @param unformattedValue The unformatted number string
         * @param formattedValue   The formatted number string
         */
        void onChange(String unformattedValue, String formattedValue);
    }
}
