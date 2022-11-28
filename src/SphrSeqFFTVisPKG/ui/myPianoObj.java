package SphrSeqFFTVisPKG.ui;

import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.Base_DispWindow;
import SphrSeqFFTVisPKG.note.NoteData;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import base_Math_Objects.vectorObjs.doubles.myPoint;

public class myPianoObj{
	public static SeqVisFFTOcean pa;
	//public static mySequencerWindow win;
	public static final float whiteKeyWidth = 78;
	//dimensions of piano keys, for display and mouse-checking
	public float[][] pianoWKeyDims, pianoBKeyDims;	
	//array of note data for each piano key - played if directly clicked on
	public NoteData[] pianoWNotes, pianoBNotes, allNotes;
	//background color of window
	public int[] winFillClr;
	
	public final float wkOff_X = .72f;
	//sound analysis
	//harmonic series for piano tuning
	public float[][] pianoFreqsHarmonics, pianoMinFreqsHarmonics;
	//equal temperment harmonic series
	public float[][] eqTempFreqsHarms, eqTempMinFreqsHarms;

	//number of harmonics to track
	public int numHarms = 8;

	//holds on-screen y locations of centers of each key at edge of piano
	public float[] pianoKeyCtrYLocs;
	
	
	//location and dimension of piano keyboard in parent display window, location and size of display window
	public float[] pianoDim, winDim;
	
	//piano display
	public float //whiteKeyWidth, 
		bkModY;				//how long, in pixels, is a white key, blk key is 2/3 as long
	//window size modifiers
	public static final float gridYMult = 1.0f/67.0f, gridXMult = .5625f * gridYMult;
	
	public float keyX, keyY;										//x, y resolution of grid/keys, mod amount for black key
	public int numWhiteKeys;										//# of white keys on piano - should be 52, maybe resize if smaller?
	public static  final int numKeys = 88;
	public int numNotesWide;										//# of notes to show as grid squares
	public myPianoObj(SeqVisFFTOcean _p, float kx, float ky, float[] _pianoDim, int[] _winFillClr, float[] _winDim){
		pa = _p;
		pianoDim = new float[_pianoDim.length];
		winFillClr = new int[_winFillClr.length]; for(int i=0;i<_winFillClr.length;++i){winFillClr[i]=_winFillClr[i];}
		winDim = new float[_winDim.length];
		updateDims(kx, ky, _pianoDim, _winDim);	
	}
	//if the window or piano dimensions change, update them here
	public void updateDims(float kx, float ky, float[] _pianoDim, float[] _winDim){
		keyX = kx; keyY = ky; updatePianoDim(_pianoDim);updateWinDim(_winDim);
		bkModY = .3f * keyY;
		numWhiteKeys = 52;//PApplet.min(52,(int)(_winDim[3]/keyY));		//height of containing window will constrain keys in future maybe.
		numNotesWide = (int)((winDim[2] - pianoDim[2])/keyX);
		buildKeyDims();
	}
	//build key dimensions array for checking and displaying
	private void buildKeyDims(){
		pianoWKeyDims = new float[numWhiteKeys][];		//88 x 5, last idx is 0 if white, 1 if black
		pianoWNotes = new NoteData[numWhiteKeys];
		pianoKeyCtrYLocs = new float[numKeys];
		int numBlackKeys = numKeys - numWhiteKeys;
		allNotes = new NoteData[numKeys];
		int allNoteIDX = numKeys-1;
		pianoBKeyDims = new float[numBlackKeys][];		//88 x 5, last idx is 0 if white, 1 if black
		pianoBNotes = new NoteData[numBlackKeys];
		float bHigh = 2.0f * bkModY, bWide = .6f*whiteKeyWidth;	
		int blkKeyCnt = 0, octave = 8;
		float stY = pianoDim[1];
		for(int i =0; i < numWhiteKeys; ++i){
			pianoWKeyDims[i] = new float[]{0,stY,whiteKeyWidth,keyY};	
			int iMod = i % noteValType.wKeyVals.length;
			pianoWNotes[i] = new NoteData(pa,noteValType.wKeyVals[iMod], octave);
			if(noteValType.wKeyVals[iMod] == noteValType.C){
				octave--;
			}
			if((iMod != 4) && (iMod != 0) && (i != numWhiteKeys-1)&& (i != 0)){
				pianoBKeyDims[blkKeyCnt] = new float[]{0,stY+(keyY-bkModY),bWide,bHigh};
				pianoBNotes[blkKeyCnt] = new NoteData(pa, noteValType.bKeyVals[blkKeyCnt%noteValType.bKeyVals.length], octave);
				blkKeyCnt++;
			}
			stY +=keyY;
		}	
	}//buildKeyDims
	
