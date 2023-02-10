package SphrSeqFFTVisPKG;

import java.util.ArrayList;

import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Render_Interface.IRenderInterface;

/**
 * class to hold functionality to calculate offset "sidewalks"   
 * 3 types - normal, ball and radial, where normal is normal to 
 * stroke line, radial is normal to resultant curve (by centering
 * the ball on the center line) and ball is normal to both, by
 * centering the ball at a particular radius away from the stroke line.                                                         
 * @author john
 *
 */
public abstract class Base_Offset {
	public int ID;
	public static int IDcnt = 0;
	public String name;
	public int capSize = 20;
	public boolean endCaps;

	public Base_Offset(boolean _ec){
		ID = IDcnt++;
		endCaps = _ec;
	}	
	
	public Base_Offset(){this(true);}
	
	/**
	 * calculate the offset points for the drawn stroke line contained in _obj
	 * @param _obj drawn stroke to build offset myPoints from
	 */
	public abstract ArrayList<myPoint> calcOffset(myCntlPt[] cntlPts, myVector[] nAra, myVector[] tAra);				
	public abstract void drawCntlPts(IRenderInterface ri,myCntlPt[] myPoints, myVector[] nAra, myVector[] tAra, boolean derived);
	
	/**
	 * build an array of points that sweeps around c clockwise in plane of norm and tan, with starting radius c.r * norm
	 * @param c control point
	 * @param norm normal of point (binormal in world frame, this is direction of offset)
	 * @param tan tangent at point
	 * @return myPoint array of sequence of points in an arc for an endcap
	 */
	public ArrayList<myPoint> buildCapPts (myCntlPt c, myVector norm, myVector tan, float mult){
		ArrayList<myPoint> tmp = new ArrayList<myPoint>();
		float angle = MyMathUtils.PI_F/(1.0f*capSize), sliceA = angle;			//10 slices
		tmp.add(new myPoint(c, mult * -c.r, norm));
		//for(int i=1;i<capSize-1;++i){	tmp.add(pa.R(tmp.get(i-1), sliceA, norm, tan, c));}
		for(int i=1;i<capSize-1;++i){	tmp.add(tmp.get(i-1).rotMeAroundPt(sliceA, norm, tan, c));}
		tmp.add(new myPoint(c, mult * c.r, norm));
		return tmp;	
	}//buildCapPts
	
	public String toString(){
		String res = name + "Offset ID : "+ID;		
		return res;
	}
}//myOffset