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
import android.widget.TextView;
import android.widget.Toast;

import com.yoctopuce.YoctoAPI.YAPI;
import com.yoctopuce.YoctoAPI.YAPI_Exception;
import com.yoctopuce.YoctoAPI.YDigitalIO;
import com.yoctopuce.YoctoAPI.YModule;

import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import static com.yoctopuce.YoctoAPI.YDigitalIO.FindDigitalIO;

// MICROSOFT AZURE LIBRARIES
// YOCTOPUCE LIBRARIES
// LIBRARIES FOR SAVING DATA ON FILE


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

    // INDICATE WHEN YOCTO IS IN USE (AVAILABLE)
    private boolean UseYocto= false;

    // AZURE
    /*private MobileServiceClient mClient;
    public class Item {
        public String Id;
        public String string_acc;
        public String string_gyro;
        public String string_mtr;
    }
    */

    // constants useful when movement detection is needed
    private static final float SHAKE_THRESHOLD = SensorManager.GRAVITY_EARTH + 2;
    private static final float STILL_THRESHOLD = SensorManager.GRAVITY_EARTH + 1/10;


    // SOURCES:
    public static final short Motor_ID      = 0;
    public static final short Acc_ID        = 1;
    public static final short Gyro_ID       = 2;
    public static final short Battery_ID    = 3;

    // MOTOR STATES:
    public static final short Motor_OFF_ID = 0;
    public static final short Motor_ON_ID = 1;

    // DATA STRUCTURES AND DIMENSIONS
    static final short buffer_dim_inert =           1000;
    static final short buffer_dim_batt_motor =      5;
    sample3axes Acc_data =          new sample3axes(buffer_dim_inert);
    sample3axes Gyro_data =         new sample3axes(buffer_dim_inert);
    sampleMotor Motor_data =        new sampleMotor(buffer_dim_batt_motor);
    sampleBattery Battery_data =    new sampleBattery(buffer_dim_batt_motor);
    // indexes needed to browse arrays
    int Acc_data_array_index = 0;
    int Gyro_data_array_index = 0;
    int Motor_data_array_index = 0;
    int Battery_data_array_index = 0;

    // PATHS OF STORED FILES
    String Acc_Path="";
    String Gyro_Path="";
    String Motor_Path="";
    String Battery_Path="";
    static String mFileName = null;

    // CLASSES FOR COMMUNICATIONS BETWEEN ACTIVITIES
    User user;//input
    LastFiles lastfiles;//output




    // DEBUG THINGS
    TextView tsave_view;
    TextView tsample_view;
    long tsample = 0;
    long tsave = 0;



    @Override
    //==========================================================================
    protected void onCreate(Bundle savedInstanceState) {
        //==========================================================================
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        user = (User) intent.getSerializableExtra("user");      // GET INPUT FROM INIT ACTIVITY
        lastfiles=new LastFiles();                              // SET OUTPUT TO INIT ACTIVITY

        // INITIALISE SENSOR MANAGER
        mSensorManager =        (SensorManager) getSystemService(SENSOR_SERVICE);
        mAcc =                  mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro =                 mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // START WITH ACQUISITIONS
        WheelChair_ON(null);

        // REGISTER BROADCAST RECEIVER FOR BATTERY EVENTS
        registerReceiver(mBatChargeOff, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED));
        registerReceiver(mBatLow,       new IntentFilter(Intent.ACTION_BATTERY_LOW));
        registerReceiver(mBatOkay,      new IntentFilter(Intent.ACTION_BATTERY_OKAY));
        registerReceiver(mBatChanged,   new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        lastfiles.isyoctoinuse = UseYocto;
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
    //  CHARGE CONTROL
    //==============================================================================================
    //==============================================================================================

    //==========================================================================
    private BroadcastReceiver mBatChargeOff = new BroadcastReceiver() {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            call_toast("battery OFF");
            WheelChair_OFF(null);           // STOP ALL
        }
    };
    //==========================================================================
    private BroadcastReceiver mBatLow = new BroadcastReceiver() {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            call_toast("battery LOW");
            WheelChair_OFF(null);           // STOP ALL
        }
    };
    //==========================================================================
    private BroadcastReceiver mBatOkay = new BroadcastReceiver() {
        @Override
        //When Event is published, onReceive method is called
        public void onReceive(Context c, Intent i) {
            call_toast("battery OKAY");
            WheelChair_ON(null);            // RESTART EVERYTHING
        }
    };
    //==========================================================================
    private BroadcastReceiver mBatChanged = new BroadcastReceiver(){
        @Override
        public void onReceive(Context cont, Intent battery_intent) {
            TextView view = (TextView) findViewById(R.id.battery_view);

            // GET BATTERY LEVEL
            int level = battery_intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            view.setText("Battery: " + level + "%");

            // APPEND NEW DATA TO BATTERY DATA STRUCTURE
            if(Battery_data_array_index < Battery_data.Time.length) {
                Battery_data.Time[Battery_data_array_index] = System.currentTimeMillis();
                Battery_data.BatLev[Battery_data_array_index] = level;

                Battery_data_array_index++;
            }

            // IF THE ARRAY IS FULL THEN SAVE DATA ON FILE
            if(Battery_data_array_index == Battery_data.Time.length){
                Background_Save bg_save = new Background_Save(null, null, null, Battery_data, Battery_Path);
                bg_save.execute();
                Battery_data_array_index = 0;
            }
        }
    };


    //==============================================================================================
    //==============================================================================================
    //  MOTOR AND WHEELCHAIR EVENTS HANDLING
    //==============================================================================================
    //==============================================================================================

    //==========================================================================
    public void WheelChair_ON(View view) {
    //==========================================================================
        // CREATE LOCAL FILES
        CreateMyFile();

        // CHECK IF YOCTOPUCE IS CONNECTED AND START SAMPLING
        IsYoctoConnected();
        Acc_OnResume();
        Gyro_OnResume();
        if(UseYocto==true) {
            Start_Yocto();
        }
        else
        call_toast("you are not using Yoctopuce");

        call_toast("Wheelchair ON");

        // DEBUG
        tsample = System.currentTimeMillis();
    }

    //==========================================================================
    public void WheelChair_OFF(View view) {
    //==========================================================================

        // DEBUG
        tsample = System.currentTimeMillis() - tsample;
        tsample_view = (TextView) findViewById(R.id.tsample_view);
        tsample_view.setText("Tsample= " + tsample + " ms");

        // STOP ACQUISITIONS
        if(UseYocto==true) {
            Stop_Yocto();}
        Acc_OnPause();
        Gyro_OnPause();

        call_toast("Wheelchair OFF");

       // myAzure_TransferData();

        // SWITCH OFF ACTIVITY AND SET RESULT TO INIT ACTIVITY
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

        // APPEND INERTIAL SENSORS DATA AND SAVE THEM TO FILES
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
                    // MANAGES FULL ARRAY
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
                    // MANAGES FULL ARRAY
                }
                break;
        }


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
        mSensorManager.registerListener(this, mAcc, 20000);// 20.000 us ----> FsAMPLE = 50Hz
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
        mSensorManager.registerListener(this, mGyro, 20000);// 20.000 us ----> FsAMPLE = 50Hz
    }


    //==============================================================================================
    //==============================================================================================
    //  DATA STORAGE AND TRANSFERRING
    //==============================================================================================
    //==============================================================================================

    //==========================================================================
    protected void CreateMyFile() {
        //==========================================================================
        FileOutputStream outputStream;
        String SillyString="";

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date now = new Date();
        mFileName = user.tellAcquisitionsFolder();

        // MOTOR
        mFileName += "/Motor_"+ formatter.format(now).toString()+ ".txt";
        lastfiles.set_motor(mFileName);
        Motor_Path = mFileName;

        try {
            outputStream = new FileOutputStream(mFileName, true);
            outputStream.write(SillyString.getBytes());
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        // ACCELEROMETER
        mFileName = user.tellAcquisitionsFolder();
        mFileName += "/Acc_"+ formatter.format(now).toString()+ ".txt";
        lastfiles.set_acc(mFileName);
        Acc_Path = mFileName;

        try {
            outputStream = new FileOutputStream(mFileName, true);
            outputStream.write(SillyString.getBytes());
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        // GYROSCOPE
        mFileName = user.tellAcquisitionsFolder();
        mFileName += "/Gyro_"+ formatter.format(now).toString()+ ".txt";
        lastfiles.set_gyro(mFileName);
        Gyro_Path = mFileName;

        try {
            outputStream = new FileOutputStream(mFileName, true);
            outputStream.write(SillyString.getBytes());
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
            outputStream.write(SillyString.getBytes());
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }


    }

    //==========================================================================
    private void call_toast(CharSequence text){
    //==========================================================================
        // SETS A KIND OF POP-UP MESSAGE
        Context context = getApplicationContext();
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }

    //==============================================================================================
    //==============================================================================================
    // YOCTOPUCE - MAXI-IO CONTROL
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


    //==============================================================================================
    //==============================================================================================
    //  YOCTOPUCE: EVENTS HANDLING
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

                    // DO THIS EVERYTIME TO LET IT WORK PROPERLY
                    io.set_portDirection(0x0F);             //bit 0-3: OUT; bit 4-7: IN ( bit set to 0)
                    io.set_portPolarity(0);                 // polarity set to regular
                    io.set_portOpenDrain(0);                // No open drain

                    // read motor value
                    //int inputdata = io.get_bitState(7);      // read bit value

                } catch (YAPI_Exception e) {
                    e.printStackTrace();
                }
            }
            handler.postDelayed(this, 1000);
        }
    };



    int OldInputData;
    int NewInputData;

    // NEW VALUE ON PORT:
    @Override
    //==========================================================================
    public void yNewValue(YDigitalIO yDigitalIO, String s) {
        //==========================================================================
        TextView view = (TextView) findViewById(R.id.event_view);
        view.setText(s);

        try {
            OldInputData = NewInputData;
            NewInputData = MaxiIO.get_bitState(7);      // CHECK MOTOR PIN VALUE

            if (NewInputData != OldInputData) {         // something occurred

                sampleMotor tmp = new sampleMotor(1);
                tmp.Time[0] = System.currentTimeMillis();

                if (NewInputData == 1 && OldInputData == 0) {           // occurred motor event: now it is ON
                    tmp.Status[0] = Motor_ON_ID;

                } else if (NewInputData == 0 && OldInputData == 1) {    // occurred motor event: now it is OFF
                    tmp.Status[0] = Motor_OFF_ID;}

                // APPEND DATA AND SAVE ON FILE
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
                    // MANAGE FULL ARRAY
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
                        view.setText("MaxiIO connected: YES");
                    }
                    else{
                        UseYocto = false;
                        view.setText("MaxiIO connected: NO");
                    }
                }
                else {
                    UseYocto = false;
                }
                tmp = tmp.nextModule();
            }
        } catch (YAPI_Exception e) {
            e.printStackTrace();
        }
        lastfiles.isyoctoinuse = UseYocto;
    }

} // fine della MainActivity