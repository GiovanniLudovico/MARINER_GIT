package dianagio.wheelchair;


import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.android.AuthActivity;
import com.dropbox.client2.session.AccessTokenPair;
import com.dropbox.client2.session.AppKeyPair;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public class Init_Activity extends Activity {

    private int REQUEST_DATA=0;                             //code for the identification of the activitystart

    //===========DROPBOX AUTH STRINGS=======
    final static private String APP_KEY = "0kt21gmsf9ldsk2";



    final static private String APP_SECRET = "gay3xog7cc0ombt";
    private static final String ACCOUNT_PREFS_NAME = "prefs";
    private static final String ACCESS_KEY_NAME = "ACCESS_KEY";
    private static final String ACCESS_SECRET_NAME = "ACCESS_SECRET";

    private boolean mLoggedIn=false;                        // variable that indicates status of the autentication of the app with dropbox

    DropboxAPI<AndroidAuthSession> mDBApi;                  // DropBox API :)

    public User user_child;                                 // handle of user parameters

    //===========================================================
    public EditText edit_name;                              //NAME and SURNAME input fields
    public EditText edit_surname;
    //===========================================================
    private boolean upload_finished=false;                  //flag che diventa vera quando l'upload finisce con successo
    //==========================================================
    private UploadData upload;                               // class used to implement the data upload
    //============================================================

    public List<String> NotSentFilesName = new ArrayList<>(); //not sent files memory

     LastFiles lastfiles;                                     //handle of the last files to upload

    @Override
    //==========================================================================
    protected void onCreate(Bundle savedInstanceState) {
        //==========================================================================
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_init_);                 //loads layout
        registerReceiver(mBatChargeOn, new IntentFilter(Intent.ACTION_POWER_CONNECTED));
        // We create a new AuthSession so that we can use the Dropbox API.
        AndroidAuthSession session = buildSession();
        mDBApi = new DropboxAPI<AndroidAuthSession>(session);
        mLoggedIn = (mDBApi.getSession().isLinked());              //checks if the app has received the auth token from dropbox

        edit_name= (EditText)findViewById(R.id.text_name);         //handles of edittexts
        edit_surname= (EditText)findViewById(R.id.text_surname);

        load_user();                                                // load previously set user parameters

        checkAppKeySetup();                                          //
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    protected void onStop() {
        super.onStop();
        // The activity is no longer visible (it is now "stopped")
    }
    @Override
    //==========================================================================
    protected void onResume() {
        //==========================================================================
        super.onResume();
        AndroidAuthSession session = mDBApi.getSession(); //

        if (session.authenticationSuccessful()) {        // when the app restarts saves the auth token of dropbox
            try {
                // Mandatory call to complete the auth
                session.finishAuthentication();

                // Store it locally in our app for later use
                storeAuth(session);
                //setLoggedIn(true);
            } catch (IllegalStateException e) {
                showToast("Couldn't authenticate with Dropbox:" + e.getLocalizedMessage());
                // Log.i(TAG, "Error authenticating", e);
                SaveErrorLog(e.toString());
            }
        }
    }

    //==========TRIES TO UPLOAD THE LAST FILES ACQUIRED============
    private void upload_lastFiles()
    //==========================================================================
    {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date now = new Date(); //salva la data per creare la cartella di dropbox

        String s1; //variabili ausiliarie per estrarre il nome del file dal path
        String s2 ;

        if(lastfiles.isyoctoinuse) { //se si sta usando lo yoctopuce (provvisorio)
            s1 = lastfiles.tell_motor();
            s2 = user_child.tellAcquisitionsFolder();


            final String filenameMotor= "/" + formatter.format(now).toString() +"/" +(s1.substring(s2.length(),s1.length()));     //estrae il nome del file da caricare sottraendo dal path completo del file salvato la parte relativa alla locazione nella memoria interna del telefono
            //metodo costruttore di uploaddata, se si vuole recuperare la stringa d'uscita bisogna implementare il metodo process finish
            //dell'interfaccia AsyncResponse
            //the uploaddata constructor requires the implementation of an asyncresponce interface if it necessary to retrieve informations about the transfer
            upload = new UploadData(

                    new AsyncResponse() {

                @Override
                public void processFinish(String output) {// in questo caso process finish permette di salvare in un array sia i path che i nomi dei file non caricati
                    if (output!=null)//se e' diverso da nullallora c'è un errore nel caricamento output contiene il path del file non inviato
                    {
                        showToast("transfer failed");
                        //Adding item to List
                        NotSentFilesName.add(NotSentFilesName.size(), output);      //this will add string con il path del fil eat the next index
                        NotSentFilesName.add(NotSentFilesName.size(), filenameMotor);// nome del filee cartella  con l'orario a cui è partito il caricamento
                    }
                    else showToast("tutto ok");
                }
            });

            upload.UploadData_parameters(this, mDBApi, lastfiles.tell_motor(), user_child.tellUserFolder(), user_child.Acquisitions +filenameMotor);
            upload.execute();
        }
        s1 = lastfiles.tell_accelerometer();
        s2 = user_child.tellAcquisitionsFolder();


        final String filenameAcc="/" + formatter.format(now).toString() +"/" +(s1.substring(s2.length(),s1.length()));//estrae il nome del file da caricare sottraendo dal path completo del file salvato la parte relativa alla locazione nella memoria interna del telefono
        upload = new UploadData(new AsyncResponse() {

            @Override
            public void processFinish(String output) {
                if (output!=null)//se e' diverso da null vuol dire che contiene il path del file non caricato
                {
                    showToast("transfer failed");
                    //Adding item to List
                    NotSentFilesName.add(NotSentFilesName.size(), output);//this will add string con il path del fil eat the next index
                    NotSentFilesName.add(NotSentFilesName.size(), filenameAcc);// nome del filee cartella  con l'orario a cui è partito il caricamento

                }
                else showToast("tutto ok");
            }
        });
        upload.UploadData_parameters(this, mDBApi, lastfiles.tell_accelerometer(), user_child.tellUserFolder(), user_child.Acquisitions + filenameAcc);
        upload.execute();
        s1 = lastfiles.tell_gyroscope();
        s2 = user_child.tellAcquisitionsFolder();


        final String filenameGyro="/" + formatter.format(now).toString() +"/" +(s1.substring(s2.length(),s1.length()));
        upload = new UploadData(new AsyncResponse() {

            @Override
            public void processFinish(String output) {
                if (output!=null)//se e' diverso da null
                {
                    showToast("transfer failed");
                    //Adding item to List
                    NotSentFilesName.add(NotSentFilesName.size(), output);               //this will add string at the next index
                    NotSentFilesName.add(NotSentFilesName.size(), filenameGyro);

                }
                else showToast("tutto ok");
            }
        });
        upload.UploadData_parameters(this, mDBApi, lastfiles.tell_gyroscope(), user_child.tellUserFolder(), user_child.Acquisitions +filenameGyro);
        upload.execute();


        s1 = lastfiles.tell_battery();
        s2 = user_child.tellAcquisitionsFolder();


        final String filenameBattery="/" + formatter.format(now).toString() +"/" +(s1.substring(s2.length(),s1.length()));
        upload = new UploadData(new AsyncResponse() {

            @Override
            public void processFinish(String output) {
                if (output!=null)//se e' diverso da null
                {
                    showToast("transfer failed");
                    //Adding item to List
                    NotSentFilesName.add(NotSentFilesName.size(), output);               //this will add string at the next index
                    NotSentFilesName.add(NotSentFilesName.size(), filenameBattery);

                }
                else showToast("tutto ok");
            }
        });
        upload.UploadData_parameters(this, mDBApi, lastfiles.tell_gyroscope(), user_child.tellUserFolder(), user_child.Acquisitions +filenameBattery);
        upload.execute();




        UploadNotSentFiles(NotSentFilesName);//caricati gli ultimi file carica dei file che non sono stati caricati

        download(null);//prova a scaricare un aggiornamento e ad installarlo

    }



// metodo che viene chiamato dalla fine della mainactivity, a cui vengono passati i path dei file appena creati
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK&&requestCode==REQUEST_DATA) {
            lastfiles = (LastFiles)data.getSerializableExtra("files"); //gli vengono passati qui in lastfiles

            upload_lastFiles(); //allora prova a caricare tutto sul cloud
        }
    }
    //==============================================================================================
    //==============================================================================================
    //  CONTROLLO CHARGE
    //==============================================================================================
    //==============================================================================================

    //==========================================================================
    private BroadcastReceiver mBatChargeOn = new BroadcastReceiver() {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            //==========================================================================
            showToast("batteryON");
            start_other(null);


        }
    };
     //PLOT TOAST
    private void showToast(String msg) {
        Toast error = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
        error.show();
    }
    //==========================================================================
    private void store_user(String n,String s){
        //==========================================================================
        user_child=new User(n,s,null);
        XML_handler handler=new XML_handler();
        handler.write(user_child);



     /*   SharedPreferences prefs = getSharedPreferences(USER_PREFS, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.putString(USER_NAME, n);
        edit.putString(USER_SURNAME, s);
        edit.commit();*/
        return;
    }
    //==========================================================================
    private void load_user()
    //==========================================================================
    {
        //SharedPreferences prefs = getSharedPreferences(USER_PREFS, 0);
        //String name = prefs.getString(USER_NAME, null);
        //String surname = prefs.getString(USER_SURNAME, null);

        XML_handler handler=new XML_handler();

        user_child=handler.read();

        edit_name.setText(user_child.tellName());
        edit_surname.setText(user_child.tellSurname());


    }
    //==========================================================================
    private void storeAuth(AndroidAuthSession session) {
        //==========================================================================
        // Store the OAuth 2 access token, if there is one.
        String oauth2AccessToken = session.getOAuth2AccessToken();
        if (oauth2AccessToken != null) {
            SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
            SharedPreferences.Editor edit = prefs.edit();
            edit.putString(ACCESS_KEY_NAME, "oauth2:");
            edit.putString(ACCESS_SECRET_NAME, oauth2AccessToken);
            edit.commit();
            return;
        }
    }

    //==========================================================================
    private void loadAuth(AndroidAuthSession session) {
        //==========================================================================
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        String key = prefs.getString(ACCESS_KEY_NAME, null);
        String secret = prefs.getString(ACCESS_SECRET_NAME, null);
        if (key == null || secret == null || key.length() == 0 || secret.length() == 0) return;

        if (key.equals("oauth2:")) {
            // If the key is set to "oauth2:", then we can assume the token is for OAuth 2.
            session.setOAuth2AccessToken(secret);
        } else {
            // Still support using old OAuth 1 tokens.
            session.setAccessTokenPair(new AccessTokenPair(key, secret));
        }
    }
    //==========================================================================
    private AndroidAuthSession buildSession() {
        //==========================================================================
        AppKeyPair appKeyPair = new AppKeyPair(APP_KEY, APP_SECRET);

        AndroidAuthSession session = new AndroidAuthSession(appKeyPair);
        loadAuth(session);
        return session;
    }

    //==========================================================================
    public void connect_click(View v) {
        //==========================================================================

        if (mLoggedIn) {
            showToast("You are connected");
        } else {
            //====RETRIEVES CHILD DATA========
            store_user(edit_name.getText().toString(),edit_surname.getText().toString());
            // Start the remote authentication
            showToast("Connecting");
            mDBApi.getSession().startOAuth2Authentication(Init_Activity.this);

        }
        mLoggedIn=(mDBApi.getSession().isLinked());
    }
    private void logOut() {
        // Remove credentials from the session
        mDBApi.getSession().unlink();

        // Clear our stored keys
        clearKeys();
        // Change UI state to display logged out version

    }
    //==========================================================================
    public void upload_click(View v){
        //==========================================================================


        if (mLoggedIn) {
            String path =user_child.tellPathToUserMetadata();
            UploadData upload = new UploadData(new AsyncResponse() {

                @Override
                public void processFinish(String output) {
                    if (output!=null)//se e' diverso da null
                    {
                        showToast("transfer failed");
                        //Adding item to List
                        NotSentFilesName.add(NotSentFilesName.size(), output);//this will add string at the next index

                    }
                    else showToast("tutto ok");
                }
            });
            upload.UploadData_parameters(this, mDBApi, path, user_child.tellUserFolder(), user_child.Metadata);
            upload.execute();
        } else {
            //====RETRIEVES CHILD DATA========

            showToast("Please Press Connect");

        }
        mLoggedIn=(mDBApi.getSession().isLinked());
    }
    public void unlink_click(View v){

        if (mLoggedIn) {
            showToast("Disconnecting");
            logOut();
        } else {
            showToast("Please Press Connect");
        }
        mLoggedIn=(mDBApi.getSession().isLinked());



    }
    //==========================================================================
    private void clearKeys() {
        //==========================================================================
        SharedPreferences prefs = getSharedPreferences(ACCOUNT_PREFS_NAME, 0);
        SharedPreferences.Editor edit = prefs.edit();
        edit.clear();
        edit.commit();
    }


    //==========================================================================
    private void checkAppKeySetup() {
        //==========================================================================
        // Check to make sure that we have a valid app key
        if (APP_KEY.startsWith("CHANGE") || APP_SECRET.startsWith("CHANGE")) {
            showToast(" You haven't log your app properly on DB");
            finish();
            return;
        }

        // Check if the app has set up its manifest properly.
        Intent testIntent = new Intent(Intent.ACTION_VIEW);
        String scheme = "db-" + APP_KEY;
        String uri = scheme + "://" + AuthActivity.AUTH_VERSION + "/test";
        testIntent.setData(Uri.parse(uri));
        PackageManager pm = getPackageManager();
        if (0 == pm.queryIntentActivities(testIntent, 0).size()) {
            showToast("URL scheme in your app's " +
                    "manifest is not set up correctly. You should have a " +
                    "com.dropbox.client2.android.AuthActivity with the " +
                    "scheme: " + scheme);
            finish();
        }
    }


    //==========================================================================
    public void start_other(View v){
        //==========================================================================

        Intent intent=new Intent(Init_Activity.this,MainActivity.class);
        intent.putExtra("user", user_child);
        startActivityForResult(intent, REQUEST_DATA);

    }
    //==========================================================================
    public void download(View v)  //scarica un .apk a cui è stato cambiata l'estensione a .zip
    //==========================================================================
    {
        String filename = "/Update.zip";
        String filenamefinal = "/Update.apk";
        //anche nel costruttore di downloaddata va implementato processfinish di asyncresponse se si vuole ottenere un risultato dal thread in background
        DownloadData download=new DownloadData(this,mDBApi,user_child.tellBaseFolder()+filenamefinal,user_child.tellBaseFolder(),"/UPDATES",filename,new AsyncResponse() {

            @Override
            public void processFinish(String output) {
                if (output.equals("failed"))
                {

                     //se e' failed è fallito il download, quindi non c'è niente da fare

                }
                else if(output.equals(user_child.tellcurrentSWVersion())) {
                    //se la versione scaricata è uguale a quella corrente non fare nulla;
                }
                else {
                    //c'è una nuova versione online! installala

                    user_child.setcurrentSWVersion(output);//si salva la nuova stringa della versione;
                    update(null);//la installa

                    XML_handler handler= new XML_handler();
                    handler.write(user_child);//aggiorna XML
                }


            }
        });

        download.execute();
    }
    //==========================================================================
    public void update(View v)
    //==========================================================================
    {

        UpdateApp updateapp=new UpdateApp(user_child);
        updateapp.setContext(this);
        updateapp.execute();
    }

    public void UploadNotSentFiles(List<String> list_of_files){

        int numOfFilesRemaining=list_of_files.size()-1;

        while(numOfFilesRemaining>=0) {

            UploadData upload = new UploadData(new AsyncResponse() {

                @Override
                public void processFinish(String output) {
                    if (output != null)//se e' diverso da null
                    {
                        showToast("transfer failed");
                        //Adding item to List


                    } else {

                        upload_list_cleaner();
                    }
                }
            });//??????perche devo farlo ogni volta?
            upload.UploadData_parameters(this, mDBApi, list_of_files.get(numOfFilesRemaining - 1), user_child.tellUserFolder()+"/Acquisitions", list_of_files.get(numOfFilesRemaining));
            upload.execute();
            numOfFilesRemaining = numOfFilesRemaining - 2;//sottrae due, perchè uno è il path del file e uno e il relativo path su db
            if (numOfFilesRemaining == -1)//se èarrivata a -1 vuol dire che può resettare la lista. li ha caricati tutti
            {
              upload_finished=true;
            }
        }
        }
    private void upload_list_cleaner()
    {
        if(upload_finished)
        {
            NotSentFilesName.clear();//resetta la lista
            showToast("list cleared");
        }
        upload_finished=false;
    }


    //==========================================================================
    private void SaveErrorLog(String msg){
        //==========================================================================
        String StringToSend = "" + SystemClock.elapsedRealtime() + "\t" + msg +"\n";
        LogFile_Handler BkgSave_LogHandler = new LogFile_Handler(StringToSend);
        BkgSave_LogHandler.execute();
    }
}
