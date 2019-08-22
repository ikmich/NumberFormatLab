package com.ikmich.localeaware;

import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class LocaleAwareCurrencyInput {

    private EditText et;
    private LocaleAwareCurrencyWatcher inputWatcher;
    private Builder mBuilder;
    private Locale mLocale;
    private static final String DIGITS = "-0123456789";
    private String unformattedValue;

    private LocaleAwareCurrencyInput(EditText editText, @NonNull Locale locale) {
        et = editText;
        mLocale = locale;
        inputWatcher = new LocaleAwareCurrencyWatcher();
    }

    /**
     * Called to setup the Locale-aware behaviour.
     *
     * @param clearField Whether to clear the EditText field's contents or not. Pass true
     *                   in the event of a configuration change. i.e. when
     *                   savedInstanceState != null
     */
    public void setup(final boolean clearField) {
        et.post(new Runnable() {
            @Override
            public void run() {
                if (clearField) {
                    et.setText(null);
                }

                String contents = et.getText().toString();
                et.setText(String.format("%s%s", getCurrencyString(), contents));
                et.setKeyListener(DigitsKeyListener.getInstance(getAcceptedInputs()));

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

                et.addTextChangedListener(inputWatcher);
            }
        });
    }

    private void attachBuilder(@NonNull Builder builder) {
        mBuilder = builder;
    }

    private class LocaleAwareCurrencyWatcher implements TextWatcher {
        private boolean hasDecimalSeparator;
        private int startLen = 0;

        LocaleAwareCurrencyWatcher() {
        }

        @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
        private String filter(String input) {
            if (input == null) {
                input = "";
            }

            // Remove currency string
            String quote = Pattern.quote(String.format("%s", getCurrencyString()));
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
                return getCurrencyString() + input;
            }

            // Remove disallowed items
            StringBuilder sb = new StringBuilder();
            List<Character> allowedChars = Arrays.asList(getDecimalChar());
            for (char c : input.toCharArray()) {
                if (allowedChars.contains(c) || DIGITS.indexOf(c) > -1) {
                    sb.append(c);
                }
            }
            input = sb.toString();
            unformattedValue = input;

            if (mBuilder.shouldFormatText) {
                // Get number of digits after decimal???
                try {
                    NumberFormat nf = NumberFormat.getInstance(mLocale);
                    nf.setMaximumFractionDigits(9);
                    Number number = nf.parse(input);
                    input = nf.format(number);
                } catch (ParseException | ClassCastException e) {
                    e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            return getCurrencyString() + input;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            hasDecimalSeparator = s.toString().indexOf(getDecimalChar()) > -1;
            startLen = s.length();
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            String str = s.toString();
            boolean isDelete = str.length() < startLen;

            if (!isDelete && str.length() == 0) {
                return;
            }

            if (!isDelete) {
                char inputChar = str.charAt(start);
                if (hasDecimalSeparator && inputChar == getDecimalChar()) {
                    // Prevent repeated decimal separator character.
                    str = removeCharAt(str, start);
                    if (start > 0) {
                        // Adjust the 'start' pointer, since an item has been removed from the input
                        start--;
                    }
                }
            }

            String filtered = filter(str);

            et.removeTextChangedListener(this);

            et.setText(filtered);

            int diff = filtered.length() - str.length();
            int selPos = start + diff;
            int gap = isDelete ? 0 : 1;
            if (start < s.length()) {
                et.setSelection(selPos + gap);
            } else {
                et.setSelection(selPos);
            }

            et.addTextChangedListener(this);

            Log.d("dibug", "filtered: " + filtered);
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
        return unformattedValue;
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

        public LocaleAwareCurrencyInput buildFor(EditText editText) {
            LocaleAwareCurrencyInput inputFormatter = new LocaleAwareCurrencyInput(editText, mLocale);
            inputFormatter.attachBuilder(this);
            return inputFormatter;
        }
    }
}
