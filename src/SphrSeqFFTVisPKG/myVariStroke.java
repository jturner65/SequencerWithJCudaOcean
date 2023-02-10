package SphrSeqFFTVisPKG;

import java.util.ArrayList;
import java.util.Arrays;

import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PApplet;
import processing.core.PConstants;

public class myVariStroke extends myDrawnObject {

	protected final int numVerts = 200;							

	public int offsetType;						//1:Q-bspline w/normal offset, 2:Q-bspline w/ball offset, 3:Q-bspline w/radial offset
	public Base_Offset _offset;					//offset used by this stroke to calculate poly loop
	public final int numIntCntlPts = 200, numCntlPts = 6;			//# of control points to use to draw line
	private boolean ptsDerived;					//whether or not the points making up the loop of this stroke have been derived yet

	
	public myPoint[] drawnCntlPts;						//control point locations - start point +  vel myVectortor (scaled tangent), repeat
	public float[] d_drwnCntlPts;					//distance between each drawnCntlPts points
	public float drwnCntlLen;						//len of arc of drawnCntlPts pts

	public float[] cntlPtIntrps;				//interpolants for each control point as part of total, based upon radius of controlpoint - larger will be bigger. 
												//for each cntl myPoint, this value will be cntl point rad / total control point rads.
	//interpolated drawn curve, weighted by drawn speed
	public myPoint[] interpCntlPts;					//interpolated control point locations - start point +  vel myVectortor (scaled tangent), repeat
	public float[] d_interpPts;					//distance between interpolated points
	public float interpLen;						//len of interp pts
	
	public myVariStroke(SeqVisFFTOcean _pa, myVector _canvNorm, int fillClrCnst, int strkClrCnst) {
		super(_pa, _canvNorm);
		flags[isClosed] = false;	
		fillClr = fillClrCnst;
		strkClr= strkClrCnst;
		flags[drawCntlRad] = true;
	    cntlPts = new myCntlPt[0];
	    interpCntlPts = new myPoint[0];
		_offset = new Normal_Offset();
		flags[usesCntlPts] = true;
		flags[interpStroke] = true;
		ptsDerived = false;
		flags[cntlWInvRad] = false;			//whether slow drawing makes rad larger or smaller
	}

	//as drawing, add points to -cntlPoints-, not pts array.
	public void addPt(myPoint p){
		ArrayList<myCntlPt> tmp = new ArrayList<myCntlPt>(Arrays.asList(cntlPts));
		int i = tmp.size()-1;
		if(i > 0 ){tmp.get(i).w = calcCntlWeight(p,tmp.get(i),tmp.get(i-1));}//previous point's weight 
		myCntlPt tmpPt = new myCntlPt(p);
		tmp.add(tmpPt);
		setCPts(tmp);
	}//
	
	public void finalizeDrawing(boolean procPts){
		//by here we have the drawn points from user input we want use the offset to determine the actual points of the curve we want to put this in a function so that any changes to the 
		//cntlpoints can be cascaded down to the actual loop		
		buildPointsUsingOffset(procPts, numReps);
		//calculate line points from control points
		//find loop around stroke line by cntl points' radii once loop is built, treat as poly loop
		flags[isMade] = true;
		flags[drawCntlRad] = false;
		//build array of weights from 
		buildInterpAra();
	}//finalize
	
