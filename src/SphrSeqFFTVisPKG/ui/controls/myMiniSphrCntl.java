package SphrSeqFFTVisPKG.ui.controls;

import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PShape;

public class myMiniSphrCntl{
	public myMusicSimWindow win;
	public static int sphMiniCntl = 0;
	public int ID;	
	public mySphereCntl own;
	
	public String name;
	public PShape sh;
	public int[] txtFillClr;
	
	public int instrCntlIDX;			//the instrument control this control controls. controllingly.
	
	//public myPoint ctr, drawCtr;
		
	public myVector panAxis,			//rotation axis to show pan
					rotAxis,			//rotation axis to show rolling
					distFromCtr,
					cntlRotAxis,		//axis to rotate controls around
					rayCastDotVec;		//axis to dot against for modifying control - should be horizontal for side-to-side controls, and vertical for up and down controls
	
	public float[] vals;
	public static final int 
					valIDX 		= 0,	//amount represented by mini sphere
					minAmtIDX 	= 1,	//min value this cntl can take
					maxAmtIDX 	= 2,	//max value this cntl can take
					rotAmtIDX 	= 3,	//rotation for display			
					minRAmtIDX 	= 4,	//min rot value this cntl can take
					maxRAmtIDX 	= 5;	//max rot value this cntl can take
	public static final int numVals = 6;
	
	
	public float radius, invRadius;
	public boolean[] mSphrFlags;
	public static final int 
				horizBIDX = 0,				//whether this control's travel is predominantly horizontal
				cWiseBIDX = 1,				//whether this control increases in the cwise dir
				expValBIDX = 2;				//whether this control's value is exponential or linear
	public static final int numBFlags = 3;
	
	
	public myMiniSphrCntl(myMusicSimWindow _win, mySphereCntl _o, int _instCntlIDX, myVector[] _cntlAxis,
			String _name, PShape _sh, float[] _vals, boolean[] _flags, int[] _txtFl, float _radius){
		win = _win;
		own = _o;
		ID = sphMiniCntl++;
		panAxis = new myVector();
		rotAxis = new myVector();
		cntlRotAxis = new myVector();
		distFromCtr = new myVector();
		panAxis.set(_cntlAxis[0]);				//axis that globe rotates around to reflect pan
		rotAxis.set(_cntlAxis[1]);				//axis that globe "rolls" around
		cntlRotAxis.set(_cntlAxis[2]);			//axis that controls rotate around
		distFromCtr.set(_cntlAxis[3]);
		initBFlags(_flags);
		instrCntlIDX = _instCntlIDX;
		rayCastDotVec = new myVector();
		//rayCastDotVec.set((ID == 2 ? pa.P(1,0,0) : pa.P(0,1,0)));
		rayCastDotVec.set((mSphrFlags[horizBIDX] ? rotAxis : panAxis));		
		if(!mSphrFlags[horizBIDX]){rayCastDotVec._mult(-1);}
		rayCastDotVec._normalize();
		txtFillClr = _txtFl;
		radius = _radius;
		invRadius = 1.0f/radius;
		name = _name;
		sh = _sh;
		initVals(_vals);
	}
	
	protected void initBFlags(boolean[] _bflags){mSphrFlags = new boolean[numBFlags];for(int i=0;i<numBFlags;++i){mSphrFlags[i] = _bflags[i];}}
	//set up value array
	protected void initVals(float [] _vals){
		vals = new float[numVals];
		for(int i=0; i<numVals; ++i){vals[i]=_vals[i];}
	}
	
	public myVector getDispVec(){
		double mag = distFromCtr._mag();
		myVector disp = myVector._rotAroundAxis(myVector._unit(distFromCtr), cntlRotAxis, vals[rotAmtIDX]);
		disp._mult(mag);
		//disp.z = 0;
		return disp;
	}
	
