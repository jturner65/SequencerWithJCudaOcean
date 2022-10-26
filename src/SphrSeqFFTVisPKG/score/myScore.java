package SphrSeqFFTVisPKG.score;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import SphrSeqFFTVisPKG.instrument.myInstrument;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.staff.myKeySig;
import SphrSeqFFTVisPKG.staff.myStaff;
import SphrSeqFFTVisPKG.staff.myTimeSig;
import SphrSeqFFTVisPKG.ui.myPianoObj;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import base_Math_Objects.MyMathUtils;

public class myScore {
	public static IRenderInterface pa;
	public myMusicSimWindow win;
	public static int sngCnt = 0;
	public int ID;
	public String songName;	

	public float[] scoreDim;			//part of containing window holding the pa.score
	
	//instrument-specific staffs - string is instrument name in all structs here
	public TreeMap<String,myStaff>  staffs;
	public ArrayList<String> staffDispOrder;		//order by which staff should be displayed

	private TreeMap<String,myInstrument> instruments;	
	private TreeMap<String, Boolean> staffSelList;	//list of staff select boxes
	private myStaff currentStaff;
	
	public boolean[] scrFlags;
	public static final int showSelStfIDX = 0;		//whether to show only selected staff
	
	public static final int numScrFlags = 1;

	//distance between staffs
	public static final float stOff = 90, boxStX = 0, boxStY = 10;
	
	public myScore(IRenderInterface _pa, myMusicSimWindow _win,String _name, float[] _scoreDim, ArrayList<String> staffName, ArrayList<myInstrument> _inst) {
		pa=_pa;
		win=_win;
		ID = sngCnt++;
		songName = _name;
		scoreDim = new float[_scoreDim.length];
		for(int i =0;i<scoreDim.length;++i){scoreDim[i]=_scoreDim[i];}
		instruments = new TreeMap<String,myInstrument>();
		staffs = new TreeMap<String,myStaff>();
		staffDispOrder = new ArrayList<String>();
		staffSelList = new TreeMap<String, Boolean>();
		for(int i =0; i< _inst.size(); ++i){
			addStaff(staffName.get(i), _inst.get(i));
		}	
		initScrFlags();
	}	

	public myScore(IRenderInterface _pa,myMusicSimWindow _win, String _name, float[] _scoreDim) {
		this(_pa,_win,_name,_scoreDim,new ArrayList<String>(),new ArrayList<myInstrument>());
	}		
	public void initScrFlags(){		scrFlags = new boolean[numScrFlags];for(int i=0;i<numScrFlags;++i){scrFlags[i]=false;}	}
	
	//set current staff for adding notes
	public void setCurrentStaff(String instName){
		currentStaff = staffs.get(instName);
	}
	//ptInRange(double x, double y, double minX, double minY, double maxX, double maxY){return ((x > minX)&&(x < maxX)&&(y > minY)&&(y < maxY));}
	//find where mouse is clicked - add notes manually, set staff selected/deselected
	public boolean hndlMouseClick(int mouseX, int mouseY){
		boolean mod = false;
		//pa.outStr2Scr("click made it to pa.score code");
		float xVal = (float)(boxStX + scoreDim[0]),
			  yVal;
		for(int i = 0; i < staffDispOrder.size(); ++i){
			yVal = (float)(boxStY + scoreDim[1]) + (i*stOff);
			if(MyMathUtils.ptInRange(mouseX, mouseY, xVal, yVal, xVal+10, yVal+10)){
				String stfName = staffDispOrder.get(i);
				//need to make the current staff the staff that is clicked
				staffSelList.put(stfName, !staffSelList.get(stfName));
				return true;
			}			
		}					
		//handle other mouse click events here
		return mod;
	}//handleMouseClick
	
	public boolean hndlMouseDrag(int mouseX, int mouseY){
		boolean mod = false;
		//handle mouse interaction in this pa.score - if interaction successful, return true;
		return mod;
	}//handleMouseDrag
	
	public TreeMap<String,myInstrument> getInstrumentList(){return instruments;}
	
	//add a staff to this song
	public void addStaff(String name, myInstrument _inst){
		//pa.outStr2Scr("adding staff name : " + name + " for inst : " + _inst.ID);
		instruments.put(name,_inst);
		myStaff tmp = new myStaff(pa, win,this, _inst, name);
		staffSelList.remove(name);
		staffSelList.put(name, true);
		staffs.put(name, tmp);
		if(!staffDispOrder.contains(name)){staffDispOrder.add(name);	}
	}
	//clear out staff of all notes
	public void clearStaffNotes(String staffName, int idx){
		if(idx == 0){win.getMsgObj().dispInfoMessage("myScore","clearStaffNotes","Clear all staff notes : ");}
		myStaff oldStaff = staffs.get(staffName);
		SortedMap<Integer,myNote> dmmyAllNotes = oldStaff.getAllNotesAndClear(-1,100000000, true);
	}//clearStaffNotes