	//build pts array using cntlpoints and offset chosen
	public void buildPointsUsingOffset(boolean procPts, int repCnt){
		if(procPts){
		    finalizeCntlW();
		    for(int i=0;i<cntlPts.length;++i){cntlPts[i].calcRadFromWeight(cntl_len/cntlPts.length, flags[cntlWInvRad], pa.wScale);}           //sets all radii based on weights
		    processCntlPts(flags[interpStroke] ? numIntCntlPts : numCntlPts, repCnt);
	    }
		buildCntlFrameVecAras();
		buildPtsFromCntlPts();
	}
	//build interpolants based upon weight of each cntl point.  these can be used to determine how far the edge moves (velocity) and how much it rotates per frame
	//when this is done, we have multipliers to apply to each tangent myVectortor to determine displacement for each of the frames
	public void buildInterpAra(){
		cntlPtIntrps = new float[numIntCntlPts];			//interpolant from each control point - weights
		
		float[] cmyPointInterps = new float[numIntCntlPts];
		interpCntlPts = new myPoint[numIntCntlPts];
		drawnCntlPts = new myPoint[numIntCntlPts];
		float sumWts = 0;
		for(int i=0;i<cntlPts.length;++i){sumWts += cntlPts[i].w;cmyPointInterps[i] = (float) cntlPts[i].w;cntlPtIntrps[i]=sumWts;}
		//for(int i=0;i<cntlPts.length;++i){sumWts += cntlPts[i].w;cntlPtIntrps[i]=cntlPts[i].w;}
		//System.out.println("total weight = " + sumWts);
		for(int i=0;i<cntlPts.length;++i){cntlPtIntrps[i]/=sumWts;cmyPointInterps[i]/=sumWts;}
		//smooth interpolants now
		cntlPtIntrps = dualFloats(cntlPtIntrps);
		cmyPointInterps = dualFloats(cmyPointInterps);
		
		interpCntlPts[0] = pa.P(cntlPts[0]);			//set first point
		drawnCntlPts[0] = pa.P(cntlPts[0]);
		double distStToEnd = myPoint._dist(cntlPts[0], cntlPts[cntlPts.length-1]);			//distance from first to last point
		
		for(int i=1;i<cntlPts.length;++i){
			interpCntlPts[i] = pa.P(interpCntlPts[i-1],pa.W(distStToEnd * cmyPointInterps[i], c_tAra[i]));		
			drawnCntlPts[i] = pa.P(cntlPts[i]);
		}	
		for(int i= cntlPts.length; i<numIntCntlPts; ++i){
			interpCntlPts[i] = pa.P(interpCntlPts[i-1],pa.W(distStToEnd * cmyPointInterps[i], c_tAra[c_tAra.length-1]));		
			drawnCntlPts[i] = pa.P(cntlPts[cntlPts.length-1]);
			
		}
		d_interpPts = getPtDist(interpCntlPts, false);	
		interpLen = length(interpCntlPts, false);
		d_drwnCntlPts = getPtDist(drawnCntlPts, false);	
		drwnCntlLen = length(drawnCntlPts, false);		
		//smooth/equi-space interpolated cntl points
		processInterpPts( numIntCntlPts, numReps);
	}	
	//subdivide, tuck, respace, resample, etc. pts of this curve
	public void processInterpPts(int numPts, int numReps){
		//setInterpPts(procInterpPts(_subdivide, interpCntlPts, 2, interpLen));										//makes 1 extra vert  equilspaced between each vert, to increase resolution of curve
		for(int i = 0; i < numReps; ++i){
			if(i % 2 == 0){setInterpPts(procInterpPts(_subdivide, interpCntlPts, 2, interpLen));}
			setInterpPts(procInterpPts(_tuck, interpCntlPts, .5f, interpLen));
			setInterpPts(procInterpPts(_tuck, interpCntlPts, -.5f, interpLen));
		}		//smooth curve - J4
		setInterpPts(procInterpPts(_equaldist, interpCntlPts, .5f, interpLen));
		for(int i = 0; i < numReps; ++i){
			//if(i % 2 == 0){setInterpPts(procInterpPts(_subdivide, interpCntlPts, 2, interpLen));}
			setInterpPts(procInterpPts(_tuck, interpCntlPts, .5f, interpLen));
			setInterpPts(procInterpPts(_tuck, interpCntlPts, -.5f, interpLen));
		}		//smooth curve - J4
		setInterpPts(procInterpPts(_resample, interpCntlPts, numPts, interpLen));	
	}	
	
