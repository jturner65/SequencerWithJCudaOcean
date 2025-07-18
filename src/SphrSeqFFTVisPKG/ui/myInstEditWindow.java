package SphrSeqFFTVisPKG.ui;
import java.util.*;

import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.Base_DispWindow;
import SphrSeqFFTVisPKG.myDrawnSmplTraj;
import SphrSeqFFTVisPKG.myGUIObj;
import SphrSeqFFTVisPKG.note.NoteData;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.noteDurType;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.staff.myKeySig;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import ddf.minim.*;
import ddf.minim.analysis.*;

public class myInstEditWindow extends Base_DispWindow {
    
    public static final int 
        trajEditsADSR             = 0;                    //drawn trajectory edits ADSR for instrument, otherwise we're editing oscil mults
    public static final int numPrivFlags = 1;
//    //GUI Objects    
    //idx's of objects in gui objs array - relate to modifications of oceanFFT sim code
    public final static int 
        instToEditIDX         = 0;             //            currently editing instrument
                        
    public final int numGUIObjs = 1;    
    
    public myInstEditWindow(SeqVisFFTOcean _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed, String _winTxt, boolean _canDrawTraj) {
        super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
        float stY = rectDim[1]+rectDim[3]-4*txtHeightOff,stYFlags = stY + 2*txtHeightOff;        
        
        
        super.initThisWin(_canDrawTraj, false);
    }
    @Override
    //initialize all private-flag based UI buttons here - called by base class
    public void initAllUIButtons(){
        truePrivFlagNames = new String[]{                                //needs to be in order of flags
                "ADSR->Harm  "
        };
        falsePrivFlagNames = new String[]{            //needs to be in order of flags
                "Harm->ADSR  "
        };
        privModFlgIdxs = new int[]{trajEditsADSR};
        numClickBools = privModFlgIdxs.length;    
        //maybe have call for         initPrivBtnRects(0);    
        initPrivBtnRects(0,numClickBools);
    }

    protected void initMe() {        
        dispFlags[canDrawTraj] = true;            //to edit instrument qualities need to use drawn trajectories        
//        dispFlags[uiObjsAreVert] = true;
        //init specific sim flags
        initPrivFlags(numPrivFlags);
    }//initMe
    
    @Override
    //set flag values and execute special functionality for this sequencer
    public void setPrivFlags(int idx, boolean val){
        privFlags[idx] = val;
        switch(idx){
        case trajEditsADSR             : {break;}            //placeholder    
        }            
    }//setInstFlags
    
    //init any extra ui objs
    @Override
    protected void initXtraUIObjsIndiv() {}
    
    //initialize structure to hold modifiable menu regions
    @Override
    protected void setupGUIObjsAras(){    
        //pa.outStr2Scr("setupGUIObjsAras in :"+ name);
        int numInstrs = 0;
        if(pa.score!=null){
            numInstrs = pa.score.staffs.size()-1;
        } 
        if(numInstrs < 0){numInstrs = 0;}
        guiMinMaxModVals = new double [][]{  
            {0,numInstrs,.05},
//            {0.0, 100.0, 0.1},            //tmp placeholder            
        };                    
        guiStVals = new double[]{0};                                //starting value
        //guiObjNames = new String[]{"Inst Edit UI Obj 1"};    //name/label of component    
        guiObjNames = new String[]{"Instrument to Edit"};    //name/label of component    
                    
        //idx 0 is treat as int, idx 1 is obj has list vals, idx 2 is object gets sent to windows
        guiBoolVals = new boolean [][]{
            {true, true, true},      //ui placeHolder     
        };                        //per-object  list of boolean flags
        
        //since horizontal row of UI comps, uiClkCoords[2] will be set in buildGUIObjs        
        guiObjs_Numeric = new myGUIObj[numGUIObjs];            //list of modifiable gui objects
        if(numGUIObjs > 0){
            buildGUIObjs(guiObjNames,guiStVals,guiMinMaxModVals,guiBoolVals,new double[]{xOff,txtHeightOff});            //builds a horizontal list of UI comps
        }
    }

