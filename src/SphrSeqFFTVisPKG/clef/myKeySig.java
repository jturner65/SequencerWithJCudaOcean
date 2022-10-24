package SphrSeqFFTVisPKG.clef;

import java.util.ArrayList;
import java.util.Arrays;

import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.clef.enums.keySigVals;
import SphrSeqFFTVisPKG.note.enums.nValType;

public class myKeySig {
	public SeqVisFFTOcean p;
	public static int sigCnt = 0;
	public int ID;
	//there are 12 key signatures, each with a specific mapping of "allowed" notes
	private static final nValType[][] keyNotes = new nValType[][]{//circle of 5ths
		{nValType.C,nValType.D, nValType.E, nValType.F, nValType.G, nValType.A, nValType.B},		//CMaj(0) 0 sharps
		{nValType.G, nValType.A, nValType.B,nValType.C,nValType.D, nValType.E, nValType.Fs},		//GMaj(1) 1 sharp : Fs
		{nValType.D, nValType.E, nValType.Fs, nValType.G, nValType.A, nValType.B,nValType.Cs},		//DMaj(2) 2 sharps : Fs, Cs
		{nValType.A, nValType.B,nValType.Cs,nValType.D, nValType.E, nValType.Fs, nValType.Gs},		//Amaj(3) 3 sharps : Fs, Cs, Gs
		{nValType.E, nValType.Fs, nValType.Gs,nValType.A, nValType.B,nValType.Cs,nValType.Ds},		//EMaj(4) 4 sharps : Fs, Cs, Gs, Ds
		{nValType.B,nValType.Cs,nValType.Ds,nValType.E, nValType.Fs, nValType.Gs,nValType.As},		//BMaj(5) 5 sharps : Fs, Cs, Gs, Ds As
		{nValType.Fs, nValType.Gs,nValType.As,nValType.B,nValType.Cs,nValType.Ds,nValType.F},		//FsMaj(6) 6 sharps : Fs, Cs, Gs, Ds As Es
		{nValType.Cs,nValType.Ds,nValType.F,nValType.Fs, nValType.Gs,nValType.As,nValType.C},		//CsMaj(7) 5 flats :  Db, Eb, Gb, Ab, Bb
		{nValType.Gs,nValType.As,nValType.C,nValType.Cs,nValType.Ds,nValType.F,nValType.G},			//GsMaj(8) 4 flats :  Eb, Gb, Ab, Bb
		{nValType.Ds,nValType.F,nValType.G,nValType.Gs,nValType.As,nValType.C,nValType.D},			//DsMaj(9) 3 flats :  Gb, Ab, Bb
		{nValType.As,nValType.C,nValType.D,nValType.Ds,nValType.F,nValType.G,nValType.A},			//AsMaj(10) 2 flats : Ab, Bb
		{nValType.F,nValType.G,nValType.A,nValType.As,nValType.C,nValType.D,nValType.E},			//Fmaj(11) 1 flat   : Bb
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
	
	public myKeySig(SeqVisFFTOcean _p, keySigVals _key) {
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
	public nValType getRoot(){return keyNotes[key.getVal()][0];}
	public static nValType[] getKeyNotes(keySigVals _psdKey){return  keyNotes[_psdKey.getVal()];}
	public static ArrayList<nValType> getKeyNotesAsList(keySigVals _psdKey){return  new ArrayList<nValType>(Arrays.asList(keyNotes[_psdKey.getVal()]));}
	//return array of alloweable note types for this key signature
	public nValType[] getKeyVals(){	return keyNotes[key.getVal()];}	
	public ArrayList<nValType> getKeyNotesAsList(){return  new ArrayList<nValType>(Arrays.asList(keyNotes[keyIdx]));}

	//assumes starting at upper left of measure bound - offY is for offset from clef, to align with correct notes
	public void drawMe(float offX, float offY){
		p.pushMatrix();p.pushStyle();
		p.translate(drawDim[0] + offX,drawDim[1] + offY);		
		for(int i=0;i<occsDisp[keyIdx].length;++i){
			p.translate(occsDimAra[keyIdx][i][0], occsDimAra[keyIdx][i][1]);
			p.pushMatrix();p.pushStyle();
			p.scale(1, 1.6f);
			p.text(occsDisp[keyIdx][i], 0, 0);
			p.popStyle();p.popMatrix();	
		}
		p.popStyle();p.popMatrix();	
	}
	
	public boolean equals(myKeySig _ot){return (_ot.key.getVal() == this.key.getVal());}
	
	public String toString(){
		String res = "Key ID : "+ID+" Key Sig : "+ key;
		return res;
	}

}//class myKeySig

