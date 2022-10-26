package SphrSeqFFTVisPKG.note;

import java.util.ArrayList;

import SphrSeqFFTVisPKG.clef.base.myClefBase;
import SphrSeqFFTVisPKG.measure.myMeasure;
import SphrSeqFFTVisPKG.note.enums.durType;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.staff.myKeySig;
import SphrSeqFFTVisPKG.staff.myStaff;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import SphrSeqFFTVisPKG.ui.controls.mySphereCntl;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myVector;

/**
 * fundamental class to hold and process a single note played by a single instrument
 * @author 7strb
 *
 */
public class myNote {
	public myMusicSimWindow win;
	public static int nCnt = 0;
	public int ID;		
	public float noteC4DispLoc;		//displacement for this note from C4 for display purposes, governed by owning staff
	public myMeasure meas;		//owning measure
	public NoteData n;			//all specific note data
	public int tupleVal;		//# of notes in space of 2, if tuple (valid are only >= 3)
	public boolean[] flags;
	public static final int isDotted = 0,
							isTuple = 1,
							isRest = 2,
							isChord = 3,
							drawStemUp = 4,			//whether note stem should be drawn up or down ( based on location in staff) - stem should be down only above middle ledger line, or if first note in group is down
							drawCnncted = 5,		//should be connected to neighbor note (flags for 8ths, etc
							showDispMsg = 6,		//show that this note should be played higher/lower than written
							isInStaff = 7,			//note is in range of staff - if false, draw appropriate ledger line through or under note
							isFlipped = 8,			//if note is within 1 note of another chord note that is not flipped, flip this note (put head on other side of stem)
							isOnLdgrLine = 9,		//ledger line goes through note center
							isFromSphere = 10;		//note is made from Sphere UI
	public static final int numFlags = 11;
	public myStaff owningStaff;
	public static float noteWidth;// = staff.getlOff() * 2;			//width of note to display
	//where this note should live on the staff, from lowest ledger line of treble staff as baseline (E4), and then offset based on cleff defined in staff
	public float dispYVal, staffSize;						//translation modifier for note display from top of staff; staffSize to minimize recalc
	public int transMod;						
	public String dispMsg;						//what message to show, if any, above staff for 1 or 2 octave displacement up or down
	
	public float[] gridDims;
	
	//sphere UI values
	public mySphereCntl sphrOwn;
	public float[] sphereDims;
	public float sphereAlpha;
	public int sphereDur;
	public int sphereRing;	
	
	//build note then set duration
	public myNote(myMusicSimWindow _win, noteValType _name, int _octave, myMeasure _measure, myStaff _owningStaff) {
		win=_win;
		ID = nCnt++;	
		meas = _measure;
		owningStaff = _owningStaff;
		sphrOwn = null;
		noteWidth = owningStaff.getlOff() * win.ntWdthMult;		//modify based on duration ? TODO
		n = new NoteData(_name, _octave);
		tupleVal = -1;
		gridDims = new float[]{0,0,0,0};
		sphereDims = new float[]{0,0,0,0};
		sphereAlpha = 0;
		sphereDur = 0;
		sphereRing = 0;
		
		flags = new boolean[numFlags];
		initFlags();
		if (n.name == noteValType.rest){	setRest();}
		setDispMsgVals(owningStaff.getC4DistForClefsAtTime(n.stTime));
		//not every note has an owning measure, but every note has an owning staff
		flags[isInStaff] = (owningStaff.getClefsAtTime(n.stTime).isOnStaff(n) == 0);
	}	
	//ctor for note data for notes in spherical UI
	public myNote(myMusicSimWindow _win, float _alphaSt, float _alphaEnd, int _ring, mySphereCntl _sphrOwn){
		win=_win;
		ID = nCnt++;	
		meas = null;	
		sphrOwn = _sphrOwn;
		setSphereDims(_alphaSt,  _alphaEnd,  _ring);		
		flags = new boolean[numFlags];
		initFlags();
		flags[isFromSphere] = true;
	}
	
