package dianagio.wheelchair;

import android.os.AsyncTask;
import android.os.SystemClock;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

/**
 * Created by DianaM on 10/07/2015.
 */
//==========================================================================
public class Background_Save extends AsyncTask<Void, Boolean, Boolean> {
    //==========================================================================
    // buffers dimensions ( amount of samples to be saved each time)
    static final short buffer_dim_inert =       1000;
    static final short buffer_dim_batt_motor =  1;

    sample3axes acc_data =          new sample3axes(buffer_dim_inert);
    sample3axes gyro_data =         new sample3axes(buffer_dim_inert);
    sampleMotor motor_data =        new sampleMotor(buffer_dim_batt_motor);
    sampleBattery battery_data =    new sampleBattery(buffer_dim_batt_motor);
    sampleMotor wheelchair_data =   new sampleMotor(buffer_dim_batt_motor);
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
    //==========================================================================
    public Background_Save(sampleMotor motor_in_data, sample3axes acc_in_data, sample3axes gyro_in_data, sampleBattery battery_in_data, sampleMotor wheelchair_in_data, String inFilePath) {
        //==========================================================================

        motor_data =        motor_in_data;
        acc_data =          acc_in_data;
        gyro_data =         gyro_in_data;
        battery_data =      battery_in_data;
        wheelchair_data =   wheelchair_in_data;
        FilePath =          inFilePath;
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
        String BinaryFilePath = FilePath.replace( "txt", "bin"); // changes file extension by replacing part of the string
        FileOutputStream outputStream;
        FileOutputStream BinaryOutputStream;

        // open binary file
        try {
            BinaryOutputStream = new FileOutputStream(BinaryFilePath, true); //true: append to file
            BufferedOutputStream out = new BufferedOutputStream(BinaryOutputStream);

            // fulfill string
            if (acc_data!=null) {
                for(int i=0; i<acc_data.Time.length; i++){
                    StringToSave += acc_data.Time[i] + "\t" + acc_data.X[i] + "\t" + acc_data.Y[i] + "\t" + acc_data.Z[i] + "\n";

                    int tmpi=0;
                    tmpi=(int)((acc_data.Time[i]>>32) & 0xffffffff);
                    out.write( tmpi );     //MSB
                    tmpi = (int)(acc_data.Time[i] & 0xffffffff);
                    out.write( tmpi );           //LSB
                    out.write( (int) acc_data.X[i]);
                    out.write( (int) acc_data.Y[i]);
                    out.write( (int) acc_data.Z[i]);
                    out.flush();

                }

            }else if(gyro_data!=null){
                for(int i=0; i<gyro_data.Time.length; i++){
                    StringToSave += gyro_data.Time[i] + "\t" + gyro_data.X[i] + "\t" + gyro_data.Y[i] + "\t" + gyro_data.Z[i] + "\n";

                    BinaryOutputStream.write( (int)((gyro_data.Time[i]>>32) & 0xffffffff) );
                    BinaryOutputStream.write( (int) (gyro_data.Time[i] & 0xffffffff) );
                    BinaryOutputStream.write( (int) gyro_data.X[i]);
                    BinaryOutputStream.write( (int) gyro_data.Y[i]);
                    BinaryOutputStream.write( (int) gyro_data.Z[i]);
                }

            }else if(motor_data!=null){
                for(int i=0; i<motor_data.Time.length; i++){
                    StringToSave += motor_data.Time[i] + "\t" + motor_data.Status[i] + "\n";

                    /*BinaryOutputStream.write( (int)((motor_data.Time[i]>>32) & 0xffffffff) );
                    BinaryOutputStream.write( (int)(motor_data.Time[i] & 0xffffffff) );
                    BinaryOutputStream.write( (int) motor_data.Status[i]);
                    */
                    out.write( (int)((motor_data.Time[i]>>32) & 0xffffffff) );
                    out.write( (int)(motor_data.Time[i] & 0xffffffff) );
                    out.write( (int) motor_data.Status[i]);
                }

            }else if(battery_data!=null){
                for(int i=0; i<battery_data.Time.length; i++){
                    StringToSave += battery_data.Time[i] + "\t" + battery_data.BatLev[i] + "\n";

                    BinaryOutputStream.write( (int)((battery_data.Time[i]>>32) & 0xffffffff) );
                    BinaryOutputStream.write( (int)(battery_data.Time[i] & 0xffffffff) );
                    BinaryOutputStream.write( (int) battery_data.BatLev[i]);
                }
            }
            else if(wheelchair_data!=null){
                for(int i=0; i<wheelchair_data.Time.length; i++){
                    StringToSave += wheelchair_data.Time[i] + "\t" + wheelchair_data.Status[i] + "\n";

                    BinaryOutputStream.write( (int)((wheelchair_data.Time[i]>>32) & 0xffffffff) );
                    BinaryOutputStream.write( (int)(wheelchair_data.Time[i] & 0xffffffff) );
                    BinaryOutputStream.write( (int) wheelchair_data.Status[i]);
                }

            }

            BinaryOutputStream.close(); //close binary file
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        // append string in FilePath

        try {
            outputStream = new FileOutputStream(FilePath, true); //true: append string to file (non-binary file)
            outputStream.write(StringToSave.getBytes());
            outputStream.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

     return true;
    }


}
