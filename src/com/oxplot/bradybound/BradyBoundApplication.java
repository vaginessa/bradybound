package com.oxplot.bradybound;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import android.app.Application;
import android.util.Log;

public class BradyBoundApplication extends Application {

  private static final double KB_PER_PACKET = 1.3;

  private static final String TEXT_ENC = "UTF-8";
  private static final String IPROUTE_RULE_NAME = "BRADYBOUNDHN";

  private static final String INBOUND_SH = "inbound.sh";

  private static final String TAG = "App";

  public boolean installInboundShaper(int speed) {
    int packetEst = Math.max((int) Math.round(speed / KB_PER_PACKET), 1);
    if (!"1".equals(runInShell("su",
        getAssetAsString(INBOUND_SH, IPROUTE_RULE_NAME, packetEst, "install")))) {
      Log.e(TAG, "installInboundShaper failed");
      return false;
    }
    return true;
  }

  public boolean uninstallInboundShaper() {
    if (!"1".equals(runInShell("su",
        getAssetAsString(INBOUND_SH, IPROUTE_RULE_NAME, 0, "uninstall")))) {
      Log.e(TAG, "uninstallInboundShaper failed");
      return false;
    }
    return true;
  }

  private String runInShell(String shell, String script) {
    Process proc = null;
    try {
      proc = Runtime.getRuntime().exec(new String[] { shell });
      OutputStreamWriter outs = new OutputStreamWriter(proc.getOutputStream(),
          TEXT_ENC);
      InputStreamReader ins = new InputStreamReader(proc.getInputStream(),
          TEXT_ENC);
      char[] inBuf = new char[4096];

      // Check if we have access to this shell to run commands

      outs.write("echo -n T\n");
      outs.flush();
      if (ins.read(inBuf, 0, 1) != 1 || inBuf[0] != 'T') {
        Log.e(TAG, "access to shell denied");
        return null;
      }

      // We throw away stderr as not to fill up the error stream buffer

      outs.write("exec 2>/dev/null\n" + script + "\n");
      outs.close();

      // Read the shell output

      StringBuilder shellOutput = new StringBuilder();
      int bytesRead;
      while ((bytesRead = ins.read(inBuf, 0, inBuf.length)) > 0) {
        shellOutput.append(inBuf, 0, bytesRead);
      }

      return shellOutput.toString();

    } catch (IOException e) {
      Log.e(TAG, "access to shell denied/unavailable", e);
    } finally {
      if (proc != null)
        proc.destroy();
    }
    return null;
  }

  private String getAssetAsString(String name, Object... args) {
    try {
      char[] buffer = new char[4096];
      int bytesRead;
      StringBuilder output = new StringBuilder();
      InputStreamReader asset = new InputStreamReader(getAssets().open(name),
          TEXT_ENC);

      while ((bytesRead = asset.read(buffer, 0, buffer.length)) > 0)
        output.append(buffer, 0, bytesRead);

      asset.close();
      return String.format(output.toString(), args);
    } catch (IOException e) {
      return null;
    }
  }
}
