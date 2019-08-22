package com.example.numberformatlab;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.ikmich.localeaware.LocaleAwareCurrencyInput;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvOutput;
    private EditText etInput;

    LocaleAwareCurrencyInput.Builder builder;
    LocaleAwareCurrencyInput localeAwareCurrencyInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TextView tvLocale = findViewById(R.id.tv_locale);
        tvOutput = findViewById(R.id.tv_output);
        etInput = findViewById(R.id.et_input);

        // Display the device locale
        Locale locale = Locale.getDefault();
        tvLocale.setText(String.format("%s, %s",
                locale.getCountry(), locale.getLanguage()));

        Button btnShowOutput = findViewById(R.id.btn_show_output);
        btnShowOutput.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOutput(etInput.getText().toString());
            }
        });

        builder = new LocaleAwareCurrencyInput.Builder()
                .formatInput(true);
        // .showCurrency(false);
        localeAwareCurrencyInput = builder.buildFor(etInput);
        localeAwareCurrencyInput.setup(savedInstanceState != null);
    }

    private void showOutput(String input) {
        // if (TextUtils.isEmpty(input)) {
        //     tvOutput.setText("");
        //     return;
        // }

        // tvOutput.setText(etInput.getText());
    }
}
