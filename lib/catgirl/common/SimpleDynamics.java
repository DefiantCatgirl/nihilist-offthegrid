package lib.catgirl.common;

/**
 * 
 * This is a copy of the class from the Sony Developer tutorial:
 * http://developer.sonymobile.com/2010/05/20/android-tutorial-making-your-own-3d-list-part-1/
 *
 */
public class SimpleDynamics extends Dynamics {
    private float mFrictionFactor;
 
    public SimpleDynamics(final float frictionFactor) {
        mFrictionFactor = frictionFactor;
    }
 
    @Override
    protected void onUpdate(final int dt) {
        mPosition += mVelocity * dt / 1000;
        mVelocity *= mFrictionFactor;
    }
}
