package SphrSeqFFTVisPKG.ui.controls;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.Map.Entry;

import SphrSeqFFTVisPKG.instrument.myInstrument;
import SphrSeqFFTVisPKG.note.myChord;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.durType;
import SphrSeqFFTVisPKG.ui.mySphereWindow;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import base_UI_Objects.windowUI.drawnObjs.myDrawnSmplTraj;
import base_UI_Objects.windowUI.drawnObjs.myVariStroke;
import ddf.minim.AudioOutput;
import processing.core.PImage;
import processing.core.PShape;

/**
 * object representing the spherical UI control to input control data for an instrument's notes
 * @author 7strb
 *
 */
public class mySphereCntl {
	public IRenderInterface pa;
	public static int sphCntl = 0;
	public int ID;
	
	public AudioOutput noteOut;			//here or instrument?
	
	public mySphereWindow win;	//the UI window drawing this sphere cntl	
	public myInstrument instr;	//the instrument this control handles
	public String name;
	
	public int[] sphFlags;				//state flags
	public static final int
		debugIDX 		= 0,
		hadFocusIDX 	= 1,			//was in focus but no longer - apparently not used in pre-flag ara days
		isDrumKitIDX	= 2,
		inFocusIDX		= 3;
	
	public static final int numSphFlags = 4;
	
	//graphical components for sphere UI
	//needs texture to map on sphere, and color settings for sphere and display

	public final int numNoteRings = 36;	//# of note rings to draw -> changing this will force a change to the drum machine
	
	public static final float radius = 50.0f, invRadius = 1.0f/radius;
	
	
	public float ballSpeedMult;					//multiplier for playback time for this instrument, to speed up or slow down ball rolling
	public final float ringRad = 4;			//for ray cast calc, ringrad==radius difference between drawn note rings
	public myPoint ctr, drawCtr;
	public int[] specClr, ambClr, emissiveClr;
	public float shininess = 40.0f,
				 fcsDecay = .99f;				//fcs decay is fraction of displacement for ball when it loses focus
	
	public float orbitRad,						//distance from "sun"
				initThet;						//initial rotation around sun
	public PShape mySphere;
	
	public int UIRingIDX;						//for planetary rotation - which "orbit" does this sphere belong to

	public myMiniSphrCntl[] cntls;				//mini control "moons"
	public myVector panAxis,					//rotation axis to show pan
					rotAxis,					//rotation axis to show rolling
					cntlRotAxis,				//axis to rotate controls around
					hasFocusVec,				//displacement direction toward eye when this sphere has focus
					radarBarEndPt,				//end point in circle space of radar bar (rotated to get motion)
					ntRngDispVec;				//translation vector to draw note ring
	
	public static int volIDX = 0,
					  speedIDX = 1,
					  panIDX = 2;

	public myPoint mseClkDisp;			//displacement to click location on concentric note ring
	//public static final myPoint ctrOfNotes = new myPoint(911.7756, 475.4429, -200.0001);			//ctr point in click space of notes - measure distance from this point /ringRad to get rel note value from start note
	public static final myPoint ctrOfNotes = new myPoint(1214.5973, 648.0260, 33.5801);			//ctr point in click space of notes - measure distance from this point /ringRad to get rel note value from start note
	public float curFcsZoom, 
				rollAmt,				//for rolling animation
				rotAmt,					//rotation around center				
				curSwpRotAmt,			//for sweeping arc that "plays" notes - 0->2pi
				//curSwpTime,			//for sweeping arc that plays notes - this is st time/duration (curSwpRotAmt * # tics in whole note)
				
				bncAmt;				//for beat bounce
	
	public myVector noteAlphaSt = new myVector(0,-1,0);			//measure angles from straight up from center of sphere
	public static final float noteAlphaWidth = 0.1745329251994329f;//10 degrees//0.0872664625997165f;//5 degrees //0.0174532925199433f; //default here is 1 degree//0.0872664625997165f;	
	
	public TreeMap<Integer, myNote> notes; 						//the notes of this control, keyed by integer subdivisions of 2pi (corresponding to ticks? degrees?)	
	public TreeMap<Integer,ArrayList<myNote>> noteEndLocs;			//list of notes of this control that end at a particular time
	public int[] noteClr;
	
	private int clickOnCntl;
	
	private float lastPlayAlpha;
	private static final float radarBarLen = 72;
	
	//private boolean resendNotes;
	
