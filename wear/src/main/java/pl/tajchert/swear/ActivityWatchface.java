package pl.tajchert.swear;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.util.Calendar;

import pl.tajchert.swearcommon.Tools;


public class ActivityWatchface extends Activity {
    private static final String TAG = ActivityWatchface.class.getSimpleName();
    private BroadcastReceiver dataChangedReceiver;
    private IntentFilter dataChangedIntentFilter;
    private AutoSizeTextView swearContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        swearContainer = (AutoSizeTextView) findViewById(R.id.TextViewSwearContainer);

        dataChangedIntentFilter = new IntentFilter(Tools.DATA_CHANGED_ACTION);
        sendNotificationToMobile();

        dataChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Tools.DATA_CHANGED_ACTION.equals(intent.getAction())) {
                    String swearText = ActivityWatchface.this.getSharedPreferences(Tools.PREFS, MODE_PRIVATE).getString(Tools.PREFS_KEY_SWEAR_TEXT, "got null");
                    if(swearText == null){
                        return;
                    }
                    swearContainer.setText(swearText);
                    ActivityWatchface.this.getSharedPreferences(Tools.PREFS, MODE_PRIVATE).edit().putLong(Tools.PREFS_KEY_TIME_LAST_UPDATE, Calendar.getInstance().getTimeInMillis()).commit();
                }
            }
        };
    }

    private void sendNotificationToMobile(){
        //Send empty string to ask phone to refresh weather data
        Log.d(TAG, "sendNotificationToMobile ");
        final GoogleApiClient googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        googleApiClient.connect();
        String value = "update_request";
        value = value +  Calendar.getInstance().getTimeInMillis();
        PutDataMapRequest dataMap = PutDataMapRequest.create(Tools.WEAR_PATH_ACTION_UPDATE);
        dataMap.getDataMap().putString(Tools.WEAR_ACTION_UPDATE, value);
        PutDataRequest request = dataMap.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult = Wearable.DataApi.putDataItem(googleApiClient, request);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(DataApi.DataItemResult dataItemResult) {
                Log.d(TAG, "Sent: " + dataItemResult.toString());
                googleApiClient.disconnect();
            }
        });
    }

    private boolean timeToRefresh(){
        if(swearContainer != null && swearContainer.getText().equals(ActivityWatchface.this.getString(R.string.swear_null)) || swearContainer.getText().equals(ActivityWatchface.this.getString(R.string.swear_null))){
            return true;
        }
        if(Calendar.getInstance().getTimeInMillis() - ActivityWatchface.this.getSharedPreferences(Tools.PREFS, MODE_PRIVATE).getLong(Tools.PREFS_KEY_TIME_LAST_UPDATE, 0) > (Tools.REFRESH_INTERVAL)) {
            return true;
        }
        return false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        this.registerReceiver(dataChangedReceiver, dataChangedIntentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(dataChangedReceiver);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(timeToRefresh()){
            sendNotificationToMobile();
        }
    }
}
