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

    NumberInputFormatter.Builder numberInputBuilder;
    NumberInputFormatter numberInputFormatter;

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

        numberInputBuilder = new NumberInputFormatter.Builder();
                //.formatInput(false)
                //.showCurrency(true, Currency.getInstance(Locale.getDefault()).getCurrencyCode());
        numberInputFormatter = numberInputBuilder.buildFor(inputEditText);
        numberInputFormatter.setInputListener(new NumberFormatterTextWatcher.InputListener() {
            @Override
            public void onChange(String unformattedValue, String formattedValue) {
                mUnformatted = unformattedValue;
                mFormatted = formattedValue;
            }
        });
        numberInputFormatter.setup(savedInstanceState != null);
    }

    private void showOutput(String input) {
        if (TextUtils.isEmpty(input)) {
            outputTextView.setText("");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Unformatted: ").append(mUnformatted).append("\n")
                .append("Formatted: ").append(mFormatted);

        outputTextView.setText(sb);

    }
}
