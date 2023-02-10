package SphrSeqFFTVisPKG;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_Math_Objects.vectorObjs.floats.myPointf;
import base_Math_Objects.vectorObjs.floats.myVectorf;
import base_Render_Interface.IRenderInterface;
import base_UI_Objects.GUI_AppManager;
import processing.core.PConstants;
import processing.core.PMatrix3D;
import processing.opengl.PGL;
import processing.opengl.PGraphics3D;

public class Disp3DCanvas {
	public IRenderInterface p;
	
	/**
	 * Screen ctr location in world coords
	 */		
	private myPoint scrCtrInWorld;
	private myPoint eyeInWorld; 
	private myPoint oldMseLoc;
	private myPoint dfCtr;														//mouse location projected onto current drawing canvas

	private final float canvasDim = 15000,
			canvasDimOvSqrt2 = MyMathUtils.INV_SQRT_2_F * canvasDim; 			//canvas dimension for "virtual" 3d		
	private myPoint[] canvas3D;													//3d plane, normal to camera eye, to be used for drawing - need to be in "view space" not in "world space", so that if camera moves they don't change
	private myVector eyeToMse,													//eye to 2d mouse location 
					eyeToCtr,													//vector from eye to center of cube, to be used to determine which panels of bounding box to show or hide
					drawSNorm;													//current normal of viewport/screen
		
	private int viewDimW, viewDimH,viewDimW2, viewDimH2;
	public float curDepth;
	
	
	private int[] mseFillClr;
	
	/**
	 * Screen-space depth in window of screen-space halfway point.
	 */
	private float rawCtrDepth;
	
	
	
	public Disp3DCanvas(IRenderInterface _p,  int w, int h) {
		p = _p;
		mseFillClr = new int[] {0,0,0,255};
		initCanvasVars();
		setViewDim(w,h);
	}
	
	private void initCanvasVars(){
		canvas3D = new myPoint[4];		//3 points to define canvas
		canvas3D[0]=new myPoint();canvas3D[1]=new myPoint();canvas3D[2]=new myPoint();canvas3D[3]=new myPoint();
		eyeInWorld = new myPoint();		
		scrCtrInWorld = new myPoint();
		eyeInWorld = new myPoint();
		oldMseLoc  = new myPoint();
		dfCtr = new myPoint();											//mouse location projected onto current drawing canvas
		eyeToMse = new myVector();		
		eyeToCtr = new myVector();	
		drawSNorm = new myVector();	
	}//initCanvasVars
	
	/**
	 * Set the view dimensions and (re)build the canvas
	 * @param w
	 * @param h
	 */
	public void setViewDim(int w, int h) {
		viewDimW = w; viewDimH = h;
		viewDimW2 = viewDimW/2; viewDimH2 = viewDimH/2;	
		//Only changes when viewDims change
		rawCtrDepth = p.getDepth(viewDimW2, viewDimH2);
		buildCanvas();
	}

		//find points to define plane normal to camera eye, at set distance from camera, to use drawing canvas 	
	public void buildCanvas(){
		//float rawCtrDepth = p.getDepth(viewDimW2, viewDimH2);
		myPoint rawScrCtrInWorld = p.getWorldLoc(viewDimW2, viewDimH2,rawCtrDepth);		
		myVector A = new myVector(rawScrCtrInWorld,  p.getWorldLoc(viewDimW-10, 10,rawCtrDepth)),
				B = new myVector(rawScrCtrInWorld,  p.getWorldLoc(viewDimW-10, viewDimH-10,rawCtrDepth));	//ctr to upper right, ctr to lower right
		drawSNorm = myVector._cross(A,B)._normalize();
		//build plane using norm - have canvas go through canvas ctr in 3d
		myVector planeTan = myVector._cross(drawSNorm, myVector._normalize(new myVector(drawSNorm.x+10000,drawSNorm.y+10,drawSNorm.z+10)))._normalize();			//result of vector crossed with normal will be in plane described by normal
     	myPoint lastPt = new myPoint(myPoint.ZEROPT, canvasDimOvSqrt2, planeTan);
     	planeTan = myVector._rotAroundAxis(planeTan, drawSNorm, MyMathUtils.THREE_QTR_PI);
		for(int i =0;i<canvas3D.length;++i){		
			//build invisible canvas to draw upon
     		canvas3D[i].set(myPoint._add(lastPt, canvasDim, planeTan));
     		//rotate around center point by 90 degrees to build a square canvas
     		planeTan = myVector._rotAroundAxis(planeTan, drawSNorm);
     		lastPt = canvas3D[i];
     	}

		//normal to canvas through eye moved far behind viewer
		eyeInWorld = p.getWorldLoc(viewDimW2, viewDimH2,-.00001f);
		//eyeInWorld =myPoint._add(rawScrCtrInWorld, myPoint._dist( p.pick(0,0,-1), rawScrCtrInWorld), drawSNorm);								//location of "eye" in world space
		eyeToCtr.set(eyeInWorld, rawScrCtrInWorld);
		scrCtrInWorld = getPlInterSect(rawScrCtrInWorld, myVector._normalize(eyeToCtr));
		
		myPoint mseLocInWorld = getMseLocInWorld();	
		eyeToMse.set(eyeInWorld, mseLocInWorld);		//unit vector in world coords of "eye" to mouse location
		eyeToMse._normalize();
		oldMseLoc.set(dfCtr);
		dfCtr = getPlInterSect(mseLocInWorld, eyeToMse);

	}//buildCanvas()
	
	//return a unit vector from the screen location of the mouse pointer in the world to the reticle location in the world - for ray casting onto objects the mouse is over
	public myVector getMse2DtoMse3DinWorld(myPoint glbTrans){			
		int[] mse = p.getMouse_Raw_Int();
		myVector res = new myVector(p.getWorldLoc(mse[0], mse[1],-.00001f),getMseLoc(glbTrans) );		
		return res._normalize();
	}

