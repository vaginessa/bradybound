package com.oxplot.bradybound;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;

import android.app.Application;
import android.util.Log;

public class BradyBoundApplication extends Application {

  public static final int SHELL_OK = 0;
  public static final int SHELL_UNAVAILABLE = 1;
  public static final int SHELL_CMD_FAILED = 2;

  private static final String TEXT_ENC = "UTF-8";
  private static final String CHECK_LABEL = "BRADYBOUND";
  private static final String SHELL_CMD = "su";

  private static final String TAG = "App";

  public int installInboundShaper(int speed) {
    return runInShell(SHELL_CMD, new ShellScript[] { new UninstallScript(),
        new InstallScript(speed) });
  }

  public int uninstallInboundShaper() {
    return runInShell(SHELL_CMD, new ShellScript[] { new UninstallScript() });
  }

  private int runInShell(String shell, ShellScript... scripts) {
    Process proc = null;
    try {
      proc = Runtime.getRuntime().exec(new String[] { shell });
      OutputStreamWriter outs = new OutputStreamWriter(proc.getOutputStream(),
          TEXT_ENC);
      BufferedReader ins = new BufferedReader(new InputStreamReader(
          proc.getInputStream(), TEXT_ENC));

      // Check if we have access to this shell to run commands

      outs.write("exec 2>/dev/null\necho " + CHECK_LABEL + "\n");
      outs.flush();

      if (!CHECK_LABEL.equals(ins.readLine())) {
        Log.e(TAG, "access to shell denied");
        return SHELL_UNAVAILABLE;
      }

      // Run the shell scripts

      for (ShellScript script : scripts) {
        int ret = script.run(ins, outs);
        if (ret != SHELL_OK)
          return ret;
      }

      return SHELL_OK;

    } catch (IOException e) {
      Log.e(TAG, "access to shell denied/unavailable", e);
      return SHELL_UNAVAILABLE;
    } finally {
      try {
        proc.getOutputStream().close();
      } catch (Exception e) {}
    }
  }

  private static interface ShellScript {
    int run(BufferedReader ins, OutputStreamWriter outs) throws IOException;
  }

  private static class InstallScript implements ShellScript {

    private static final double KB_PER_PACKET = 1.3;
    private int packetThresh;

    public InstallScript(int speed) {
      packetThresh = Math.max((int) Math.round(speed / KB_PER_PACKET), 1);
    }

    @Override
    public int run(BufferedReader ins, OutputStreamWriter outs)
        throws IOException {
      String[] vars = new String[] { "iptables|INPUT|30|",
          "iptables|FORWARD|30|-d 192.0.0.0/8", "ip6tables|INPUT|40|" };
      for (String var : vars) {
        String[] parts = var.split("\\|", -1);
        String cmd = parts[0] + " -I " + parts[1] + " 1 " + parts[3]
            + " -m state --state ESTABLISHED -p 6 -m length --length "
            + parts[2] + ":10000 -m hashlimit --hashlimit-name " + CHECK_LABEL
            + " --hashlimit-above " + packetThresh
            + "/s -j DROP >/dev/null && echo " + CHECK_LABEL + "\n";
        outs.write(cmd);
        outs.flush();
        if (!CHECK_LABEL.equals(ins.readLine())) {
          new UninstallScript().run(ins, outs);
          return SHELL_CMD_FAILED;
        }
      }
      return SHELL_OK;
    }

  }

  private static class UninstallScript implements ShellScript {
    @Override
    public int run(BufferedReader ins, OutputStreamWriter outs)
        throws IOException {
      for (String iptables : new String[] { "iptables", "ip6tables" }) {
        outs.write(iptables + " -S; echo; echo " + CHECK_LABEL + "\n");
        outs.flush();
        for (String rule : getOutputUntil(ins, CHECK_LABEL)) {
          if (!rule.contains(CHECK_LABEL))
            continue;
          outs.write(iptables + " -D " + rule.replaceFirst("^-A ", "")
              + " >/dev/null \n");
        }
        outs.flush();
      }
      return SHELL_OK;
    }
  }

  private static Collection<String> getOutputUntil(BufferedReader ins,
      String stopLine) throws IOException {
    ArrayList<String> lines = new ArrayList<String>();
    while (true) {
      String line = ins.readLine();
      if (line == null || stopLine.equals(line))
        break;
      lines.add(line);
    }
    return lines;
  }
}