	public myNote(myNote _note){
		win=_note.win;
		ID = nCnt++;	
		meas = _note.meas;
		owningStaff = _note.owningStaff;
		sphrOwn = _note.sphrOwn;
		noteWidth = owningStaff.getlOff() * win.ntWdthMult;		
		n = new NoteData(_note.n);
		tupleVal = _note.tupleVal;
		gridDims = new float[]{0,0,0,0};
		sphereDims = new float[]{0,0,0,0};
		for(int i =0; i<_note.gridDims.length;++i){
			gridDims[i]=_note.gridDims[i];
			sphereDims[i]=_note.sphereDims[i];
		}
		sphereAlpha = _note.sphereAlpha;
		sphereDur = _note.sphereDur;
		sphereRing = _note.sphereRing;
		flags = new boolean[numFlags];
		for(int i =0; i<numFlags;++i){	flags[i]=_note.flags[i];}
		if (n.name == noteValType.rest){	setRest();}
		setDispMsgVals(owningStaff.getC4DistForClefsAtTime(_note.n.stTime));
		flags[isInStaff] = (owningStaff.getClefsAtTime(_note.n.stTime).isOnStaff(n) == 0);
	}	
	//sphere dims are alphaSt, alphaEnd, ring, thickness
	public void setSphereDims(float _alphaSt, float _alphaEnd, int _ring){
		sphereAlpha = _alphaSt;
		if(sphereAlpha < 0){ sphereAlpha = MyMathUtils.TWO_PI_F + sphereAlpha;}		//add 2 pi if negative
		float tmpAlphaEnd = _alphaEnd;
		if(tmpAlphaEnd < 0){ tmpAlphaEnd = MyMathUtils.TWO_PI_F + tmpAlphaEnd;}		//add 2 pi if negative
		//if(tmpAlphaEnd < sphereAlpha){float tmp = sphereAlpha; sphereAlpha = tmpAlphaEnd; tmpAlphaEnd=tmp;}			
		if(tmpAlphaEnd < sphereAlpha){tmpAlphaEnd = MyMathUtils.TWO_PI_F + tmpAlphaEnd;}
		sphereDims = new float[]{sphereAlpha,tmpAlphaEnd,(_ring + .5f)*sphrOwn.ringRad,sphrOwn.ringRad};
		sphereRing = _ring;	
		sphereDur = sphrOwn.getTickFromAlpha(sphereDims[1] - sphereDims[0]);
		buildSphereNoteName();
	}
	//build the note data given the loaded sphere dims
	public void buildSphereNoteName(){
		if(sphrOwn == null){ win.getMsgObj().dispErrorMessage("myNote","buildSphereNoteName","Error buildSphereNoteName : sphrOwn is null"); return;}
		myClefBase clef = sphrOwn.instr.clef;
		n = new NoteData(clef.getSphereMidNote());		//use the middle note of the clef as the starting point, then assign that note data value to the middle of the note rings, find disp of actual ring from middle, and displace note data accordingly
		int numRings = sphrOwn.numNoteRings,
			numNotesDisp = -(numRings/2) + sphereRing;	
		if(numNotesDisp >= 12){numNotesDisp -= 12; n.editNoteVal(n.name, n.octave+1);}
		if(numNotesDisp < 0){
			n.editNoteVal(n.name, n.octave-1);
			if(numNotesDisp < -12){numNotesDisp += 12; n.editNoteVal(n.name, n.octave-1);}
		}
		int[] indNDisp = win.getNoteDisp(n, numNotesDisp);		
		this.n.editNoteVal(noteValType.getVal(indNDisp[0]), indNDisp[1]);
		//p.outStr2Scr("new note : "+ n.toString() + " #rings : "+sphereRing + " notes disp : " +numNotesDisp);
	}
	