	//overrides all key settings set in measures :  forceNotesSetKey(myKeySig _key, ArrayList<nValType> glblKeyNotesAra, boolean moveUp, myPianoObj dispPiano){			
	public void forceAllNotesToKey(myKeySig _key, ArrayList<noteValType> glblKeyNotesAra, boolean moveUp, myPianoObj dispPiano){		
		for(int i =0; i<staffDispOrder.size(); ++i){	
			staffs.get(staffDispOrder.get(i)).forceNotesSetKey( _key,  glblKeyNotesAra,  moveUp,  dispPiano);				
		}
	}
	
	//overrides all time sig settings set in measures :  forceNotesSetKey(myKeySig _key, ArrayList<nValType> glblKeyNotesAra, boolean moveUp, myPianoObj dispPiano){			
	public void forceAllNotesToTime(myTimeSig ts){		
		for(int i =0; i<staffDispOrder.size(); ++i){	
			staffs.get(staffDispOrder.get(i)).forceNotesSetTimeSig(ts);				
		}
	}
	
	//send out key sig info to every staff setKeySigAtTime(float stTime, myKeySig newKey){
	public void setCurrentKeySig(float timeToSet, myKeySig ks, ArrayList<noteValType> glblKSNoteVals){
		for(int i =0; i<staffDispOrder.size(); ++i){	
			staffs.get(staffDispOrder.get(i)).setKeySigAtTime(timeToSet, ks);				
		}		
	}
	
	//send out time sig info to every staff setKeySigAtTime(float stTime, myKeySig newKey){
	public void setCurrentTimeSig(float timeToSet, myTimeSig ts){
		for(int i =0; i<staffDispOrder.size(); ++i){	
			staffs.get(staffDispOrder.get(i)).setTimeSigAtTime(timeToSet, ts);				
		}		
	}
	
	//find note clicked on in staff - either select existing note or add new one
	public myNote checkStaffArea(int x, int y){
		myNote res = null;
		//TODO check staff location using y value (pxl) with staff offset
//		if(resIdx != -1){
//			nrDims[0] =(((int)(x/keyX)) * keyX);
//			NoteData tmp = ( keyType == 0 ? pianoWNotes[resIdx] : pianoBNotes[resIdx]);
//			res = new myNote(p, tmpa.name, tmpa.octave, null);
//			//pa.outStr2Scr("Note name in checkRollArea : " + res.n.name, true );
//			if(isNatural){//modify note grid dim so box doesn't overlap black keys
//				if (pa.chkHasSharps(res.n.name)){nrDims[1] += bkModY; nrDims[3] -= bkModY;}//increase y0, decrease y1 coord to make room for black key
//				if (pa.chkHasFlats(res.n.name) && (resIdx != pianoWKeyDims.length-1)){nrDims[3] -= bkModY;}//decrease y1 coord to make room for black key				
//			}
//			//pa.outStr2Scr("Note : " + res.toString() );
//		} else {
//			pa.outStr2Scr("Note is null ");
//		}
		return res;	
	}//checkStaffArea
	
	//build note as being entered, then add it
	public void addNoteToStaff(String staffName, myNote _n ){staffs.get(staffName).addNoteAtNoteTime(_n);}

	//display pa.score in window
	public void drawScore(){
		pa.pushMatState();
		pa.translate(scoreDim[0],scoreDim[1]);		
		for(int i =0; i<staffDispOrder.size(); ++i){
			String stfName = staffDispOrder.get(i);
			//drawStfSelBox(staffSelList.get(stfName));
			staffs.get(stfName).drawStaff();
			pa.translate(0, stOff);
		}
//		}
		pa.popMatState();		
	}
	
//	//play all note data - should this be on measure by measure basis?
//	public void playScore(){
//		ArrayList<NoteData> allNoteData = new ArrayList<NoteData>();
//		for(int i =0; i<staffDispOrder.size(); ++i){
//			String stfName = staffDispOrder.get(i);
//			if(staffSelList.get(stfName)){	allNoteData.addAll(staffs.get(stfName).play());}
//		}	
//	}
	
	public String toString(){
		String res = "Song name : "+songName+" Instruments : ";
		for(int i =0; i<staffDispOrder.size(); ++i){
			String stfName = staffDispOrder.get(i);
			res += staffs.get(stfName).toString();
		}
		return res;
	}
	

}//myScore class