package com.example.numberformatlab;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import com.ikmich.cif.CurrencyInputFormatter;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvOutput;
    private EditText etInput;

    CurrencyInputFormatter currencyInputFormatter;

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

        CurrencyInputFormatter.Builder builder = new CurrencyInputFormatter.Builder()
                /*.dontFormat()*/
                /*.showCurrency(true)*/;
        currencyInputFormatter = builder.buildFor(etInput);
        currencyInputFormatter.setup();
    }


    private void showOutput(String input) {
        if (TextUtils.isEmpty(input)) {
            tvOutput.setText("");
            return;
        }

        // tvOutput.setText(formatCurrencyInput(input));
        // tvOutput.setText(currencyInputFormatter.getFormattedValue());
    }
}
