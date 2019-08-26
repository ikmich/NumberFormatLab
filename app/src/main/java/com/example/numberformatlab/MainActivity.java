package com.example.numberformatlab;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ikmich.numberformat.NumberFormatterTextWatcher;
import com.ikmich.numberformat.NumberInputFormatter;

import java.util.Currency;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView outputTextView;
    private EditText inputEditText;

    NumberInputFormatter.Builder formatterBuilder;
    NumberInputFormatter inputFormatter;

    String mFormatted = "";
    String mUnformatted = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvLocale = findViewById(R.id.tv_locale);
        outputTextView = findViewById(R.id.tv_output);
        inputEditText = findViewById(R.id.et_input);

        // Display the device locale
        Locale locale = Locale.getDefault();
        tvLocale.setText(String.format("%s, %s",
                locale.getCountry(), locale.getLanguage()));

        Button btnShowOutput = findViewById(R.id.btn_show_output);
        btnShowOutput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOutput(inputEditText.getText().toString());
            }
        });

        formatterBuilder = new NumberInputFormatter.Builder();
        // formatterBuilder.formatInput(false);
        formatterBuilder.showCurrency(true, getCurrencyString());

        inputFormatter = formatterBuilder.buildFor(inputEditText);
        inputFormatter.setInputListener(new NumberFormatterTextWatcher.InputListener() {
            @Override
            public void onChange(String unformattedValue, String formattedValue) {
                mUnformatted = unformattedValue;
                mFormatted = formattedValue;
                showOutput(inputEditText.getText().toString());
            }
        });

        inputFormatter.setup(savedInstanceState != null);

    }

    private String getCurrencyString() {
        return Currency.getInstance(Locale.getDefault()).getSymbol();
    }

    private void showOutput(String input) {
        if (TextUtils.isEmpty(input)) {
            outputTextView.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Unformatted: %s\n", mUnformatted))
                .append(String.format("Formatted: %s\n", mFormatted));

        outputTextView.setText(sb);
    }
}
