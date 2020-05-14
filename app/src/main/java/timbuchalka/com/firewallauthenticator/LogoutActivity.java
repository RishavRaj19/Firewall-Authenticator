package timbuchalka.com.firewallauthenticator;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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
import java.util.Objects;

public class LogoutActivity extends AppCompatActivity {
    private static final String TAG = "LogoutActivity";

    private String logoutLink;
    private String toast = "";

    private CountDownTimer timer = null;
    private long timesLoggedIn = 1;
    private boolean reLogin = true;

    private String magicValue = null;
    private String username, password;
    private Boolean usernameFound = false, passwordFound = false;

    private int counter_send_data = 0;

    private Boolean successFound = false, failedFound = false, overLimitfound = false;
    private String success = "This browser window is used to keep your authentication session active. Please leave it open in the background and open a new window to continue.";
    private String failed = "Firewall authentication failed. Please try again.";
    private String overlimit = "Sorry, user's concurrent authentication is over limit";

    private ProgressDialog mProgressDialog;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate: called");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logout);

        Button logoutBtn = findViewById(R.id.btnLogout);
        logoutLink = Objects.requireNonNull(getIntent().getExtras()).getString("logoutLink");
        username = getIntent().getExtras().getString("username");
        password = getIntent().getExtras().getString("password");

        Log.d(TAG, "onCreate: " + logoutLink + " " + username + " " + password);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Logging out!\nPlease Wait :)");
        if(logoutBtn !=null) {
            logoutBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mProgressDialog.show();
                    Log.d(TAG, "onClick: called");
                    reLogin = false;
                    new Logout().execute(logoutLink);
                }
            });
        }

        //keepAlive
        reLogin();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null)
            timer.cancel();
    }

    private void reLogin() {
        Log.d(TAG, "reLogin: called");

        timesLoggedIn = timesLoggedIn + 1;
        timer = new CountDownTimer(2400000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                long sec = millisUntilFinished/1000;
                Log.d(TAG, "onTick: " + sec);
            }

            @Override
            public void onFinish() {
                Log.d(TAG, "onFinish: called");
                Log.d(TAG, "onFinish: ReLogin using new link: TimesLoggedIn---> " + timesLoggedIn);
                Log.d(TAG, "onFinish: logoutLink" + logoutLink);

                reLogin = true;
                counter_send_data = 0;
                new Logout().execute(logoutLink);
                reLogin();
            }
        }.start();
    }

    private class Logout extends AsyncTask<String, Void, String>{
        DefaultHttpClient client = new DefaultHttpClient();

        @Override
        protected String doInBackground(String...strings) {
            Log.d(TAG, "doInBackground: called");
            Log.d(TAG, "doInBackground: " + strings[0]);

            HttpGet request = new HttpGet(strings[0]);
            Log.d(TAG, "doInBackground: " + request);

            try{
                HttpResponse response = client.execute(request);
                Log.d(TAG, "doInBackground: " + response);

                HttpEntity entity = response.getEntity();
                String result = EntityUtils.toString(entity);
                Log.d(TAG, "doInBackground: " + result);

                //parsing and checking if logged out or not
                Document doc = Jsoup.parse(result);
                if("Firewall Authentication Logout".equals(doc.title())) {
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
            if(!reLogin) {
                mProgressDialog.dismiss();
                if ("Logout successful".equals(toast)) {
                    Log.d(TAG, "onPostExecute: " + toast);
                    Toast.makeText(LogoutActivity.this, toast, Toast.LENGTH_SHORT).show();
                    finish();
                } else if (!toast.isEmpty()) {
                    Log.d(TAG, "onPostExecute: Logout unsuccessful.");
                    Toast.makeText(LogoutActivity.this, toast + "\nLogout Unsuccessful", Toast.LENGTH_SHORT).show();
                } else {
                    Log.d(TAG, "onPostExecute: Logout unsuccessful.");
                    Toast.makeText(LogoutActivity.this, "Logout Unsuccessful", Toast.LENGTH_SHORT).show();
                }
            }else {
                if ("Logout successful".equals(toast)) {
                    Log.d(TAG, "onPostExecute: " + toast);
                    new Login().execute("http://detectportal.firefox.com/status.txt");
                } else {
                    //if logout doesn't happen then what sd happen
                    new Logout().execute(logoutLink);
                }
            }
        }

        private class Login extends AsyncTask<String, Void, String> {

            @Override
            protected void onPostExecute(String s) {
                Log.d(TAG, "onPostExecute: called");
                if(!toast.isEmpty() && !"Logout successful".equals(toast)) {
                    Log.d(TAG, "onPostExecute: " + toast);
                    Toast.makeText(LogoutActivity.this, toast, Toast.LENGTH_SHORT).show();
                }
            }

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
                        toast = "Already logged in!";

                        //what sd be done

                    }
                } catch (IOException e) {
                    Log.e(TAG, "doInBackground: IO: " + e.getMessage());
                    toast = e.getMessage();

                    finish();
                } catch (Exception e) {
                    Log.e(TAG, "doInBackground: normal: " + e.getMessage());
                    toast = e.getMessage();

                    finish();
                }
                return null;
            }

            private void sendDataToServer(String url) {
                Log.d(TAG, "sendDataToServer: called");
                counter_send_data++;

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

                    if(counter_send_data < 4)
                        sendDataToServer(url);
                    else
                        finish();
                } catch (IOException e) {
                    Log.e(TAG, "sendDataToServer: IOE: " + e.getMessage());
                    toast = e.getMessage();

                    if(counter_send_data < 4)
                        sendDataToServer(url);
                    else
                        finish();
                } catch (Exception e) {
                    Log.e(TAG, "sendDataToServer: normal: " + e.getMessage());
                    toast = e.getMessage();

                    if(counter_send_data < 4)
                        sendDataToServer(url);
                    else
                        finish();
                }
            }

            private void isUserLoggedIn(String string) {
                Log.d(TAG, "isUserLoggedIn: called");

                Document document = Jsoup.parse(string);
                if ("Access Denied".equals(document.title())) {
                    Log.d(TAG, "isUserLoggedIn: Already Logged In");
                    toast = "Already Logged In";
                } else {
                    Elements headings = document.getElementsByTag("h2");
                    for (Element heading : headings) {
                        String h2 = heading.text();
                        Log.d(TAG, "isUserLoggedIn: " + h2);
                        if (failed.equals(h2)) {
                            failedFound = true;
                            Log.d(TAG, "isUserLoggedIn: " + failed);
                            toast = failed;
                            finish();
                        } else if (overlimit.equals(h2)) {
                            overLimitfound = true;
                            Log.d(TAG, "isUserLoggedIn: " + overlimit);
                            toast = overlimit;
                            finish();
                        } else if (success.equals(h2)) {
                            successFound = true;
                            Log.d(TAG, "isUserLoggedIn: " + success);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed: called");
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("If you continue, this app won't be able to refresh your session!\n\nDo you still want to continue?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                }).show();
    }
}