	//return appropriate ara of points based on using velocities or not
	public myPoint[] getDrawnPtAra(boolean useVels){	return (useVels ? interpCntlPts : cntlPts);}
	
	//remake drawn trajectory after edit
	public void remakeDrawnTraj(boolean useVels){
		if(useVels){
			for(int i = 0; i < 10; ++i){
				if(i % 2 == 0){setInterpPts(procInterpPts(_subdivide, interpCntlPts, 2, interpLen));}
				setInterpPts(procInterpPts(_tuck, interpCntlPts, .5f, interpLen));
				setInterpPts(procInterpPts(_tuck, interpCntlPts, -.49f, interpLen));
			}		//smooth curve - J4
			setInterpPts(procInterpPts(_equaldist, interpCntlPts, .5f, interpLen));
			setInterpPts(procInterpPts(_resample, interpCntlPts, numIntCntlPts, interpLen));	
		} else {
			for(int i = 0; i < 10; ++i){
				//setInterpPts(procInterpPts(_subdivide, interpCntlPts, 2, interpLen));
				setCPts(procCntlPt(_tuck, cntlPts, .5f, cntlPts.length));
				setCPts(procCntlPt(_tuck, cntlPts, -.49f, cntlPts.length));
			}		//smooth curve - J4
			setCPts(procCntlPt(_equaldist, cntlPts, .5f, cntlPts.length));
			setCPts(procCntlPt(_resample, cntlPts, numIntCntlPts, cntlPts.length));
		}	
	}//remakeDrawnTraj
	
	//modify curve via editing
	public void handleMouseDrag(myVector dispVec, int drawnTrajPickedIdx){		
		if((drawnTrajPickedIdx == 0) || (drawnTrajPickedIdx == pts.length-1)) {return;}	//rough bounds checking
		myPoint[] pts = getDrawnPtAra(false);
		int minBnd = PApplet.max(drawnTrajPickedIdx - pa.drawnTrajEditWidth, 0),
			maxBnd = PApplet.min(drawnTrajPickedIdx + pa.drawnTrajEditWidth, pts.length-1);		
		//System.out.println("drag in drag zone inside disp calc -> idx bounds : " + minBnd + " | " + maxBnd);
		float modAmt, invdistLow = 1.0f/(drawnTrajPickedIdx - minBnd), invdistHigh = 1.0f/(maxBnd - drawnTrajPickedIdx);
		for(int idx = minBnd; idx < maxBnd; ++idx){
			float divMultVal = (idx > drawnTrajPickedIdx) ? invdistHigh:invdistLow;
			modAmt = pa.trajDragScaleAmt* PApplet.cos((idx-drawnTrajPickedIdx) * PConstants.HALF_PI * divMultVal);//trajDragScaleAmt/abs(1 + (idx-drawnTrajPickedIdx));
			//modAmt *= modAmt;
			pts[idx]._add(pa.W(modAmt,dispVec));
		}
	}
	//scale points to be a scaleAmt * current distance from line of myPoint a -> myPoint b
	public void scalePointsAboveAxis(myPoint a, myPoint b, myVector perpVec, double scaleAmt){
		myPoint[] pts = getDrawnPtAra(false);//, newPts = new myPoint[pts.length];\
		int numPoints = pts.length;
//		if((Double.isNaN(pts[pts.length-1].x)) || 
//				(Double.isNaN(pts[pts.length-1].y)) ||
//				(Double.isNaN(pts[pts.length-1].z))){
//			//pa.outStr2Scr("NaN pts size at start of scalePointsAboveAxis",true);
//			numPoints--;										//toss last NaN Point
//		}
		//myPoint[] newPts = new myPoint[numPoints];
		double dist;
		//pa.outStr2Scr("cntlPts size at scalePointsAboveAxis : " + pts.length,true);
		for(int i =0; i<numPoints; ++i){
			dist = pa.distToLine(pts[i], a,b);
			//if(Double.isNaN(dist)){dist = 0;}
			myPoint pointOnLine = pa.projectionOnLine(pts[i], a,b);
			myVector resVec = pa.V(dist*scaleAmt,pa.U(pointOnLine,pts[i]));
			//pa.outStr2Scr("cntlPts : st : dist*scale : "+ (dist*scaleAmt)+" dist : "+ (dist)+" scale : "+ (scaleAmt)+" stPoint : " + pts[i].toStrBrf() + " | linePt : " + pointOnLine.toStrBrf() + " | resVec : " +resVec.toStrBrf() ,true);
			pts[i].set(pa.P(pointOnLine, resVec));
		}
		//pa.outStr2Scr("cntlPts size at scalePointsAboveAxis end : " + pts.length,true);
//		for(int i =0; i<newPts.length; ++i){
//			pts[i].set(newPts[i]);
//		}
	}
	
