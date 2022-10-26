package SphrSeqFFTVisPKG.note;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.Map.Entry;

import SphrSeqFFTVisPKG.measure.myMeasure;
import SphrSeqFFTVisPKG.note.enums.chordType;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.staff.myKeySig;
import SphrSeqFFTVisPKG.staff.myStaff;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import SphrSeqFFTVisPKG.ui.controls.mySphereCntl;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;

//collection of notes related in some way for a single instrument, also holds data for root of chord
public class myChord extends myNote{
	public static int cCnt = 0;
	public int CID;		
	public String cname;
	
	public chordType type;
	
	public TreeMap<String, myNote> cnotes;	//keyed by frequency
		
	//note passed is root of chord
	public myChord(myMusicSimWindow _win, noteValType _name, int _octave, myMeasure _measure, myStaff _stf) {
		super(_win,_name,_octave,_measure,_stf);
		initChord();
	} 
	//turn a note into a chord
	public myChord(myMusicSimWindow _win,  float _alphaSt, float _alphaEnd, int _ring, mySphereCntl _sphrOwn){
		super(_win, _alphaSt, _alphaEnd, _ring, _sphrOwn);
		initChord();
	}
	//turn a note into a chord
	public myChord(myNote _note){
		super(_note);
		initChord();
	}
	
	private void initChord(){
		cname = "";
		CID = cCnt++;		
		cnotes = new TreeMap<String,myNote>();	
		cnotes.put(this.n.nameOct,this);		//idx 0 is always root of chord
		flags[isChord] = true;		
		type = chordType.None;
	}
	
	@Override
	//take current duration and set to nearest integral duration 
	public void quantize(){
		n.quantMe();
		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().quantize();}}
	}