	public final myVector getUScrUpInWorld(){			myVector res = new myVector(p.getWorldLoc(viewDimW2, viewDimH2,-.00001f),p.getWorldLoc(viewDimW2, viewDimH,-.00001f));		return res._normalize();}	
	public final myVector getUScrRightInWorld(){		myVector res = new myVector(p.getWorldLoc(viewDimW2, viewDimH2,-.00001f),p.getWorldLoc(viewDimW, viewDimH2,-.00001f));		return res._normalize();}
	public final myVectorf getUScrUpInWorldf(){		myVectorf res = new myVectorf(p.getWorldLoc(viewDimW2, viewDimH2,-.00001f),p.getWorldLoc(viewDimW2,viewDimH,-.00001f));	return res._normalize();}	
	public final myVectorf getUScrRightInWorldf(){	myVectorf res = new myVectorf(p.getWorldLoc(viewDimW2, viewDimH2,-.00001f),p.getWorldLoc(viewDimW, viewDimH2,-.00001f));	return res._normalize();}
	
	
	public myVector getDrawSNorm() {return drawSNorm;}
	public myVectorf getDrawSNorm_f() {return new myVectorf(drawSNorm);}
	
	
	//find pt in drawing plane that corresponds with point and camera eye normal
	public myPoint getPlInterSect(myPoint pt, myVector unitT){
		myPoint dctr = new myPoint(0,0,0);	//actual click location on visible plane
		 // if ray from E along T intersects triangle (A,B,C), return true and set proposal to the intersection point
		intersectPl(pt, unitT, canvas3D[0],canvas3D[1],canvas3D[2],  dctr);//find point where mouse ray intersects canvas
		return dctr;		
	}//getPlInterSect	
	
	/**
	 * Only have here until we get AppManager going
	 * @param E
	 * @param T
	 * @param A
	 * @param B
	 * @param C
	 * @param X
	 * @return
	 */
	private boolean intersectPl(myPoint E, myVector T, myPoint A, myPoint B, myPoint C, myPoint X) { // if ray from E along T intersects triangle (A,B,C), return true and set proposal to the intersection point
		myVector EA=new myVector(E,A), AB=new myVector(A,B), AC=new myVector(A,C); 		double t = (float)(myVector._mixProd(EA,AC,AB) / myVector._mixProd(T,AC,AB));		X.set(myPoint._add(E,t,T));		return true;
	}	

	
	
	/**
	 * Mouse location in world at given depth
	 * @return
	 */
	public myPoint getMseLocInWorld() {
		float ctrDepth = p.getSceenZ((float)scrCtrInWorld.x, (float)scrCtrInWorld.y, (float)scrCtrInWorld.z);
		int[] mse = p.getMouse_Raw_Int();
		return p.getWorldLoc(mse[0],mse[1],ctrDepth);		
	}

	/**
	 * 
	 * @return
	 */
	public myPoint getMseLoc(){return new myPoint(dfCtr);	}
	/**
	 * 
	 * @return
	 */
	public myPointf getMseLoc_f(){return new myPointf(dfCtr.x,dfCtr.y,dfCtr.z);	}
	/**
	 * 
	 * @return
	 */
	public myPoint getOldMseLoc(){return new myPoint(oldMseLoc);	}	
	/**
	 * 
	 * @return
	 */
	public myVector getMseDragVec(){return new myVector(oldMseLoc,dfCtr);}
	
	/**
	 * relative to passed origin
	 * @param glbTrans
	 * @return
	 */
	public myPoint getMseLoc(myPoint glbTrans){return myPoint._sub(dfCtr, glbTrans);	}
	/**
	 * move by passed translation
	 * @param glbTrans
	 * @return
	 */
	public myPointf getTransMseLoc(myPointf glbTrans){return myPointf._add(dfCtr, glbTrans);	}
	/**
	 * dist from mouse to passed location
	 * @param glbTrans
	 * @return
	 */
	public float getMseDist(myPointf glbTrans){return new myVectorf(dfCtr, glbTrans).magn;	}
	/**
	 * 
	 * @param glbTrans
	 * @return
	 */
	public myPoint getOldMseLoc(myPoint glbTrans){return myPoint._sub(oldMseLoc, glbTrans);	}
	/**
	 * 
	 * @return
	 */
	public myPoint getEyeInWorld() {return eyeInWorld;}
	
	//get normalized ray from eye loc to mouse loc
	public myVectorf getEyeToMouseRay_f() {
		myVectorf ray = new myVectorf(eyeInWorld, dfCtr);
		return ray._normalize();
	}
	

	
	public void drawMseEdge(boolean projOnBox){//draw mouse sphere and edge normal to cam eye through mouse sphere 
		p.pushMatState();
			p.setStrokeWt(1f);
			p.setStroke(255, 0,255, 255);
			//draw line through mouse point and eye location in world	
			p.drawLine(eyeInWorld, dfCtr);
			p.translate(dfCtr);
			//project mouse point on bounding box walls
			if(projOnBox){((SeqVisFFTOcean)p).drawProjOnBox(dfCtr);}
			((SeqVisFFTOcean)p).drawAxes(10000,1f, myPoint.ZEROPT, 100, true);//
			//draw intercept with box
			//draw center point
			p.showPtAsSphere(myPointf.ZEROPT,3.0f, 5, IRenderInterface.gui_Black, IRenderInterface.gui_Black);

			((SeqVisFFTOcean)p).drawText(""+dfCtr+ "|fr:"+p.getFrameRate(),4.0f, 15.0f, 4.0f, mseFillClr);
		p.popMatState();			
	}//drawMseEdge		
}

