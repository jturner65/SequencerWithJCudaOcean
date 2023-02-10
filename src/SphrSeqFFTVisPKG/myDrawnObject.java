package SphrSeqFFTVisPKG;

//Lots of stuff in here that is unnecessary for this project (i(JT) built this class for final project for 6491
import java.util.ArrayList;
import java.util.Arrays;

import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PApplet;

public abstract class myDrawnObject {
	public static SeqVisFFTOcean pa;

	public int fillClr, strkClr;
	public float len;								//length of object
	protected static final int numReps = 4;				//default number of repetitions of subdivid/tuck/untuck
	
	public static final int trajPtRad = 2;			//radius of points
	
	public myVector canvasNorm;							//normal to drawing canvas == normal to plane of poly
	protected myPoint[] origpts;							//originally drawn points making up this curve
	protected myPoint[] pts;								//points making up this curve
	protected float[] dpts;						//array holding distance to each point from beginning
	
	//beautiful pts
	public myCntlPt[] cntlPts;						//control points describing object, if used	
	
	protected float[] d_cntlPts;
	protected float cntl_len;
	
	public myPoint COV,									//center of verts
			COM;
	//boolean flags about object
	public boolean[] flags;							//init'ed to all false, then set specifically when appropriate
	//flag idxs
	public final int 
		//construction
			isClosed 		= 0,					//object is a closed poly
			isMade 			= 1,					//whether or not the object is finished being drawn
			isFlipped		= 2,					//points being displayed are flipped (reflected)
			usesCntlPts 	= 3,					//if this curve is governed by control points (as opposed to just drawn freehand)
	    //calculation
			reCalcPoints	= 4,					//recalculate points from cntl point radii - use if radius is changed on stroke from user input
			cntlWInvRad		= 5,					//whether the weight impact on cntl radius is inverse or direct - inverse has slow drawing be wider, direct has slow drawing be narrower
			interpStroke	= 6,					//whether or not control-point based strokes are interpolating or not
		//display			 
			showCntlPnts 	= 7,					//show this object's cntl points
			vertNorms 		= 8,					//use vertex normals to shade curve
			drawNorms 		= 9,					//display normals for this object as small arrows
			drawCntlRad 	= 10,	
			useProcCurve	= 11;					//toggles whether we use straight lines in vertex building or processing's curve vertex			
	public final int numFlags = 12;					//always 1 more than last flag const
	
	//flags about type of operation that uses interpolation being done
	public int lnI_Typ,								//what interpolation type will this curve use for line operations (tuck, find myPoint at ratio of length, etc) 
				sbI_Typ,							//interp type for subdivision
				swI_Typ;							//what kind of interpolation will be used for this curve as it is swemyPoint around axis (if this is a closed sweep-poly)
	
	//flags about which interpolation type should be done
	public final int linear_int = 0,				//denotes linear interpolation 
					ball_int = 1;					//ball-morph interpolation, for sweep morph
	//add more here when we have more 
	
	public final int 	//point processing flags
		_subdivide		=0,
		_tuck			=1,
		_equaldist		=2,
		_resample		=3;

	//array of normals, tans for every control point,
	protected myVector[] c_nAra, c_tAra, c_bAra;
	
	
	protected float[][] distFromIntAxis;			//keeps the distance to internal axis of stroke			
	protected static final int 
	//idxs in distFromAxis array
			_d = 0,									//index for dist from axis
			_t = 1,									//interpolant along rot axis for proj
			_a = 2,									//idx1 for myPoint in ara for rotational axis - proj lies between these two
			_b = 3;									//idx2 for myPoint in ara for rotational axis
	protected final int nAxisVals = 4;
	
	public myDrawnObject(SeqVisFFTOcean _pa, myVector _canvNorm) {
		pa = _pa;
		canvasNorm = _canvNorm;		//c.drawSNorm  draw surface normal
		initFlags();
		lnI_Typ = linear_int;
		swI_Typ = linear_int;
		sbI_Typ = linear_int;
		c_nAra = new myVector[0];c_tAra = new myVector[0];c_bAra = new myVector[0];
		COM = pa.P();COV = pa.P();
//		flags[firstDrawn] = true;
	}
	