	public mySphereCntl(IRenderInterface _pa, mySphereWindow _win, myInstrument _instr, String _name, float _orbitRad, float _initAngle, int _UIRingIDX, PImage _txtr, int[][] _clrs){
		pa = _pa;
		ID = sphCntl++;
		win = _win;
		instr = _instr;
		UIRingIDX = _UIRingIDX;
		name = _name;
		initThet = _initAngle;
		orbitRad = _orbitRad;
		initFlags();
		panAxis = win.AppMgr.getUScrUpInWorld();
		rotAxis = win.AppMgr.getUScrRightInWorld();
		cntlRotAxis = myVector._cross(panAxis, rotAxis)._normalize();
		radarBarEndPt = new myVector();
		ctr = new myPoint();
		setCtr(initThet, 0);
		//ctr=pa.P(_ctr);
		drawCtr = new myPoint(ctr);
		specClr = new int[_clrs[0].length];
		ambClr = new int[_clrs[1].length];
		emissiveClr = new int[_clrs[2].length];
		noteClr = new int[]{0,0,0,255};
		System.arraycopy(_clrs[0],0,specClr,0,_clrs[0].length);
		System.arraycopy(_clrs[1],0,ambClr,0,_clrs[1].length);
		System.arraycopy(_clrs[2],0,emissiveClr,0,_clrs[2].length);
		ballSpeedMult = 20;	//default
		curFcsZoom = 0;
		lastPlayAlpha = -.01f;
		
		cntls = new myMiniSphrCntl[3];
//		horizBIDX = 0,				//whether this control's travel is predominantly horizontal
//		cWiseBIDX = 1,				//whether this control increases in the cwise dir
//		expValBIDX = 2;				//whether this control's value is exponential or linear
//
		float miniSphereRad = radius*.2f;
		cntls[0] = new myMiniSphrCntl(win, this, myInstrument.volCntlIDX, new myVector[]{panAxis,rotAxis,cntlRotAxis,new myVector(0,-2.0f*radius,0)},
				"Volume",
				win.buildSphere(0, miniSphereRad, ambClr, specClr, emissiveClr, shininess),
				new float[]{100.0f,0,100.0f,MyMathUtils.QUARTER_PI_F,MyMathUtils.QUARTER_PI_F,2.0f*MyMathUtils.THIRD_PI_F}, 
				new boolean[]{false,false,false},new int[]{255,255,255,255}, miniSphereRad);
		
		cntls[1] = new myMiniSphrCntl(win, this, -1, new myVector[]{panAxis,rotAxis,cntlRotAxis,new myVector(0,-2.0f*radius,0)},
				"Speed",
				win.buildSphere(1, miniSphereRad, ambClr, specClr, emissiveClr, shininess),				
				new float[]{1.0f, 0.001f,2.0f, 3.0f*MyMathUtils.HALF_PI_F, 4.0f*MyMathUtils.THIRD_PI_F,7.0f*MyMathUtils.QUARTER_PI_F}, 
				new boolean[]{false,true,true}, new int[]{255,255,255,255}, radius*.2f);
		
		cntls[2] = new myMiniSphrCntl(win, this,myInstrument.panCntlIDX, new myVector[]{panAxis,rotAxis,cntlRotAxis,new myVector(0,-2.0f*radius,0)},
				"Pan",
				win.buildSphere(2, miniSphereRad, ambClr, specClr, emissiveClr, shininess),		
				new float[]{0.0f, -1.0f, 1.0f, MyMathUtils.PI_F, 3.0f*MyMathUtils.QUARTER_PI_F,5.0f*MyMathUtils.QUARTER_PI_F}, 
				new boolean[]{true,false,false}, new int[]{255,255,255,255}, radius*.2f);
		
				
		mySphere = win.buildSphere(_txtr, radius, ambClr, specClr, emissiveClr, shininess);
	
		//set up note structure
		clearNotes();		
		clickOnCntl = -1;
//		resendNotes = true;
		setFlag(isDrumKitIDX, instr.instFlags[myInstrument.isDrumTrackIDX]);
		//isDrumKit = instr.instFlags[instr.isDrumTrackIDX];//		drumkit has special layout
	//	if(isDrumKit){win.getMsgObj().dispInfoMessage("mySphereCntl","ctor","Made a drum kit sphere : " + ID);}
	}
	
	
	//allow rotAngle to vary around some initial amount
	public void setCtr(float rotAngle, float thumpAmt){	
		float orbMult = orbitRad + thumpAmt;
		ctr.set(win.ctrVec.x+(orbMult * Math.sin(rotAngle)), win.ctrVec.y+(orbMult * Math.cos(rotAngle)),win.ctrVec.z);
	}
	
	//clear all notes for this control
	public void clearNotes(){
		notes = new TreeMap<Integer,myNote>();	
		noteEndLocs = new TreeMap<Integer,ArrayList<myNote>>();		
	}
	
	private void initFlags(){sphFlags = new int[1 + numSphFlags/32]; for(int i = 0; i<numSphFlags; ++i){setFlag(i,false);}}
	public void setFlag(int idx, boolean val){
		int flIDX = idx/32, mask = 1<<(idx%32);
		sphFlags[flIDX] = (val ?  sphFlags[flIDX] | mask : sphFlags[flIDX] & ~mask);
		switch (idx) {//special actions for each flag
			case debugIDX 		: {break;}			//debug mode
			case hadFocusIDX 	: {break;}			//had, but no longer has, focus - shrinking
			case isDrumKitIDX	: {break;}			//this is a drumkit control
			case inFocusIDX		: {break;}			//this control is currently in focus
		}
	}//setFlag	
	public boolean getFlag(int idx){int bitLoc = 1<<(idx%32);return (sphFlags[idx/32] & bitLoc) == bitLoc;}	
	//toggle value of passed flag with given probability
	public void toggleFlag(int idx, double prob){
		//walking = (Math.random() < .950) ? walking : !walking;
		if(Math.random() <= prob){setFlag(idx,!getFlag(idx));}		
	}
	
	protected int getClickRing(myVector clickVec) {
		return (int)(clickVec._mag()/(ringRad*.5f));
	}
  	