	//sets required info for points array - points and dist between pts, length, etc
	protected void setInterpPts(ArrayList<myPoint> tmp){
		interpCntlPts = tmp.toArray(new myPoint[0]);
		d_interpPts = getPtDist(interpCntlPts, false);	
		interpLen=length(interpCntlPts, false);
	}//setPts	
	//tuck untuck float values
	public float[] dualFloats(float[] src){
		float[] res = new float[src.length],res1 = new float[src.length];
		res1[0]=src[0];
		res1[src.length-1]=src[src.length-1];
		for(int i=1; i<src.length-1;++i){
			res1[i]=_Interp(src[i],.5f,_Interp(src[i-1],.5f,src[i+1],lnI_Typ),lnI_Typ);
		}
		res[0]=res1[0];
		res[src.length-1]=res1[src.length-1];
		for(int i=1; i<res1.length-1;++i){
			res[i]=_Interp(res1[i],-.5f,_Interp(res1[i-1],.5f,res1[i+1],lnI_Typ),lnI_Typ);
		}			
		return res;		
	}
	
	/**
	 * process all points using passed algorithm on passed array of points - not all args are used by all algs.
	 * @param _typ type of point processing
	 * @param _pts array to be processed
	 * @param val quantity used by variou processing : subdivision-> # of new pts +1, tuck-> amt to be tucked,  resample-> # of new verts
	 * @param len length of segment described by points, including ends if closed
	 * @return arraylist of processed points
	 */
	public ArrayList<myPoint> procInterpPts(int _typ, myPoint[] _pts, float val, float _len){
		ArrayList<myPoint> tmp = new ArrayList<myPoint>(); // temporary array
		switch(_typ){
			case _subdivide	:{
			    for(int i = 0; i < _pts.length-1; ++i){tmp.add(_pts[i]); for(int j=1;j<val;++j){tmp.add(makeNewPoint(_pts,new int[]{i,i+1}, (j/(val))));}}
			    tmp.add(_pts[_pts.length-1]);				
			    return tmp;}
			case _tuck		:{
				tmp.add(0,_pts[0]);
			    for(int i = 1; i < _pts.length-1; ++i){	tmp.add(i,makeNewPoint(_pts,new int[]{i,i-1,i+1}, val));   }
		    	tmp.add(_pts[_pts.length-1]);		
		    	return tmp;}
			case _equaldist	:{
				float ratio = _len/(1.0f * _pts.length),curDist = 0;					 //new distance between each vertex, iterative dist travelled so far			 
				for(int i =0; i<_pts.length; ++i){tmp.add(at_I(curDist/_len));curDist+=ratio;}	
				tmp.add(_pts[_pts.length-1]);				
				return tmp;}	
			case _resample	:{
				float ratio = _pts.length/(1.0f * (val-1)),f;					//distance between each vertex		 
				int idx, newIdx=0;		
				for(float i = 0; i<_pts.length-1; i+=ratio){idx = (int)i;	f = i-idx;tmp.add(newIdx++,makeNewPoint(_pts,new int[]{idx,idx+1},f));}
				tmp.add(_pts[_pts.length-1]);			//always add another point if open line/loop - want to preserve end point
				break;}	
			default :
		}
		
		return tmp;
	}