	public float getSphereNoteDur(){return (sphereDims[1]-sphereDims[0]);	}
	
	//must be called after setFlags, set display message and displacement vals
	public void setDispMsgVals(float c4DspLc){//limit octave to 8
		//p.outStr2Scr("setDispMsgVals : " + this.ID + "  c4DspLc : " + c4DspLc);
		if(flags[isRest]){flags[showDispMsg] = false;dispMsg = "";transMod = 0;return;}
		switch (n.octave){
			case 0 : {flags[showDispMsg] = true; dispMsg = "15mb"; transMod = 2;break;}
			case 1 : {flags[showDispMsg] = true; dispMsg = "8vb";transMod = 1;break;}
			case 7 : {flags[showDispMsg] = true; dispMsg = "8va";transMod = -1;break;}
			case 8 : {flags[showDispMsg] = true; dispMsg = "15ma";transMod = -2;break;}
			default : {flags[showDispMsg] = false; dispMsg = "";transMod = 0;break;}		
		}		
		calcDispYVal(c4DspLc);
	}
	public void moveNoteHalfStep(myKeySig _key, ArrayList<noteValType> keyAra, boolean up){moveNoteHalfStepPriv(_key,keyAra,up);}
	protected void moveNoteHalfStepPriv(myKeySig _key, ArrayList<noteValType> keyAra, boolean up){
		//p.outStr2Scr("Before move:  " + n.toString());
		if(flags[isRest]){return;}
		if(!keyAra.contains(n.name)){
			n.moveHalfStep(up);
			setDispMsgVals(owningStaff.getC4DistForClefsAtTime(n.stTime));
			flags[isInStaff] = (owningStaff.getClefsAtTime(n.stTime).isOnStaff(n) == 0);
			//p.outStr2Scr("After move:  " + n.toString());
			//need to recalc rectangle dims
			int mult = (up? -1 : 1);		
			gridDims[1] += mult * gridDims[3];			//need to resize grid dims!
	//		if(p.isNaturalNote(n.name)){
	//			if (p.chkHasSharps(n.name)){gridDims[1] += bkModY; gridDims[3] -= bkModY;}//increase y0, decrease y1 coord to make room for black key
	//			if (p.chkHasFlats(n.name) && (!lowestNote)){gridDims[3] -= bkModY;}//decrease y1 coord to make room for black key							
	//		}
		}
		//p.outStr2Scr("After move:  " + n.toString());
	}
	//add grid dimensions relating to duration-  idx 2 (length in x of added note)
	
	public void addDurGridDims(float[] _gridDims){addDurGridDims(_gridDims,1);}
	public void addDurGridDims(float[] _gridDims, int mult){gridDims[2] += mult* _gridDims[2];	}	
	
	//calculate appropriate displacement in Y for this note, based upon note value and octave
	protected void calcDispYVal(float c4DispMult){
		int tmpOctave = n.octave + transMod - 4;		//starting at c4
		float tmpDisp = tmpOctave * 7* .5f;		//# of ledger lines per octave is 7
		tmpDisp +=  getLedgerLine() * .5f;
		if(!flags[isFromSphere]){
			noteC4DispLoc = c4DispMult * owningStaff.getlOff();
			dispYVal = noteC4DispLoc - (tmpDisp*owningStaff.getlOff()) ;
			flags[isOnLdgrLine] = ((int)dispYVal % 10 == 0);		//if not on ldgr line then will have a 5 in ones' place
			staffSize = owningStaff.getlOff() * 4;
		}
		//p.outStr2Scr("Calc DispYVal : Name : " +n.name + " is on ledger line : " + flags[isOnLdgrLine] +  " isChord : "+ this.flags[isChord]+ "  DispYVal :" + String.format("%.4f", dispYVal)+ " noteC4DispLoc :" + String.format("%.4f", noteC4DispLoc)  + " : tmpOctave : "+ tmpOctave + " tmpDisp : " + tmpDisp ); 
	}
	//# of ledger lines to get to each note, from c
	protected int getLedgerLine(){		
		switch (n.name){
			case C : 
			case Cs : {return 0;}
			case D  : 
			case Ds : {return 1;}
			case E  : {return 2;}
			case F  : 
			case Fs : {return 3;}
			case G  : 
			case Gs : {return 4;}
			case A  : 
			case As : {return 5;}
			case B  : {return 6;}
		default:
			break;
		}
		return 0;
	}
	