	//initialize point referencing structs - using both for speed concerns.
	public void startDrawing(){	pts = new myPoint[0];len = 0; dpts = new float[0]; flags[drawCntlRad] = false;}	
	public void addPt(myPoint p){
		ArrayList<myPoint> tmp = new ArrayList<myPoint>(Arrays.asList(pts));
		tmp.add(p);
		setPts(tmp);
	}//setPt
	
	//subdivide, tuck, respace, etc, cntlpts of this curve
	public void processCntlPts(int numPts, int numReps){
		float origLen = cntl_len;
		setCPts(procCntlPt(_subdivide, cntlPts, 2, origLen));										//makes 1 extra vert  equilspaced between each vert, to increase resolution of curve
		for(int i = 0; i < numReps; ++i){
			//setCPts(procCntlPt(_subdivide, cntlPts, 2, origLen));
			setCPts(procCntlPt(_tuck, cntlPts, .5f, origLen));
			setCPts(procCntlPt(_tuck, cntlPts, -.5f, origLen));
		}		//smooth curve - J4
		setCPts(procCntlPt(_equaldist, cntlPts, .5f, origLen));
		for(int i = 0; i < numReps; ++i){
			//setCPts(procCntlPt(_subdivide, cntlPts, 2, origLen));
			setCPts(procCntlPt(_tuck, cntlPts, .5f, origLen));
			setCPts(procCntlPt(_tuck, cntlPts, -.5f, origLen));
		}		//smooth curve - J4
		setCPts(procCntlPt(_resample, cntlPts, numPts, origLen));
	}			

	//subdivide, tuck, respace, resample, etc. pts of this curve
	public void processPts(myPoint[] pts, int numPts, int numReps){
		setPts(procPts(_subdivide, pts, 2, len, flags[isClosed]));										//makes 1 extra vert  equilspaced between each vert, to increase resolution of curve
		for(int i = 0; i < numReps; ++i){
			setPts(procPts(_subdivide, pts, 2, len, flags[isClosed]));
			setPts(procPts(_tuck, pts, .5f, len, flags[isClosed]));
			setPts(procPts(_tuck, pts, -.5f, len, flags[isClosed]));
		}		//smooth curve - J4
		setPts(procPts(_equaldist, pts, .5f, len, flags[isClosed]));
		for(int i = 0; i < numReps; ++i){
			setPts(procPts(_subdivide, pts, 2, len, flags[isClosed]));
			setPts(procPts(_tuck, pts, .5f, len, flags[isClosed]));
			setPts(procPts(_tuck, pts, -.5f, len, flags[isClosed]));
		}		//smooth curve - J4
		setPts(procPts(_resample, pts, numPts, len, flags[isClosed]));		
	}	
	
	
	//sets required info for points array - points and dist between pts, length, etc
	protected void setPts(ArrayList<myPoint> tmp){
		pts = tmp.toArray(new myPoint[0]);
		dpts = getPtDist(pts, flags[isClosed]);	
		len=length(pts, flags[isClosed]);
	}//setPts	
	//make a new point interpolated between either 2 or 3 points in pts ara, described by # of idxs
	public myPoint makeNewPoint(myPoint[] pts, int[] idxs, float s){	return _Interp(pts[idxs[0]], s, (idxs.length == 2 ? pts[idxs[1]] : _Interp(pts[idxs[1]],.5f,pts[idxs[2]], lnI_Typ)),lnI_Typ );	}
	