//	@Override
//	//playback - returns all notedata for all notes in this chord
//	public ArrayList<NoteData> play(){
//		ArrayList<NoteData> resVals = new ArrayList<NoteData>();
//		resVals.add(playMe().get(0));			//add this note's notedata
//		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){resVals.addAll(note.getValue().play());}}		//return this chord's notes			
//		return resVals;
//	}

	//force the notes of this chord to be the passed type, adding notes if necessary, removing any extraneous notes.
	//keyBased : if true, set chord to be specific chord in current key, otherwise treat root note as key of chord
	public void setChordType(chordType chrd, boolean keyBased, myKeySig key){
		this.type = chrd;
		if(this.type == chordType.None){return;}	//do not modify "none" chords
		int[] noteDispAra = chordType.getChordDisp(chrd), indNDisp;
		int numNotes = noteDispAra.length;
		ArrayList<myNote> newCNotes = new ArrayList<myNote>();
		myNote newNote;
		if(keyBased){
			//TODO : force root to be key root?  or find closest chord note matching root?  for now, force note to be key root
			noteValType root = key.getRoot();
			n.editNoteVal(root, n.octave);			
		} 
		else {	}//treat root as key of chord - move notes appropriately - root remains unchanged	
		//		if(numNotesDisp > 12){numNotesDisp -= 12; n.editNoteVal(n.name, n.octave+1);}
		//	if(numNotesDisp < -12){numNotesDisp += 12; n.editNoteVal(n.name, n.octave-1);}

		//
		if(flags[isFromSphere]){
			for(int i =1; i<numNotes; ++i){
				newNote = new myNote(win, this.sphereDims[1], this.sphereDims[1], this.sphereRing, this.sphrOwn);
				if(noteDispAra[i] > 12){noteDispAra[i] -= 12; newNote.n.editNoteVal(newNote.n.name, newNote.n.octave+1);}
				if(noteDispAra[i] < -12){noteDispAra[i] += 12; newNote.n.editNoteVal(newNote.n.name, newNote.n.octave-1);}
				indNDisp = win.getNoteDisp(newNote.n, noteDispAra[i]);
				newNote.n.editNoteVal(noteValType.getVal(indNDisp[0]), indNDisp[1]);
			}		
		
		} else {
			for(int i =1; i<numNotes; ++i){
				newNote = new myNote(this);
				if(noteDispAra[i] > 12){noteDispAra[i] -= 12; newNote.n.editNoteVal(newNote.n.name, newNote.n.octave+1);}
				if(noteDispAra[i] < -12){noteDispAra[i] += 12; newNote.n.editNoteVal(newNote.n.name, newNote.n.octave-1);}
				indNDisp = win.getNoteDisp(newNote.n, noteDispAra[i]);
				newNote.n.editNoteVal(noteValType.getVal(indNDisp[0]), indNDisp[1]);
			}		
		}
		//this gets rid of existing chord notes and builds new chord
		this.rebuildCNotes(this, newCNotes);
	}//setChordType
	
	@Override
	public void drawMePRL(IRenderInterface p){
		p.drawRect(gridDims);
		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().drawMePRL(p);}}
	}
	
	@Override
	public void drawMeSphere(IRenderInterface p){
		this.drawMeSpherePriv(p);
		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().drawMeSphere(p);}}	
	}

	@Override
	public void drawMe(IRenderInterface p){
		drawMePriv(p);
		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().drawMe(p);}}
	}
	@Override	
	public void moveNoteHalfStep(myKeySig _key, ArrayList<noteValType> keyAra, boolean up){		
		ArrayList<myNote> newCNotes = new ArrayList<myNote>();
		moveNoteHalfStepPriv(_key, keyAra, up);
		for(Entry<String, myNote> note : cnotes.entrySet()){
			if(this.ID != note.getValue().ID){
				note.getValue().moveNoteHalfStep(_key, keyAra, up);
				newCNotes.add(note.getValue());				
			}
		}
		//need to rebuild cnotes struct with moved notes, since some may be the same after moving
		rebuildCNotes(this,newCNotes);
//		cnotes = new TreeMap<String,myNote>();	//rebuild array
//		cnotes.put(this.n.nameOct,this);		//idx 0 is always root of chord
//		for(int i =0; i<newCNotes.size();++i){	addNote(newCNotes.get(i));}
	}
	//rebuild cnotes array with new array(that doesn't include root) and root note
	public void rebuildCNotes(myNote root, ArrayList<myNote> newCNotes){
		//need to rebuild cnotes struct with moved notes, since some may be the same after moving
		cnotes = new TreeMap<String,myNote>();	//rebuild array
		cnotes.put(root.n.nameOct,root);		//idx 0 is always root of chord
		for(int i =0; i<newCNotes.size();++i){	addNote(newCNotes.get(i));}		
	}
	@Override
	public void addDurationSphere(float _scrAlpha){
		addDurationSphereIndiv(_scrAlpha);
		for(Entry<String, myNote> note : cnotes.entrySet()){
			if(this.ID != note.getValue().ID){	note.getValue().addDurationSphere(_scrAlpha);}
		}
	}
	public void setChordName(String _name){cname = _name;}	
	//add a note to this chord
	public void addNote (myNote _nt){cnotes.put(_nt.n.nameOct,_nt);}
	//have all notes start at time of first note in chord (root)
	public void alignStart(){ for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().setStart(this.n.stTime);}}}
	//have all notes last as long as root (this note)
	public void alignDur(){for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().setDuration(this.n.dur, flags[isDotted],flags[isTuple],tupleVal);}}}	
	
	@Override
	//override for chord
	public void modStDur(int newDur, int newSt){
		setStart(newSt);								//shift start time to end of new note (notes share start time
		n.setDur(newDur);
		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){note.getValue().modStDur(newDur, newSt);}}		
	}
	
	public String toString(){
		String res = "|Chord ID : "+CID + " Chord Name : " + cname + "| # notes : "+ cnotes.size()+"\n";
		res+="\t(Root) #0 : "+super.toString()+"\n";
		int i =1;
		for(Entry<String, myNote> note : cnotes.entrySet()){if(this.ID != note.getValue().ID){myNote tmpNote = note.getValue();res += "\t       #"+i+++" : "+((myNote)(tmpNote)).toString()+"\n";}}
		return res;
	}		
}//class mychord