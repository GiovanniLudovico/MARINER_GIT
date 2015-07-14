package dianagio.wheelchair;

import java.io.Serializable;

/**
 * Created by Giovanni on 24/06/2015.
 */
public class LastFiles implements Serializable {
    private String motor;
    private String accelerometer;
    private String gyroscope;
    private String battery;

    public boolean isyoctoinuse; //provvisorio
    public void set_motor(String s)         { motor=s;    }
    public void set_acc(String s)           { accelerometer=s;    }
    public void set_gyro(String s)          { gyroscope=s;    }
    public void set_battery(String s)       { battery = s;   }
    public String tell_motor()              { return motor;    }
    public String tell_accelerometer()      { return accelerometer;    }
    public String tell_gyroscope()          { return gyroscope;    }
}
