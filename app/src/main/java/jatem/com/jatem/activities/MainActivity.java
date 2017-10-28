package jatem.com.jatem.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;
import com.twitter.sdk.android.core.Twitter;
import com.twitter.sdk.android.core.TwitterCore;

import jatem.com.jatem.R;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;


public class MainActivity extends BaseActivity implements View.OnClickListener, GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks {

    private FirebaseUser user;
    private ImageView ivPhoto;
    private EditText etIdea;
    private TextView tvCountDown;
    private TextView tvLogoff;
    private TextView tvMinCharacters;
    private Button btnJaTem;
    ProgressDialog pDialog;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Twitter.initialize(this);

        getSupportActionBar().setTitle(R.string.appName);

        user = FirebaseAuth.getInstance().getCurrentUser();

        etIdea = (EditText) findViewById(R.id.etIdea);
        tvCountDown = (TextView) findViewById(R.id.tvCountDown);
        tvLogoff = (TextView) findViewById(R.id.tvLogoff);
        tvMinCharacters = (TextView) findViewById(R.id.tvMinCharacters);
        ivPhoto = (ImageView) findViewById(R.id.ivUserPhoto);
        findViewById(R.id.btnJaTem).setOnClickListener(this);
        findViewById(R.id.tvLogoff).setOnClickListener(this);

        tvLogoff.setText(getString(R.string.logoff_text, user.getDisplayName().split(" ")));

        Picasso.with(this).load(user.getProviderData().get(1).getPhotoUrl()).into(ivPhoto);

        etIdea.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                tvCountDown.setText(String.valueOf(140 - charSequence.length()));

                btnJaTem = (Button) findViewById(R.id.btnJaTem);

                if(charSequence.length() < 30){
                    btnJaTem.setEnabled(false);
                    tvMinCharacters.setVisibility(View.VISIBLE);
                } else {
                    tvMinCharacters.setVisibility(View.GONE);
                    btnJaTem.setEnabled(true);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).
                requestIdToken("967528374405-465044jviguadjoults2eb0c6jgfa3ae.apps.googleusercontent.com").
                requestScopes(new Scope(Scopes.PLUS_LOGIN)).
                requestScopes(new Scope(Scopes.PLUS_ME)).
                requestEmail().
                build();

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, signInOptions).
                        addConnectionCallbacks(this). //comentar
                addOnConnectionFailedListener(this) //comentar
                .build();
    }

    private void signOut() {
        FirebaseAuth.getInstance().signOut();
        TwitterCore.getInstance().getSessionManager().clearActiveSession();
        LoginManager.getInstance().logOut();

        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(
                new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {

                    }
                });

        finish();
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.tvLogoff) {
            signOut();
        } else if (i == R.id.btnJaTem){
            String status = etIdea.getText().toString();

            if (status.trim().length() > 0) {
                // update status
                new updateTwitterStatus().execute(status);
            }
        }
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    //AsyncTask para envio do tweet (FUNCIONOU CARALHO)
    class updateTwitterStatus extends AsyncTask<String, String, String> {

        /**
         * Before starting background thread Show Progress Dialog
         * */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage(getString(R.string.searching));
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        protected String doInBackground(String... args) {
            Log.d("Tweet Text", "> " + args[0]);

            twitter4j.Twitter twitter = new TwitterFactory().getInstance();
            twitter.setOAuthConsumer(getString(R.string.twitter_consumer_key), getString(R.string.twitter_consumer_secret));

            AccessToken accessToken = null;
            accessToken = new AccessToken(getString(R.string.twitter_access_key), getString(R.string.twitter_access_secret));
            twitter.setOAuthAccessToken(accessToken);

            twitter4j.Status status = null;
            try {
                //Eu sei, mas funciona
                status = twitter.updateStatus(etIdea.getText().toString()+"\n"+getString(R.string.hashtag_jatem));
            } catch (TwitterException e) {
                e.printStackTrace();
            }

            return null;
        }

        /**
         * After completing background task Dismiss the progress dialog and show
         * the data in UI Always use runOnUiThread(new Runnable()) to update UI
         * from background thread, otherwise you will get error
         * **/
        protected void onPostExecute(String file_url) {
            // dismiss the dialog after getting all products
            pDialog.dismiss();
            // updating UI from Background Thread
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this, R.style.CustomAlertDialog);
                    View view = getLayoutInflater().inflate(R.layout.dialog_ja_tem, null);
                    builder.setView(view);
                    final AlertDialog dialog = builder.create();
                    dialog.show();


                    final Button btnOk = view.findViewById(R.id.btnOk);

                    btnOk.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dialog.hide();
                        }
                    });

                    etIdea.setText("");
                }
            });
        }
    }
}
