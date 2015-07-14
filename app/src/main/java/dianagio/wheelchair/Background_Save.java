package dianagio.wheelchair;

import android.os.AsyncTask;

import java.io.FileOutputStream;

/**
 * Created by DianaM on 10/07/2015.
 */
public class Background_Save extends AsyncTask<Void, Boolean, Boolean> {
    // buffer dimension ( amount of samples to be saved each time)
    static final short buffer_dim_inert =       1000;
    static final short buffer_dim_batt_motor =  1;

    sample3axes acc_data =          new sample3axes(buffer_dim_inert);
    sample3axes gyro_data =         new sample3axes(buffer_dim_inert);
    sampleMotor motor_data =        new sampleMotor(buffer_dim_batt_motor);
    sampleBattery battery_data =    new sampleBattery(buffer_dim_batt_motor);
    /*
    sample3axes acc_data =          new sample3axes(2160000);
    sample3axes gyro_data =         new sample3axes(2160000);
    sampleMotor motor_data =        new sampleMotor(43200);
    sampleBattery battery_data =    new sampleBattery(720);

*/
    String FilePath ="";
    public static final short Motor_ID      = 0;
    public static final short Acc_ID        = 1;
    public static final short Gyro_ID       = 2;
    public static final short Battery_ID    = 3;

    // constructor
    public Background_Save(sampleMotor motor_in_data, sample3axes acc_in_data, sample3axes gyro_in_data, sampleBattery battery_in_data, String inFilePath) {

        motor_data =    motor_in_data;
        acc_data =      acc_in_data;
        gyro_data =     gyro_in_data;
        battery_data =  battery_in_data;
        FilePath =      inFilePath;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }
    @Override
    //==========================================================================
    protected Boolean doInBackground(Void... params) {
        //==========================================================================
        String StringToSave="";

        // fulfill string
        if (acc_data!=null) {
            for(int i=0; i<acc_data.Time.length; i++){
                StringToSave += acc_data.Time[i] + "\t" + acc_data.X[i] + "\t" + acc_data.Y[i] + "\t" + acc_data.Z[i] + "\n";
            }

        }else if(gyro_data!=null){
            for(int i=0; i<gyro_data.Time.length; i++){
                StringToSave += gyro_data.Time[i] + "\t" + gyro_data.X[i] + "\t" + gyro_data.Y[i] + "\t" + gyro_data.Z[i] + "\n";
            }

        }else if(motor_data!=null){
            for(int i=0; i<motor_data.Time.length; i++){
                StringToSave += motor_data.Time[i] + "\t" + motor_data.Status[i] + "\n";
            }

        }else if(battery_data!=null){
            for(int i=0; i<battery_data.Time.length; i++){
                StringToSave += battery_data.Time[i] + "\t" + battery_data.BatLev[i] + "\n";
            }
        }

        // append string in FilePath
        FileOutputStream outputStream;
        try {
            outputStream = new FileOutputStream(FilePath, true); //true: append to file
            outputStream.write(StringToSave.getBytes());
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }


     return false;
    }

    @Override
    protected void onPostExecute(Boolean result) {

        return;
    }

}
