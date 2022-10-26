package SphrSeqFFTVisPKG.clef;

import SphrSeqFFTVisPKG.clef.base.myClefBase;
import SphrSeqFFTVisPKG.clef.enums.clefType;
import SphrSeqFFTVisPKG.note.NoteData;
import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;
import processing.core.PImage;

public class myGrandClef extends myClefBase{	
	public myClef[] clefs;

	public myGrandClef(myMusicSimWindow _win, String _name, clefType _clef, NoteData _mdNote, PImage _img, float[] _drawDim, float _ocf) {
		super(_win, _name, _clef, _mdNote, _img,_drawDim, _ocf);		
		initGrandClef();
	}
	public myGrandClef(myGrandClef _c){
		super(_c);	
		initGrandClef();
	}
	protected void initGrandClef() {
		isGrandStaff = true;
		clefs = new myClef[2];
		clefs[0] = new myClef((myClef) win.clefs[0]);		//first clef is treble clef
		clefs[1] = new myClef((myClef) win.clefs[1]);		//2nd clef is bass clef
		//ignore this staff's values	
		midNote = null;
		clefImage = null;	
	}
	
	

	@Override
	public boolean isAboveMiddle(NoteData _note) 	{
		//should be stem down if lower than middle c but higher than bass clef middle note or higher than treble clef mid note
		return (((_note.isLowerThan(win.C4)) && (clefs[1].getMidNote().isLowerThan(_note))) || (clefs[0].getMidNote().isLowerThan(_note)));
	}
	@Override
	public void setImage(PImage[] _clf) {
		//set both images for both clefs in this clef
		for(int i =0; i<_clf.length;++i){clefs[i].setImage(new PImage[]{_clf[i]});}
	}
	//whether or not a note is on the staff or above or below it : -1 is below, 0 is on, 1 is above
	@Override
	public int isOnStaff(NoteData _note) {
		int clf0Val = clefs[0].isOnStaff(_note),
			clf1Val = clefs[0].isOnStaff(_note);		
		return clf0Val * clf1Val * (clf1Val < 0 ? -1 : 1);		//will be -1 if below clf1 or above clf1 but below clf 0, if in either staff will be 0, will be 1 if above both
	}

	@Override
	public void drawMe(IRenderInterface p,float offset) {
		p.pushMatState();
		clefs[0].drawMe(p,offset);
		p.translate(0, 5*offset);			//TODO figure correct offset amount
		clefs[1].drawMe(p,offset);	
		p.popMatState();	
	}
	
	@Override
	public String toString(){
		String res = "Grand Staff : Name: "+name+":\n";
		res +="\t"+clefs[0].toString()+"\n";
		res +="\t"+clefs[1].toString()+"\n";
		return res;
	}

}//myGrandClef