    //if any ui values have a string behind them for display
    @Override
    public String getUIListValStr(int UIidx, int validx) {
        //pa.outStr2Scr("getUIListValStr : " + UIidx+ " Val : " + validx + " inst len:  "+pa.InstrList.length+ " | "+pa.InstrList[(validx % pa.InstrList.length)].instrName );
        switch(UIidx){//pa.score.staffs.size()
            case instToEditIDX         : {return pa.score.staffs.get(pa.score.staffDispOrder.get(validx % pa.score.staffs.size())).instrument.instrName; }
        }
        return "";
    }
    @Override
    public void setUIWinVals(int UIidx) {
        switch(UIidx){
            case instToEditIDX : {curTrajAraIDX = (int)guiObjs_Numeric[UIidx].getVal(); break;}
        }
    }
    @Override
    public void setScoreInstrValsIndiv(){
        
        
    }
    //- music is told to start
    @Override
    protected void playMe() {
        
        
    }
    //when music stops
    @Override
    protected void stopMe() {
        
        
    }
    //move current play position when playing mp3/sample
    @Override
    protected void modMySongLoc(float modAmt) {
        
    };
    @Override
    protected void drawMe(float animTimeMod) {
        
    }
    @Override
    public void clickDebug(int btnNum){
        pa.outStr2Scr("click debug in "+name+" : btn : " + btnNum);
        switch(btnNum){
            case 0 : {//note vals
                break;
            }
            case 1 : {//measure vals
                break;
            }
            case 2 : {
                break;
            }
            case 3 : {
                break;
            }
            default : {break;}
        }        
    }    
    
    @Override
    protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
        return false;
    }
    
    @Override
    protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
        boolean mod = false;
        
        
        if(mod) {return mod;}
        else {return checkUIButtons(mouseX, mouseY);}
    }

    @Override
    protected boolean hndlMouseDragIndiv(int mouseX, int mouseY,int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
        boolean mod = false;
        
        return mod;
    }

    @Override
    protected void hndlMouseRelIndiv() {
        // TODO Auto-generated method stub
    }
    
    @Override
    protected myPoint getMouseLoc3D(int mouseX, int mouseY){return pa.P(mouseX,mouseY,0);}
    
    @Override
    protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc){}//not a snap-to window
    @Override
    protected void processTrajIndiv(myDrawnSmplTraj drawnNoteTraj){
        
        //traj processing
    }
    @Override
    protected SortedMap<Integer, myNote> getNotesNow() {return null;}
    @Override
    //set current key signature, at time passed - for score, set it at nearest measure boundary
    protected void setGlobalKeySigValIndiv(int idx, float time){    }//setCurrentKeySigVal
    @Override
    //set time signature at time passed - for score, set it at nearest measure boundary
    protected void setGlobalTimeSigValIndiv(int tsnum, int tsdenom, noteDurType _d, float time){    }//setCurrentTimeSigVal
    @Override
    //set time signature at time passed - for score, set it at nearest measure boundary
    protected void setGlobalTempoValIndiv(float tempo, float time){    }//setCurrentTimeSigVal
    @Override
    //set current key signature, at time passed - for score, set it at nearest measure boundary
    protected void setLocalKeySigValIndiv(myKeySig lclKeySig, ArrayList<noteValType> lclKeyNotesAra, float time){}
    @Override
    //set time signature at time passed - for score, set it at nearest measure boundary
    protected void setLocalTimeSigValIndiv(int tsnum, int tsdenom, noteDurType _beatNoteType, float time){}
    @Override
    //set time signature at time passed - for score, set it at nearest measure boundary
    protected void setLocalTempoValIndiv(float tempo, float time){}
    @Override
    protected void endShiftKey_Indiv() {}
    @Override
    protected void endAltKey_Indiv() {
        // TODO Auto-generated method stub    
    }
    @Override
    protected void endCntlKey_Indiv() {}
    @Override
    protected void addSScrToWinIndiv(int newWinKey){}
    @Override
    protected void addTrajToScrIndiv(int subScrKey, String newTrajKey){}
    @Override
    protected void delSScrToWinIndiv(int idx) {}    
    @Override
    protected void delTrajToScrIndiv(int subScrKey, String newTrajKey) {}
    
    //resize drawn all trajectories
    @Override
    protected void resizeMe(float scale) {
        
        //any resizing done
    }
    @Override
    protected void closeMe() {}
    @Override
    protected void showMe() {}
    @Override
    public String toString(){
        String res = super.toString();
        return res;
    }

}//myInstEditWindow