	public boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, myVector mseClickRayDir){
		boolean mod = false;
		//win.getMsgObj().dispInfoMessage("mySphereCntl","hndlMouseClickIndiv","Click in sphere : " + ID + " mseClckInWorld : " + mseClckInWorld.toStrBrf());
		myPoint clickLocInRings = getMouseLoc3D(mouseX, mouseY);
		myVector clickVec =  getClickVec(mouseX, mouseY);
		int clickRing = getClickRing(clickVec);		
		if(clickLocInRings != null){				
			win.getMsgObj().dispInfoMessage("mySphereCntl","hndlMouseClickIndiv","sphere ID : " + ID + " clickLocInRings not null : sphere ui getMouseLoc3D : " + mouseX + ","+mouseY   
					+  " \tPick Res (clickLoc) : " + clickLocInRings.toStrBrf() 
			+ "\n\tClick from ctr : " + clickLocInRings.toStrBrf()+ "  drawCtr : "+ drawCtr.toStrBrf() + " Clicked ring : " + clickRing 
			+ "\n\tVec from ring ctr to click : " +clickVec.toStrBrf() + " | mseClkDisp : "+ mseClkDisp.toStrBrf());// " disp to ctr of sphere " + myPoint._add(ctr,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar)).toStrBrf() );//" hasFocusVec : " + hasFocusVec.toStrBrf());// + " radar bar end pt : " +radarBarEndPt.toStrBrf()) ; 
		} else { //outside of sweep circle, check if on controlls
			myPoint clickLoc = getClickLoc(mouseX, mouseY);
			myPoint	clickFromCtr = myPoint._sub(clickLoc,mseClkDisp);
//			double clickDist = clickVec._mag();
			if(clickRing <= 0){//clear out trajectories
				win.clearAllTrajectories();
				clearNotes();
//				win.getMsgObj().dispInfoMessage("mySphereCntl","hndlMouseClickIndiv","hndlMouseClickIndiv clear traj sphere ID : " + ID + " sphere ui getMouseLoc3D : " + mouseX + ","+mouseY  + " curTrajAraIDX :" + win.curTrajAraIDX + " instr ID : " + instr.ID
////						+  " \tPick Res (clickLoc) : " + clickLoc.toStrBrf() 
////				+ "\n\tClick from ctr : " + clickFromCtr.toStrBrf()+ "  drawCtr : "+ drawCtr.toStrBrf() 
//				+ " Clicked ring :\t" + clickRing 
//				+ "\n\tVec from ring ctr to click :\t" +clickVec.toStrBrf() + " | mseClkDisp :\t"+ mseClkDisp.toStrBrf());// " disp to ctr of sphere " + myPoint._add(ctr,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar)).toStrBrf() );//" hasFocusVec : " + hasFocusVec.toStrBrf());// + " radar bar end pt : " +radarBarEndPt.toStrBrf()) ; 
			}
			
			win.getMsgObj().dispInfoMessage("mySphereCntl","hndlMouseClickIndiv","outside note circles sphere ID : " + ID + " sphere ui getMouseLoc3D : " + mouseX + ","+mouseY   +  " \tclickVec : " + clickVec.toStrBrf()
			+ "\n\tClick from ctr : " + clickFromCtr.toStrBrf()+ "  drawCtr : "+ drawCtr.toStrBrf() + " Clicked ring :\t" + clickRing 
			+ "\n\tVec from ring ctr to click :\t" +clickVec.toStrBrf() + " | mseClkDisp :\t"+ mseClkDisp.toStrBrf());// " disp to ctr of sphere " + myPoint._add(ctr,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar)).toStrBrf() );//" hasFocusVec : " + hasFocusVec.toStrBrf());// + " radar bar end pt : " +radarBarEndPt.toStrBrf()) ; 
//			myVector curMseLookVec = pa.getMse2DtoMse3DinWorld(pa.sceneCtrVals[pa.sceneIDX]);
//			myVector noteCtr = new myVector(ctrOfNotes);
			for(int i=0;i<3;++i){		//get rid of these once we have values being set via UI input
				boolean hit = cntls[i].hitMeMini(new myVector(clickVec));
				if(hit){clickOnCntl = i; return true; }
			}
			clickOnCntl = -1;
			//check control, set clickOnCntl  to be idx of control clicked on
		}
		return mod;		
	}

	
	public boolean hndlMouseDragIndiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseClickRayDir, myVector mseDragInWorld) {
		boolean mod = false;
		if(-1 != clickOnCntl){//modify mini control with drag either + or 
			myVector // clickVec =  getClickVec(mouseX, mouseY), oldVec = getClickVec(pmouseX, pmouseY), 
					dispVec = new myVector(mouseX-pmouseX, mouseY-pmouseY, 0);
			//win.getMsgObj().dispInfoMessage("mySphereCntl","hndlMouseDragIndiv","Mod obj : " + clickOnCntl + " in sphere: "+ID + " by dispVec : " + dispVec.toStrBrf());// + " mseClickRayDir : " + mseDragInWorld.toStrBrf());
			//cntls[clickOnCntl].modValAmt((clickOnCntl<2?-.5f:1)* modAmt, .75f*MyMathUtils.QUARTER_PI_F + (clickOnCntl/2)*.25f*MyMathUtils.QUARTER_PI_F);
			cntls[clickOnCntl].modValAmt((float)dispVec.x *.005f, (float)dispVec.y * -.005f);
			mod = true;
		}			
		return mod;
	}
	
	public void hndlMouseRelIndiv() {
		win.getMsgObj().dispInfoMessage("mySphereCntl","hndlMouseRelIndiv","release in sphere : " + ID);
		clickOnCntl = -1;
	}
	protected myPoint getClickLoc(int mouseX, int mouseY){
		float curDepth = 0.5779974f;// need to force this here so that swapping windows with sequencer doesn't screw up pick depth.  
		return pa.getWorldLoc(mouseX, mouseY, curDepth);
	}
	protected myVector getClickVec(int mouseX, int mouseY){
		//float curDepth = 0.5779974f;// need to force this here so that swapping windows with sequencer doesn't screw up pick depth.  
		myPoint clickLoc = getClickLoc(mouseX, mouseY),
				clickFromCtr = myPoint._sub(clickLoc,mseClkDisp);
		myVector clickVec = new myVector(ctrOfNotes,clickFromCtr);
		int clickRing = getClickRing(clickVec);							//ring clicked in - corresponds to note
		win.getMsgObj().dispInfoMessage("mySphereCntl","getClickVec","sphere ID : " + ID + " sphere ui getMouseLoc3D : " + mouseX + ","+mouseY  +  " \tPick Res (clickLoc) : " + clickLoc.toStrBrf()// + " cur depth : " + curDepth
		+ "\n\tRing disp vec (ntRngDispVec) : " + ntRngDispVec.toStrBrf() + " Click ring : " + clickRing
		+ "\n\tClick from ctr : " + clickFromCtr.toStrBrf()+ "  drawCtr : "+ mySphereWindow.fcsCtr.toStrBrf() 
		+ "\n\tVec from ring ctr to click :\t" +clickVec.toStrBrf() + " | mseClkDisp :\t"+ mseClkDisp.toStrBrf()		
		+"\n");// " disp to ctr of sphere " + myPoint._add(ctr,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar)).toStrBrf() );//" hasFocusVec : " + hasFocusVec.toStrBrf());// + " radar bar end pt : " +radarBarEndPt.toStrBrf()) ; 
		return new myVector(ctrOfNotes,clickFromCtr);
	}
	
	//called by traj drawing routine
	public myPoint getMouseLoc3D(int mouseX, int mouseY) {
		myVector clickVec =  getClickVec(mouseX, mouseY);
		//int clickRing = (int)(clickVec._mag()/(ringRad*.5f));	
		int clickRing = getClickRing(clickVec);		
		if((clickRing >=  numNoteRings) || (clickRing <= 0)) {	return null;}
		return clickVec;	
	}

	//single entrypoint to notes and staff.notesGlblLoc structs
	public myNote putNoteInAra(int stTime, myNote note){
		myNote tmp = notes.put(stTime, note);				//if collision at this time
		int totClicks = getTickFromAlpha(MyMathUtils.TWO_PI_F);	//total ticks in a circle
		int endTime = (stTime + note.sphereDur)%totClicks;
		ArrayList<myNote> tmpAra = noteEndLocs.get(endTime);
		if(tmpAra==null){			
			tmpAra = new ArrayList<myNote>();
		}
		tmpAra.add(note);
		noteEndLocs.put(endTime, tmpAra);
		//staff.notesGlblLoc.put(note.n.stTime, note);			
		return tmp;
	}	
	//add note at note's alpha
	public void addSphereNote(myNote note, int noteAddTime){
		//p.outStr2Scr("Add sphere Note : " +note.ID+":\t" +note.n.nameOct + "\ttime/dur : "+ note.n.stTime +"/"+note.n.dur +" add Time : " + noteAddTime + "|Meas : seqNum : "+ m.seqNum + " stTime : "+ m.stTime + " endTime : " + m.endTime );
		if(note.flags[myNote.isChord]) { addSphereChord((myChord)note, noteAddTime);}//note : do not attempt to add a chord of rests
			//if same time as another note, make a chord
		myNote tmp = putNoteInAra(noteAddTime, note); 
		if(tmp != null){
			//p.outStr2Scr(" Add Note tmp != null : "+tmp.toString());
			if(!tmp.flags[myNote.isChord]){						//no chord here, make new chord, put in treemap here
				//p.outStr2Scr("Add Note tmp ! chord: Tmp : " +tmp.toString()+":\tNote :" +note.toString());
				if(!tmp.equals(note)){				//if tmp!=note then make tmp a chord, and add note to temp
					//CAProject5 _p, float _alphaSt, float _alphaEnd, int _ring, mySphereCntl _sphrOwn
					myChord tmpChord = new myChord(win, tmp.sphereDims[0], tmp.sphereDims[1], tmp.sphereRing, tmp.sphrOwn);					
					tmpChord.addNote(note);
					putNoteInAra(noteAddTime, tmpChord);
				}
			} else {											//add note to existing chord if different 
				//p.outStr2Scr("Add Note tmp is chord: Tmp : " +tmp.toString()+":\tNote :" +note.toString());
				((myChord)tmp).addNote(note);
				putNoteInAra(noteAddTime, tmp);					//put chord back in notes map
			}				 
		}
	}//addNote
	
	private void addSphereChord(myChord note, int noteAddTime){
		//p.outStr2Scr("****Add Chord : " +note.ID+":\t" +note.n.nameOct + "\ttime/dur : "+ note.n.stTime +"/"+note.n.dur +" add Time : " + noteAddTime + "|Meas : seqNum : "+ m.seqNum + " stTime : "+ m.stTime + " endTime : " + m.endTime );
		//if same time as another note or chord, add note's notes to make a (bigger) chord
		myNote tmp = putNoteInAra(noteAddTime, note);
		if(tmp != null){
			note.addNote(tmp);
			if(tmp.flags[myNote.isChord]){										//tmp is a chord, add tmps notes to note's chord
				tmp.flags[myNote.isChord] = false;
				myChord tmpC = ((myChord)tmp);
				//for(int i = 1; i<tmpC.cnotes.size(); ++i){note.addNote(tmpC.cnotes.get(i));}
				for(Entry<String, myNote> cNoteEntry : tmpC.cnotes.entrySet()){note.addNote(cNoteEntry.getValue());}
			}
		}		
	}//addChord

	public void addDrawnNotesToStruct(boolean clearNotes, ArrayList<myNote> drawnNotes){
		if(clearNotes){clearNotes();}
		for(int i =0; i<drawnNotes.size();++i){	myNote n = drawnNotes.get(i); addSphereNote(n, getTickFromAlpha(n.sphereAlpha));}
	}	
	
	private final static float tickScale = 4 * durType.Whole.getVal() * 1.0f/(MyMathUtils.DEG_TO_RAD_F * 360.0f);
	//return integer key for notes for map - degrees? 
	//public int getTickFromAlpha(float alpha){ return (int)(alpha * (PConstants.RAD_TO_DEG/360.0f) * 4 * durType.Whole.getVal());}		//convert from alpha to ticks - one whole circle is 4 whole notes
	public int getTickFromAlpha(float alpha){ return (int)(alpha * tickScale);}		//convert from alpha to ticks - one whole circle is 4 whole notes
	//return the note corresponding to the passed point
	public myNote getNoteFromSphereLoc(myPoint pt){
		myVector ptDirCtrVec = new myVector(pt);
		double clickDist = ptDirCtrVec.magn;
		int clickRing = getClickRing(ptDirCtrVec);		
		ptDirCtrVec._normalize();
		//int clickRing = (int)(clickDist/(ringRad*.5f));						//ring clicked in - corresponds to note
		if(clickDist > .5*(ringRad * (numNoteRings+1))){
			win.getMsgObj().dispInfoMessage("mySphereCntl","getNoteFromSphereLoc","Bad Note value - outside ring bounds : " + clickRing + " loc : " + pt.toStrBrf());
			return null;
		}	
		float alphaSt = (float)myVector._angleBetween_Xprod(noteAlphaSt, ptDirCtrVec); 	//measure angles from straight up from center of sphere
		//win.getMsgObj().dispInfoMessage("mySphereCntl","getNoteFromSphereLoc","sphere ID : " + ID + " pt's ring loc :\t" + clickRing + "\tPoint in Traj :\t" +pt.toStrBrf() +"\n");// " disp to ctr of sphere " + myPoint._add(ctr,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar)).toStrBrf() ); 
		myNote res = new myNote(win, alphaSt, (alphaSt + noteAlphaWidth), clickRing, this);		
		//win.getMsgObj().dispInfoMessage("mySphereCntl","getNoteFromSphereLoc","New sphere note : " + res.toString());
		return res;
	}
	
	//return the drum sample "note" corresponding to the passed point
	public myNote getDrumNoteFromSphereLoc(myPoint pt){
		myVector ptDirCtrVec = new myVector(pt);
		double clickDist = ptDirCtrVec.magn;
		int noteSphereRing = getClickRing(ptDirCtrVec);		
		ptDirCtrVec._normalize();
		//int noteSphereRing = (int)(clickDist/(ringRad*.5f));
		int clickRing = numNoteRings -(4*((numNoteRings-noteSphereRing)/4));						//ring clicked in - corresponds to note
		//win.getMsgObj().dispInfoMessage("mySphereCntl","getDrumNoteFromSphereLoc","click ring in getDrum note : " + clickRing + "  original noteSphrRing : " + noteSphereRing );
		if(clickDist > .5*(ringRad * (numNoteRings+1))){
			win.getMsgObj().dispInfoMessage("mySphereCntl","getDrumNoteFromSphereLoc","Bad Note value - outside ring bounds : " + clickRing + " loc : " + pt.toStrBrf());
			return null;
		}	
		float alphaSt = (float)myVector._angleBetween_Xprod(noteAlphaSt, ptDirCtrVec); //measure angles from straight up from center of sphere
		//win.getMsgObj().dispInfoMessage("mySphereCntl","getDrumNoteFromSphereLoc","getNoteFromSphereLoc sphere ID : " + ID + " pt's ring loc :\t" + clickRing + "\tPoint in Traj :\t" +pt.toStrBrf() +"\n");// " disp to ctr of sphere " + myPoint._add(ctr,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar)).toStrBrf() ); 
		myNote res = new myNote(win, alphaSt, (alphaSt + noteAlphaWidth), clickRing, this);		
		//win.getMsgObj().dispInfoMessage("mySphereCntl","getDrumNoteFromSphereLoc","New sphere note : " + res.toString());
		return res;
	}
			
	private void convTrajToDrumBeats(myDrawnSmplTraj drawnNoteTraj){
		myPoint[] pts = ((myVariStroke)drawnNoteTraj.drawnTraj).getDrawnPtAra(false);
		ArrayList<myNote> tmpDrawnSphereNotes = new ArrayList<myNote>();										//new trajectory of notes to play
		myNote newDrumNote,lastDrumNote = null;
		boolean checkedFirstNote = false;		
		for(int i=0; i< pts.length;++i){
			newDrumNote = getDrumNoteFromSphereLoc(pts[i]);
			if (newDrumNote == null){continue;}
			if(!checkedFirstNote){					//first note of trajectory
				checkedFirstNote = true;
				tmpDrawnSphereNotes.add(newDrumNote);
				lastDrumNote = newDrumNote;
			} else {		//check to make note of longer duration - if same note and new note extends past old note, then add duration to old note
				if((newDrumNote.sphereRing == lastDrumNote.sphereRing) && (newDrumNote.sphereDims[1] > lastDrumNote.sphereDims[1])){	//new note lasts longer than old note, add difference
					lastDrumNote.addDurationSphere(newDrumNote.sphereDims[1] - lastDrumNote.sphereDims[1]);						
				} else if (newDrumNote.sphereRing != lastDrumNote.sphereRing){
					lastDrumNote.setSphereDims(lastDrumNote.sphereDims[0], newDrumNote.sphereAlpha, lastDrumNote.sphereRing);
					tmpDrawnSphereNotes.add(newDrumNote);					
					lastDrumNote = newDrumNote;
				} else {//equal - just ignore
				}
			}			
		}//for each point
		addDrawnNotesToStruct(win.clearStaffTrajOnDraw(),tmpDrawnSphereNotes);
	}
	private void convTrajToNotes(myDrawnSmplTraj drawnNoteTraj){
		myPoint[] pts = ((myVariStroke)drawnNoteTraj.drawnTraj).getDrawnPtAra(false);
		//TreeMap<Integer,myNote> tmpdrawnStaffNotes = new TreeMap<Integer,myNote>();		
		ArrayList<myNote> tmpDrawnSphereNotes = new ArrayList<myNote>();										//new trajectory of notes to play
		myNote newClickNote,lastNewNote = null;
		boolean checkedFirstNote = false;		
		
		for(int i=0; i< pts.length;++i){
			newClickNote = getNoteFromSphereLoc(pts[i]);
			if (newClickNote == null){continue;}
			if(!checkedFirstNote){					//first note of trajectory
				checkedFirstNote = true;
				tmpDrawnSphereNotes.add(newClickNote);
				lastNewNote = newClickNote;
			} else {		//check to make note of longer duration - if same note and new note extends past old note, then add duration to old note
				if((newClickNote.sphereRing == lastNewNote.sphereRing) && (newClickNote.sphereDims[1] > lastNewNote.sphereDims[1])){	//new note lasts longer than old note, add difference
					lastNewNote.addDurationSphere(newClickNote.sphereDims[1] - lastNewNote.sphereDims[1]);						
				} else if (newClickNote.sphereRing != lastNewNote.sphereRing){
					lastNewNote.setSphereDims(lastNewNote.sphereDims[0], newClickNote.sphereAlpha, lastNewNote.sphereRing);
					tmpDrawnSphereNotes.add(newClickNote);					
					lastNewNote = newClickNote;
				} else {//equal - just ignore
				}
			}			
		}//for each point
		addDrawnNotesToStruct(win.clearStaffTrajOnDraw(),tmpDrawnSphereNotes);
	}//convTrajToNotes	
	
	//convert points in drawnNoteTraj to notes : convert traj notes to actual notes on sphere 
	public void processTrajIndiv(myDrawnSmplTraj drawnNoteTraj){	
		if(getFlag(isDrumKitIDX)){	convTrajToDrumBeats(drawnNoteTraj);	} 
		else {				convTrajToNotes(drawnNoteTraj);}
	}
	
	//tick is in seconds
	private float totTick = 0.0f;
	public void calcDispVals(float tick, float focusZoom){
		totTick += tick;
		//win.getMsgObj().dispInfoMessage("mySphereCntl","calcDispVals","tot tick : " + totTick);
		//setCtr(initThet + (pa.sin(totTick*1000.0f/orbitRad) * 100.0f/orbitRad));//oscillates 
		setCtr(initThet + (totTick * 100.0f/orbitRad), (float) (.1f * orbitRad * Math.sin(2.0f * cntls[speedIDX].vals[myMiniSphrCntl.valIDX]*totTick + orbitRad )));			//modify to use tempo in place of totTickMult
		
		lastPlayAlpha = curSwpRotAmt;
		curSwpRotAmt += tick * cntls[speedIDX].vals[myMiniSphrCntl.valIDX];		//value of speed of sweep
		curSwpRotAmt = curSwpRotAmt % MyMathUtils.TWO_PI_F;
		//win.getMsgObj().dispInfoMessage("mySphereCntl","calcDispVals","Cur swp rot amt : " + curSwpRotAmt);
		if(lastPlayAlpha > curSwpRotAmt){
			lastPlayAlpha -= MyMathUtils.TWO_PI_F;
		}
		
		hasFocusVec = new myVector(ctr,mySphereWindow.fcsCtr);							//	 unit vector toward eye from non-displaced ctr	
		ntRngDispVec = myVector._mult(cntlRotAxis, 1.1f*radius );
		rollAmt += tick * ballSpeedMult;		
		if(focusZoom == 0){//not zooming any more - either decay or never had focus
			radarBarEndPt.set(0,-50,0);			//sweeping radar bar showing notes that are playing
			curFcsZoom *= fcsDecay;	
			if(curFcsZoom < .000001f){
				curFcsZoom = 0;
				myVector tmp = myVector._unit(myVector._unit(hasFocusVec),.5f,cntlRotAxis);					//vector 1/2 way between these two unit vectors
				ntRngDispVec = myVector._mult(tmp, 1.1f*radius );		
				drawCtr.set(ctr);
			} else {
				drawCtr = myPoint._add(ctr, hasFocusVec._mult(curFcsZoom));	
			}
		} 
		else {
			curFcsZoom = focusZoom;
			radarBarEndPt.set(0,-(50+(curFcsZoom * (radarBarLen-50))),0);			//sweeping radar bar showing notes that are playing
			drawCtr = myPoint._add(ctr, hasFocusVec._mult(curFcsZoom));				
		}
//		radarBarEndPt = new myVector(0,-(50+(focusZoom * (radarBarLen-50))),0);			//sweeping radar bar showing notes that are playing
		//drawCtr = myPoint._add(ctr, hasFocusVec._mult(curFcsZoom));	
		//mseClkDisp = myPoint._add(myPoint._add(ntRngDispVec,drawCtr), pa.sceneCtrVals[pa.sceneIDX]);
		mseClkDisp = myPoint._add(myPoint._add(ntRngDispVec,drawCtr), win.getScreenLocation());
		//mseClkDisp = myPoint._add(ntRngDispVec,myPoint._add(pa.sceneCtrVals[pa.sceneIDX],pa.focusTar));
	}

	public void sendNotesToPlayAndStop(){	
		float durMod = noteAlphaWidth/ (3.0f*cntls[speedIDX].vals[myMiniSphrCntl.valIDX]);//durMod = getTickFromAlpha(noteAlphaWidth) / 1.1f*cntls[speedIDX].value;
		//get all notes from last play alpha
		int fromKey = getTickFromAlpha(lastPlayAlpha), toKey = getTickFromAlpha(curSwpRotAmt);
		SortedMap<Integer,myNote> subMapPlay = notes.subMap(fromKey, toKey);
		SortedMap<Integer,ArrayList<myNote>> subMapStop = noteEndLocs.subMap(fromKey, toKey);
		//if((subMapPlay == null) || (subMapPlay.size() <= 0)){return;}
		int stTime = 0;
		if(!((subMapPlay == null) || (subMapPlay.size() <= 0))){
			//win.getMsgObj().dispInfoMessage("mySphereCntl","sendNotesToPlayAndStop","ID : " + ID + " play Submap has notes : Cur swp rot amt : " + curSwpRotAmt + " last play alpha : " + lastPlayAlpha + " from key, to key : " + fromKey + ","+toKey);
			win.addSphereNoteToPlayNow(instr,subMapPlay, durMod,stTime);
		}
		if(!((subMapStop == null) || (subMapStop.size() <= 0))){
//			/win.getMsgObj().dispInfoMessage("mySphereCntl","sendNotesToPlayAndStop","ID : " + ID + " stop Submap has notes : Cur swp rot amt : " + curSwpRotAmt + " last play alpha : " + lastPlayAlpha + " from key, to key : " + fromKey + ","+toKey);
			win.addSphereNoteToStopNow(instr,subMapStop);
		}
	}