	//return note clicked on if clicked on piano directly
	public myNote checkClick(Base_DispWindow win, int mouseX, int mouseY, myPoint snapClickLoc){
		myNote res = null;
		if(!pa.ptInRange(mouseX, mouseY, pianoDim[0], pianoDim[1], pianoDim[0]+pianoDim[2], pianoDim[1]+pianoDim[3])){return res;}//not in this window)
		int resIdx = -1, keyType = -1;
		double xLoc = 0, yLoc = 0;
		boolean found = false;
		//black keys
		for(int i =0; i<pianoBKeyDims.length;++i){
			if(pa.ptInRange(mouseX, mouseY, pianoBKeyDims[i][0],pianoBKeyDims[i][1],pianoBKeyDims[i][0]+pianoBKeyDims[i][2],pianoBKeyDims[i][1] + pianoBKeyDims[i][3])){
				resIdx = i;	keyType = 1;found = true; xLoc =pianoBKeyDims[i][0]+(.5f*pianoBKeyDims[i][2]); yLoc  =pianoBKeyDims[i][1]+(.5f*pianoBKeyDims[i][3]); 	break; 
			}
		}
		if(!found){//prevent double-taps with black keys
			for(int i =0; i<pianoWKeyDims.length;++i){
				if(pa.ptInRange(mouseX, mouseY, pianoWKeyDims[i][0],pianoWKeyDims[i][1],pianoWKeyDims[i][0]+pianoWKeyDims[i][2],pianoWKeyDims[i][1]+pianoWKeyDims[i][3])){
					resIdx = i;keyType = 0;found = true;xLoc =pianoWKeyDims[i][0]+(wkOff_X*pianoWKeyDims[i][2]); yLoc =pianoWKeyDims[i][1]+(.5f*pianoWKeyDims[i][3]); 	break;
				}
			}
		}
		if(resIdx != -1){
			//measure-less note to be played immediately
			NoteData tmpa = ( keyType == 0 ? pianoWNotes[resIdx] : pianoBNotes[resIdx]);
			res = new myNote(pa, tmpa.name, tmpa.octave, null, win.pa.score.staffs.get(win.getScoreStaffName(win.curTrajAraIDX)));
			snapClickLoc.set(xLoc,yLoc,0);			
		}
		//pa.outStr2Scr("Key x : " + keyClickX+ " Key y : "+keyClickY + " idx : "+resIdx+" Key Type : "+ keyType, true);
		return res;	
	}//checkClick		
	
	//given a y coordinate in mouse space (piano roll area), return the note this is, or null
	//called directly by piano roll, so no range checking necessary
	public myNote checkRollArea(Base_DispWindow win, int x, int y, float[] nrDims){
		myNote res = null;
		int resIdx = -1, keyType = -1;
		boolean found = false, isNatural = false;
		//boolean isBlkKey = false;
		for(int i =0; i<pianoBKeyDims.length;++i){
			if(pa.ptInRange(x, y, whiteKeyWidth,pianoBKeyDims[i][1],winDim[2],pianoBKeyDims[i][1] + pianoBKeyDims[i][3])){
				resIdx = i;	keyType = 1;found = true;  	nrDims[1] = pianoBKeyDims[i][1]; nrDims[2] = keyX;nrDims[3] = pianoBKeyDims[i][3];
				//isBlkKey = true;
				break; 
			}
		}
		if(!found){//prevent double-taps with black keys
			for(int i =0; i<pianoWKeyDims.length;++i){
				if(pa.ptInRange(x, y, whiteKeyWidth,pianoWKeyDims[i][1],winDim[2],pianoWKeyDims[i][1]+pianoWKeyDims[i][3])){
					resIdx = i;keyType = 0;found = true;nrDims[1] = pianoWKeyDims[i][1]; nrDims[2] = keyX;nrDims[3] = pianoWKeyDims[i][3];
					isNatural=(i != 0);	//treat all keys but c7 as naturals - this is to decrease size of drawn box
					break;
				}
			}
		}
		if(resIdx != -1){
			nrDims[0] =(((int)((x-whiteKeyWidth)/keyX)) * keyX)+whiteKeyWidth;
			//pa.outStr2Scr("checkRollArea NRDIMS 0 : " + nrDims[0] + " orig x : " + x + " | " +  keyX);
			NoteData tmpa = ( keyType == 0 ? pianoWNotes[resIdx] : pianoBNotes[resIdx]);
			res = new myNote(pa, tmpa.name, tmpa.octave, null, pa.score.staffs.get(win.getScoreStaffName(win.curTrajAraIDX)));
			//pa.outStr2Scr("Note name in checkRollArea : " + res.n.name, true );
			if(isNatural){//modify note grid dim so box doesn't overlap black keys
				if (res.n.name.chkHasSharps()){nrDims[1] += bkModY; nrDims[3] -= bkModY;}//increase y0, decrease y1 coord to make room for black key
				if (res.n.name.chkHasFlats() && (resIdx != pianoWKeyDims.length-1)){nrDims[3] -= bkModY;}//decrease y1 coord to make room for black key				
			} 
//			else if(isBlkKey){
//				if (pa.chkHasSharps(res.n.name)){nrDims[1] -= bkModY; nrDims[3] += bkModY;}//increase y0, decrease y1 coord to make room for black key
//				if (pa.chkHasFlats(res.n.name)){nrDims[3] += bkModY;}//decrease y1 coord to make room for black key								
//			}
			//pa.outStr2Scr("Note : " + res.toString() );
		} else {
		//	pa.outStr2Scr("Note is null ");
		}
		return res;	
	}//checkRollArea
	
