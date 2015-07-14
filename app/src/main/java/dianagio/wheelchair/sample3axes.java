package dianagio.wheelchair;

/**
 * Created by DianaM on 10/07/2015.
 */
public class sample3axes {
    protected  long Time[];
    protected  float X[];
    protected  float Y[];
    protected  float Z[];

    public sample3axes(int dim)
    {
        this.Time=new long[dim];
        this.X=new float[dim];
        this.Y=new float[dim];
        this.Z=new float[dim];

    }
}
