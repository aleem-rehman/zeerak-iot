package com.example.arehman.iot_v01;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.support.v4.content.ContextCompat;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;

public class MotorAdapter extends BaseAdapter {
    private Context mContext;
    private HttpHandler httpHandler = new HttpHandler();
    private String operation;
    private TimerTask timerTask;
    private Timer timer;
    Typeface font;


    private ToggleButton buttonMotor;
    private Spinner spinnerMotorSpellLength;
    private TextView textMinutes;
    private ProgressBar progressBarMotor;

    private boolean internalChange;

    private static final String MOTOR_IP = "192.168.15.215";
    private static final String MANUAL = "DIY";

//    private static String MOTOR_IP = "192.168.122.1:8080";
//    public static String DEBUG_ALWAYS_ON = "192.168.122.1:8080";
//    public static String DEBUG_ALWAYS_OFF = "10.42.0.1:8080";

    public void turnOnStateRefresher() {
        if (timerTask != null) {
            timerTask.cancel();
        }

        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
                new StateRefresher().execute();
            }
        };
        timer.schedule(timerTask, 0, 1000 * 5); // Check state every 15 seconds.
    }

    public void turnOffStateRefresher() {
        if (timerTask != null) {
            timerTask.cancel();
        }
    }

    public MotorAdapter(Context c) {
        mContext = c;
    }

    public int getCount() {
        return 1;
    }

    public Object getItem(int position) {
        return this;
    }

    public long getItemId(int position) {
        return 0;
    }

    View view;

    public View getView(int position, View convertView, ViewGroup parent) {

        if (view == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);
            view = layoutInflater.inflate(R.layout.activity_device, null);
            setupView(view);
        }
        return view;
    }

    private void setupView(View view) {
        buttonMotor = (ToggleButton) view.findViewById(R.id.toggleButtonMotor);
        spinnerMotorSpellLength = (Spinner) view.findViewById(R.id.spell_lengths);
        textMinutes = (TextView) view.findViewById(R.id.textViewMins);
        progressBarMotor = (ProgressBar) view.findViewById(R.id.progressMotorSpell);

        /*** Set Event Handlers***/
        buttonMotor.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                if (internalChange) return;
                try {
                    if (isChecked) {
                        new SendCommandJob().execute("ON_" + toggleButton.getId(), spinnerMotorSpellLength.getSelectedItem().toString());
                    } else {
                        new SendCommandJob().execute("OFF_" + toggleButton.getId());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        spinnerMotorSpellLength.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (parent.getItemAtPosition(position).toString().toLowerCase().contains(MANUAL.toLowerCase())) {
                    textMinutes.setVisibility(android.view.View.INVISIBLE);
                } else if (parent.getVisibility() == android.view.View.VISIBLE) {
                    textMinutes.setVisibility(android.view.View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        /*** GUI Init ***/
        font = Typeface.createFromAsset(mContext.getAssets(), "fontawesome-webfont.ttf");
        buttonMotor.setTypeface(font, Typeface.BOLD);
        buttonMotor.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);

        // Set water pump spell lengths
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(mContext,
                R.array.spell_lengths_in_mins, R.layout.spinner_item);
        spinnerMotorSpellLength.setAdapter(adapter);
        spinnerMotorSpellLength.setSelection(adapter.getPosition("25"));

        spinnerMotorSpellLength.setEnabled(false);

        enableGUIForMotor(false);
        progressBarMotor.setVisibility(android.view.View.INVISIBLE);
        Toast.makeText(mContext, "Checking up current state of devices.", Toast.LENGTH_SHORT).show();
    }

    protected void updateGUIForMotorStarted() {
        spinnerMotorSpellLength.setVisibility(android.view.View.INVISIBLE);
        textMinutes.setVisibility(android.view.View.INVISIBLE);
        progressBarMotor.setVisibility(android.view.View.VISIBLE);
        internalChange = true;
        buttonMotor.setChecked(true);
        internalChange = false;
        adjustButtonTextColor();
    }

    protected void updateGUIForMotorStopped() {
        spinnerMotorSpellLength.setVisibility(android.view.View.VISIBLE);

        if (spinnerMotorSpellLength.getSelectedItem().toString().toLowerCase().contains(MANUAL.toLowerCase())) {
            textMinutes.setVisibility(android.view.View.INVISIBLE);
        } else {
            textMinutes.setVisibility(android.view.View.VISIBLE);
        }
        progressBarMotor.setVisibility(android.view.View.INVISIBLE);
        internalChange = true;
        buttonMotor.setChecked(false);
        internalChange = false;
        adjustButtonTextColor();
    }

    protected void enableGUIForMotor(boolean enable) {
        spinnerMotorSpellLength.setEnabled(enable);
        buttonMotor.setEnabled(enable);
        adjustButtonTextColor();
    }

    protected void adjustButtonTextColor() {

        boolean isChecked = buttonMotor.isChecked();
        boolean isEnabled = buttonMotor.isEnabled();

        if (!isChecked && isEnabled) {
            buttonMotor.setTextColor(ContextCompat.getColor(mContext, R.color.colorLightGreen));
        } else if (isChecked && isEnabled) {
            buttonMotor.setTextColor(ContextCompat.getColor(mContext, R.color.colorLightRed));
        } else if (!isEnabled) {
            buttonMotor.setTextColor(ContextCompat.getColor(mContext, R.color.colorDarkGrey));
        }

    }

    protected class StateRefresher extends AsyncTask<Object, Object, Object> {
        // Get devices current state and update its entry
        String state = "-1";

        @Override
        protected Object doInBackground(Object... params) {
            state = getMotorCurrentState();
            ((Activity) mContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    enableGUIForMotor(true);
                    if (state.equals("0")) {
                        updateGUIForMotorStopped();
                    } else if (state.equals("1")) {
                        updateGUIForMotorStarted();
                    } else if (state.equals("-1")) {
                        enableGUIForMotor(false);
                        displayStatusToast(state, "");
                    }
                    adjustButtonTextColor();
                }
            });
            return state;
        }
    }

    protected class SendCommandJob extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(final String[] params) {
            String answer = "";
            operation = "";

            try {
                String cmdURL = "";
                if (params[0].startsWith("ON")) {
//                    MOTOR_IP = DEBUG_ALWAYS_ON;
                    cmdURL = "http://" + MOTOR_IP + "/?pin=ON7";
                    if (params.length > 1 && !params[1].toLowerCase().contains(MANUAL.toLowerCase())) {
                        cmdURL = cmdURL.concat("&duration=" + params[1]);
                    }

                } else if (params[0].startsWith("OFF")) {
//                    MOTOR_IP = DEBUG_ALWAYS_OFF;
                    cmdURL = "http://" + MOTOR_IP + "/?pin=OFF7";
                }

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enableGUIForMotor(false);
                    }
                });

                HttpResponse response = httpHandler.sendGet(cmdURL);
                if (response.getResponseCode() == 200) {
                    ((Activity) mContext).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            enableGUIForMotor(true);
                            if (params[0].startsWith("ON")) {
                                updateGUIForMotorStarted();
                            } else if (params[0].startsWith("OFF")) {
                                updateGUIForMotorStopped();
                            }

                        }
                    });
                } else if (response.getResponseCode() == 400) {
                    if (response.getResponseMessage().contains("invalid_duration")) {
                        operation = "Invalid command. ";
                        throw new Exception(); // Error message display is handled in catch
                    }
                }

            } catch (Exception e) {

                e.printStackTrace();
                final String state = getMotorCurrentState();

                ((Activity) mContext).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        enableGUIForMotor(true);

                        if (params[0].startsWith("ON")) {
                            operation = operation.concat("Start motor operation failed. ");
                            updateGUIForMotorStopped();
                        } else if (params[0].startsWith("OFF")) {
                            operation = operation.concat("Stop motor operation failed. ");
                            updateGUIForMotorStarted();
                        }

                        adjustButtonTextColor();
                        displayStatusToast(state, operation);
                    }
                });

            }

            return answer;
        }

    }

    /**
     * Get if the motor pin is Hi or Low
     *
     * @return 1 if motor is running, 0 if nto running, -1 for undefined codition.
     */
    protected String getMotorCurrentState() {
        String state = "-1";
        try {
            HttpResponse response = httpHandler.sendGet("http://" + MOTOR_IP + "/?pin=STATUS7");
            if (response.getResponseMessage().contains("state=1")) {
                state = "1";
            } else if (response.getResponseMessage().contains("state=0")) {
                state = "0";
            }

        } catch (Exception e) {
        }
        return state;
    }

    protected void displayStatusToast(String state, String messagePrefix) {
        if (state.equals("-1")) {
            Toast.makeText(mContext, messagePrefix + "Cannot determine motor state. Check your connectivity.", Toast.LENGTH_LONG).show();
        } else if (state.equals("0")) {
            Toast.makeText(mContext, messagePrefix + "Motor is currently stopped", Toast.LENGTH_LONG).show();
        } else if (state.equals("1")) {
            Toast.makeText(mContext, messagePrefix + "Motor is currently running", Toast.LENGTH_LONG).show();
        }
    }

}