	/**
	 * process all points using passed algorithm on passed array of points - not all args are used by all algs.
	 * @param _typ type of point processing
	 * @param pts array to be processed
	 * @param val quantity used by variou processing : subdivision-> # of new pts +1, tuck-> amt to be tucked,  resample-> # of new verts
	 * @param len length of segment described by points, including ends if closed
	 * @param wrap whether the point list wraps around or not
	 * @return arraylist of processed points
	 */
	public ArrayList<myPoint> procPts(int _typ, myPoint[] pts, float val, float _len, boolean wrap){
		ArrayList<myPoint> tmp = new ArrayList<myPoint>(); // temporary array
		switch(_typ){
			case _subdivide	:{
			    for(int i = 0; i < pts.length-1; ++i){tmp.add(pts[i]); for(int j=1;j<val;++j){tmp.add(makeNewPoint(pts,new int[]{i,i+1}, (j/(val))));}}
			    tmp.add(pts[pts.length-1]);				
			    return tmp;}
			case _tuck		:{
				if(wrap){tmp.add(makeNewPoint(pts,new int[]{0,pts.length-1,1}, val));} else {tmp.add(0,pts[0]);}
			    for(int i = 1; i < pts.length-1; ++i){	tmp.add(i,makeNewPoint(pts,new int[]{i,i-1,i+1}, val));   }
		    	if(wrap){tmp.add(makeNewPoint(pts,new int[]{pts.length-1,pts.length-2,0}, val));} else {tmp.add(pts[pts.length-1]);}			
		    	return tmp;}
			case _equaldist	:{
				float ratio = _len/(1.0f * pts.length),curDist = 0;					 //new distance between each vertex, iterative dist travelled so far			 
				for(int i =0; i<pts.length; ++i){tmp.add(at(curDist/_len));curDist+=ratio;}	
				tmp.add(pts[pts.length-1]);				
				return tmp;}	
			case _resample	:{
				float ratio = pts.length/(1.0f * (val-1)),f;					//distance between each vertex		 
				int idx, newIdx=0;		
				for(float i = 0; i<pts.length-1; i+=ratio){idx = (int)i;	f = i-idx;tmp.add(newIdx++,makeNewPoint(pts,new int[]{idx,idx+1},f));}
				if(wrap) {
					if(myPoint._dist(tmp.get(newIdx-1), tmp.get(0)) > ratio){	tmp.add(makeNewPoint(new myPoint[]{tmp.get(newIdx-1), tmp.get(0)},new int[]{0,1},.5f));}		//want to only add another point if last 2 points are further than ratio appart
				} else {		tmp.add(pts[pts.length-1]);}			//always add another point if open line/loop - want to preserve end point
				break;}	
			default :
		}
		
		return tmp;
	}
		
	//CONTROL POINT-RELATED FUNCTIONS
	//build essential orientation vectors for control points
	public void buildCntlFrameVecAras(){
		c_nAra = buildNormals(cntlPts);
		c_tAra = buildTangents(cntlPts, false);
		c_bAra = buildBinormals(cntlPts, c_nAra, c_tAra);		//use these with cntl point radius to build stroke pts
	}//buildCntlFrameVecAras
		