	public boolean hitMeMini(myVector clickVec){
		myVector disp= getDispVec();
		myPoint tmp = new myPoint(clickVec);
		tmp.z = 0;
		double dist = tmp._dist(disp);
		//pa.outStr2Scr("Checking Hit in mini sphere ID : " + ID + " disp val (target ctr) : " + disp.toStrBrf() + " click vec : " + clickVec.toStrBrf() + " Dist from sphere : " + dist);
		return (dist < 30);
	}
	
	
	public void drawMiniLocked(IRenderInterface pa){
		pa.pushMatState();	
		pa.rotate(vals[rotAmtIDX], cntlRotAxis);	
		pa.translate(distFromCtr);
		pa.pushMatState();	
			pa.rotate(-vals[rotAmtIDX], distFromCtr);	
			win.drawShape(sh);								
		pa.popMatState();
		pa.setFill(txtFillClr, 255);
		pa.rotate(-vals[rotAmtIDX], cntlRotAxis);	
		pa.pushMatState();	
			pa.translate(-4,1,10);	
			pa.scale(.6f,.6f,.6f);
			pa.showText(String.format("%.2f",vals[valIDX]), 0,0);			//text of value
		pa.popMatState();
		pa.setFill(255,255,255,255);
		pa.translate(myVector._mult(distFromCtr,.25));
		pa.translate(-.5f*5*name.length(),-8,0);			//text of name of sphere
		pa.showText(name, 0,0);
		pa.popMatState();
	}

	public void drawMini(IRenderInterface pa){
		pa.pushMatState();	
		pa.rotate(vals[rotAmtIDX], cntlRotAxis);	
		pa.translate(distFromCtr);
		pa.pushMatState();	
			pa.rotate(-vals[rotAmtIDX], distFromCtr);	
			win.drawShape(sh);							
		pa.popMatState();
		pa.popMatState();
	}
	//take in an incrementing value, return a rotated value
	//protected float getRotAmt(float val, float tick, float mult){return (PApplet.cos(val + tick) * mult);}
	protected float getLinInterp(float val){return  (val - vals[minAmtIDX])/(vals[maxAmtIDX] - vals[minAmtIDX]);}
	protected float getRotInterp(float val){return  (val - vals[minRAmtIDX])/(vals[maxRAmtIDX] - vals[minRAmtIDX]);}
	protected float getLinVal(float interp){float newVal = vals[minAmtIDX] + interp *(vals[maxAmtIDX] - vals[minAmtIDX]);  return bndLinVal(newVal);}
	protected float getRotVal(float interp){float newVal = vals[minRAmtIDX] + interp *(vals[maxRAmtIDX] - vals[minRAmtIDX]); return bndRotVal(newVal);}
	protected float bndLinVal(float val){return MyMathUtils.min(vals[maxAmtIDX], MyMathUtils.max(vals[minAmtIDX],val));}
	protected float bndRotVal(float val){return MyMathUtils.min(vals[maxRAmtIDX], MyMathUtils.max(vals[minRAmtIDX],val));}
	//return the actual value, not the stored value (if exp then act value is stored value sqred)
	public float getValue(){
		return (mSphrFlags[expValBIDX] ? vals[valIDX] * vals[valIDX] : vals[valIDX]);		
	}
	//sets value to be either passed val or its sqrt - using this to be able to represent exponential values with linear faders
	public void setValue(float _val){
		vals[valIDX] = bndLinVal(mSphrFlags[expValBIDX] ? (float)Math.sqrt(_val) : _val);
		recalcRotAmt();
	}
	
	//take existing value, map between min and max, and 
	public void recalcRotAmt(){
		float lItrp = getLinInterp(vals[valIDX]);
		vals[rotAmtIDX] = getRotVal((mSphrFlags[cWiseBIDX] ? lItrp : 1.0f-lItrp));
	}
	
	public boolean modValAmt(float tickX, float tickY){
		float tickToUse = (mSphrFlags[horizBIDX] ? tickX : tickY);
		if(Math.abs(tickToUse) < MyMathUtils.EPS_F){return false;}
		//pa.outStr2Scr("In mini sphere :  valIncr : " + valIncr + " tick : " + tick);
		float lItrp = getLinInterp(vals[valIDX]);
		vals[valIDX] = getLinVal(lItrp + tickToUse);		
		recalcRotAmt();
		//if((own.ID == 0) &&(ID ==1)){pa.outStr2Scr("Speed value : " + value);}	
		if(instrCntlIDX != -1){	own.instr.setInstCntlVals(instrCntlIDX, vals[valIDX], null);}
		return true;
	}
	
}//myMiniSphrCntl