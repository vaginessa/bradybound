package com.oxplot.bradybound;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class ControlPanelActivity extends Activity implements
    OnSeekBarChangeListener, OnClickListener {

  private static final String PREF_INBOUND_SPEED = "inbound_speed";
  private static final int INBOUND_SEEKBAR_STEPS = 20000;

  private SeekBar inboundSpeedSeekbar;
  private Button setButton;
  private Button unsetButton;
  private TextView inboundSpeedLabel;
  private SharedPreferences prefs;
  private BradyBoundApplication app;

  private int minSpeed;
  private int maxSpeed;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    minSpeed = Integer.parseInt(getString(R.string.config_min_inbound_speed));
    maxSpeed = Integer.parseInt(getString(R.string.config_max_inbound_speed));

    app = ((BradyBoundApplication) getApplication());

    setContentView(R.layout.activity_control_panel);
    inboundSpeedSeekbar = (SeekBar) findViewById(R.id.speed_seekbar);
    inboundSpeedLabel = (TextView) findViewById(R.id.inbound_speed);
    setButton = (Button) findViewById(R.id.set_button);
    unsetButton = (Button) findViewById(R.id.unset_button);

    prefs = PreferenceManager.getDefaultSharedPreferences(this);
    inboundSpeedSeekbar.setOnSeekBarChangeListener(this);
    inboundSpeedSeekbar.setMax(INBOUND_SEEKBAR_STEPS);

    setButton.setOnClickListener(this);
    unsetButton.setOnClickListener(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
  }

  @Override
  protected void onResume() {
    super.onResume();
    int inboundSpeed = prefs.getInt(PREF_INBOUND_SPEED, -1);
    if (inboundSpeed == -1) {
      inboundSpeed = Integer
          .parseInt(getString(R.string.config_def_inbound_speed));
      prefs.edit().putInt(PREF_INBOUND_SPEED, inboundSpeed).commit();
    }
    inboundSpeedSeekbar.setProgress(toLogSpeed(inboundSpeed));
    setButton.setEnabled(true);
  }

  private int toLogSpeed(int speed) {
    int max = inboundSpeedSeekbar.getMax();
    double logMax = Math.log10(maxSpeed - minSpeed + 10) - 1;
    double logDelta = Math.log10(speed - minSpeed + 10) - 1;
    return (int) Math.round((logDelta * max) / logMax);
  }

  private int fromLogSpeed(int speed) {
    int max = inboundSpeedSeekbar.getMax();
    double logMax = Math.log10(maxSpeed - minSpeed + 10) - 1;
    double nonLogSpeed = Math.pow(10, ((speed * logMax) / max) + 1) - 10
        + minSpeed;
    return (int) Math.round(nonLogSpeed);
  }

  private String toReadableSpeed(int speed) {
    if (speed < 1000)
      return String.format("%d KB/s", speed);
    else if (speed < 10000)
      return String.format("%.1f MB/s", speed / 1000.0);
    else
      return String.format("%d MB/s", speed / 1000);
  }

  @Override
  public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
    int nonLogSpeed = fromLogSpeed(progress);
    inboundSpeedLabel.setText(toReadableSpeed(nonLogSpeed));
    if (!fromUser)
      return;
    prefs.edit().putInt(PREF_INBOUND_SPEED, nonLogSpeed).commit();
    setButton.setEnabled(true);
    setButton.setTypeface(null, Typeface.BOLD);
  }

  @Override
  public void onStartTrackingTouch(SeekBar seekBar) {
  }

  @Override
  public void onStopTrackingTouch(SeekBar seekBar) {
  }

  @Override
  public void onClick(View button) {
    if (button == setButton) {
      if (!app.installInboundShaper(prefs.getInt(PREF_INBOUND_SPEED, -1))) {
        Toast.makeText(this, R.string.inbound_install_failed,
            Toast.LENGTH_SHORT).show();
        ;
      } else {
        setButton.setEnabled(false);
        setButton.setTypeface(null, Typeface.NORMAL);
      }
    } else if (button == unsetButton) {
      if (!app.uninstallInboundShaper()) {
        Toast.makeText(this, R.string.inbound_uninstall_failed,
            Toast.LENGTH_SHORT).show();
        ;
      } else {
        setButton.setEnabled(true);
      }
    }
  }
}