	//sets required info for points array - points and dist between pts, length, etc
	protected void setCPts(ArrayList<myCntlPt> tmp){
		cntlPts = tmp.toArray(new myCntlPt[0]);
		d_cntlPts = getPtDist(cntlPts, false);	
		cntl_len=length(cntlPts, false);
	}//setPts	
	//make a new point interpolated between either 2 or 3 points in pts ara, described by # of idxs
	public myCntlPt makeNewPoint(myCntlPt[] pts, int[] idxs, float s){	return _Interp(pts[idxs[0]], s, (idxs.length == 2 ? pts[idxs[1]] : _Interp(pts[idxs[1]],.5f,pts[idxs[2]], lnI_Typ)),lnI_Typ );	}
	/**
	 * process all points using passed algorithm on passed array of points - not all args are used by all algs.
	 * @param _typ type of point processing
	 * @param pts array to be processed
	 * @param val quantity used by variou processing : subdivision-> # of new pts +1, tuck-> amt to be tucked,  resample-> # of new verts
	 * @param len length of segment described by points, including ends if closed
	 * @param wrap whether the point list wraps around or not
	 * @return arraylist of processed points
	 */	
	public ArrayList<myCntlPt> procCntlPt(int _typ, myCntlPt[] pts, float val, float _len){
		ArrayList<myCntlPt> tmp = new ArrayList<myCntlPt>(); // temporary array
		switch(_typ){
			case _subdivide	:{
			    for(int i = 0; i < pts.length-1; ++i){tmp.add(pts[i]); for(int j=1;j<val;++j){tmp.add(makeNewPoint(pts,new int[]{i,i+1}, (j/(val))));}}
			    tmp.add(pts[pts.length-1]);				
			    return tmp;}
			case _tuck		:{
				tmp.add(0,pts[0]);//no wrap on control points, so no  need to check
			    for(int i = 1; i < pts.length-1; ++i){	tmp.add(i,makeNewPoint(pts,new int[]{i,i-1,i+1}, val));   }
		    	tmp.add(pts[pts.length-1]);			
		    	return tmp;}
			case _equaldist	:{
				float ratio = _len/(1.0f * pts.length),curDist = 0;					 //new distance between each vertex, iterative dist travelled so far			 
				for(int i =0; i<pts.length; ++i){tmp.add(at_C(curDist/_len, pts));curDist+=ratio;}	
				tmp.add(pts[pts.length-1]);				
				return tmp;}	
			case _resample	:{
				float ratio = pts.length/(1.0f * (val-1)),f;					//distance between each vertex		 
				int idx, newIdx=0;		
				for(float i = 0; i<pts.length-1; i+=ratio){idx = (int)i;	f = i-idx;tmp.add(newIdx++,makeNewPoint(pts,new int[]{idx,idx+1},f));}			
				tmp.add(pts[pts.length-1]);
				break;}	
			default :
		}
		return tmp;
	}	
	//end cntlmyPoint related
	//normals, tangents, binormals at each point
	public myVector[] buildNormals(myPoint[] _pts){
		ArrayList<myVector> tmp = new ArrayList<myVector>();				
		for(int i =0; i<_pts.length; ++i){tmp.add(pa.U(canvasNorm));	}		//make normal the canvas normal
		return tmp.toArray(new myVector[0]);
	}
	public myVector[] buildTangents(myPoint[] _pts, boolean close){
		ArrayList<myVector> tmp = new ArrayList<myVector>();
		for(int i=0; i<_pts.length-1; ++i){tmp.add(pa.U(_pts[i], _pts[i+1]));}
		if(close){tmp.add(pa.U(_pts[_pts.length-1], _pts[0]));}
		else {tmp.add(pa.U(_pts[_pts.length-2], _pts[_pts.length-1]));}
		return tmp.toArray(new myVector[0]);
	}	
	public myVector[] buildBinormals(myPoint[] _pts, myVector[] n_ara, myVector[] t_ara){//build last
		ArrayList<myVector> tmp = new ArrayList<myVector>();
		for(int i=0; i<_pts.length; ++i){tmp.add(pa.U(pa.N(n_ara[i],t_ara[i])));}
		return tmp.toArray(new myVector[0]);
	}

	//find location of center of verts
	public myPoint calcCOV(){myPoint C = pa.P();for(int i=0;i<pts.length;++i){C._add(pts[i]);}myPoint Ct = pa.P(1.0f/pts.length,C); COV=pa.P(Ct);return COV;}
	//find COV of passed verts
	public myPoint calcCOVOfAra(myPoint[] pts){myPoint C = pa.P();for(int i=0;i<pts.length;++i){C._add(pts[i]);}myPoint Ct = pa.P(1.0f/pts.length,C); return Ct;}

	
	/**
	 * return the interpolated myVectortor between two myPoint's myVectortors given the adjacent idx's of two points in pts and the array of myVectortors
	 * @param idxA, idxB : 2 idxs in myVector aras to be interped between
	 * @param s : interpolant
	 * @param vAra : array to find myVectors in
	 * @return interpolated myVectortor
	 */
	public myVector getInterpVec(int idxA, float s, int idxB, myVector[] vAra, int interpMech){return _Interp(vAra[idxA], s, vAra[idxB], interpMech);}
	public myVector getUInterpVec(int idxA, float s, int idxB, myVector[] vAra, int interpMech){return pa.U(_Interp(vAra[idxA], s, vAra[idxB], interpMech));}
	
