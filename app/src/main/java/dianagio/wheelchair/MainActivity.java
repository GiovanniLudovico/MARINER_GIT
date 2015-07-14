package dianagio.wheelchair;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.table.TableOperationCallback;
import com.yoctopuce.YoctoAPI.YAPI;
import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YDigitalIO;
import com.yoctopuce.YoctoAPI.YModule;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import static com.yoctopuce.YoctoAPI.YDigitalIO.FindDigitalIO;

// YOCTOPUCE LIBRARIES


//==========================================================================
public class MainActivity extends Activity
        implements SensorEventListener, YDigitalIO.UpdateCallback {
//==========================================================================

    SensorManager mSensorManager;
    Sensor mAcc;
    Sensor mGyro;
    TextView acc_view;
    TextView gyro_view;
    TextView MaxiIO_view;
    TextView battery_view;

    /*String Acc_data;        // qui accodo campioni
    String Gyro_data;
    String Motor_data;
    String Battery_data;
    */


     // ciao belli
    //per disattivare yocto (debug gio)
    private boolean UseYocto= false;

    // PER AZURE
    private MobileServiceClient mClient;

    public class Item {
        public String Id;
        public String string_acc;
        public String string_gyro;
        public String string_mtr;
    }

    private static final float SHAKE_THRESHOLD = SensorManager.GRAVITY_EARTH + 2;
    private static final float STILL_THRESHOLD = SensorManager.GRAVITY_EARTH + 1/10;
    private boolean isMotorOn = false;
    private long lastUpdate = 0;

    // SOURCE:
    //  0: MOTOR
    //  1: ACCELEROMETER
    //  2: GYROSCOPE
    public static final short Motor_ID      = 0;
    public static final short Acc_ID        = 1;
    public static final short Gyro_ID       = 2;
    public static final short Battery_ID    = 3;

    // MOTOR STATE:
    // 0: OFF
    // 1: ON
    public static final short Motor_OFF_ID = 0;
    public static final short Motor_ON_ID = 1;

    // alloco le strutture di memoria per i dati
    /*public sample3axes Acc_data_array[] =       new sample3axes[2160000];       // numero di campioni nell'hp di acquisizione a 50Hz continuata per 12h
    public sample3axes Gyro_data_array[] =      new sample3axes[2160000];       // numero di campioni nell'hp di acquisizione a 50Hz continuata per 12h
    public sampleMotor Motor_data_array[] =     new sampleMotor[43200];         // numero di campioni nell'hp di acquisizione a 1Hz continuata per 12h
    public sampleBattery Battery_data_array[] = new sampleBattery[720];         // numero di campioni nell'hp di acquisizione 1 volta al minuto continuata per 12h
*/
    static final short buffer_dim_inert =           1000;
    static final short buffer_dim_batt_motor =      5;

    sample3axes Acc_data =          new sample3axes(buffer_dim_inert);
    sample3axes Gyro_data =         new sample3axes(buffer_dim_inert);
    sampleMotor Motor_data =        new sampleMotor(buffer_dim_batt_motor);
    sampleBattery Battery_data =    new sampleBattery(buffer_dim_batt_motor);

    String Acc_Path="";
    String Gyro_Path="";
    String Motor_Path="";
    String Battery_Path="";


    // indici per scorrere gli array
    int Acc_data_array_index = 0;
    int Gyro_data_array_index = 0;
    int Motor_data_array_index = 0;
    int Battery_data_array_index = 0;

    //classi per comunicazione tra activity
    User user;//input
    LastFiles lastfiles;//output

    static String mFileName = null;


    // timer per controllo batteria
    Timer MyTimer;
    Timer Timer4Save;

    //==========================================================================
    //==========================================================================
    // per debug e per migliorare salvataggio
    TextView tsave_view;
    TextView tsample_view;
    long tsample = 0;
    long tsave = 0;

    int motor_bytes_written = 0;
    int acc_bytes_written = 0;
    int gyro_bytes_written = 0;
    int battery_bytes_written = 0;

    public static final short motor_sample_dim_bytes = 2 + 8;       // 2bytes(short) + 8byte(long)
    public static final short acc_sample_dim_bytes = (3*4) +8;      // 3*4byte(float) + 8byte(long)
    public static final short gyro_sample_dim_bytes = (3*4) +8;     // 3*4byte(float) + 8byte(long)
    public static final short battery_sample_dim_bytes = 4 + 8;     // 3*4byte(float) + 8byte(long)ù

    int Motor_whereToStart_index = 0;
    int Acc_whereToStart_index = 0;
    int Gyro_whereToStart_index = 0;
    int Battery_whereToStart_index = 0;

    //==========================================================================
    //==========================================================================


    @Override
    //==========================================================================
    protected void onCreate(Bundle savedInstanceState) {
        //==========================================================================
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        user = (User) intent.getSerializableExtra("user"); //ingresso da initactivity
        lastfiles=new LastFiles();//uscita verso initactivity

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAcc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        WheelChair_ON(null);
       /* registerReceiver(mBatChargeOn, new IntentFilter(
                Intent.ACTION_POWER_CONNECTED));*/

        // da spostare nel click start
        registerReceiver(mBatChargeOff, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
        registerReceiver(mBatLow,       new IntentFilter(Intent.ACTION_BATTERY_LOW));
        registerReceiver(mBatOkay,      new IntentFilter(Intent.ACTION_BATTERY_OKAY));
        registerReceiver(mBatChanged,   new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        //aggiunta 25 giugno morning
        lastfiles.isyoctoinuse=UseYocto;
    }


    @Override
    //==========================================================================
    public boolean onCreateOptionsMenu(Menu menu) {
        //==========================================================================
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    //==========================================================================
    public boolean onOptionsItemSelected(MenuItem item) {
        //==========================================================================
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
    protected void onRestart(){
        super.onRestart();
    }
    @Override
    protected void onPause() {
        super.onPause();
    }
    @Override
    protected void onResume() { super.onResume();   }


    //==============================================================================================
    //==============================================================================================
    //  CONTROLLO CHARGE
    //==============================================================================================
    //==============================================================================================

    //==========================================================================
    private BroadcastReceiver mBatChargeOff = new BroadcastReceiver() {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            call_toast("battery OFF");
            WheelChair_OFF(null);
        }
    };
    //==========================================================================
    private BroadcastReceiver mBatLow = new BroadcastReceiver() {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            call_toast("battery LOW");
            WheelChair_OFF(null);
        }
    };
    //==========================================================================
    private BroadcastReceiver mBatOkay = new BroadcastReceiver() {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            call_toast("battery OKAY");
            WheelChair_ON(null);
        }
    };
    //==========================================================================
    private BroadcastReceiver mBatChanged = new BroadcastReceiver(){
        @Override
        public void onReceive(Context cont, Intent battery_intent) {
            int level = battery_intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);

            if(Battery_data_array_index < Battery_data.Time.length) {
                Battery_data.Time[Battery_data_array_index] = System.currentTimeMillis();
                Battery_data.BatLev[Battery_data_array_index] = level;

                Battery_data_array_index++;
            }
            if(Battery_data_array_index == Battery_data.Time.length){
                Background_Save bg_save = new Background_Save(null, null, null, Battery_data, Battery_Path);
                bg_save.execute();
                Battery_data_array_index = 0;
            }
        }
    };

    //==========================================================================
   /* private BroadcastReceiver mBatChargeOn = new BroadcastReceiver() {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            //==========================================================================
            call_toast("batteryON");
            start_timer();
            Acc_OnResume();
            Gyro_OnResume();
            //Get Battery %
        }
    };*/


    //==============================================================================================
    //==============================================================================================
    //  EVENTI CARROZZINA E MOTORE
    //==============================================================================================
    //==============================================================================================

    //==========================================================================
    public void WheelChair_ON(View view) {
    //==========================================================================
        CreateMyFile();

        IsYoctoConnected();
        Acc_OnResume();
        Gyro_OnResume();

        if(UseYocto==true) {
            Start_Yocto();
        }
        else
        call_toast("you are not using Yoctopuce");

        call_toast("Wheelchair ON");

        //Start_BatteryCheck();

        tsample = System.currentTimeMillis();

        /*this.Timer4Save = new Timer();
        TimerTask_Save myTimerTask_Save = new TimerTask_Save();
        this.Timer4Save.schedule(myTimerTask_Save, 10, 500);*/
    }

    /*
    //==========================================================================
    class TimerTask_Save extends TimerTask {
        //==========================================================================
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (motor_bytes_written >= 512){
                        Data_FromArrayToString(Motor_ID);
                        motor_bytes_written=0;
                    }
                    if (acc_bytes_written >= 1024){
                        Data_FromArrayToString(Acc_ID);
                        acc_bytes_written=0;
                    }
                    if (gyro_bytes_written >= 1024){
                        Data_FromArrayToString(Gyro_ID);
                        gyro_bytes_written=0;
                    }
                    if (battery_bytes_written >= 512){
                        Data_FromArrayToString(Battery_ID);
                        battery_bytes_written=0;
                    }
                }
            });
        }
    }
    */
    //==========================================================================
    public void WheelChair_OFF(View view) {
    //==========================================================================

        /*this.Timer4Save.cancel();
        this.Timer4Save.purge();   // Removes all cancelled tasks from this timer's task queue.
        this.Timer4Save = null;
        */

        tsample = System.currentTimeMillis() - tsample;
        tsample_view = (TextView) findViewById(R.id.tsample_view);
        tsample_view.setText("Tsample= " + tsample + " ms");


        //Stop_BatteryCheck();

        //FileSaver_Binary(Battery_ID);

        if(UseYocto==true) {
            Stop_Yocto();
            //Data_FromArrayToString(Motor_ID);
            //FileSaver(Motor_ID);
        }
        else {
        }
        Acc_OnPause();
        Gyro_OnPause();

        call_toast("Wheelchair OFF");

        tsave = System.currentTimeMillis();
/*
        Data_FromArrayToString(Acc_ID);
        Data_FromArrayToString(Gyro_ID);
        Data_FromArrayToString(Battery_ID);


        FileSaver(Acc_ID);
        FileSaver(Gyro_ID);
        FileSaver(Battery_ID);
*/
        tsave = System.currentTimeMillis() - tsave;
        tsave_view = (TextView)findViewById(R.id.tsave_view);
        tsave_view.setText("Tsave= " + tsave + " ms");

       // myAzure_TransferData();

        // pausa della activity: spegne acquisizioni e messaggi
        Intent intent= new Intent();
        intent.putExtra("files", lastfiles);
        setResult(RESULT_OK, intent);
        finish();
    }

    //==========================================================================
    public void Motor_ON(View view) {
        //==========================================================================
        // salva su buffer info motore ON + timestamp

        call_toast("Motor ON");

        //AppendNewData(Motor_ID, null, 1);
        if(UseYocto==true) {
            Init_Yocto(MaxiIO);
        }
        else
            call_toast("you are not using Yoctopuce");


    }

    //==========================================================================
    public void Motor_OFF(View view) {
        //==========================================================================
        // salva su buffer info motore OFF + timestamp

        call_toast("Motor OFF");

        //AppendNewData(Motor_ID, null, 0);
    }

    @Override
    //==========================================================================
    public void onSensorChanged(SensorEvent event) {
        //==========================================================================

        switch(event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                if(Acc_data_array_index < Acc_data.Time.length) {

                    Acc_data.Time[Acc_data_array_index] = System.currentTimeMillis();
                    Acc_data.X[Acc_data_array_index] = event.values[0];
                    Acc_data.Y[Acc_data_array_index] = event.values[1];
                    Acc_data.Z[Acc_data_array_index] = event.values[2];

                    Acc_data_array_index++;

                    if(Acc_data_array_index == Acc_data.Time.length){
                        Background_Save bg_save=new Background_Save(null, Acc_data, null, null , Acc_Path);
                        bg_save.execute();
                        Acc_data_array_index = 0;
                    }
                }
                else {
                    // gestione array troppo pieno
                }
                break;

            case Sensor.TYPE_GYROSCOPE:
                if(Gyro_data_array_index < Gyro_data.Time.length) {

                    Gyro_data.Time[Acc_data_array_index] = System.currentTimeMillis();
                    Gyro_data.X[Acc_data_array_index] = event.values[0];
                    Gyro_data.Y[Acc_data_array_index] = event.values[1];
                    Gyro_data.Z[Acc_data_array_index] = event.values[2];

                    Gyro_data_array_index++;

                    if(Gyro_data_array_index == Gyro_data.Time.length){
                        Background_Save bg_save = new Background_Save(null, Gyro_data, null, null , Gyro_Path);
                        bg_save.execute();
                        Gyro_data_array_index = 0;
                    }
                }
                else {
                    // gestione array troppo pieno
                }
                break;
        }


    }

    //==========================================================================
    public float LPF(float data){
        //==========================================================================
        float x0 = data/2;
        float y0;
        float y1 = 0;

        y0 = y1 /2;
        y1=x0+y0;

        return y1;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    //==========================================================================
    protected void Acc_OnPause(){
        //==========================================================================
        super.onPause();
        mSensorManager.unregisterListener(this, mAcc);
    }

    //==========================================================================
    protected void Acc_OnResume(){
        //==========================================================================
        super.onResume();
        acc_view = (TextView)findViewById(R.id.acc_view);
        mSensorManager.registerListener(this, mAcc, 20000);//20.000: us per ottenere Fs=50Hz
    }

    //==========================================================================
    protected void Gyro_OnPause(){
        //==========================================================================
        super.onPause();
        mSensorManager.unregisterListener(this, mGyro);
    }

    //==========================================================================
    protected void Gyro_OnResume(){
        //==========================================================================
        super.onResume();
        gyro_view = (TextView)findViewById(R.id.gyro_view);
        mSensorManager.registerListener(this, mGyro, 20000);//20.000: us per ottenere Fs=50Hz
    }



    //==============================================================================================
    //==============================================================================================
    //  SALVATAGGIO E TRASFERIMENTO DEI DATI
    //==============================================================================================
    //==============================================================================================

    // MOTOR STATE:
    // 0: OFF
    // 1: ON

    //==========================================================================
    protected void CreateMyFile() {
        //==========================================================================
        FileOutputStream outputStream;
        String stringafarlocca="";

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date now = new Date();

        //gestisce il nome dei file con la data
        mFileName = user.tellAcquisitionsFolder();

        // MOTORE
        mFileName += "/Motor_"+ formatter.format(now).toString()+ ".txt";
        lastfiles.set_motor(mFileName);
        Motor_Path = mFileName;

        try {
            outputStream = new FileOutputStream(mFileName, true);
            outputStream.write(stringafarlocca.getBytes());
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // ACCELEROMETRO
        mFileName = user.tellAcquisitionsFolder();
        mFileName += "/Acc_"+ formatter.format(now).toString()+ ".txt";
        lastfiles.set_acc(mFileName);
        Acc_Path = mFileName;

        try {
            outputStream = new FileOutputStream(mFileName, true);
            outputStream.write(stringafarlocca.getBytes());
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        // GIROSCOPIO
        mFileName = user.tellAcquisitionsFolder();
        mFileName += "/Gyro_"+ formatter.format(now).toString()+ ".txt";
        lastfiles.set_gyro(mFileName);
        Gyro_Path = mFileName;

        try {
            outputStream = new FileOutputStream(mFileName, true);
            outputStream.write(stringafarlocca.getBytes());
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // BATTERY
        mFileName = user.tellAcquisitionsFolder();
        mFileName += "/Battery_"+ formatter.format(now).toString()+ ".txt";
        lastfiles.set_battery(mFileName);
        Battery_Path = mFileName;

        try {
            outputStream = new FileOutputStream(mFileName, true);
            outputStream.write(stringafarlocca.getBytes());
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }


    }




    /*
    // APPENDS NEW DATA TO SOURCES STRINGS
    //==========================================================================
    protected void AppendNewData( int source , SensorEvent event, int MotorState){
        //==========================================================================
        switch(source) {
            case Motor_ID:
                Motor_data = Motor_data + System.currentTimeMillis() + "\t" + MotorState + "\n";
                break;

            case Acc_ID: //dati provenienti dall'accelerometro
                Acc_data = Acc_data + System.currentTimeMillis() + "\t" + event.values[0] + "\t" + event.values[1] + "\t" + event.values[2] + "\n";
                break;

            case Gyro_ID: //dati provenienti dal giroscopio
                Gyro_data = Gyro_data + System.currentTimeMillis() + "\t" + event.values[0] + "\t" + event.values[1] + "\t" + event.values[2] + "\n";
                break;
        }
    }
    */

    /*
    //==========================================================================
    private void Data_FromArrayToString(int source){
        //==========================================================================

        switch(source) {
            case Motor_ID:
                sampleMotor tmp1;
                for (int aux = Motor_whereToStart_index; aux < Motor_data_array_index; aux++) {
                    tmp1 = Motor_data_array[aux];
                    Motor_data = Motor_data + tmp1.Time + "\t" + tmp1.Status +"\n";
                }
                Motor_whereToStart_index = Motor_data_array_index;
                break;
            case Acc_ID:
                sample3axes tmp2;

                for (int aux = Acc_whereToStart_index; aux < Acc_data_array_index; aux++) {
                    tmp2 = Acc_data_array[aux];
                    Acc_data = Acc_data + tmp2.Time + "\t" + tmp2.X + "\t" + tmp2.Y + "\t" + tmp2.Z +"\n";
                }
                Acc_whereToStart_index = Acc_data_array_index;
                break;
            case Gyro_ID:
                sample3axes tmp3;

                for (int aux = Gyro_whereToStart_index; aux < Gyro_data_array_index; aux++) {
                    tmp3 = Gyro_data_array[aux];
                    Gyro_data = Gyro_data + tmp3.Time + "\t" + tmp3.X + "\t" + tmp3.Y + "\t" + tmp3.Z +"\n";
                }
                Gyro_whereToStart_index = Gyro_data_array_index;
                break;
            case Battery_ID:
                sampleBattery tmp4;

                for (int aux = Battery_whereToStart_index; aux < Battery_data_array_index; aux++) {
                    tmp4 = Battery_data_array[aux];
                    Battery_data = Battery_data + tmp4.Time + "\t" + tmp4.BatLev + "\n";
                }
                Battery_whereToStart_index = Battery_data_array_index;
                break;
        }
    }
*/


/*
    //==========================================================================
    private int FileSaver(int source){
        //==========================================================================
        FileOutputStream outputStream;

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date now = new Date();

        //gestisce il nome dei file con la data
        mFileName = user.tellAcquisitionsFolder();
        switch (source){
            case Motor_ID:
                mFileName += "/Motor_"+ formatter.format(now).toString()+ ".txt";
                lastfiles.set_motor(mFileName);

                try {
                    outputStream = new FileOutputStream(mFileName);
                    outputStream.write(Motor_data.getBytes());
                    outputStream.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                break;
            case Acc_ID:
                mFileName += "/Acc_"+ formatter.format(now).toString()+ ".txt";
                lastfiles.set_acc(mFileName);

                try {
                    outputStream = new FileOutputStream(mFileName);
                    outputStream.write(Acc_data.getBytes());
                    outputStream.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            case Gyro_ID:
                mFileName += "/Gyro_"+ formatter.format(now).toString()+ ".txt";
                lastfiles.set_gyro(mFileName);

                try {
                    outputStream = new FileOutputStream(mFileName);
                    outputStream.write(Gyro_data.getBytes());
                    outputStream.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case Battery_ID:
                mFileName += "/Battery_"+ formatter.format(now).toString()+ ".txt";
                lastfiles.set_battery(mFileName);

                try {
                    outputStream = new FileOutputStream(mFileName);
                    outputStream.write(Battery_data.getBytes());
                    outputStream.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }

    return 0;
    }
*/

/*
    //==========================================================================
    private void FileSaver_Binary(int source) {
        //==========================================================================
        FileOutputStream fos = null;
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date now = new Date();
        //gestisce il nome dei file con la data
        mFileName = user.tellAcquisitionsFolder();

        try {


            switch (source) {
                case Battery_ID:
                    int tmp_int = 0;

                    mFileName += "/Batt_" + formatter.format(now).toString() + ".bin";
                    fos = new FileOutputStream(mFileName);
                    BufferedOutputStream out = new BufferedOutputStream(fos);

                    lastfiles.set_motor(mFileName);

                    for (int aux = 0; aux < Battery_data_array_index; aux++) {
                        //MSB
                        tmp_int = (int) (Battery_data_array[aux].Time & 0xffffffff);
                        out.write( tmp_int );
                        //LSB
                        tmp_int = (int) ((Battery_data_array[aux].Time >> 32) & 0xffffffff);
                        out.write( tmp_int);

                        //out.write( Acc_data_array[aux].X );
                    }

                    break;
            }

            //out.flush();
            fos.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }

    }

    */


    /*
        //==========================================================================
    private void myAzure_TransferData(){
        //==========================================================================
        Item myItem = new Item();
        myItem.string_mtr = Motor_data;
        myItem.string_acc = Acc_data;
        myItem.string_gyro = Gyro_data;

        try {
            // Create the Mobile Service Client instance, using the provided
            // Mobile Service URL and key
            mClient = new MobileServiceClient(
                    "https://progcarrozzine.azure-mobile.net/",
                    "dYxeKlECJxnkeXzqNWhmcUBKjTnBNB20",
                    this);

            // Get the Mobile Service Table instance to use

        }catch (MalformedURLException e) {
            call_toast("ERROR");
        }

        mClient.getTable(Item.class).insert(myItem, new TableOperationCallback<Item>() {
            public void onCompleted(Item entity, Exception exception, ServiceFilterResponse response) {
                if (exception == null) {
                    // Insert succeeded
                    call_toast("DATA SENT");
                } else {
                    // Insert failed
                    call_toast("DATA SEND FAIL");
                }
            }
        });
        // FINE AZURE==========================================================
    }
    */

    //==========================================================================
    private void call_toast(CharSequence text){
    //==========================================================================
        //queste righe fanno saltare fuori un toast è una specie di pop-up con dentro text come testo
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    //==============================================================================================
    //==============================================================================================
    // YOCTOPUCE - CONTROLLO MAXI-IO
    //==============================================================================================
    //==============================================================================================

    String MaxiIO_SerialN;
    YDigitalIO MaxiIO;
    YModule tmp;

    //==========================================================================
    protected void Start_Yocto() {
        //==========================================================================
        // Connect to Yoctopuce Maxi-IO
        try {
            YAPI.EnableUSBHost(getApplicationContext());
            YAPI.RegisterHub("usb");

            tmp = YModule.FirstModule();
            while (tmp != null) {
                if (tmp.get_productName().equals("Yocto-Maxi-IO")) {

                    MaxiIO_SerialN = tmp.get_serialNumber();
                    MaxiIO = FindDigitalIO(MaxiIO_SerialN);
                    if(MaxiIO.isOnline()) {
                        call_toast("Maxi-IO connected");
                        MaxiIO.registerValueCallback(this);
                        YAPI.HandleEvents();
                    }
                }
                else {
                    call_toast("MAXI-IO NOT CONNECTED");
                }
                tmp = tmp.nextModule();
            }
            r.run();
        } catch (YAPI_Exception e) {
            e.printStackTrace();
        }
        handler.postDelayed(r, 1000);
    }

    //==========================================================================
    protected void Init_Yocto(YDigitalIO moduleName){
        //==========================================================================

        // set the port as input
        try {
            moduleName.set_portDirection(0x0F);             //bit 0-3: OUT; bit 4-7: IN
            moduleName.set_portPolarity(0);                 // polarity set to regular
            moduleName.set_portOpenDrain(0);                // No open drain
            moduleName.set_portState(0x00);                 // imposta valori logici di uscita inizialmente tutti bassi
        }
        catch(YAPI_Exception e){
            e.printStackTrace();
        }
    }

    //==========================================================================
    protected void Stop_Yocto() {
        //==========================================================================
    YAPI.FreeAPI();
    }


    public void use_yocto_check(View v)
    {
        CheckBox check= (CheckBox)findViewById(R.id.use_yocto);
        if (check.isChecked()==true) {
        UseYocto=true;
            call_toast("Yocto ON");
        }
        else {
            UseYocto = false;
            call_toast("Yocto OFF");
        }
        //aggiunta 25 giugno morning
        lastfiles.isyoctoinuse=UseYocto;
    }

    //==============================================================================================
    //==============================================================================================
    //  GESTIONE EVENTI YOCTO
    //==============================================================================================
    //==============================================================================================

    private Handler handler = new Handler();
    private int _outputdata;
    final Runnable r = new Runnable()
    {
        public void run()
        {
            if (MaxiIO_SerialN != null) {
                YDigitalIO io = YDigitalIO.FindDigitalIO(MaxiIO_SerialN);
                try {
                    YAPI.HandleEvents();

                    io.set_portDirection(0x0F);             //bit 0-3: OUT; bit 4-7: IN ( bit set to 0)
                    io.set_portPolarity(0);                 // polarity set to regular
                    io.set_portOpenDrain(0);                // No open drain

                    //_outputdata = (_outputdata + 1) % 16;   // cycle ouput 0..15
                    //io.set_portState(_outputdata);          // set output value

                    // read motor value
                    int inputdata = io.get_bitState(7);      // read bit value
                    /*
                    String line = "";                        // display part state value as binary
                    line = line + inputdata;

                    TextView view = (TextView) findViewById(R.id.MaxiIO_view);
                    view.setText("port value = " + line);
                    */
                } catch (YAPI_Exception e) {
                    e.printStackTrace();
                }
            }
            handler.postDelayed(this, 1000);
        }
    };



    int OldInputData;
    int NewInputData;
    @Override
    //==========================================================================
    public void yNewValue(YDigitalIO yDigitalIO, String s) {
        //==========================================================================
        TextView view = (TextView) findViewById(R.id.event_view);
        view.setText(s);

        try {
            OldInputData = NewInputData;
            NewInputData = MaxiIO.get_bitState(7);      // read bit value (motor input)

            if (NewInputData != OldInputData) { // something occurred

                sampleMotor tmp = new sampleMotor(1);
                tmp.Time[0] = System.currentTimeMillis();

                if (NewInputData == 1 && OldInputData == 0) {
                    // occurred motor event: now it is ON
                    tmp.Status[0] = Motor_ON_ID;

                } else if (NewInputData == 0 && OldInputData == 1) {
                    // occurred motor event: now it is OFF
                    tmp.Status[0] = Motor_OFF_ID;
                }

                if (Motor_data_array_index < Motor_data.Time.length) {
                    Motor_data.Time[Motor_data_array_index] = tmp.Time[0];
                    Motor_data.Status[Motor_data_array_index] = tmp.Status[0];
                    Motor_data_array_index++;

                    if(Motor_data_array_index == Motor_data.Time.length){
                        Background_Save bg_save = new Background_Save(Motor_data, null, null, null , Motor_Path);
                        bg_save.execute();
                        Motor_data_array_index = 0;
                    }

                } else {
                    // gestione array troppo pieno
                }

            }
        } catch (YAPI_Exception e) {
            e.printStackTrace();
        }
    }
    //==========================================================================
    public void IsYoctoConnected() {
        //==========================================================================
        try {
            YAPI.EnableUSBHost(getApplicationContext());
            YAPI.RegisterHub("usb");

            TextView view = (TextView) findViewById(R.id.MaxiIO_view);

            tmp = YModule.FirstModule();
            while (tmp != null) {
                if (tmp.get_productName().equals("Yocto-Maxi-IO")) {

                    MaxiIO_SerialN = tmp.get_serialNumber();
                    MaxiIO = FindDigitalIO(MaxiIO_SerialN);

                    if(MaxiIO.isOnline()) {
                        UseYocto = true;
                        view.setText("MaxiIO connected");
                    }
                    else{
                        UseYocto = false;
                    }
                }
                else {
                    UseYocto = false;
                }
                tmp = tmp.nextModule();
            }
           /* if (tmp == null || !UseYocto) {
                view.setText("MaxiIO NOT connected");
            }*/
        } catch (YAPI_Exception e) {
            e.printStackTrace();
        }
    }


    //==============================================================================================
    //==============================================================================================
    //  GESTIONE TIMER PER CONTROLLO CARICA
    //==============================================================================================
    //==============================================================================================


    private static final int ONE_MINUTE = 60000;  // ONE MINUTE IN MILLISECONDS
    int i=0;

    //==========================================================================
    public void Start_BatteryCheck() {
        //==========================================================================
        /*this.MyTimer = new Timer();
        MyTimerTask myTimerTask = new MyTimerTask();
        this.MyTimer.schedule(myTimerTask, 1000, ONE_MINUTE);  // PARAM: task to schedule, delay for first execution, period between subsequent executions.(time: [msec])

*/
        battery_view = (TextView)findViewById(R.id.battery_view);
    }

    /*
    //==========================================================================
    public void Stop_BatteryCheck(){
        //==========================================================================
        this.MyTimer.cancel();
        this.MyTimer.purge();   // Removes all cancelled tasks from this timer's task queue.
        this.MyTimer = null;
    }

//==========================================================================
    class MyTimerTask extends TimerTask {
    //==========================================================================
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    sampleBattery tmp = new sampleBattery(1);

                    IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                    Intent batteryStatus = registerReceiver(null, ifilter);
                    int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                    int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

                    int batteryPct = level;//   / (float)scale;

                    i++;
                    battery_view.setText(i + "Battery: " + batteryPct +"%");

                    tmp.Time = System.currentTimeMillis();
                    tmp.BatLev = level;

                    if(Battery_data_array_index < Battery_data_array.length) {
                        Battery_data_array[Battery_data_array_index++] = tmp;
                        battery_bytes_written += battery_sample_dim_bytes;
                    }
                    else {
                        // gestione array troppo pieno
                    }

                    /*
                    SE SERVIRà IN FUTURO (non testato, solo copiato da esempio)

                    //get battery temperature
                    int temp = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
                    tvTempOutput.setText(temp + "Grad");
                    pbTemp.incrementProgressBy(temp);
                    pbTemp.invalidate();

                    //get battery voltage
                    int voltage = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1);
                    tvVoltageOutput.setText(voltage + " V");
                    pbVoltage.incrementProgressBy(voltage);
                    pbVoltage.invalidate();


                }
            });
        }
    }
                    */
} // fine della MainActivity