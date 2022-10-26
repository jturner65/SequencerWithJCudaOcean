package SphrSeqFFTVisPKG.staff;

import java.util.ArrayList;
import java.util.Arrays;

import SphrSeqFFTVisPKG.clef.enums.keySigVals;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import base_JavaProjTools_IRender.base_Render_Interface.IRenderInterface;

public class myKeySig {
	public IRenderInterface p;
	public static int sigCnt = 0;
	public int ID;
	//there are 12 key signatures, each with a specific mapping of "allowed" notes
	private static final noteValType[][] keyNotes = new noteValType[][]{//circle of 5ths
		{noteValType.C,noteValType.D, noteValType.E, noteValType.F, noteValType.G, noteValType.A, noteValType.B},		//CMaj(0) 0 sharps
		{noteValType.G, noteValType.A, noteValType.B,noteValType.C,noteValType.D, noteValType.E, noteValType.Fs},		//GMaj(1) 1 sharp : Fs
		{noteValType.D, noteValType.E, noteValType.Fs, noteValType.G, noteValType.A, noteValType.B,noteValType.Cs},		//DMaj(2) 2 sharps : Fs, Cs
		{noteValType.A, noteValType.B,noteValType.Cs,noteValType.D, noteValType.E, noteValType.Fs, noteValType.Gs},		//Amaj(3) 3 sharps : Fs, Cs, Gs
		{noteValType.E, noteValType.Fs, noteValType.Gs,noteValType.A, noteValType.B,noteValType.Cs,noteValType.Ds},		//EMaj(4) 4 sharps : Fs, Cs, Gs, Ds
		{noteValType.B,noteValType.Cs,noteValType.Ds,noteValType.E, noteValType.Fs, noteValType.Gs,noteValType.As},		//BMaj(5) 5 sharps : Fs, Cs, Gs, Ds As
		{noteValType.Fs, noteValType.Gs,noteValType.As,noteValType.B,noteValType.Cs,noteValType.Ds,noteValType.F},		//FsMaj(6) 6 sharps : Fs, Cs, Gs, Ds As Es
		{noteValType.Cs,noteValType.Ds,noteValType.F,noteValType.Fs, noteValType.Gs,noteValType.As,noteValType.C},		//CsMaj(7) 5 flats :  Db, Eb, Gb, Ab, Bb
		{noteValType.Gs,noteValType.As,noteValType.C,noteValType.Cs,noteValType.Ds,noteValType.F,noteValType.G},			//GsMaj(8) 4 flats :  Eb, Gb, Ab, Bb
		{noteValType.Ds,noteValType.F,noteValType.G,noteValType.Gs,noteValType.As,noteValType.C,noteValType.D},			//DsMaj(9) 3 flats :  Gb, Ab, Bb
		{noteValType.As,noteValType.C,noteValType.D,noteValType.Ds,noteValType.F,noteValType.G,noteValType.A},			//AsMaj(10) 2 flats : Ab, Bb
		{noteValType.F,noteValType.G,noteValType.A,noteValType.As,noteValType.C,noteValType.D,noteValType.E},			//Fmaj(11) 1 flat   : Bb
	};
	private static final String[][] occsDisp = new String[][]{
		{},{"#"},{"#","#"},{"#","#","#"},{"#","#","#","#"},{"#","#","#","#","#"},{"#","#","#","#","#","#"},
		{"b","b","b","b","b"},{"b","b","b","b"},{"b","b","b"},{"b","b"},{"b"}};

	private static final float occsDim = 10;
	
	private static final float[][][] occsDimAra = new float[][][]{
		{{0,0}},
		{{0,8}},
		{{0,8},{occsDim,15}},
		{{0,8},{occsDim,15},{occsDim,-20}},
		{{0,8},{occsDim,15},{occsDim,-20},{occsDim,15}},
		{{0,8},{occsDim,15},{occsDim,-20},{occsDim,15},{occsDim,15}},
		{{0,8},{occsDim,15},{occsDim,-20},{occsDim,15},{occsDim,15},{occsDim,-20}},		//here and above is sharps, below is flats
		{{0,25},{occsDim,-15},{occsDim,20},{occsDim,-15},{occsDim,20}},
		{{0,25},{occsDim,-15},{occsDim,20},{occsDim,-15}},
		{{0,25},{occsDim,-15},{occsDim,20}},
		{{0,25},{occsDim,-15}},
		{{0,25}}
	};
	public keySigVals key;
	public float[] drawDim;
	public int keyIdx;
	
	public myKeySig(IRenderInterface _p, keySigVals _key) {
		p=_p;
		ID = sigCnt++;
		key = _key;		
		keyIdx = key.getVal();
		drawDim = new float[4];
		drawDim[0]=-10;
		drawDim[1]=0;		
		drawDim[2]= occsDisp[keyIdx].length * occsDim;
		drawDim[3]=50;
	}
	
	public myKeySig(myKeySig _ks){this(_ks.p, _ks.key);}
	//get root of key
	public noteValType getRoot(){return keyNotes[key.getVal()][0];}
	public static noteValType[] getKeyNotes(keySigVals _psdKey){return  keyNotes[_psdKey.getVal()];}
	public static ArrayList<noteValType> getKeyNotesAsList(keySigVals _psdKey){return  new ArrayList<noteValType>(Arrays.asList(keyNotes[_psdKey.getVal()]));}
	//return array of alloweable note types for this key signature
	public noteValType[] getKeyVals(){	return keyNotes[key.getVal()];}	
	public ArrayList<noteValType> getKeyNotesAsList(){return  new ArrayList<noteValType>(Arrays.asList(keyNotes[keyIdx]));}

	//assumes starting at upper left of measure bound - offY is for offset from clef, to align with correct notes
	public void drawMe(float offX, float offY){
		p.pushMatState();
		p.translate(drawDim[0] + offX,drawDim[1] + offY);		
		for(int i=0;i<occsDisp[keyIdx].length;++i){
			p.translate(occsDimAra[keyIdx][i][0], occsDimAra[keyIdx][i][1]);
			p.pushMatState();
			p.scale(1, 1.6f);
			p.showText(occsDisp[keyIdx][i], 0, 0);
			p.popMatState();	
		}
		p.popMatState();
	}
	
	public boolean equals(myKeySig _ot){return (_ot.key.getVal() == this.key.getVal());}
	
	public String toString(){
		String res = "Key ID : "+ID+" Key Sig : "+ key;
		return res;
	}

}//class myKeySig

