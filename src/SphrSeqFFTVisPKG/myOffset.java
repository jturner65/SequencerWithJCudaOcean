package SphrSeqFFTVisPKG;

import java.util.ArrayList;

import processing.core.PConstants;

//class to hold functionality to calculate offset "sidewalks"
//3 types - normal, ball and radial, where normal is normal to 
//stroke line, radial is normal to resultant curve (by centering
//the ball on the center line) and ball is normal to both, by
//centering the ball at a particular radius away from the stroke
//line.

public abstract class myOffset {
	public static SeqVisFFTOcean pa;
	public int ID;
	public static int IDcnt = 0;
	public String name;
	public int capSize = 20;
	public boolean endCaps;

	public myOffset(SeqVisFFTOcean _pa, boolean _ec){
		pa = _pa;
		ID = IDcnt++;
		endCaps = _ec;
	}	
	
	public myOffset(SeqVisFFTOcean _pa){this(_pa, true);}
	
	/**
	 * calculate the offset points for the drawn stroke line contained in _obj
	 * @param _obj drawn stroke to build offset myPoints from
	 */
	public abstract ArrayList<myPoint> calcOffset(cntlPt[] cntlPts, myVector[] nAra, myVector[] tAra);				
	public abstract void drawCntlPts(cntlPt[] myPoints, myVector[] nAra, myVector[] tAra, boolean derived);
	
	/**
	 * build an array of points that sweeps around c clockwise in plane of norm and tan, with starting radius c.r * norm
	 * @param c control point
	 * @param norm normal of point (binormal in world frame, this is direction of offset)
	 * @param tan tangent at point
	 * @return myPoint array of sequence of points in an arc for an endcap
	 */
	public ArrayList<myPoint> buildCapPts (cntlPt c, myVector norm, myVector tan, float mult){
		ArrayList<myPoint> tmp = new ArrayList<myPoint>();
		float angle = PConstants.PI/(1.0f*capSize), sliceA = angle;			//10 slices
		tmp.add(pa.P((myPoint)c, mult * -c.r, norm));
		for(int i=1;i<capSize-1;++i){	tmp.add(pa.R(tmp.get(i-1), sliceA, norm, tan, c));}
		tmp.add(pa.P((myPoint)c, mult * c.r, norm));
		return tmp;	
	}//buildCapPts
	
	public String toString(){
		String res = name + "Offset ID : "+ID;		
		return res;
	}
}//myOffset

/**
 * calculates normal offset - distance r, normal from stroke line
 * @author john
 *
 */
//make other classes to use different offset mechanism
class myNormOffset extends myOffset{
	myNormOffset(SeqVisFFTOcean _pa){super(_pa); name = "Normal offset";}

	@Override
	public  ArrayList<myPoint> calcOffset(cntlPt[] cntlPts, myVector[] nAra, myVector[] tAra) {
		if(nAra.length != cntlPts.length){return  new ArrayList<myPoint>();}	
		ArrayList<myPoint> tmp = new ArrayList<myPoint>();
		int numCmyPointsM1 = cntlPts.length-1;		
		//start at first point and build endcap
		if(endCaps){tmp.addAll(buildCapPts(cntlPts[0], nAra[0], tAra[0], 1));}
		for(int i = 0; i<cntlPts.length;++i){	tmp.add(pa.P((myPoint)cntlPts[i], cntlPts[i].r, nAra[i]));}//add cntl point + rad offset from norm
		//build endcap on last cntlpoint
		if(endCaps){tmp.addAll(buildCapPts(cntlPts[numCmyPointsM1], nAra[numCmyPointsM1], tAra[numCmyPointsM1],-1));}
		for(int i = numCmyPointsM1; i>=0;--i){	tmp.add(pa.P((myPoint)cntlPts[i], -cntlPts[i].r, nAra[i]));}//add cntl point + rad offset from norm negated, in backwards order, so all points are added properly
		return tmp;
	}
	
	public String toString(){
		String res = name +super.toString();		
		return res;
	}
	
    @Override
//    public void drawCntlPts(cntlPt[] myPoints, myVector[] nAra, myVector[] tAra, boolean derived) {
//        for(int i = 0; i < myPoints.length; ++i){
//            myPoints[i].drawNorm((derived ? 0 : 1), nAra[i], tAra[i]);
//        }
//    }
    public void drawCntlPts(cntlPt[] myPoints, myVector[] nAra, myVector[] tAra, boolean derived) {
    	pa.pushStyle();
    	int clrInt = 0;
        for(int i = 0; i < myPoints.length; ++i){
        	clrInt = (int)(i/(1.0f * myPoints.length) * 255.0f);
            pa.fill(clrInt,0,(255 - clrInt),255);  
            pa.stroke(clrInt,0,(255 - clrInt),255); 
            myPoints[i].drawRad(nAra[i], tAra[i]);
        }
        pa.popStyle();
    }
        



}//myNormOffset