	/**
	 * using arc length parameterisation this will return a point along the curve at a 
	 * particular fraction of the length of the curve (0,1 will return endpoints, .5 will return halfway along curve)
	 * @param t fraction of curve length we are interested in returning a point - should be 0-1
	 * @return point @t along curve
	 */
	public myPoint at(float t){return at(t,new float[1], len, pts, dpts);}//put interpolant between adjacent axis points in s ara if needed
	public myPoint at(float t, float[] s){return at(t,s, len, pts, dpts);}//put interpolant between adjacent axis points in s ara if needed
	public myPoint at(float t, float[] s, float _len, myPoint[] pts, float[] _dpts){//call directly if wanting interpolant between adj axis points too
		if(t<0){PApplet.println("In at : t="+t+" needs to be [0,1]");return pts[0];} else if (t>1){PApplet.println("In at : t="+t+" needs to be [0,1]");return pts[pts.length-1];}
		float dist = t * _len;
		for(int i=0; i<_dpts.length-1; ++i){										//built off dpts so that it will get wrap for closed curve
			if(_dpts[i+1] >= dist){
				s[0] = ((dist-_dpts[i])/(_dpts[i+1]-_dpts[i]));					//needs to stay between 0 and 1 (since interpolation functions between pts will be 0-1 based), so normalize by distance dpts[i]
				return makeNewPoint(pts,new int[]{i,((i+1)%pts.length)}, s[0]);		//put interpolant between adjacent axis points in s ara if needed		
			}					
		}		
		return pts[0];
	}//at	
	
	public myCntlPt at_C(float t, myCntlPt[] pts){float[] _dpts = this.getPtDist(pts, false);float _len = this.length(pts, false);return at_C(t,new float[1], _len, pts, _dpts);}//put interpolant between adjacent axis points in s ara if needed
	public myCntlPt at_C(float t, float[] s, float _len, myCntlPt[] pts, float[] _dpts){//call directly if wanting interpolant between adj axis points too
		if(t<0){PApplet.println("In at : t="+t+" needs to be [0,1]");return pts[0];} else if (t>1){PApplet.println("In at : t="+t+" needs to be [0,1]");return pts[pts.length-1];}
		float dist = t * _len;
		for(int i=0; i<_dpts.length-1; ++i){										//built off dpts so that it will get wrap for closed curve
			if(_dpts[i+1] >= dist){
				s[0] = ((dist-_dpts[i])/(_dpts[i+1]-_dpts[i]));					//needs to stay between 0 and 1 (since interpolation functions between pts will be 0-1 based), so normalize by distance dpts[i]
				return makeNewPoint(pts,new int[]{i,((i+1)%pts.length)}, s[0]);		//put interpolant between adjacent axis points in s ara if needed		
			}			
		}		
		return new myCntlPt();
	}//at_C	
	
	/**
	 * this will conduct the appropriate interpolation on the two passed points, based on what type is set for the interpolation requested
	 * as more interpolation schemes are implemented, add cases
	 * @param A, B : points to be interpolated
	 * @param s : interpolant
	 * @param _typ : what is being interpolated - smoothing a line, sweeping the curve around the axis, etc.  for each _typ, a value should be set for this curve (i.e.smInterpTyp)
	 * @return : resultant point
	 */
	protected myPoint _Interp(myPoint A, float s, myPoint B, int _typ){
		switch (_typ){
			case linear_int : {	return pa.L(A, s, B);}
			//add more cases for different interpolation		
			default : {	return pa.L(A, s, B);}			//defaults to linear
		}	
	}//_Interp
	//same as above, but with myVectortors
	protected myVector _Interp(myVector A, float s, myVector B, int _typ){
		switch (_typ){
			case linear_int : {	return pa.V(A, s, B);}
			//add more cases for different interpolation		
			default : {	return pa.V(A, s, B);}			//defaults to linear
		}	
	}//_Interp
	//same as above but with doubles
	protected float _Interp(float A, float s, float B, int _typ){
		switch (_typ){
			case linear_int : {	return (1-s)*A + (s*B);}
			//add more cases for different interpolation		
			default : {	return (1-s)*A + (s*B);}			//defaults to linear
		}	
	}//_Interp
	protected myCntlPt _Interp(myCntlPt A, float s, myCntlPt B, int _typ){
		switch (_typ){
			case linear_int : {	return myCntlPt.L(A, s, B);}
			//add more cases for different interpolation		
			default : {	return myCntlPt.L(A, s, B);}			//defaults to linear
		}	
	}//_Interp	

