package timbuchalka.com.firewallauthenticator;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private Button loginBtn;
    private EditText usernameEt, passwordEt;
    private TextView logoutHelp;
    private ProgressDialog mProgressDialog;

    private String magicValue = null, logoutLink = null;
    private String username, password;
    private Boolean usernameFound = false, passwordFound = false;

    private Boolean successFound = false, failedFound = false, overLimitfound = false;
    private String success = "This browser window is used to keep your authentication session active. Please leave it open in the background and open a new window to continue.";
    private String failed = "Firewall authentication failed. Please try again.";
    private String overlimit = "Sorry, user's concurrent authentication is over limit";
    private String toast = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setUpVariables();
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Please, Wait!");

        final ConnectivityManager cm = (ConnectivityManager)getApplicationContext().getSystemService(CONNECTIVITY_SERVICE);
        assert cm != null;
        final NetworkInfo networkInfo = cm.getNetworkInfo(1);

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(networkInfo!=null) {
                    WifiManager wifiManager = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
                    if(wifiManager != null) {
                        if(!wifiManager.isWifiEnabled()) {
                            wifiManager.setWifiEnabled(true);
                            try {
                                Thread.sleep(5000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        mProgressDialog.show();
                        new BackgroundClass().execute("http://detectportal.firefox.com/status.txt");
                    }
                }
            }
        });

        logoutHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick: logoutHelp called");
                startActivity(new Intent(MainActivity.this, LogoutHelperActivity.class));
            }
        });
    }

    private class BackgroundClass extends AsyncTask<String, Void, String> {

        DefaultHttpClient client = new DefaultHttpClient();

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected String doInBackground(String... strings) {
            Log.d(TAG, "doInBackground: called");

            Log.d(TAG, "doInBackground: GETTING THE LOGIN PAGE FROM URL");
            HttpGet request = new HttpGet(strings[0]);
            Log.d(TAG, "doInBackground: " + request);

            try {
                HttpResponse response = client.execute(request);
                Log.d(TAG, "doInBackground: " + response);

                HttpEntity entity = response.getEntity();

                Log.d(TAG, "doInBackground: PARSING THE RESPONSE FROM THE SERVER");
                Document document = Jsoup.parse(EntityUtils.toString(entity));
                Elements inputs = document.getElementsByTag("input");
                for (Element input : inputs) {
                    if ("magic".equals(input.attr("name")))
                        magicValue = input.val();
                    else if ("username".equals(input.attr("name")))
                        usernameFound = true;
                    else if ("password".equals(input.attr("name")))
                        passwordFound = true;
                }

                Log.d(TAG, "doInBackground: " + magicValue + " " + usernameFound + " " + passwordFound);
                if (magicValue != null && usernameFound && passwordFound) {
                    Log.d(TAG, "doInBackground: GOT THE LOGIN PAGE");
                    sendDataToServer(strings[0]);
                } else {
                    Log.d(TAG, "doInBackground: UNABLE TO GET THE LOGIN PAGE.");
                    ////////////////////////may be user is logged in currently////////////////////////////////////
                    toast = "Already logged in!";
                }
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: IO: " + e.getMessage());
                toast = e.getMessage();
            } catch (Exception e) {
                Log.e(TAG, "doInBackground: normal: " + e.getMessage());
                toast = e.getMessage();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            Log.d(TAG, "onPostExecute: called");
            if(!toast.isEmpty()) {
                Log.d(TAG, "onPostExecute: " + toast);
                mProgressDialog.dismiss();
                Toast.makeText(MainActivity.this, toast, Toast.LENGTH_SHORT).show();
            }
        }

        private void sendDataToServer(String url) {
            Log.d(TAG, "sendDataToServer: called");

            username = usernameEt.getText().toString().trim();
            password = passwordEt.getText().toString().trim();
            if(!username.isEmpty() && !password.isEmpty()) {
                HttpPost httppost = new HttpPost(url);
                try {
                    List<NameValuePair> nameValuePairs = new ArrayList<>(3);

                    nameValuePairs.add(new BasicNameValuePair("magic", magicValue));
                    nameValuePairs.add(new BasicNameValuePair("username", username));
                    nameValuePairs.add(new BasicNameValuePair("password", password));

                    httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                    HttpResponse response = client.execute(httppost);
                    Log.d(TAG, "sendDataToServer: " + response);

                    HttpEntity entity = response.getEntity();

                    String content = EntityUtils.toString(entity);
                    Log.d(TAG, "sendDataToServer: " + content);

                    isUserLoggedIn(content);

                } catch (ClientProtocolException e) {
                    Log.e(TAG, "sendDataToServer: CPE: " + e.getMessage());
                    toast = e.getMessage();
                } catch (IOException e) {
                    Log.e(TAG, "sendDataToServer: IOE: " + e.getMessage());
                    toast = e.getMessage();
                } catch (Exception e) {
                    Log.e(TAG, "sendDataToServer: normal: " + e.getMessage());
                    toast = e.getMessage();
                }
            }else{
                Log.d(TAG, "sendDataToServer: username and password cannot be empty");
                toast = "Username and Password cannot be Empty";
            }
        }

        private void isUserLoggedIn(String string) {
            Log.d(TAG, "isUserLoggedIn: called");

            Document document = Jsoup.parse(string);
            if("Access Denied".equals(document.title())){
                Log.d(TAG, "isUserLoggedIn: Already Logged In");
                toast = "Already Logged In";
            }
            else {
                Elements headings = document.getElementsByTag("h2");
                for (Element heading : headings) {
                    String h2 = heading.text();
                    Log.d(TAG, "isUserLoggedIn: " + h2);
                    if (failed.equals(h2)) {
                        failedFound = true;
                        Log.d(TAG, "isUserLoggedIn: " + failed);
                        toast = failed;
                    } else if (overlimit.equals(h2)) {
                        overLimitfound = true;
                        Log.d(TAG, "isUserLoggedIn: " + overlimit);
                        toast = overlimit;
                    } else if (success.equals(h2)) {
                        successFound = true;
                        Log.d(TAG, "isUserLoggedIn: " + success);
                        toast = "Login Successful";

                        Elements links = document.getElementsByAttribute("href");
                        for(Element link : links){
                            Log.d(TAG, "isUserLoggedIn: links: " + link.attr("href") + " " + link.text());
                            if("logout".equals(link.text())){
                                logoutLink = link.attr("href");
                            }
                        }
                        Log.d(TAG, "isUserLoggedIn: success_intent: " + magicValue + " " + username + " " + password + " " + logoutLink);
                        Intent intent = new Intent(MainActivity.this, LogoutActivity.class);
                        intent.putExtra("username", username);
                        intent.putExtra("password", password);
                        intent.putExtra("logoutLink", logoutLink);

                        startActivity(intent);
                    }
                }
            }
        }
    }

    private void setUpVariables(){
        Log.d(TAG, "setUpVariables: called");
        usernameEt = findViewById(R.id.etUsername);
        passwordEt = findViewById(R.id.etPassword);
        loginBtn = findViewById(R.id.btnLogin);
        logoutHelp = findViewById(R.id.tvLogout_help);
    }
}
