package mil.nga.mapcache.wizards.createtile;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;

import mil.nga.geopackage.BoundingBox;
import mil.nga.geopackage.extension.nga.scale.TileScaling;
import mil.nga.mapcache.GeoPackageMapFragment;
import mil.nga.mapcache.GeoPackageUtils;
import mil.nga.mapcache.R;
import mil.nga.mapcache.data.GeoPackageDatabases;
import mil.nga.mapcache.load.ILoadTilesTask;
import mil.nga.mapcache.load.LoadTilesTask;
import mil.nga.mapcache.utils.ViewAnimation;
import mil.nga.mapcache.view.detail.NewLayerUtil;
import mil.nga.proj.ProjectionConstants;

/**
 * UI that allows the user to pick which zoom levels to save to a geopackage and various other
 * options.  Once user click finish, it saves the tile layer to the geopackage.
 */
public class LayerOptionsUI {

    /**
     * Used to get the layout.
     */
    private FragmentActivity activity;

    /**
     * The app context.
     */
    private Context context;

    /**
     * The fragment this UI is apart of, used to get resource strings.
     */
    private Fragment fragment;

    /**
     * Active GeoPackages
     */
    private GeoPackageDatabases active;

    /**
     * The callback to pass to LoadTilesTask.
     */
    private ILoadTilesTask callback;

    /**
     * Contains a bounding box that is displayed to the user.
     */
    private IBoundingBoxManager boxManager;

    /**
     * The name of the geopackage.
     */
    private String geopackageName;

    /**
     * The name of the layer.
     */
    private String layerName;

    /**
     * The base url to the tile layer.
     */
    private String url;

    /**
     * Constructs a new layer options UI
     * @param activity Use The app context.
     * @param fragment The fragment this UI is apart of, used to get resource strings.
     * @param active The active GeoPackages
     * @param callback The callback to pass to LoadTilesTask.
     * @param boxManager Contains a bounding box that is displayed to the user.
     * @param geoPackageName The name of the geopackage.
     * @param layerName The name of the layer.
     * @param url The base url to the tile layer.
     */
    public LayerOptionsUI(FragmentActivity activity, Context context, Fragment fragment,
                          GeoPackageDatabases active, ILoadTilesTask callback,
                          IBoundingBoxManager boxManager, String geoPackageName,
                          String layerName, String url) {
        this.activity = activity;
        this.context = context;
        this.fragment = fragment;
        this.active = active;
        this.callback = callback;
        this.boxManager = boxManager;
        this.geopackageName = geoPackageName;
        this.layerName = layerName;
        this.url = url;
    }