	public myPoint at_I(float t){return at(t,new float[1], interpLen, interpCntlPts, d_interpPts);}//put interpolant between adjacent points in s ara if needed
	public myPoint at_I(float t, float[] s){	return at(t,s, interpLen, interpCntlPts, d_interpPts);}//put interpolant between adjacent points in s ara if needed
	
	
	private void buildPtsFromCntlPts(){
		ArrayList<myPoint> tmp =  _offset.calcOffset(cntlPts, c_bAra, c_tAra) ;
		pts = tmp.toArray(new myPoint[0]);		
		ptsDerived = true;		
	}//buildPtsFromCntlPts

	//calculate initial weights for last few points of drawn cntlpoint line
	private void finalizeCntlW(){
		if(cntlPts.length == 0){return ;}
		cntlPts[0].w = cntlPts.length < 2 ? 1 : cntlPts[1].w;
		if(cntlPts.length == 1){return ;}
		cntlPts[cntlPts.length-2].w = cntlPts.length < 3 ? cntlPts[0].w : calcCntlWeight(cntlPts[cntlPts.length-3],cntlPts[cntlPts.length-2],cntlPts[cntlPts.length-3]);
		cntlPts[cntlPts.length-1].w = cntlPts.length < 2 ? cntlPts[0].w : cntlPts[cntlPts.length-2].w;
	}
	
	//build poly loop points using offsets from control points radii
	public void rebuildPolyPts(){
		flags[reCalcPoints]=false;
		//buildPointsUsingOffset(true,1);
		buildPtsFromCntlPts();
		flags[isMade] = true;
	}
	
	//print out all trajectory point locations for debugging
	public void dbgPrintAllPoints(boolean useDrawnVels){
    	if(useDrawnVels){
    		pa.outStr2Scr("Drawn Vels Traj :\n");
			for(int i = 0; i < interpCntlPts.length; ++i){
				pa.outStr2Scr("\tpt " + i +" : " + interpCntlPts[i].toStrBrf());
			}
    	} else {			
       		pa.outStr2Scr("Drawn Traj :\n");
			for(int i = 0; i < cntlPts.length; ++i){
				pa.outStr2Scr("\tpt " + i +" : " + cntlPts[i].toStrBrf());
			}
    	}
	}
	
	public void drawMe(boolean useDrawnVels, boolean flat){
		pa.pushMatrix();
		pa.pushStyle();
			pa.setColorValFill(fillClr, 255);
			pa.setColorValStroke(strkClr, 255);
			pa.strokeWeight(1);
        	if(useDrawnVels){
        		//int clrInt = 0;
    			for(int i = 0; i < interpCntlPts.length; ++i){
    	        	//clrInt = (int)(i/(1.0f * interpCntlPts.length) * 255.0f);
    	            //pa.fill(clrInt,255,(255 - clrInt),255);  
    	            //pa.stroke(clrInt,255,(255 - clrInt),255); 
    				pa.show(interpCntlPts[i],trajPtRad,-1,-1, flat);
    				if(flags[drawCntlRad]){pa.circle(this.interpCntlPts[i], (float) this.cntlPts[i].r,this.c_bAra[i], this.c_tAra[i],20);}
    			}
        	} else {			
				for(int i = 0; i < cntlPts.length; ++i){
					pa.show(cntlPts[i],trajPtRad,fillClr,strkClr, flat);
				}
				if(flags[drawCntlRad]){this._offset.drawCntlPts(pa, this.cntlPts, this.c_bAra, this.c_tAra, ptsDerived);}
        	}
		pa.popStyle();			
		pa.popMatrix();		
	}//
	
