package pl.eselter.aaphenotypepatcher;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.apps_list:
                Intent intent = new Intent(this, AppsList.class);
                this.startActivity(intent);
            break;

            case R.id.copy:
                ClipboardManager clipboard = (ClipboardManager)
                        getSystemService(Context.CLIPBOARD_SERVICE);
                TextView textView = findViewById(R.id.logs);
                ClipData clip = ClipData.newPlainText("logs", textView.getText());
                clipboard.setPrimaryClip(clip);
            break;

            case R.id.reboot:
                DialogFragment rebootDialog = new RebootDialog();
                rebootDialog.show(getSupportFragmentManager(), "RebootDialog");
            break;

            case R.id.trigger_manager:
                copyAssets();
                DialogFragment triggerManagerDialog = new TriggerManagerDialog();
                triggerManagerDialog.show(getSupportFragmentManager(), "triggerManagerDialog");
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu( Menu menu )
    {
        getMenuInflater().inflate( R.menu.menu, menu );
        return true;
    }

    public void patch(final View view) {
        final TextView logs = findViewById(R.id.logs);
        logs.setHorizontallyScrolling(true);
        logs.setMovementMethod(new ScrollingMovementMethod());
        logs.setText(null);

        SharedPreferences appsListPref = getApplicationContext().getSharedPreferences("appsListPref", 0);
        Map<String, ?> allEntries = appsListPref.getAll();
        if (allEntries.isEmpty()) {
            Intent intent = new Intent(this, AppsList.class);
            this.startActivity(intent);
            Toast.makeText(getApplicationContext(), "Choose apps to whitelist.", Toast.LENGTH_LONG).show();
        } else {
            logs.append("--  Apps which will be added to whitelist: --\n");
            String whiteListString = "";
            for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
                logs.append("\t\t- " + entry.getValue() + " (" + entry.getKey() + ")\n");
                whiteListString += "," + entry.getKey();
            }

            whiteListString = whiteListString.replaceFirst(",", "");
            final String whiteListStringFinal = whiteListString;

            new Thread() {
                @Override
                public void run() {
                    String path = getApplicationInfo().dataDir;
                    boolean suitableMethodFound = true;
                    copyAssets();

                    appendText(logs, "\n\n-- Drop Triggers  --");
                    appendText(logs, runSuWithCmd(
                            path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                    "'DROP TRIGGER after_delete;'"
                    ).getStreamLogsWithLabels());

                    appendText(logs, "\n\n--  DELETE old Flags  --");
                    appendText(logs, runSuWithCmd(
                            path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                    "'DELETE FROM Flags WHERE name=\"app_white_list\";'"
                    ).getStreamLogsWithLabels());

                    if (runSuWithCmd(
                            path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                    "'SELECT 1 FROM Packages WHERE packageName=\"com.google.android.gms.car#car\"'").getInputStreamLog().trim().equals("1")) {

                        appendText(logs, "\n\n--  run SQL method #1  --");
                        appendText(logs, runSuWithCmd(
                                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                        "'INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", 234, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", 230, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 234, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 230, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car_setup\", 234, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car_setup\", 230, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", (SELECT version FROM Packages WHERE packageName=\"com.google.android.gms.car#car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", (SELECT version FROM Packages WHERE packageName=\"com.google.android.gms.car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car_setup\", (SELECT version FROM Packages WHERE packageName=\"com.google.android.gms.car#car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);'"
                        ).getStreamLogsWithLabels());

                        appendText(logs, runSuWithCmd(
                                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                        "'CREATE TRIGGER after_delete AFTER DELETE\n" +
                                        "ON Flags\n" +
                                        "BEGIN\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", (SELECT version FROM Packages WHERE packageName=\"com.google.android.gms.car#car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", 230, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", 234, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", (SELECT version FROM Packages WHERE packageName=\"com.google.android.gms.car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 230, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 234, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car_setup\", (SELECT version FROM Packages WHERE packageName=\"com.google.android.gms.car#car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car_setup\", 230, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car_setup\", 234, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "END;'"
                        ).getStreamLogsWithLabels());

                        appendText(logs, "\n--  end SQL method #1  --");

                    } else if (runSuWithCmd(
                            path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                    "'SELECT 1 FROM Packages WHERE packageName=\"com.google.android.gms.car\"'").getInputStreamLog().trim().equals("1")) {

                        appendText(logs, "\n\n--  run SQL method #2  --");
                        appendText(logs, runSuWithCmd(
                                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                        "'INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", 234, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", 230, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 234, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 230, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car_setup\", 234, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car_setup\", 230, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", (SELECT version FROM Packages WHERE packageName=\"com.google.android.gms.car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", (SELECT version FROM Packages WHERE packageName=\"com.google.android.gms.car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car_setup\", (SELECT version FROM Packages WHERE packageName=\"com.google.android.gms.car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);'"
                        ).getStreamLogsWithLabels());

                        appendText(logs, runSuWithCmd(
                                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                        "'CREATE TRIGGER after_delete AFTER DELETE\n" +
                                        "ON Flags\n" +
                                        "BEGIN\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", (SELECT version FROM Packages WHERE packageName=\"com.google.android.gms.car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", 230, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", 234, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", (SELECT version FROM Packages WHERE packageName=\"com.google.android.gms.car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 230, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 234, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car_setup\", (SELECT version FROM Packages WHERE packageName=\"com.google.android.gms.car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car_setup\", 230, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car_setup\", 234, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "END;'"
                        ).getStreamLogsWithLabels());
                        appendText(logs, "\n--  end SQL method #2  --");

                    } else if (runSuWithCmd(
                            path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                    "'SELECT 1 FROM ApplicationStates WHERE packageName=\"com.google.android.gms.car#car\"'").getInputStreamLog().trim().equals("1")) {

                        appendText(logs, "\n\n--  run SQL method #3  --");
                        appendText(logs, runSuWithCmd(
                                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                        "'INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", 240, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", (SELECT version FROM ApplicationStates WHERE packageName=\"com.google.android.gms.car#car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 240, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", (SELECT version FROM ApplicationStates WHERE packageName=\"com.google.android.gms.car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);'"
                        ).getStreamLogsWithLabels());

                        appendText(logs, runSuWithCmd(
                                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                        "'CREATE TRIGGER after_delete AFTER DELETE\n" +
                                        "ON Flags\n" +
                                        "BEGIN\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", 240, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car#car\", (SELECT version FROM ApplicationStates WHERE packageName=\"com.google.android.gms.car#car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 240, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", (SELECT version FROM ApplicationStates WHERE packageName=\"com.google.android.gms.car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "END;'"
                        ).getStreamLogsWithLabels());
                        appendText(logs, "\n--  end SQL method #3  --");

                    } else if (runSuWithCmd(
                            path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                    "'SELECT 1 FROM ApplicationStates WHERE packageName=\"com.google.android.gms.car\"'").getInputStreamLog().trim().equals("1")) {

                        appendText(logs, "\n\n--  run SQL method #4  --");
                        appendText(logs, runSuWithCmd(
                                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                        "'INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 240, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", (SELECT version FROM ApplicationStates WHERE packageName=\"com.google.android.gms.car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);'"
                        ).getStreamLogsWithLabels());

                        appendText(logs, runSuWithCmd(
                                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                        "'CREATE TRIGGER after_delete AFTER DELETE\n" +
                                        "ON Flags\n" +
                                        "BEGIN\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", 240, 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "INSERT OR REPLACE INTO Flags (packageName, version, flagType, partitionId, user, name, stringVal, committed) VALUES (\"com.google.android.gms.car\", (SELECT version FROM ApplicationStates WHERE packageName=\"com.google.android.gms.car\"), 0, 0, \"\", \"app_white_list\", \"" + whiteListStringFinal + "\",1);\n" +
                                        "END;'"
                        ).getStreamLogsWithLabels());
                        appendText(logs, "\n--  end SQL method #4  --");

                    } else {
                        suitableMethodFound = false;
                        appendText(logs, "\n\n--  Suitable method NOT found!  --");
                    }

                    // Check Start
                    if (suitableMethodFound) {
                        StreamLogs checkStep1 = runSuWithCmd(
                                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                        "'SELECT * FROM Flags WHERE name=\"app_white_list\";'"
                        );
                        String[] checkStep1Sorted = checkStep1.getInputStreamLog().split("\n");
                        Arrays.sort(checkStep1Sorted);

                        String checkStep1SortedToString = "";
                        for (String s : checkStep1Sorted) {
                            checkStep1SortedToString += "\n" + s;
                        }
                        checkStep1SortedToString.replaceFirst("\n", "");
                        checkStep1.setInputStreamLog(checkStep1SortedToString);

                        appendText(logs, "\n\n--  Check (1/3)  --" + checkStep1.getStreamLogsWithLabels());

                        appendText(logs, "\n--  Check (2/3)  --" + runSuWithCmd(
                                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                        "'DELETE FROM Flags WHERE name=\"app_white_list\";'"
                        ).getStreamLogsWithLabels());

                        StreamLogs checkStep3 = runSuWithCmd(
                                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                        "'SELECT * FROM Flags WHERE name=\"app_white_list\";'"
                        );
                        String[] checkStep3Sorted = checkStep3.getInputStreamLog().split("\n");
                        Arrays.sort(checkStep3Sorted);

                        String checkStep3SortedToString = "";
                        for (String s : checkStep3Sorted) {
                            checkStep3SortedToString += "\n" + s;
                        }
                        checkStep3SortedToString.replaceFirst("\n", "");
                        checkStep3.setInputStreamLog(checkStep3SortedToString);

                        appendText(logs, "\n--  Check (3/3)  --" + checkStep3.getStreamLogsWithLabels());

                        if (checkStep1.getInputStreamLog().length() == checkStep3.getInputStreamLog().length()) {
                            appendText(logs, "\n\n--  Check seems OK :)  --");

                            DialogFragment newFragment = new RebootDialog();
                            newFragment.show(getSupportFragmentManager(), "reboot");
                        } else {
                            appendText(logs, "\n\n--  Check NOT OK.  --");
                            appendText(logs, "\n     Length before delete and after was not equal.");
                            appendText(logs, "\n        Before: " + checkStep1.getInputStreamLog().length());
                            appendText(logs, "\n        After:  " + checkStep3.getInputStreamLog().length());
                        }
                    }
                    // Check End
                }
            }.start();
        }
    }

    public static StreamLogs runSuWithCmd(String cmd) {
        DataOutputStream outputStream = null;
        InputStream inputStream = null;
        InputStream errorStream = null;

        StreamLogs streamLogs = new StreamLogs();
        streamLogs.setOutputStreamLog(cmd);

        try{
            Process su = Runtime.getRuntime().exec("su");
            outputStream = new DataOutputStream(su.getOutputStream());
            inputStream = su.getInputStream();
            errorStream = su.getErrorStream();
            outputStream.writeBytes(cmd + "\n");
            outputStream.flush();

            outputStream.writeBytes("exit\n");
            outputStream.flush();

            try {
                su.waitFor();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            streamLogs.setInputStreamLog(readFully(inputStream));
            streamLogs.setErrorStreamLog(readFully(errorStream));
        } catch (IOException e){
            e.printStackTrace();
        }

        return streamLogs;
    }

    public static String readFully(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length = 0;
        while ((length = is.read(buffer)) != -1) {
            baos.write(buffer, 0, length);
        }
        return baos.toString("UTF-8");
    }

    private void copyAssets() {
        String path = getApplicationInfo().dataDir;
        final TextView logs = findViewById(R.id.logs);
        File file = new File(path, "sqlite3");
        if (!file.exists()) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    logs.setText("\n--  Copy sqlite3 to data directory  --");
                }
            });
            InputStream in;
            OutputStream out;
            try {
                in = this.getResources().openRawResource(R.raw.sqlite3);

                String outDir = getApplicationInfo().dataDir;

                File outFile = new File(outDir, "sqlite3");

                out = new FileOutputStream(outFile);
                copyFile(in, out);
                in.close();
                out.flush();
                out.close();
            } catch(IOException e) {
                Log.e("eselter", "Failed to copy asset file: sqlite3", e);
            }
        }
        appendText(logs, "\n--  chmod 775 sqlite3  --");
        appendText(logs, runSuWithCmd("chmod 775 " + path + "/sqlite3").getStreamLogsWithLabels());
        appendText(logs, "\n--  end chmod 775 sqlite3  --");
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    private void appendText(final TextView textView, final String s){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textView.append(s);
            }
        });
    }
}
