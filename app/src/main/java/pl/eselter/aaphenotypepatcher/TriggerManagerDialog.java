package pl.eselter.aaphenotypepatcher;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;

public class TriggerManagerDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String path = getActivity().getApplicationInfo().dataDir;
        final ArrayList<Integer> mSelectedItems = new ArrayList();  // Where we track the selected items
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Set the dialog title
        StreamLogs streamLogsTriggerList = MainActivity.runSuWithCmd(
                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                        "'SELECT name FROM sqlite_master WHERE type = \"trigger\";'"
        );
        final CharSequence[] triggersList = streamLogsTriggerList.getInputStreamLog().split("\n");
                // Specify the list array, the items to be selected by default (null for none),
                // and the listener through which to receive callbacks when items are selected

        if (streamLogsTriggerList.getInputStreamLog().isEmpty()) {
            builder.setMessage("Triggers not found.");
        } else {
            builder.setTitle("Choose trigger(s) to drop:");
            builder.setMultiChoiceItems(triggersList, null,
                    new DialogInterface.OnMultiChoiceClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which,
                                            boolean isChecked) {
                            if (isChecked) {
                                // If the user checked the item, add it to the selected items
                                mSelectedItems.add(which);
                            } else if (mSelectedItems.contains(which)) {
                                // Else, if the item is already in the array, remove it
                                mSelectedItems.remove(Integer.valueOf(which));
                            }
                        }
                    });
            // Set the action buttons
            builder.setPositiveButton("Drop", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // User clicked OK, so save the mSelectedItems results somewhere
                    // or return them to the component that opened the dialog


                    final TextView textViewLogs = getActivity().findViewById(R.id.logs);
                    Toast.makeText(getContext(), "Dropping...", Toast.LENGTH_SHORT).show();
                    textViewLogs.setText("--  DROP TRIGGERs  --");
                    for (Integer mSelectedItem : mSelectedItems) {
                        StreamLogs streamLogsDropTrigger = MainActivity.runSuWithCmd(
                                path + "/sqlite3 /data/data/com.google.android.gms/databases/phenotype.db " +
                                        "'DROP TRIGGER \"" + triggersList[mSelectedItem].toString() + "\";'"
                        );
                        textViewLogs.append(streamLogsDropTrigger.getStreamLogsWithLabels());
                    }
                }
            });
        }
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });

        return builder.create();
    }
}