	public void initFlags(){for(int i =0; i<numFlags;++i){	flags[i]=false;}}
	//set this note to be a rest
	public void setRest(){
		flags[isRest] = true;
		n.freq = 0;
		n.octave = 0;
	}	
	
	public void setVals(int _stTime, durType _typ, boolean _isDot, boolean _isTup, int _tplVal){setStart(_stTime);setDuration( _typ.getVal(),_isDot, _isTup, _tplVal);}
	public void setVals(int _stTime, int _dur, boolean _isDot, boolean _isTup, int _tplVal){setStart(_stTime);setDuration( _dur,_isDot, _isTup, _tplVal);}	
	//dotted noted means note duration is x 1.5; tuple is x in the space of 2 notes, so duration is 2/x where x is the tuple val (>=3)
	public void setDuration(durType _typ, boolean _isDot, boolean _isTup, int _tplVal){	setDuration( _typ.getVal(),_isDot, _isTup, _tplVal);	}	
	public void setDuration(int _dur, boolean _isDot, boolean _isTup, int _tplVal){
		if(_isDot){_dur = (int)(_dur * 1.5f); flags[isDotted] = true;} 
		else if(_isTup){ if(tupleVal < 3){return;} tupleVal = _tplVal; _dur =(int)(_dur * 2.0/(tupleVal)); flags[isTuple] = true;} 
		n.setDur(_dur);
	}
	
	public void setDurationPRL(durType _typ, int defaultVal, boolean _isDot, boolean _isTup, int _tplVal){	setDuration( _typ.getVal(),_isDot, _isTup, _tplVal);	}
	
	public void setDurationPRL(float _scrDur, int defaultVal,  boolean _isDot, boolean _isTup, int _tplVal){
		if(_isDot){_scrDur = _scrDur * 1.5f; flags[isDotted] = true;} 
		else if(_isTup){ if(tupleVal < 3){return;} tupleVal = _tplVal; _scrDur =_scrDur * 2.0f/(tupleVal); flags[isTuple] = true;} 
		n.setDurScroll(_scrDur,defaultVal);
	}
	
	public void addDurationSphere(float _scrAlpha){addDurationSphereIndiv(_scrAlpha);}
	protected void addDurationSphereIndiv(float _scrAlpha){
		sphereDims[1] += _scrAlpha;		
		sphereDur = sphrOwn.getTickFromAlpha(sphereDims[1] - sphereDims[0]);
	}
	
	public void addDurationPRL(float _scrDur, boolean _isDot, boolean _isTup, int _tplVal){
		if(_isDot){_scrDur = _scrDur * 1.5f; flags[isDotted] = true;} 
		else if(_isTup){ if(tupleVal < 3){return;} tupleVal = _tplVal; _scrDur =_scrDur * 2.0f/(tupleVal); flags[isTuple] = true;} 
		n.addDurScroll(_scrDur);
	}
	
	//override for chord
	public void modStDur(int newDur, int newSt){
		setStart(newSt);								//shift start time to end of new note (notes share start time
		n.setDur(newDur);	
	}
	
