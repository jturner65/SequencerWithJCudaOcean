package SphrSeqFFTVisPKG.clef;

import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.clef.base.myClefBase;
import SphrSeqFFTVisPKG.note.NoteData;
import processing.core.PImage;

public class myGrandClef extends myClefBase{	
	public myClef[] clefs;

	public myGrandClef(SeqVisFFTOcean _p, String _name, clefVal _clef, NoteData _mdNote, PImage _img, float[] _drawDim, float _ocf) {
		super(_p, _name, _clef, _mdNote, _img,_drawDim, _ocf);		
		isGrandStaff = true;
		clefs = new myClef[2];
		clefs[0]=new myClef(p.clefs[0]);		//first clef is treble clef
		clefs[1]=new myClef(p.clefs[1]);		//2nd clef is bass clef
		//ignore this staff's values	
		clef = null;
		midNote = null;
		clefImage = null;
		c4LocMultForClef = p.getC4LocMultClef(clef, true);
	}
	public myGrandClef(myClefBase _c){this(_c.p,_c.name,_c.getClef(),null,null, _c.drawDim, _c.occsOffset);}

	@Override
	public boolean isAboveMiddle(NoteData _note) 	{
		//should be stem down if lower than middle c but higher than bass clef middle note or higher than treble clef mid note
		return (((_note.isLowerThan(p.C4)) && (clefs[1].getMidNote().isLowerThan(_note))) || (clefs[0].getMidNote().isLowerThan(_note)));
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
	public void drawMe(float offset) {
		p.pushMatrix();p.pushStyle();
		clefs[0].drawMe(offset);
		p.translate(0, 5*offset);			//TODO figure correct offset amount
		clefs[1].drawMe(offset);	
		p.popStyle();p.popMatrix();	
	}
	
	@Override
	public String toString(){
		String res = "Grand Staff : ID: "+ID+":\n";
		res +="\t"+clefs[0].toString()+"\n";
		res +="\t"+clefs[1].toString()+"\n";
		return res;
	}

}//myGrandClef