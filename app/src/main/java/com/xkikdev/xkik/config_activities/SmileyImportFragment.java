package com.xkikdev.xkik.config_activities;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.dd.processbutton.iml.ActionProcessButton;
import com.xkikdev.xkik.R;
import com.xkikdev.xkik.Settings;
import com.xkikdev.xkik.Util;
import com.xkikdev.xkik.kikSmiley;
import com.xkikdev.xkik.kikUtil;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import easyfilepickerdialog.kingfisher.com.library.model.DialogConfig;
import easyfilepickerdialog.kingfisher.com.library.model.SupportFile;
import easyfilepickerdialog.kingfisher.com.library.view.FilePickerDialogFragment;

public class SmileyImportFragment extends Fragment {
    ActionProcessButton imptxt;
    ActionProcessButton impdb;
    ActionProcessButton exptxt;
    private Settings settings;


    public SmileyImportFragment() {
        // Required empty public constructor
        try {
            settings = Settings.load(this.getActivity());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Make button show error & show toast
     *
     * @param b     Button to show on
     * @param c     Context for toast
     * @param error Error to show on toast
     */
    private void buttonError(ActionProcessButton b, Context c, String error) {
        b.setProgress(-1);
        setAllButtons(true);
        Toast.makeText(c, error, Toast.LENGTH_SHORT).show();
    }

    /**
     * Make button show error & show toast in UI thread
     *
     * @param main  Main acitivity
     * @param b     Button to show on
     * @param c     Context for toast
     * @param error Error to show on toast
     */
    private void buttonError(Activity main, final ActionProcessButton b, final Context c, final String error) {
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                b.setProgress(-1);
                setAllButtons(true);
                Toast.makeText(c, error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Set all buttons enabled/disabled
     *
     * @param to main activity
     */
    private void setAllButtons(boolean to) {
        imptxt.setEnabled(to);
        impdb.setEnabled(to);
        exptxt.setEnabled(to);
    }

    /**
     * Set all buttons enabled/disabled on main thread
     *
     * @param to   enabled/disabled
     * @param main main activity
     */
    private void setAllButtons(final boolean to, Activity main) {
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                imptxt.setEnabled(to);
                impdb.setEnabled(to);
                exptxt.setEnabled(to);
            }
        });
    }

    /**
     * Updates button
     *
     * @param progress progress
     * @param mode     button mode
     * @param b        button
     */
    private void updateImportProgress(int progress, ActionProcessButton.Mode mode, ActionProcessButton b, String text) {
        b.setMode(mode);
        b.setProgress(progress);
        b.setText(text);
    }

    /**
     * Updates button on main thread
     *
     * @param main     main activity
     * @param progress progress (int)
     * @param mode     button mode
     * @param b        button
     */
    private void updateImportProgress(Activity main, final int progress, final ActionProcessButton.Mode mode, final ActionProcessButton b, final String text) {
        main.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateImportProgress(progress, mode, b, text);
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_smiley_import, container, false);
        final Activity activity = this.getActivity();

        imptxt = (ActionProcessButton) v.findViewById(R.id.imptxt);
        impdb = (ActionProcessButton) v.findViewById(R.id.impdb);
        exptxt = (ActionProcessButton) v.findViewById(R.id.exptxt);
        imptxt.setMode(ActionProcessButton.Mode.ENDLESS);
        impdb.setMode(ActionProcessButton.Mode.ENDLESS);
        exptxt.setMode(ActionProcessButton.Mode.ENDLESS);

        imptxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                DialogConfig dialogConfig = new DialogConfig.Builder()
                        .supportFiles(new SupportFile(".txt", 0), new SupportFile(".xsmileydb", R.drawable.ic_insert_emoticon_black_24dp))
                        .build();
                new FilePickerDialogFragment.Builder()
                        .configs(dialogConfig)
                        .onFilesSelected(new FilePickerDialogFragment.OnFilesSelectedListener() {
                            @Override
                            public void onFileSelected(final List<File> list) {
                                new Thread() {
                                    @Override
                                    public void run() {
                                        File f = list.get(0);
                                        String data = null;
                                        try {
                                            data = FileUtils.readFileToString(f, "UTF-8");
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                        importFromString(data, activity, v, imptxt);
                                    }
                                }.start();
                            }
                        })
                        .build()
                        .show(((FragmentActivity) v.getContext()).getSupportFragmentManager(), null);
            }
        });

        impdb.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                impdb.setMode(ActionProcessButton.Mode.ENDLESS); // set to endless in case of slow processing
                impdb.setProgress(1); // activate endless maraquee
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            String out = IOUtils.toString(new URL("https://raw.githubusercontent.com/xkik-dev/xkik-smileydb/master/smileys.txt"), "UTF-8"); // github URL for database
                            importFromString(out, activity, v, impdb);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }.start();
            }
        });

        exptxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String out = "xsmileys:";
                for (kikSmiley k : settings.getSmileys()) {
                    out += k.getId();
                    if (!settings.getSmileys().get(settings.getSmileys().size() - 1).equals(k)) {
                        out += ",";
                    }
                }
                try {
                    String savedir = Settings.getSaveDir().getPath() + File.separator + "out.xsmileydb";
                    Util.writeToFile(out, savedir);
                    Toast.makeText(v.getContext(), "Saved to " + savedir, Toast.LENGTH_LONG).show();
                } catch (IOException e) {
                    // do something
                }
            }
        });

        exptxt.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(final View v) {
                new MaterialDialog.Builder(v.getContext())
                        .title("Store export")
                        .content("Are you sure?")
                        .positiveText("Yes")
                        .negativeText("No")
                        .onAny(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                if (which.equals(DialogAction.POSITIVE)) {
                                    new Thread() {
                                        @Override
                                        public void run() {
                                            int count = 0;
                                            Looper.prepare();
                                            try {
                                                String out = "xsmileys:";
                                                JSONObject shoplist = new JSONObject(IOUtils.toString(new URL("https://sticker-service.appspot.com/v1/home?debug=false"), "UTF-8"));
                                                JSONArray collections = shoplist.getJSONArray("collections");
                                                boolean first = false;
                                                for (int i = 0; i < collections.length(); i++) {
                                                    JSONArray smileys = collections.getJSONObject(i).getJSONArray("smileys");
                                                    for (int j = 0; j < smileys.length(); j++) {
                                                        String id = smileys.getJSONObject(j).getString("id");
                                                        count++;
                                                        if (!first) {
                                                            first = true;
                                                        } else {
                                                            out += ",";
                                                        }
                                                        out += id;
                                                        Log.i("xkik", "loaded smiley " + id);
                                                    }
                                                }
                                                Util.writeToFile(out, Settings.getSaveDir().getPath() + File.separator + "shop.xsmileydb");
                                                Toast.makeText(v.getContext(), "import " + count + " smileys from shop complete", Toast.LENGTH_SHORT).show();
                                            } catch (JSONException | IOException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }.start();
                                } else {
                                    dialog.cancel();
                                }
                            }
                        })
                        .show();
                return true;
            }
        });


        return v;
    }

    /**
     * @param data     data to import from
     * @param activity main activity
     * @param v        view
     * @param b        button to update progress on
     */
    private void importFromString(String data, final Activity activity, final View v, final ActionProcessButton b) {
        Looper.prepare();
        try {

            updateImportProgress(activity, 1, ActionProcessButton.Mode.ENDLESS, b, "Loading..");
            if (settings == null) {
                buttonError(b, v.getContext(), "Failed to load settings");
                return;
            }
            if (data.startsWith("xsmileys:") && data.contains(",")) { // simple check, just to prevent loading in the wrong file
                String dta = data.substring(9);
                String[] ids = dta.split(",");
                updateImportProgress(activity, 1, ActionProcessButton.Mode.PROGRESS, b, "Loading...");
                setAllButtons(false, activity);
                for (int i = 0; i < ids.length; i++) {
                    Float pct = (i / (ids.length * 1.0F)) * 100;
                    updateImportProgress(activity, (int) Math.floor(pct), ActionProcessButton.Mode.PROGRESS, b, "Import " + i + "/" + ids.length);
                    if (ids[i].contains("%0A") || ids[i].contains("%0a")) {
                        Log.i("xposed", "replaced");
                        ids[i] = ids[i].replace("%0A", "").replace("%0a", "");
                    }
                    if (!settings.containsSmiley(ids[i])) {
                        settings.addSmiley(kikUtil.smileyFromID(ids[i]), false);
                    }
                }
                settings.save(true);
                updateImportProgress(activity, 100, ActionProcessButton.Mode.PROGRESS, b, "Done :)");
                setAllButtons(true, activity);
            } else {
                buttonError(activity, b, v.getContext(), "Invalid File");
            }
        } catch (IOException e) {
            e.printStackTrace();
            buttonError(activity, b, v.getContext(), "Error reading file");
        }
    }

}
