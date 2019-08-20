package com.example.numberformatlab;

import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.DigitsKeyListener;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    TextView tvOutput;
    EditText etInput;
    Button btnShowOutput;
    private static final String DIGITS = "0123456789";

    LocaleCurrencyInput localeCurrencyInput;

    class InputWatcher implements TextWatcher {
        private boolean hasDecimalSeparator = false;
        private int prevLen = 0;
        private char prevChar;

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            hasDecimalSeparator = s.toString().indexOf(getDecimalChar()) > -1;
            prevLen = s.length();

            if (start >= 0 && start < s.length()) {
                prevChar = s.charAt(start);
            }
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            int len = s.length();
            boolean deleting = len < prevLen;
            char inputChar;
            String str = s.toString();

            if (!deleting) {
                inputChar = s.charAt(start);
                if (hasDecimalSeparator && inputChar == getDecimalChar()) {
                    // Remove repeat decimal separator character.
                    etInput.removeTextChangedListener(this);
                    String s1 = removeCharAt(str, start);
                    etInput.setText(s1);
                    etInput.setSelection(start);
                    etInput.addTextChangedListener(this);
                }
            } else {
                if (prevChar == getDecimalChar()) {
                    return;
                }
            }

            String rex1 = String.format("\\%s$", getDecimalChar());
            boolean endsInDecimal = Pattern.compile(rex1).matcher(str).find();

            String rex2 = String.format("\\%s\\d*0+$", getDecimalChar());
            boolean endsInZeroAfterDecimal = Pattern.compile(rex2).matcher(str).find();

            boolean formattingAllowed = !endsInDecimal && !endsInZeroAfterDecimal;
            if (!formattingAllowed) {
                return;
            }

            etInput.removeTextChangedListener(this);

            String formatted = formatCurrencyInput(str);
            etInput.setText(formatted);

            int diff = formatted.length() - len;
            int selPos = start + diff;
            int buffer = deleting ? 0 : 1;

            if (start < s.length()) {
                etInput.setSelection(selPos + buffer);
            } else {
                etInput.setSelection(selPos);
            }

            etInput.addTextChangedListener(this);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    }

    private String removeCharAt(String s, int index) {
        StringBuilder sb = new StringBuilder(s);
        return sb.deleteCharAt(index).toString();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        localeCurrencyInput = new LocaleCurrencyInput();

        tvOutput = findViewById(R.id.tv_output);
        etInput = findViewById(R.id.et_input);
        btnShowOutput = findViewById(R.id.btn_show_output);

        btnShowOutput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOutput(etInput.getText().toString());
            }
        });

        etInput.setText("");
        etInput.setInputType(
                InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_FLAG_DECIMAL
                        | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        etInput.setKeyListener(DigitsKeyListener.getInstance(getAcceptedInputs()));
        etInput.addTextChangedListener(new InputWatcher());

        localeCurrencyInput.init(etInput);
    }

    private String getAcceptedInputs() {
        return DIGITS + getDecimalChar(); // + getGroupingSeparator();
    }

    private char getDecimalChar() {
        return DecimalFormatSymbols.getInstance().getDecimalSeparator();
    }

    private char getGroupingSeparator() {
        return DecimalFormatSymbols.getInstance().getGroupingSeparator();
    }

    private String formatCurrencyInput(String input) {
        try {
            DecimalFormat df = (DecimalFormat) NumberFormat.getInstance();
            df.setMaximumFractionDigits(10);
            Number number = df.parse(input);
            return df.format(number);
        } catch (ParseException | ClassCastException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    private void showOutput(String input) {
        if (TextUtils.isEmpty(input)) {
            tvOutput.setText("");
            return;
        }

        tvOutput.setText(formatCurrencyInput(input));
    }
}
