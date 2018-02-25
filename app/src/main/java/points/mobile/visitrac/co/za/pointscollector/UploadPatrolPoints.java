package points.mobile.visitrac.co.za.pointscollector;

import android.*;
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

/**
 * Created by sefako@gmail.com on 2018/02/24.
 *   visitrac/rest/ClockingService/registerClockingPoints (POST)
 */

public class UploadPatrolPoints extends AsyncTask<String, Void, String> {

    private DialogInterface dialog;
    public static final String TAG = "UploadPatrolsPoints";
    public static final String SERVER_URL = "visitrac.dedicated.co.za:8080";
    private WeakReference<Context> context;
    public static final MediaType JSON
            = MediaType.parse("application/json; charset=utf-8");
    private static final String EMEI = "device-emei";
    public static String POST_PATROL_POINTS = "/visitrac/rest/ClockingService/registerClockingPoints";

    public UploadPatrolPoints(WeakReference<Context> context, DialogInterface dialog) {
        this.context = context;
        this.dialog = dialog;
    }

    @Override
    protected String doInBackground(String... params) {
        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        OkHttpClient client = new OkHttpClient().newBuilder().addInterceptor(interceptor).build();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        RequestBody body = RequestBody.create(JSON, params[0]);
        Log.i(TAG, " Created RS body " + body );
        try {
        Request request = new Request.Builder()
                .header(EMEI,  getDevideId(context.get()))
                .url(endPoint())
                .post(body)
                .build();


            Log.i(TAG, " About to Call Rest Service  " + request.body().toString() + " header " + request.headers());
            Response response = client.newCall(request).execute();
            String result =  response.code()+"";
            Log.i(TAG, " Rest Service Called with Result Code " +result);


            return result;

        }catch (Exception e){
            Log.i(TAG, " Un expected error when calling Rest ");
            e.printStackTrace();
        }

        return "-1";
    }

    @Override
    protected void onPostExecute(String resultCode) {
        if(!resultCode.contains("200")){
            Toast.makeText(context.get(), "Upload Error", Toast.LENGTH_LONG);
        }
            dialog.dismiss();
    }

    @SuppressLint("HardwareIds")
    public  String getDevideId(Context context) throws Exception{
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            return "";
        }
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                return telephonyManager.getImei();
            } else {
                 return telephonyManager.getDeviceId();
            }
        }
    }

    public String endPoint(){
        return "http://" + SERVER_URL+POST_PATROL_POINTS;
    }
}