	//scale the points - for when the window is resized
	public void scaleMeY(boolean useDrawnVels, float scAmtY, float borderOffset){
		myPoint[] pts = getDrawnPtAra(useDrawnVels);
  			for(int i = 0; i < pts.length; ++i){
				pts[i].y -= borderOffset;
				pts[i].y *= scAmtY;
				pts[i].y += borderOffset;
    	}
	}//scaleMeY
	
	public myPoint[] moveVelCurveToEndPoints(myPoint startPt, myPoint endPt, boolean flip){
		int numPoints = interpCntlPts.length;

		myPoint[] destCurve = new myPoint[numPoints];

		if(numPoints == 0){return destCurve;}
		myPoint origin = interpCntlPts[0];
		myPoint end = interpCntlPts[numPoints - 1];

		//edge params		
		myVector drawnAxis = pa.V(origin, end);
		myVector edgeAxis =  pa.V(startPt, endPt);		//angle between these two is the angle to rotate everyone
		
		//transformation params
		myVector dispToStart = pa.V(origin, startPt);			//displacement myVectortor between start of drawn curve and edge 1.

		double alpha =  -pa.angle(drawnAxis,edgeAxis);			//angle to rotate everyone
		double scaleRatio = edgeAxis._mag()/drawnAxis._mag();	//ratio of distance from start to finish of drawn traj to distance between edges - multiply all elements in drawn traj by this
	
		//displace to align with start
		destCurve = movePoints(dispToStart, interpCntlPts);
		//build displacement vector - flip across axis if flipped curve
		myVector[] dispVecAra = new myVector[numPoints];
		dispVecAra[0] = new myVector();
		for(int myPointItr = 1; myPointItr < numPoints ; ++myPointItr){
			dispVecAra[myPointItr] = pa.V(destCurve[0],destCurve[myPointItr]);
		}			
		if((flip) || flags[isFlipped]){
			myVector udAxis = pa.U(drawnAxis);
			myVector normPt, tanPt;
			for(int myPointItr = 1; myPointItr < numPoints ; ++myPointItr){
				tanPt = myVector._mult(udAxis, dispVecAra[myPointItr]._dot(udAxis));			//component in udAxis dir
				normPt = myVector._sub(dispVecAra[myPointItr],tanPt);
				normPt._mult(2);
				dispVecAra[myPointItr]._sub(normPt);
			}
			flags[isFlipped] = flip;
		}

		//displace every point to be scaled distance from start of curve equivalent to scale of edge distances to drawn curve
		for(int myPointItr = 1; myPointItr < numPoints ; ++myPointItr){
			destCurve[myPointItr].set(pa.P(destCurve[0],scaleRatio, dispVecAra[myPointItr]));//start point displaced by scaleRatio * myVectortor from start to original location of myPoint
		}
		//rotate every point around destCurve[0] by alpha
		for(int myPointItr = 1; myPointItr < numPoints ; ++myPointItr){
			destCurve[myPointItr] = pa.R(destCurve[myPointItr],alpha,this.c_bAra[0], this.c_tAra[0],destCurve[0]);
		}
		//pa.outStr2Scr("interpCntlPts size : " + interpCntlPts.length,true);
		interpCntlPts = destCurve;
		//pa.outStr2Scr("interpCntlPts size : " + interpCntlPts.length,true);
		return destCurve;
	}//
		