//	//stop all  notes that end in this period
//	public void sendNotesToStop(){
//		int fromKey = getTickFromAlpha(lastPlayAlpha), toKey = getTickFromAlpha(curSwpRotAmt);		
//		SortedMap<Integer,ArrayList<myNote>> subMap = noteEndLocs.subMap(fromKey, toKey);
//		if((subMap == null) || (subMap.size() <= 0)){return;}
//		win.getMsgObj().dispInfoMessage("mySphereCntl","sendNotesToStop","ID : " + ID + " stop Submap has notes : Cur swp rot amt : " + curSwpRotAmt + " last play alpha : " + lastPlayAlpha + " from key, to key : " + fromKey + ","+toKey);
//		win.addSphereNoteToStopNow(instr,subMap);
//	}
	

	//animate and draw this instrument in sphere UI
//	public void drawMe(float tick, float focusZoom){
//		calcDispVals(tick,focusZoom);
//		pa.pushMatState();
//			pa.translate(drawCtr);		//either ctr or displaced position
//			drawMainSphere();
//			pa.pushMatState();
//				pa.translate(0,(isFocus ? 1.5f : 0)*radius,(isFocus ? 0 : 1.5f)*radius);
//				drawName();
//			pa.popMatState();	
//			if(isFocus){drawNoteCircle();}
//			drawAllNotes();
//			drawNotePlayBar();
//			for(int i=0;i<3;++i){
//				cntls[i].drawMini(isFocus);
//			}
//
//		pa.popMatState();	
//	}
	public void drawMe(float tick, float focusZoom, boolean hasFocus){
		calcDispVals(tick,focusZoom);
		pa.pushMatState();
			pa.translate(drawCtr);		//either ctr or displaced position
			drawMainSphere();
			if(hasFocus){drawMeFocus(tick,focusZoom);} else {drawMeNoFocus(tick,focusZoom);}
		pa.popMatState();	
	}
	//draw for focus
	public void drawMeFocus (float tick, float focusZoom){
		pa.pushMatState();
			pa.translate(0,1.5f *radius,0);
			drawName();
		pa.popMatState();	
		drawNoteCircle();
		drawAllNotes();
		drawNotePlayBar();
		for(int i=0;i<3;++i){				cntls[i].drawMiniLocked(pa);			}
	}//
	
	public void drawMeNoFocus(float tick, float focusZoom){
		pa.pushMatState();
			pa.translate(0,0, radius);
			drawName();
		pa.popMatState();	
		drawAllNotes();
		drawNotePlayBar();
		for(int i=0;i<3;++i){				cntls[i].drawMini(pa);			}
	}//drawMeNoFocus
	public void stopAllNotes(){
		win.addSphereNoteToStopNow(instr,noteEndLocs);
	}
	
	public void drawAllNotes(){
		//draw all notes
		pa.pushMatState();
			pa.translate(ntRngDispVec.x,ntRngDispVec.y,ntRngDispVec.z+.01f);			
			for(Map.Entry<Integer,myNote> noteVal : notes.entrySet()){
				myNote note = noteVal.getValue();
				note.drawMeSphere(pa);
			}
		pa.popMatState();	
	}
	
	public void drawTrajPts(myDrawnSmplTraj traj, float animTimeMod){
		pa.pushMatState();
			pa.translate(drawCtr);		//either ctr or displaced position		
			pa.translate(ntRngDispVec.x,ntRngDispVec.y,ntRngDispVec.z-1.0f);
			traj.drawMe(pa, animTimeMod);
		pa.popMatState();	
	}
	
	//draw concentric rings of notes "staff"
	public void drawNoteCircle(){
		pa.pushMatState();
		pa.setFill(255,255,255,5);
		pa.setStroke(0,0,0,255);
		pa.setStrokeWt(1);
		pa.translate(ntRngDispVec);
		//note rings - fewer & wider for drums
		int ringStep = getFlag(isDrumKitIDX) ? 4 : 1;
		for(int i =0; i<numNoteRings; i+=ringStep){	pa.drawEllipse2D(myPoint.ZEROPT, (i+1)*ringRad);	}		
		pa.popMatState();			
	}//drawNoteCircle()
	
	public void drawNotePlayBar(){
		pa.pushMatState();
		pa.setFill(255,255,255,5);
		pa.translate(ntRngDispVec.x,ntRngDispVec.y,ntRngDispVec.z-.1f);
		pa.rotate(lastPlayAlpha, cntlRotAxis);
		//pa.rotate(curSwpRotAmt, cntlRotAxis);
		pa.setStroke(0,0,0,255);
		pa.setStrokeWt(3);
		pa.drawLine(myPoint.ZEROPT, radarBarEndPt);
		pa.setStroke(255,255,0,255);
		pa.setStrokeWt(1.5f);
		pa.drawLine(myPoint.ZEROPT, radarBarEndPt);
		pa.setStroke(255,0,0,255);
		pa.setStrokeWt(1);
		pa.drawLine(myPoint.ZEROPT, radarBarEndPt);
		pa.translate(myPoint._mult(radarBarEndPt, 1.1));
		pa.rotate(-lastPlayAlpha, cntlRotAxis);
		//pa.rotate(-curSwpRotAmt, cntlRotAxis);
		pa.setColorValFill(IRenderInterface.gui_Black, 255);
		
		//pa.showText(""+curSwpRotAmt, 0, -0);
		pa.popMatState();			
	}	
		
	private void drawName(){
		pa.setColorValFill(IRenderInterface.gui_DarkGray, 255);
		pa.translate(-2*name.length(),0,0);
		pa.drawRect(-10,-11,name.length()*8 + 10,15);
		pa.setColorValFill(IRenderInterface.gui_White, 255);
		pa.showText(name, 0, 0);	
	}
	
	private void drawMainSphere(){
		pa.pushMatState();
		//pa.setStroke(255,0,0,255);
		//pa.drawLine(myPoint.ZEROPT, pa.P(panAxis)._mult(100));		//axis of rot lines
		pa.rotate(cntls[panIDX].vals[myMiniSphrCntl.rotAmtIDX],panAxis);
		//pa.setStroke(111,0,110,255);
		//pa.drawLine(myPoint.ZEROPT, pa.P(rotAxis)._mult(100));		//axis of rot lines
		pa.rotate(-rollAmt, rotAxis);
		win.drawShape(mySphere);	
		pa.popMatState();			
	}