	//set time for this note from beginning of sequence
	public void setStart(int _stTime){	n.stTime = _stTime;setDispMsgVals(owningStaff.getC4DistForClefsAtTime(n.stTime));}
	//sets start time via piano roll, where pxls from edge need to be translated to edge time + pxl->click conversion
//	public void setStartPiano(float pxlsFromEdge, int edgeStartTime, float pxlsPerTick){
//		//p.outStr2Scr("setStartPiano : pxlsFromEdge : " + pxlsFromEdge + " edge start time : " + edgeStartTime + " pxlsPerTick : " +pxlsPerTick + " stTime offset : " + (int)(pxlsFromEdge/pxlsPerTick));
//		int _stTime = edgeStartTime + (int)(pxlsFromEdge/pxlsPerTick);		
//		setStart(_stTime);
//	}
	
	//take current duration and set to nearest integral duration TODO
	public void quantize(){n.quantMe();}
	
	//if notes are equal, means their st time, duration, note name and octave are all the same
	public boolean equals(myNote _n){return n.equals(_n.n);	}	
	
	//draw piano roll rectangle
	public void drawMePRL(IRenderInterface p){p.drawRect(gridDims);}
	
	public void drawMeSphere(IRenderInterface p){ drawMeSpherePriv(p);}
	//void noteArc(myPoint ctr, float alphaSt, float alphaEnd, float rad, float thickness, int[] noteClr){
	protected void drawMeSpherePriv(IRenderInterface p){//
		p.pushMatState();
		p.noteArc(sphereDims, sphrOwn.noteClr);		
		p.popMatState();
	}
	
	//draw this note
	public void drawMe(IRenderInterface p){		drawMePriv(p);	}
	protected void drawMePriv(IRenderInterface p){
		p.pushMatState();
		if(flags[isRest]){ 
			//translate to middle of measure
			p.translate(0, owningStaff.getlOff() * 2.5f);
			win.drawRest(owningStaff.getlOff(), n.typIdx, flags[isDotted]);			
		} else {	
			p.showText(dispMsg,0,0);//8va etc
			//where this note should live on the staff (assume measure has translated to appropriate x position), from C4
			p.translate(0, dispYVal);
			if(n.isSharp){//show sharp sign
				p.translate(.5f*owningStaff.getlOff(),0);
				p.pushMatState();
				p.scale(1,1.5f);
				p.showText("#",-1.5f*owningStaff.getlOff(),.5f*owningStaff.getlOff());
				p.popMatState();
			}
			win.drawNote(owningStaff.getlOff(), new myVector(0,0,0), n.typIdx,0 ,flags, (dispYVal <0 ? dispYVal : (dispYVal - staffSize > 0) ? dispYVal - staffSize : 0)/10.0f  );//TODO
		}
		p.popMatState();
	}

	public String toString(){
		String res = "Note ID : "+ID + n.toString() + " Dot : "+(flags[isDotted]?"Yes":"No");
		if(flags[isChord]){res += "| Chord : Yes";	} else {res += "| Chord : No";}		
		if(flags[isFromSphere]){//sphereDims = new float[]{_alphaSt,_alphaEnd,_ring,sphrOwn.ringRad};
			res += "|Alpha Start : " + String.format("%.5f",sphereDims[0]) + " alpha end : " + String.format("%.5f",sphereDims[1]) + " sphere dur : " + String.format("%.5f",(sphereDims[1]-sphereDims[0]))+ " sphereDur ticks : " + sphereDur+ " Ring : " + sphereRing + " Ring Dist : " +String.format("%.2f", sphereDims[2]);			
		}
		else {
			res += "|C4 : " +String.format("%.4f",noteC4DispLoc) + " dispLoc "+ String.format("%.4f",dispYVal)+ " ";
			if(flags[showDispMsg]){res += "| is displayed at a displacement from where played : Yes | Msg : "+dispMsg;	} else {res += "| is displayed at a displacement from where played : No";}		
		}
		if(flags[isTuple]){res += "| Tuple : Yes("+tupleVal+" in the space of 2)";	} else {res += "| Tuple : No";}		
		if(flags[isRest]){res += "| Rest : Yes";} else {res += "| Rest : No";}		
		return res;
	}	
}//class mynote