	//draw currently selected point
	public void drawSelPoint(int i ){
		pa.pushMatrix();		pa.pushStyle();
		pa.stroke(255,255,0,255);
		if(flags[usesCntlPts]){pa.show(cntlPts[i], 3);} else {pa.show(pts[i], 3);}
		pa.popStyle();		pa.popMatrix();
	}
	

	public abstract void rebuildPolyPts();

	//makes a copy of the points in order
	public myPoint[] cpyPoints(myPoint[] pts){myPoint[] tmp = new myPoint[pts.length]; for(int i=0; i<pts.length; ++i){	tmp[i]=pa.P(pts[i]);}	return tmp;}//cpyPoints
	//makes a copy of the points in order
	public myCntlPt[] cpyPoints(myCntlPt[] pts){myCntlPt[] tmp = new myCntlPt[pts.length]; for(int i=0; i<pts.length; ++i){	tmp[i]=new myCntlPt(pts[i]);}	return tmp;}//cpyPoints

	//move points by the passed myVectortor 
	public myPoint[] movePoints(myVector move, myPoint[] pts){for(int i =0; i<pts.length; ++i){	pts[i]._add(move);	}	return pts;}	
	//move points by the passed myVectortor 
	public myCntlPt[] movePoints(myVector move, myCntlPt[] pts){for(int i =0; i<pts.length; ++i){	pts[i]._add(move);	}	return pts;}	
	//flip the passed points and move them based on the displacement from the passed movement myVectortor
//	public myPoint[] flipPtsAndMove(myDrawnObject _obj, myPoint[] pts, myVector move,  myVector covAxis){
//		myPoint[] tmp = movePoints(move, cpyPoints(pts));
//		return tmp;
//	}//flipPtsAndMove
//	//flip the passed cntlpts and move them based on the displacement from the passed movement myVectortor
//	public cntlPt[] flipPtsAndMove(myDrawnObject _obj, cntlPt[] pts, myVector move,  myVector covAxis, boolean reverse){
//		cntlPt[] tmp = movePoints(move, (reverse ? rotPtsAroundCOV(pts, PApplet.PI, _obj.COV, pa.U(_obj.canvasNorm), covAxis) : cpyPoints(pts)));
//		return tmp;
//	}//flipPtsAndMove
	//set this object's points to be passed points, for copying
	public void setPtsToArrayList(myCntlPt[] pts){ArrayList<myCntlPt> tmp = new ArrayList<myCntlPt>(Arrays.asList(pts));setCPts(tmp);}	
	//set this object's points to be passed points, for copying
	public void setPtsToArrayList(myPoint[] pts){ArrayList<myPoint> tmp = new ArrayList<myPoint>(Arrays.asList(pts));setPts(tmp);}	

	
	//rotate points around axis that is xprod of canvas norm and the lstroke cov to the rstroke cov myVector at stroke cov.
	//should make mirror image of pts
	public myPoint[] rotPtsAroundCOV(myPoint[] pts, float angle, myPoint cov, myVector _canvasNorm, myVector _covNorm){//need to pass canvas norm since it cannot be static
		ArrayList<myPoint> tmp = new ArrayList<myPoint>();//				res.get(sl)[i] = pa.R(old, sliceA, canvasNorm, bv, myPointOnAxis);
		for(int i=0; i<pts.length; ++i){tmp.add(pa.R(pts[i], angle, pa.V(_canvasNorm), _covNorm, cov));}
		return tmp.toArray(new myPoint[0]);			
	}//	
	//rotate points around axis that is xprod of canvas norm and the lstroke cov to the rstroke cov myVector at stroke cov.
	//should make mirror image of pts
	public myCntlPt[] rotPtsAroundCOV(myCntlPt[] pts, float angle, myPoint cov, myVector _canvasNorm, myVector _covNorm){//need to pass canvas norm since it cannot be static
		ArrayList<myCntlPt> tmp = new ArrayList<myCntlPt>();//				res.get(sl)[i] = pa.R(old, sliceA, canvasNorm, bv, myPointOnAxis);
		for(int i=0; i<pts.length; ++i){tmp.add(pa.R(pts[i], angle, pa.V(_canvasNorm), _covNorm, cov));}
		return tmp.toArray(new myCntlPt[0]);			
	}//	
	//finds index of point with largest projection on passed myVectortor in passed myPoint ara
	protected int findLargestProjection(myVector v, myPoint c, myPoint[] pts){
		double prjLen = -1, d;
		int res = -1;
		for(int i=0; i<pts.length; ++i){d = myVector._dot(v,pa.V(c, pts[i]));if(d > prjLen){prjLen = d;res = i;}}	
		return res;
	}//findLargestProjection : largest projection on passed myVectortor in passed myPoint ara		
	