    /**
     * Shows the UI to the user.
     */
    public void show() {
        // Create Alert window with basic input text layout
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        View tileView = inflater.inflate(R.layout.new_tile_layer_final, null);
        ImageView closeLogo = (ImageView) tileView.findViewById(R.id.final_layer_close_logo);

        // Set the spinner values for zoom levels
        Spinner minSpinner = (Spinner)tileView.findViewById(R.id.min_zoom_spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
                R.array.zoom_levels, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        minSpinner.setAdapter(adapter);
        Spinner maxSpinner = (Spinner)tileView.findViewById(R.id.max_zoom_spinner);
        ArrayAdapter<CharSequence> maxAdapter = ArrayAdapter.createFromResource(getContext(),
                R.array.zoom_levels, android.R.layout.simple_spinner_item);
        maxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        maxSpinner.setAdapter(maxAdapter);
        maxSpinner.setSelection(maxAdapter.getPosition("5"));

        // Set a listener to adjust min and max when selections are made
        NewLayerUtil.setZoomLevelSyncListener(minSpinner, maxSpinner);

        // Name and url
        TextView finalName = (TextView) tileView.findViewById(R.id.final_tile_name);
        finalName.setText(layerName);
        TextView finalUrl = (TextView) tileView.findViewById(R.id.final_tile_url);
        finalUrl.setText(url);

        // finish button
        final MaterialButton drawButton = (MaterialButton) tileView.findViewById(R.id.create_tile_button);

        // Advanced options
        ImageButton advancedExpand = (ImageButton) tileView.findViewById(R.id.advanced_expand_button);
        View advancedView = (View)tileView.findViewById(R.id.advanceLayout);
        advancedExpand.setOnClickListener((view)->{
            toggleSection(advancedExpand, advancedView);
        });
        RadioGroup srsGroup = (RadioGroup) tileView.findViewById(R.id.srsGroup);
        RadioGroup tileFormatGroup = (RadioGroup) tileView.findViewById(R.id.tileFormatGroup);

        // Open the dialog
        AlertDialog.Builder dialog = new AlertDialog.Builder(getActivity(), R.style.AppCompatAlertDialogStyle)
                .setView(tileView);
        final AlertDialog alertDialog = dialog.create();
        alertDialog.setCanceledOnTouchOutside(false);


        TextView srsLabel = (TextView) tileView.findViewById(R.id.srsLabel);
        srsLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle(fragment.getString(R.string.srs_help_title));
                builder.setMessage(fragment.getString(R.string.srs_help));
                final AlertDialog srsDialog = builder.create();

                builder.setPositiveButton(R.string.button_ok_label, new DialogInterface.OnClickListener(){
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        srsDialog.dismiss();
                    }
                });
                builder.show();

            }
        });

        // close button
        closeLogo.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                alertDialog.dismiss();
                boxManager.clearBoundingBox();

            }
        });

        // finish button
        drawButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                int minZoom = Integer.valueOf(minSpinner.getSelectedItem().toString());
                int maxZoom = Integer.valueOf(maxSpinner.getSelectedItem().toString());

                if(minZoom > maxZoom){
                    Toast.makeText(getActivity(), "Min zoom can't be more than max zoom", Toast.LENGTH_SHORT).show();
                } else {

                    try {
                        // Get values ready for creating the layer
                        RadioButton selectedSrs = (RadioButton) tileView.findViewById(srsGroup.getCheckedRadioButtonId());
                        long epsg = Integer.valueOf(selectedSrs.getText().subSequence(5, 9).toString());
                        RadioButton selectedFormat = (RadioButton) tileView.findViewById(tileFormatGroup.getCheckedRadioButtonId());
                        String tileFormat = selectedFormat.getText().toString();
                        boolean xyzTiles = false;
                        if (tileFormat.equalsIgnoreCase("google")) {
                            xyzTiles = true;
                        }

                        Bitmap.CompressFormat compressFormat = null;
                        Integer compressQuality = 100;
                        TileScaling scaling = null;
                        double minLat = 90.0;
                        double minLon = 180.0;
                        double maxLat = -90.0;
                        double maxLon = -180.0;
                        for (LatLng point : boxManager.getBoundingBox().getPoints()) {
                            minLat = Math.min(minLat, point.latitude);
                            minLon = Math.min(minLon, point.longitude);
                            maxLat = Math.max(maxLat, point.latitude);
                            maxLon = Math.max(maxLon, point.longitude);
                        }
                        BoundingBox boundingBox = new BoundingBox(minLon,
                                minLat, maxLon, maxLat);


                        // Load tiles
                        LoadTilesTask.loadTiles(getActivity(),
                                callback, active,
                                geopackageName, layerName, url, minZoom,
                                maxZoom, compressFormat,
                                compressQuality, xyzTiles,
                                boundingBox, scaling,
                                ProjectionConstants.AUTHORITY_EPSG, String.valueOf(epsg));

                    } catch (Exception e) {
                        GeoPackageUtils
                                .showMessage(
                                        getActivity(),
                                        fragment.getString(R.string.geopackage_create_tiles_label),
                                        "Error creating tile layer: \n\n" + e.getMessage());
                    }
                    alertDialog.dismiss();
                    boxManager.clearBoundingBox();
                }

            }
        });
        alertDialog.show();
    }

    /**
     * Gets the activity.
     * @return The activity.
     */
    private FragmentActivity getActivity() {
        return this.activity;
    }

    /**
     * Gets the app context.
     * @return The app context.
     */
    private Context getContext() {
        return this.context;
    }

    /**
     * Toggles the advanced options arrow up or down.
     * @param view The view to animate.
     * @return True if the advanced options should be visible.
     */
    private boolean toggleArrow(View view) {
        if (view.getRotation() == 0) {
            view.animate().setDuration(200).rotation(180);
            return true;
        } else {
            view.animate().setDuration(200).rotation(0);
            return false;
        }
    }

    /**
     * Toggle for showing / hiding a view (used in the advanced section of create tile menu)
     * @param bt
     * @param lyt
     */
    private void toggleSection(View bt, final View lyt) {
        boolean show = toggleArrow(bt);
        if (show) {
            ViewAnimation.expand(lyt, new ViewAnimation.AnimListener() {
                @Override
                public void onFinish() {
                }
            });
        } else {
            ViewAnimation.collapse(lyt);
        }
    }
}
