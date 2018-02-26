package points.mobile.visitrac.co.za.pointscollector;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;

import com.google.android.gms.maps.model.LatLng;

import java.util.Set;

/**
 * Created by sefako@gmail.com on 2018/02/16.
 */

public class Utils {

    public static void showSaveDialog(Context context, final Set<LatLng> positions, final LatLng position) {
        AlertDialog.Builder builder1 = new AlertDialog.Builder(context);
        builder1.setMessage("Save Location. ["+position.latitude+","+ position.longitude +"]");
        builder1.setCancelable(true);

        builder1.setPositiveButton(
                "Yes",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        positions.add(position);
                        dialog.cancel();
                    }
                });

        builder1.setNegativeButton(
                "No",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert11 = builder1.create();
        alert11.show();
    }
}