//	//return distance along pt + unit ray where this ray hits target note disc
	public double hitNoteDisc(myPoint pt, myVector ray){
		myVector tmp = new myVector(pt,ctrOfNotes); 
		return (tmp._dot(cntlRotAxis))/(ray._dot(cntlRotAxis));	
	}
	
	public double hitMe(myPoint pt, myVector ray){return hitMe(pt,ray, ctr, invRadius);	}	 
	//returns value >= 0  if the passed point in the direction of the ray hits this sphere
	public double hitMe(myPoint pt, myVector ray, myPoint tarCtr, float invRad){
		double tVal = -1;
	    myVector pC = new myVector(tarCtr, pt)._mult(invRad), scRay = myVector._mult(ray, invRad);
	    double a = (scRay._SqMag()),
	    		b = (2*scRay._dot(pC)), 
	    		c = (pC._SqMag() - 1),
	    		discr = ((b*b) - (4*a*c));
	    if (!(discr < 0)){
	    	//find values of t - want those that give largest value of z, to indicate the closest intersection to the eye
	    	//quadratic equation
	    	double discr1 = Math.pow(discr,.5), t1 = (-b + discr1)/(2*a), t2 = (-b - discr1)/(2*a);
	    	tVal = Math.min(t1,t2);
	        if (tVal < MyMathUtils.EPS_F){//if min less than 0 then that means it intersects behind the viewer.  pick other t
	        	tVal = Math.max(t1,t2);
	        	if (tVal < MyMathUtils.EPS_F){tVal = -1;}//if both t's are less than 0 then don't paint anything
	        }//if the min t val is less than 0
	    }		
		return tVal;		
	}//hitme
	
	public String toString(){
		String res = "ID:"+ID+" Instr : " + instr.instrName + " ctr : " + ctr.toStrBrf();
		return res;		
	}
	
}//class mySphereCntl