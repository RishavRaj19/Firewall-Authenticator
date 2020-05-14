package timbuchalka.com.firewallauthenticator;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.HashMap;

public class LogoutHelperActivity extends AppCompatActivity {
    private static final String TAG = "LogoutHelperActivity";

    private ProgressDialog mProgressDialog;
    private String toast = "";

    private TextView Ghostel, Chostel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout_helper);

        setUpVariables();
        final HashMap<String, String> IpAddress = new HashMap<>();
        setIPAddress(IpAddress);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Please, Wait!");

        Ghostel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
                if(wifiManager != null) {
                    if(!wifiManager.isWifiEnabled()) {
                        Log.d(TAG, "onClick: wifi is off");
                        Toast.makeText(LogoutHelperActivity.this, "Switch on the device wifi!", Toast.LENGTH_SHORT).show();
                    }else {
                        mProgressDialog.show();
                        if(checkSSID())
                            new Logout().execute(IpAddress.get("G-hostel"));
                    }
                }
            }
        });

        Chostel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
                if(wifiManager != null) {
                    if(!wifiManager.isWifiEnabled()) {
                        Log.d(TAG, "onClick: wifi is off");
                        Toast.makeText(LogoutHelperActivity.this, "Switch on the device wifi!", Toast.LENGTH_SHORT).show();
                    }else {
                        mProgressDialog.show();
                        if(checkSSID())
                            new Logout().execute(IpAddress.get("C-hostel"));
                    }
                }
            }
        });

    }

    private boolean checkSSID() {
        return true;
    }

    private class Logout extends AsyncTask<String, Void, String> {
        DefaultHttpClient client = new DefaultHttpClient();

        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, "doInBackground: called");
            Log.d(TAG, "doInBackground: " + strings[0]);

            HttpGet request = new HttpGet(strings[0]);
            Log.d(TAG, "doInBackground: " + request);

            try {
                HttpResponse response = client.execute(request);
                Log.d(TAG, "doInBackground: " + response);

                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                Log.d(TAG, "doInBackground: " + result);

                //parsing and checking if logged out or not
                Document doc = Jsoup.parse(result);
                if ("Firewall Authentication".equals(doc.title())) {
                    Log.d(TAG, "doInBackground: Logout successful.");
                    toast = "Logout successful";
                    return toast;
                }
            } catch (ClientProtocolException e) {
                Log.d(TAG, "doInBackground: " + e.getMessage());
                toast = e.getMessage();
            } catch (IOException e) {
                Log.d(TAG, "doInBackground: " + e.getMessage());
                toast = e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.d(TAG, "onPostExecute: called");

            mProgressDialog.dismiss();
            if ("Logout successful".equals(toast)) {
                Log.d(TAG, "onPostExecute: " + toast);
                Toast.makeText(LogoutHelperActivity.this, toast, Toast.LENGTH_SHORT).show();
            } else if (!toast.isEmpty()) {
                Log.d(TAG, "onPostExecute: Logout unsuccessful.");
                Toast.makeText(LogoutHelperActivity.this, toast + "\nLogout Unsuccessful", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "onPostExecute: Logout unsuccessful.");
                Toast.makeText(LogoutHelperActivity.this, "Logout Unsuccessful", Toast.LENGTH_SHORT).show();
            }

        }
    }

    private void setUpVariables() {
        Ghostel = findViewById(R.id.tvGhostel);
        Chostel = findViewById(R.id.tvChostel);
    }

    private void setIPAddress(HashMap<String, String> ipAddress) {
        ipAddress.put("G-hostel", "http://172.16.24.1:1000/logout?");
        ipAddress.put("C-hostel", "http://172.16.24.1:1000/logout?");
    }
}