	public abstract int findClosestPt(myPoint p, double[] d);
	
	//finds closest point to p in sPts - put dist in d
	protected final int findClosestPt(myPoint p, double[] d, myPoint[] _pts){
		int res = -1;
		double mindist = 99999999, _d;
		for(int i=0; i<_pts.length; ++i){_d = myPoint._dist(p,_pts[i]);if(_d < mindist){mindist = _d; d[0]=_d;res = i;}}	
		return res;
	}
	//reorder verts so that they start at newStIdx - only done for closed  meshes, so should wrap around
	//made static so doesn't have to be used with pts array - should be called by implementing class
	protected ArrayList<myPoint> reorderPts(int newStIdx, myPoint[] pts){
		ArrayList<myPoint> tmp = new ArrayList<myPoint>(Arrays.asList(pts));
		for(int i = 0; i<pts.length; ++i){tmp.set(i, pts[(i+newStIdx)%pts.length]);	}
		return tmp;
	}		
	
	public abstract void remakeDrawnTraj(boolean useVels);

	//run at the end of drawing a curve - will set appropriate flags, execute smoothing, subdivision, resampling, etc
	public abstract void finalizeDrawing(boolean procPts);	

	public void drawMe() {
		pa.pushMatrix();
		pa.pushStyle();
		pa.setColorValFill(fillClr, 255);
		pa.setColorValStroke(strkClr, 255);
			pa.strokeWeight(1);
//			if(flags[useProcCurve]){pa.show(pts);} 
//			else {			
				pa.curve(pts);
				//}
		pa.popStyle();			
		pa.popMatrix();
//		if(flags[drawNorms] && (nAra != null)&& (tAra != null)&& (bAra != null)){drawNorms(pts, nAra,tAra,bAra,20);}
//		drawCOV();
	}
	public abstract void dragPicked(myVector disp, int idx);
	public abstract void dragAll(myVector disp);			//move COV to location pointed at by mouse, and move all points by same displacement
	//drag point represented by passed idx in passed array - either point or cntl point
	protected void dragPicked(myVector dispVec, int idx, myPoint[] pts) {if((-1 != idx) && (pts[idx] != null)){pts[idx]._add(dispVec);flags[reCalcPoints]=true;}}
	protected void dragAll(myVector dispVec, myPoint[] pts){if((pts == null)||(pts.length == 0)){return;}for(int i=0;i<pts.length;++i){pts[i]._add(dispVec);}flags[reCalcPoints]=true;}
	//rotate points around axis that is xprod of canvas norm and the lstroke cov to the rstroke cov vec at stroke cov.
	//should make mirror image of pts
	public myPoint[] rotPtsAroundCOV(myPoint[] _pts, double angle, myPoint cov, myVector _canvasNorm, myVector _covNorm){//need to pass canvas norm since it cannot be static
		ArrayList<myPoint> tmp = new ArrayList<myPoint>();//				res.get(sl)[i] = pa.R(old, sliceA, canvasNorm, bv, ptOnAxis);
		for(int i=0; i<_pts.length; ++i){tmp.add(pa.R(_pts[i], angle, pa.V(_canvasNorm), _covNorm, cov));}
		return tmp.toArray(new myPoint[0]);			
	}//	
	//rotate points around axis that is xprod of canvas norm and the lstroke cov to the rstroke cov vec at stroke cov.
	//should make mirror image of pts
	public myCntlPt[] rotPtsAroundCOV(myCntlPt[] _pts, double angle, myPoint cov, myVector _canvasNorm, myVector _covNorm){//need to pass canvas norm since it cannot be static
		ArrayList<myCntlPt> tmp = new ArrayList<myCntlPt>();//				res.get(sl)[i] = pa.R(old, sliceA, canvasNorm, bv, ptOnAxis);
		for(int i=0; i<_pts.length; ++i){tmp.add(pa.R(_pts[i], angle, pa.V(_canvasNorm), _covNorm, cov));}
		return tmp.toArray(new myCntlPt[0]);			
	}//		
	