	public myCntlPt[] moveCntlCurveToEndPoints(myPoint startPt,myPoint endPt, boolean flip){
		int numPoints = cntlPts.length;
//		if((Double.isNaN(cntlPts[cntlPts.length-1].x)) || 
//				(Double.isNaN(cntlPts[cntlPts.length-1].y)) ||
//				(Double.isNaN(cntlPts[cntlPts.length-1].z))){
//			//pa.outStr2Scr("NaN cntlPts size at start ",true);
//			numPoints--;										//toss last NaN Point
//		}
		myCntlPt[] destCurve = new myCntlPt[numPoints];
		//pa.outStr2Scr("cntlPts size at start : " + cntlPts.length,true);
		//pa.outStr2Scr("first and last cntlPoint : " + cntlPts[0].toStrBrf() + " | " + cntlPts[numPoints-1].toStrBrf() );
		//drawn curve params
		if(numPoints == 0){return destCurve;}
		myPoint origin = cntlPts[0], end = cntlPts[numPoints - 1];
		//edge params		
		myVector drawnAxis = pa.V(origin, end);
		myVector edgeAxis =  pa.V(startPt, endPt);		//angle between these two is the angle to rotate everyone
		
		//transformation params
		myVector dispToStart = pa.V(origin, startPt);			//displacement myVectortor between start of drawn curve and edge 1.
		double alpha =  -pa.angle(drawnAxis,edgeAxis);			//angle to rotate everyone
		double scaleRatio = edgeAxis._mag()/drawnAxis._mag();	//ratio of distance from start to finish of drawn traj to distance between edges - multiply all elements in drawn traj by this

		//displace to align with start
		destCurve = movePoints(dispToStart, cntlPts);
		//build displacement vector - flip across axis if flipped curve
		myVector[] dispVecAra = new myVector[numPoints];
		dispVecAra[0] = new myVector();
		for(int myPointItr = 1; myPointItr < numPoints ; ++myPointItr){
			dispVecAra[myPointItr] = pa.V(destCurve[0],destCurve[myPointItr]);
		}			
		if((flip) || flags[isFlipped]){
			myVector udAxis = pa.U(drawnAxis);
			myVector normPt, tanPt;
			for(int myPointItr = 1; myPointItr < numPoints ; ++myPointItr){
				tanPt = myVector._mult(udAxis, dispVecAra[myPointItr]._dot(udAxis));			//component in udAxis dir
				normPt = myVector._sub(dispVecAra[myPointItr],tanPt);
				normPt._mult(2);
				dispVecAra[myPointItr]._sub(normPt);
			}
			flags[isFlipped] = flip;
		}

		//displace every point to be scaled distance from start of curve equivalent to scale of edge distances to drawn curve
		for(int myPointItr = 1; myPointItr < numPoints ; ++myPointItr){
			destCurve[myPointItr].set(pa.P(destCurve[0],scaleRatio, dispVecAra[myPointItr]));//start point displaced by scaleRatio * myVectortor from start to original location of myPoint
		}
		//rotate every point around destCurve[0] by alpha
		for(int myPointItr = 1; myPointItr < numPoints ; ++myPointItr){
			destCurve[myPointItr] = pa.R(destCurve[myPointItr],alpha,this.c_bAra[0], this.c_tAra[0],destCurve[0]);
		}
		//pa.outStr2Scr("cntlPts size : " + cntlPts.length,true);
		cntlPts = destCurve;
		//pa.outStr2Scr("cntlPts size : " + cntlPts.length,true);
		return destCurve;
	}//
	
	//move points by the passed myVectortor 
	public myCntlPt[] movePoints(myVector move, myCntlPt[] _pts){for(int i =0; i<_pts.length; ++i){	_pts[i]._add(move);	}	return _pts;}
	
	//calculate the weight of each point by determining the distance from its two neighbors - radius is inversely proportional to weight
	public float calcCntlWeight(myPoint a, myPoint p, myPoint b){	return (float)(myPoint._dist(a,p) + myPoint._dist(p,b));}
	
	//override this for cntrl-point driven constructs
	public int findClosestPt(myPoint p, double[] d){	
		return findClosestPt(p, d,cntlPts);
	}

	//drag point represented by passed idx in passed array - either point or cntl point
	public void dragPicked(myVector disp, int idx) {dragPicked(disp, idx, cntlPts);}
	//drag all points by finding displacement to mouse for COV and moving all points by same dislacement
	public void dragAll(myVector disp) {dragAll(disp, cntlPts);}	

	
	public String toString(){
		String res = "Interpolating Spline Stroke : Offset calc : "+ _offset;
		res += super.toString();
		return res;	
	}
	
}//myVariStroke