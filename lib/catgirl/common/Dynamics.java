package lib.catgirl.common;

/**
 * 
 * This is, mostly, a copy of the class from the Sony Developer tutorial:
 * http://developer.sonymobile.com/2010/05/20/android-tutorial-making-your-own-3d-list-part-1/
 * Since Nihilist/Offthegrid need relative coordinates, it can also return the difference between frames. 
 *
 */
public abstract class Dynamics {
	 
    private static final int MAX_TIMESTEP = 50;
    protected float mPosition;
    protected float lastPosition;
    protected float mVelocity;
    protected long mLastTime = 0;
 
    public void setState(final float position, final float velocity, final long now) {
        mVelocity = velocity;
        mPosition = position;
        mLastTime = now;
        lastPosition = mPosition;
    }
 
    public float getPosition() {
        return mPosition;
    }
    
    public float getDifference()
    {
    	return mPosition - lastPosition;
    }
 
    public float getVelocity() {
        return mVelocity;
    }
 
    public boolean isAtRest(final float velocityTolerance) {
        return Math.abs(mVelocity) < velocityTolerance;
    }
 
    public void update(final long now) {
    	
    	lastPosition = mPosition;
    	
        int dt = (int)(now - mLastTime);
        if (dt > MAX_TIMESTEP) {
            dt = MAX_TIMESTEP;
        }
 
        onUpdate(dt);
 
        mLastTime = now;
    }
 
    abstract protected void onUpdate(int dt);
}