	//returns array of distances to each point from beginning - needs to retain dist from last vert to first if closed
	//public final float[] getPtDist(){float[] res = new float[pts.length+1];res[0]=0;for(int i=1; i<pts.length; ++i){res[i] = res[i-1] + pa.d(pts[i-1],pts[i]);}if(flags[isClosed]){res[pts.length] = res[pts.length-1] +  pa.d(pts[pts.length-1],pts[0]);}return res;}
	//public final float[] getPtDist(){return getPtDist(pts, flags[isClosed]);}
	//returns array of distances to each point from beginning - needs to retain dist from last vert to first if closed
	public final float[] getPtDist(myPoint[] pts, boolean wrap){
		float[] res = new float[pts.length+1];
		res[0]=0;
		for(int i=1; i<pts.length; ++i){
			//System.out.println("i : "+i);
			res[i] = res[i-1] + (float)myPoint._dist(pts[i-1],pts[i]);
			}
		if(wrap){
			//System.out.println("wrap");
			res[pts.length] = res[pts.length-1] +  (float)myPoint._dist(pts[pts.length-1],pts[0]);
		} else {
			//System.out.println("no wrap");
			
			res[pts.length] = 0;
		}
		
		return res;}
	//returns length of curve, including endpoint if closed
	//public final float length(){return length(pts, flags[isClosed]);}//{float res = 0;for(int i =0; i<pts.length-1; ++i){res += pa.d(pts[i],pts[i+1]);}if(flags[isClosed]){res+=pa.d(pts[pts.length-1],pts[0]);}return res;}
	public final float length(myPoint[] pts, boolean closed){float res = 0;for(int i =0; i<pts.length-1; ++i){res += (float)myPoint._dist(pts[i],pts[i+1]);}if(closed){res+=(float)myPoint._dist(pts[pts.length-1],pts[0]);}return res;}


	public void drawCOV(){		if(COV == null) {return;}		pa.pushMatrix();		pa.pushStyle();	pa.stroke(255,0,255,255);		pa.show(COV,3);		pa.popStyle();		pa.popMatrix();	}
	//drawCntlRad
	public myPoint getPt(int i){return pts[i];}
	
	public void initFlags(){flags = new boolean[numFlags]; for(int i=0;i<numFlags;++i){flags[i]=false;}}
	public String toString(){
		String res = "#pts : "+pts.length+" len : "+ len ;
		return res;
	}

}//myDrawnObject

