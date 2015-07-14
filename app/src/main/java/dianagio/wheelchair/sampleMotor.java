package dianagio.wheelchair;

/**
 * Created by DianaM on 10/07/2015.
 */
public class sampleMotor {
    protected long Time[];
    protected short Status[];

    public sampleMotor(int dim)
    {
        this.Time=new long[dim];
        this.Status = new short[dim];
    }
}
