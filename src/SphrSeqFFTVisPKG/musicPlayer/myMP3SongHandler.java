package SphrSeqFFTVisPKG.musicPlayer;

import SphrSeqFFTVisPKG.ui.base.myMusicSimWindow;
import ddf.minim.AudioPlayer;
import ddf.minim.analysis.FFT;
import ddf.minim.analysis.WindowFunction;

/**
 * handles an mp3 song, along with transport control
 * @author 7strb
 *
 */
public class myMP3SongHandler{
	public myMusicSimWindow win;
	public AudioPlayer playMe;
	public FFT fftMP3Log;
	public String fileName, dispName;
	public int songBufSize;  //must be pwr of 2
	//length in millis
	public int songLength;
	
	public myMP3SongHandler(myMusicSimWindow _win, String _fname, String _dname, int _sbufSize) {
		win=_win; fileName = _fname; dispName = _dname;songBufSize = _sbufSize;
		playMe = win.minim.loadFile(fileName, songBufSize);
		songLength = playMe.length();
		fftMP3Log = new FFT(playMe.bufferSize(), playMe.sampleRate() );		
//		barDims = new float[] {.8f * pa.menuWidth, 10.0f};
//		dispTransOffset = new double[] {0.0,0.0};
//		barStY = .1f * pa.menuWidth;
	}	
	
	//call before any fft analysis
	public void fftFwdOnAudio() {fftMP3Log.forward( playMe.mix );	}
	//call to get data for fft display - call before any fft analysis
	public float[][] fftSpectrumFromAudio() {	return new float[][] {fftMP3Log.getSpectrumReal(), fftMP3Log.getSpectrumImaginary()};}
	
	public float[] fftFwdBandsFromAudio() {
		int specSize = fftMP3Log.specSize();	//should be songBufSize / 2 + 1
		float[] bandRes = new float[specSize];
		for(int i=0;i<specSize;++i) {
			bandRes[i] = fftMP3Log.getBand(i);			
		}		
		return bandRes;
	}	

	
	public void play() {	playMe.play();}
	public void play(int millis) {	playMe.play(millis);}
	public void pause() {	playMe.pause();}

	public void modPlayLoc(float modAmt) {
		int curPos = playMe.position();	
		int dispSize = songLength/20, newPos = (int) (curPos + (dispSize * modAmt));
		if(newPos < 0) { newPos = 0;} else if (newPos > songLength-1){newPos = songLength-1;}
		playMe.cue(newPos);
		//System.out.println("Mod playback by " + modAmt + " song length : " + songLength + " song position : " + curPos + " new position : " + newPos);
	}
	
	public void setFFTVals(WindowFunction win, int fftMinBandwidth, int fftBandsPerOctave) {
		fftMP3Log.window(win);
		fftMP3Log.logAverages( fftMinBandwidth, fftBandsPerOctave );  		
	}
	
	public int getPlayPos() {return playMe.position();}
	public float getPlayPosRatio() {return playMe.position()/(1.0f*songLength);}


}//myMP3SongHandler