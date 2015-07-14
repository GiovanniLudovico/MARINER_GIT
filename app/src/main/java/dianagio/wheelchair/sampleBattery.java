package dianagio.wheelchair;

/**
 * Created by DianaM on 10/07/2015.
 */
public class sampleBattery {
    protected long Time[];
    protected int BatLev[];

    public sampleBattery(int dim){
        this.Time = new long[dim];
        this.BatLev = new int[dim];
    }

}