	//get piano roll rectangle dimensions given a specific note data value
	public float[] getRectDimsFromRoll(NoteData nd, float xStOffset){
		//nrDims[0] =(((int)(x/keyX)) * keyX);
		float[] res = new float[4];
		res[0]= xStOffset;
		int resIdx = 0;;
		if(nd.name.isNaturalNote()){//check white keys
			for(int i =0; i<pianoWNotes.length;++i){
				if(nd.nameOct.equals(pianoWNotes[i].nameOct)){
					res[1] = pianoWKeyDims[i][1]; res[2] = keyX;res[3] = pianoWKeyDims[i][3]; resIdx = i;break;
				}
			}
			if (nd.name.chkHasSharps()){res[1] += bkModY; res[3] -= bkModY;}//increase y0, decrease y1 coord to make room for black key
			if (nd.name.chkHasFlats() && (resIdx != pianoWKeyDims.length-1)){res[3] -= bkModY;}//decrease y1 coord to make room for black key				

		} else {				//check black keys
			for(int i =0; i<pianoBNotes.length;++i){
				if(nd.nameOct.equals(pianoBNotes[i].nameOct)){
					res[1] = pianoBKeyDims[i][1]; res[2] = keyX;res[3] = pianoBKeyDims[i][3];resIdx = i;break;					
				}
			}	
		}
		return res;
	}//getRectDimsFromRoll
		
	public void drawMe(){
		pa.pushMatrix();pa.pushStyle();
		pa.setColorValFill(SeqVisFFTOcean.gui_Red, 255);	pa.setColorValStroke(SeqVisFFTOcean.gui_Black, 255);
		pa.strokeWeight(1.0f);
		pa.rect(pianoDim);		//piano box
		//white keys		
		float[] lineYdim = new float[2];
		for(int i =0; i<pianoWKeyDims.length;++i){
			pa.pushMatrix();pa.pushStyle();
			pa.setColorValFill(SeqVisFFTOcean.gui_OffWhite, 255);	pa.setColorValStroke(SeqVisFFTOcean.gui_Black, 255);
			pa.strokeWeight(.5f);
			pa.rect(pianoWKeyDims[i]);
			lineYdim[0] = pianoWKeyDims[i][1]; lineYdim[1] = pianoWKeyDims[i][3];
			if(i!= 0){
				if (pianoWNotes[i].name.chkHasSharps()){lineYdim[0] += bkModY; lineYdim[1] -= bkModY;}//increase y0, decrease y1 coord to make room for black key
				if (pianoWNotes[i].name.chkHasFlats() && (i != pianoWKeyDims.length-1)){lineYdim[1] -= bkModY;}//decrease y1 coord to make room for black key				
			}
			pa.rect(whiteKeyWidth,lineYdim[0],winDim[2],lineYdim[1]);	

			pa.setColorValFill(SeqVisFFTOcean.gui_Gray, 255);			
			pa.text(""+pianoWNotes[i].nameOct, (wkOff_X+.05f)*whiteKeyWidth, pianoWKeyDims[i][1]+.85f*keyY);			
			pa.popStyle();pa.popMatrix();		
		}
		//black keys
		for(int i =0; i<pianoBKeyDims.length;++i){
			pa.pushMatrix();pa.pushStyle();
			pa.setColorValFill(SeqVisFFTOcean.gui_Black, 255);	pa.setColorValStroke(SeqVisFFTOcean.gui_Black, 255);
			pa.rect(pianoBKeyDims[i]);
			pa.setColorValFill(SeqVisFFTOcean.gui_LightGray,512);
			pa.noStroke();
			pa.rect(whiteKeyWidth,pianoBKeyDims[i][1]+.1f*keyY,winDim[2],pianoBKeyDims[i][3]-.2f*keyY);			
			pa.popStyle();pa.popMatrix();		
		}
		//vertical bars
		pa.setColorValStroke(SeqVisFFTOcean.gui_Black, 255);
		pa.strokeWeight(1.0f);
		float startX = pianoDim[2] + pianoDim[0];
		for(int i=0;i<numNotesWide;++i){
			pa.line(startX,pianoDim[1], startX,pianoDim[1]+ pianoDim[3]);		//piano box, p2);
			startX += keyX;
		}
		//pa.outStr2Scr("NumKeysDrawn : "+ keyCnt , true);
		pa.popStyle();pa.popMatrix();		
	}
	
	private void updateWinDim(float[] _winDim){	for(int i =0; i<_winDim.length; ++i){	winDim[i] = _winDim[i];}}
	private void updatePianoDim(float[] _pianoDim){	for(int i =0; i<_pianoDim.length; ++i){	pianoDim[i] = _pianoDim[i];}}	
	
}//myPianoObj class