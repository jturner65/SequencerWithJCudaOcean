package SphrSeqFFTVisPKG.ui;

import java.util.*;

import SphrSeqFFTVisPKG.SeqVisFFTOcean;
import SphrSeqFFTVisPKG.Base_DispWindow;
import SphrSeqFFTVisPKG.myDrawnSmplTraj;
import SphrSeqFFTVisPKG.myGUIObj;
import SphrSeqFFTVisPKG.instrument.myInstrument;
import SphrSeqFFTVisPKG.note.myNote;
import SphrSeqFFTVisPKG.note.enums.noteDurType;
import SphrSeqFFTVisPKG.note.enums.noteValType;
import SphrSeqFFTVisPKG.staff.myKeySig;
import SphrSeqFFTVisPKG.ui.controls.mySphereCntl;
import base_Math_Objects.MyMathUtils;
import base_Math_Objects.vectorObjs.doubles.myPoint;
import base_Math_Objects.vectorObjs.doubles.myVector;
import processing.core.PApplet;

public class mySphereWindow extends Base_DispWindow {
    public final int numGUIObjs = 0;                                                //# of gui objects for ui
    
    public TreeMap<String, mySphereCntl> sphereCntls;                                //controls for each instrument
    
    public String curSelSphere;
    public float focusZoom = 1;                //amt to zoom the in-focus sphere
    
    //to handle real-time update of locations of spheres
    public myVector curMseLookVec;  //pa.c.getMse2DtoMse3DinWorld()
    public myPoint curMseLoc3D;        //pa.c.getMseLoc(pa.sceneOriginVals[pa.sceneIDX])

    //private child-class flags
    public static final int 
            sphereSelIDX = 0,            //sphere has been clicked on and fixed for input
            showTrajIDX =  1;            //show drawn trajectories
    public static final int numPrivFlags = 2;
    
        
    private float sepDist,zMin, zMax, zStep;
    private int numTunnelCircles; 
    private myVector[] tunnelCrcls;
    
    private float[] orbitRads;                //radii of rings showing planets
    //private myVector ctrVec;
    //private myVector[] sphereOrbits;
    //private float[] angles,angleIncr;                    //angle for each radius
    //focus location of sphere UI 
    public static final myPoint fcsCtr = new myPoint(396,396,1190);
    
    public mySphereWindow(SeqVisFFTOcean _p, String _n, int _flagIdx, int[] fc, int[] sc, float[] rd, float[] rdClosed,String _winTxt, boolean _canDrawTraj) {
        super(_p, _n, _flagIdx, fc, sc, rd, rdClosed, _winTxt, _canDrawTraj);
        float stY = rectDim[1]+rectDim[3]-4*txtHeightOff,stYFlags = stY + 2*txtHeightOff;
        //setUIClkCoords(rectDim[0] + .1 * rectDim[2],stY,rectDim[0] + rectDim[2],stY + yOff);
        curSelSphere = "";    
        trajFillClrCnst = SeqVisFFTOcean.gui_DarkCyan;        //override this in the ctor of the instancing window class
        trajStrkClrCnst = SeqVisFFTOcean.gui_Cyan;
        super.initThisWin(_canDrawTraj, true);
    }
    
    @Override
    //initialize all private-flag based UI buttons here - called by base class
    public void initAllUIButtons(){
        truePrivFlagNames = new String[]{                                //needs to be in order of flags
                "Showing Drawn Tajectories"
        };
        falsePrivFlagNames = new String[]{            //needs to be in order of flags
                "Hiding Drawn Tajectories"
        };
        privModFlgIdxs = new int[]{showTrajIDX};
        numClickBools = privModFlgIdxs.length;    
        //maybe have call for         initPrivBtnRects(0);         here
        initPrivBtnRects(0,numClickBools);
    }
    
    @Override
    protected void initMe() {    
        dispFlags[plays] = true;                                //this window responds to travelling reticle/playing
        vsblStLoc = new float[]{0,0};
        dispFlags[trajDecays] = true;                                //this window responds to travelling reticle/playing
        //curTrajAraIDX = 0;
        
        initPrivFlags(numPrivFlags);
    }
    
    @Override
    //set flag values and execute special functionality for this sequencer
    public void setPrivFlags(int idx, boolean val){
        privFlags[idx] = val;
        switch(idx){
            case sphereSelIDX : { 
                if(val){    
                    //curTrajAraIDX = 
                } else {
                    
                }
                break;}                //a sphere has been selected and either fixed or released
            case showTrajIDX : {
                break;
            }
        }        
    }//setPrivFlags        
    
    //initialize structure to hold modifiable menu regions
    @Override
    protected void setupGUIObjsAras(){    
        //pa.outStr2Scr("setupGUIObjsAras in :"+ name);
        guiMinMaxModVals = new double [][]{{}};                    //min max mod values for each modifiable UI comp        
        guiStVals = new double[]{};                                //starting value
        guiObjNames = new String[]{};                            //name/label of component        
        //idx 0 is treat as int, idx 1 is obj has list vals, idx 2 is object gets sent to windows
        guiBoolVals = new boolean [][]{{}};                        //per-object  list of boolean flags
        
        //since horizontal row of UI comps, uiClkCoords[2] will be set in buildGUIObjs        
        guiObjs_Numeric = new myGUIObj[numGUIObjs];            //list of modifiable gui objects
        if(numGUIObjs > 0){
            buildGUIObjs(guiObjNames,guiStVals,guiMinMaxModVals,guiBoolVals,new double[]{xOff,txtHeightOff});            //builds a horizontal list of UI comps
        }
    }
    @Override
    protected void setUIWinVals(int UIidx) {
        switch(UIidx){
        default : {break;}
        }
    }
    //if any ui values have a string behind them for display
    @Override
    protected String getUIListValStr(int UIidx, int validx) {
        switch(UIidx){
            default : {break;}
        }
        return "";
    }
    //init any extra ui objs
    @Override
    protected void initXtraUIObjsIndiv() {}

    public final myVector ctrVec = new myVector(400, 400,0);
    //once score instruments have been set, this function is called
    //initSphInstrGraphics(float _rad, myPoint _ctr, PImage _txtr, int[] specClrInt, int[] ambClrInt, int[] emmClrInt)
    @Override
    public void setScoreInstrValsIndiv(){
        int cnt = 0, numBalls = instrs.size(), numCols = (int)(Math.sqrt(numBalls-1))+1, numRows = (int)(numBalls/numCols);
        initPrivFlags(numPrivFlags);
        sphereCntls = new TreeMap<String, mySphereCntl>();
        float offset = pa.sphereRad * 4.5f;
        int[][] clrAra = new int[][]{ 
                new int[]{255,255,255},
                new int[]{100,100,100},
                new int[]{0,0,0}
            };
        //set sphere detail before building instruments
        pa.sphereDetail(50);
        //myPoint ctr;
        int orbitCnt = (instrs.size()/4) + 1;        //how  many per orbit
        orbitRads = new float[orbitCnt];            //radius of orbit
        for(int i =0; i<orbitCnt;++i){orbitRads[i] = (i+1) * offset * .9f;}
        int numPerOrbit = 3,                    //starts with 3 in the ring, then up to 4, then up to 5, etc.  add offset to each ring radius
            ringRadIDX = 0,    perRingCnt = 0;
        float rotAngle = 0, angleDelta = MyMathUtils.TWO_PI_F/(1.0f*numPerOrbit);
        for(int i =0; i<instrs.size(); ++i){        //for every instrument
            if(numPerOrbit == perRingCnt){
                perRingCnt = 0;
                numPerOrbit++;
                rotAngle = pa.random(1.0f)*MyMathUtils.TWO_PI_F;
                ringRadIDX++;
                angleDelta = MyMathUtils.TWO_PI_F/(1.0f*numPerOrbit);
//                mult *= -1;
            }
            myInstrument entry = instrs.get(scoreStaffNames[i]);
            //for every instrument build a new mySphereCntl and give it appropriate values
            clrAra[2] = pa.getRndClrBright(255);
            //int idx = (int)( pa.random(cnt + 5)-5);
            //ctr =  new myPoint(((int)(cnt % numCols))*offset,((int)(cnt/numCols))*offset,0); 
            //ctr =  new myPoint(ctrVec.x+(orbitRads[ringRadIDX] * pa.sin(rotAngle)), ctrVec.y+(orbitRads[ringRadIDX] * pa.cos(rotAngle)),ctrVec.z);
            //mySphereCntl tmpSph = new mySphereCntl(pa, this, entry, scoreStaffNames[i], pa.sphereRad, ctr, orbitRads[ringRadIDX], rotAngle, 
            mySphereCntl tmpSph = new mySphereCntl(pa, this, entry, scoreStaffNames[i], pa.sphereRad, orbitRads[ringRadIDX], rotAngle, 
                    ringRadIDX,
                    pa.sphereImgs[cnt%pa.sphereImgs.length],//,pa.sphereImgs[(idx +1 + pa.sphereImgs.length)%pa.sphereImgs.length],pa.sphereImgs[(idx +2 + pa.sphereImgs.length)%pa.sphereImgs.length]},
                    clrAra);
            if(scoreStaffNames[i].toLowerCase().contains("drums")){
                //pa.outStr2Scr("Drums are set to be a drumkit");
                tmpSph.setFlag(mySphereCntl.isDrumKitIDX, true);
                //tmpSph.isDrumKit=true;
            }
            sphereCntls.put(tmpSph.name, tmpSph);
            cnt++;
            perRingCnt++;
            rotAngle += angleDelta;
        }
        sepDist = 20;
        zMin = -2250;
        zMax = 150;
        zStep = 5.1f;
        numTunnelCircles = (int)((zMax - zMin) / sepDist); 
        tunnelCrcls = new myVector[numTunnelCircles]; 
        for (int i = 0; i < numTunnelCircles; i++) { tunnelCrcls[i] = new myVector(0, 0, PApplet.map(i, 0, numTunnelCircles - 1, zMax, zMin));} 
                
    }
    
    //static int plIterStatic = 0, stIterStatic;
    //place holder to stop notes
    public void addSphereNoteToStopNow(myInstrument instr, SortedMap<Integer,ArrayList<myNote>> tmpNotes){
        //pa.outStr2Scr("Iter : " + plIterStatic++ + " Play ");
        int retCode = instr.addSphNotesToStop(tmpNotes);
        
    }//addSphereNoteToStopNow
    //static int iterStatic = 0;
    public void addSphereNoteToPlayNow(myInstrument instr, SortedMap<Integer, myNote> tmpNotes, float durMult, int stTime){
        //pa.outStr2Scr("Iter : " + stIterStatic++ + " Stop ");
        int retCode = instr.addSphNotesToPlay(tmpNotes,stTime);
        this.dispFlags[Base_DispWindow.notesLoaded] = true;
    }//addTrajNoteToPlay
//    static int iterStatic = 0;
//    protected void addSphereNoteToPlayNow(SortedMap<Integer, myNote> tmpNotes, float durMult){
//        if((null == tmpNotes) || (tmpNotes.size() == 0)){return;}
//        float noteDur, noteSt;
//        for(SortedMap.Entry<Integer, myNote> note : tmpNotes.entrySet()) {     
//            if(note.getValue().n.name == nValType.rest){continue;}
//            myNote _n = note.getValue();
//            noteSt =  0;
//            noteDur = _n.sphereDur * durMult;
//            pa.outStr2Scr("Iter : " + iterStatic + " Play note : "+ _n.n.nameOct + " start : "+ noteSt + " dur: " + noteDur);
//            playNote(pa.glblOut, noteSt,noteDur, _n.n.freq);
//            if(_n.flags[myNote.isChord]){
//                for(Entry<String, myNote> cnote : ((myChord)(_n)).cnotes.entrySet()){
//                    myNote chrdN = cnote.getValue();
//                    if(_n.ID != chrdN.ID){
//                        noteSt = 0;        //now
//                        noteDur = chrdN.sphereDur * durMult;
//                        //pa.outStr2Scr("Play note of chord : "+ chrdN.n.nameOct + " start : "+ noteSt + " dur: " + noteDur);
//                        playNote(pa.glblOut, noteSt, noteDur, chrdN.n.freq);
//                    }
//                }
//            }
//        }
//        iterStatic++;
//        this.dispFlags[Base_DispWindow.notesLoaded] = true;
//    }//addSphereNoteToPlayNow    
    
    public void setLights(){
        pa.ambientLight(102, 102, 102);
        pa.lightSpecular(204, 204, 204);
        pa.directionalLight(111, 111, 111, 0, 1, -1);    
    }
    
    //overrides function in base class mseClkDisp
    @Override
    public void drawTraj3D(float animTimeMod,myPoint trans){
        //pa.outStr2Scr("mySphereUI.drawTraj3D() : I am overridden in 3d instancing class", true);
        if(!((privFlags[sphereSelIDX]) && (curSelSphere!=""))){return;}
        pa.pushMatrix();pa.pushStyle();    
        pa.translate(0,0,1);
        mySphereCntl tmp = sphereCntls.get(curSelSphere);
        if(null != tmpDrawnTraj){tmp.drawTrajPts(tmpDrawnTraj, animTimeMod);}
        if(privFlags[showTrajIDX]){
        TreeMap<String,ArrayList<myDrawnSmplTraj>> tmpTreeMap = drwnTrajMap.get(curDrnTrajScrIDX);
            if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
                //pa.outStr2Scr("MATCh : cur sel : " + curSelSphere + " cur instr : " + instrs.get(getTrajAraKeyStr(i)).instrName + " key : " + getTrajAraKeyStr(i));
                ArrayList<myDrawnSmplTraj> tmpAra = tmpTreeMap.get(curSelSphere);            
                if(null!=tmpAra){    
                    for(int j =0; j<tmpAra.size();++j){tmp.drawTrajPts(tmpAra.get(j),animTimeMod);}
                }    
            }
        }
        pa.popStyle();pa.popMatrix();        
    }//drawTraj3D
    
    //draw tardis tunnel skeeeeeoooooooooo woooeeeooooo weeeoooeeeeee weeeoooo oooo weee oo ooooooo eee ooooo
    private static final float tcYConst = - 3325.0f/ 650.0f;    //precalc constant disp val
    public void drawTunnel(){
        pa.pushMatrix();pa.pushStyle();
        pa.noFill();
        pa.stroke(255,255,255,255);
        pa.strokeWeight(1);
        float fc = (float)pa.frameCount, a, tcScl,r; 
        float tunnelRad = pa.sphereRad * 150;
        for (int i = 0; i < numTunnelCircles; i++) { 
            tunnelCrcls[i].z += zStep; 
            tcScl = PApplet.map((float)(tunnelCrcls[i].z), zMin, zMax, 6,0) * .25f;
            r = PApplet.map((float)(tunnelCrcls[i].z), zMin, zMax, tunnelRad*.01f, tunnelRad); 
            a = PApplet.map((float)(tunnelCrcls[i].z), zMin, zMax, 111,255); 
            tunnelCrcls[i].x = (pa.noise((float)((fc - tunnelCrcls[i].z) / 650.0f) - .5f) * rectDim[2] * tcScl); 
            tunnelCrcls[i].y = (pa.noise((float)((fc + tunnelCrcls[i].z) / 650.0f) - tcYConst) * rectDim[3]* tcScl); 
            pa.stroke(0,a*.5f,a, a); 
            pa.strokeWeight(2.0f);
            pa.pushMatrix(); 
            pa.translate(tunnelCrcls[i].x, tunnelCrcls[i].y, tunnelCrcls[i].z); 
            pa.ellipse(0, 0, r, r); 
            pa.popMatrix(); 
            if (tunnelCrcls[i].z > zMax) {                tunnelCrcls[i].z = zMin;            } 
        } 
        pa.popStyle();pa.popMatrix();        
    }//drawTunnel

    @Override
    protected void drawMe(float animTimeMod) {
        pa.background(0,15,55,255);
        drawTunnel();
        curMseLookVec = pa.getMse2DtoMse3DinWorld(pa.sceneOriginVals[pa.sceneIDX]);            //needs to be here - used for determination of whether a sphere is hit or not
        curMseLoc3D = pa.getMseLoc(pa.sceneOriginVals[pa.sceneIDX]);
        //pa.outStr2Scr("Current mouse loc in 3D : " + curMseLoc3D.toStrBrf() + "| scenectrvals : " + pa.sceneOriginVals[pa.sceneIDX].toStrBrf() +"| current look-at vector from mouse point : " + curMseLookVec.toStrBrf());
        pa.pushMatrix();pa.pushStyle();
        pa.noLights();
        setLights();
        drawSpheres(animTimeMod);
        pa.noLights();
        pa.lights();
        pa.popStyle();pa.popMatrix();
        if(pa.flags[pa.playMusic]){playAllNotes();}                //need to build better mechanism to be draw-driven instead of 1-shot for play
    }

    public void playAllNotes(){
         SortedMap<Integer, myNote> res = new TreeMap<Integer, myNote> (), tmp;
        // pa.glblOut.pauseNotes();
         for(mySphereCntl cntl : sphereCntls.values()){
             if(cntl.notes.size() != 0){cntl.sendNotesToPlayAndStop();}             
         }         
//         for(mySphereCntl cntl : sphereCntls.values()){
//             if(cntl.notes.size() != 0){cntl.sendNotesToStop();}             
//         }         
    
    //     pa.glblOut.resumeNotes();
    }
    
    private void drawSpheres(float animTimeMod){    
        //pa.outStr2Scr("anim time mod:"  + animTimeMod);//varies between .01 and .02, mostly .01-.015 - driven by proc time of other functionality, to keep animations smooth
        if(privFlags[sphereSelIDX]){    //a sphere has been fixed for input
            for(mySphereCntl cntl : sphereCntls.values()){
                boolean isCurSelSphere = curSelSphere.equals(cntl.name);
                cntl.drawMe(animTimeMod,isCurSelSphere ? focusZoom : 0, isCurSelSphere);
            }        
        } else {            //no sphere is selected or a selected sphere has been deselected
            myPoint rayPt = pa.P(curMseLoc3D);
            for(mySphereCntl cntl : sphereCntls.values()){                    //minimize collision checks
                if(curSelSphere == "") {                                    //no focus sphere, check if this is one
                    double hit = cntl.hitMe(rayPt, curMseLookVec);
                    if(hit >=0 ){
                        cntl.drawMe(animTimeMod, 0, true);
                        curSelSphere = cntl.name;
                        curTrajAraIDX = getTrajAraIDXVal(curSelSphere);
                    } else {
                        cntl.drawMe(animTimeMod, 0, false);
                    }
                } else if (curSelSphere.equals(cntl.name)){                    //this one is focus sphere, check if still focus
                    //check if still focus sphere
                    double hit = cntl.hitMe(rayPt, curMseLookVec);
                    //pa.outStr2Scr("hit for cntl : " + cntl.ID + " == " + hit);
                    if(hit >=0 ){//still focus sphere
                        cntl.drawMe(animTimeMod, 0, true);
                    } else {//no longer hit, not focus sphere anymore
                        cntl.drawMe(animTimeMod, 0, false);
                        curSelSphere = "";
                        curTrajAraIDX = -1;
                    }
                } else {                                                    //there is a focus sphere and this isn't it, draw normal
                    cntl.drawMe(animTimeMod, 0, false);    
                }
                
            }
        }        
    }
    //- music is told to start
    @Override
    protected void playMe() {    }//only called 1 time
    //when music stops
    @Override
    protected void stopMe() {
        for(mySphereCntl cntl : sphereCntls.values()){            cntl.stopAllNotes();        }
    }    
    //move current play position when playing mp3/sample (i.e. something not controlled by pbe reticle
    @Override
    protected void modMySongLoc(float modAmt) {
        
    };

    
    @Override
    //gets current notes for simulation visualization - from last poll time to now - each sphere governs time using radar sweep
    protected SortedMap<Integer, myNote> getNotesNow() {
         SortedMap<Integer, myNote> res = new TreeMap<Integer, myNote> (), tmp;
         for(mySphereCntl cntl : sphereCntls.values()){
//             tmp = cntl.getNotesNow();
//             res.putAll(tmp);
             
         }              
         return res;
    }//getNotesNow
    
    //print out all trajectories for current sphere
    public void dbgShowTrajLocs(){
        if(null != tmpDrawnTraj){    tmpDrawnTraj.dbgPrintAllPoints();}
        TreeMap<String,ArrayList<myDrawnSmplTraj>> tmpTreeMap = drwnTrajMap.get(curDrnTrajScrIDX);
        if((tmpTreeMap != null) && (tmpTreeMap.size() != 0)) {
            pa.outStr2Scr("cur sel sphere: " + curSelSphere);
            ArrayList<myDrawnSmplTraj> tmpAra = tmpTreeMap.get(curSelSphere);            
            if(null!=tmpAra){    for(int j =0; j<tmpAra.size();++j){tmpAra.get(j).dbgPrintAllPoints();}    }    
        }        
    }
    
    public void dbgNoteDispVals(){        
        for(Map.Entry<Integer,myNote> noteVal : sphereCntls.get(curSelSphere).notes.entrySet()){
            pa.outStr2Scr(noteVal.getValue().toString());
        }
    }
    
    @Override
    public void clickDebug(int btnNum){
        pa.outStr2Scr("click debug in "+name+" : btn : " + btnNum);
        switch(btnNum){
            case 0 : {//note vals
                dbgShowTrajLocs();
                break;
            }
            case 1 : {//measure vals
                dbgNoteDispVals();                
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
    protected void processTrajIndiv(myDrawnSmplTraj drawnNoteTraj){
        pa.outStr2Scr("Process traj in sphere ui");
        sphereCntls.get(curSelSphere).processTrajIndiv(drawnNoteTraj);
        //traj processing
    }
    @Override
    protected myPoint getMouseLoc3D(int mouseX, int mouseY) {
        if((privFlags[sphereSelIDX]) && (curSelSphere!="")){
            return sphereCntls.get(curSelSphere).getMouseLoc3D(mouseX, mouseY);            
        } else {
            return pa.P(-100000,10000,100000);            //dummy point - remove this crap eventually when handle trajs correctly
        }
    }//getMouseLoc3D
    @Override
    protected boolean hndlMouseMoveIndiv(int mouseX, int mouseY, myPoint mseClckInWorld){
        return false;
    }
    //alt key pressed handles trajectory
    //cntl key pressed handles unfocus of spherey
    @Override
    protected boolean hndlMouseClickIndiv(int mouseX, int mouseY, myPoint mseClckInWorld, int mseBtn) {
        boolean res = checkUIButtons(mouseX, mouseY);
        if(res) {return res;}
        //pa.outStr2Scr("sphere ui click in world : " + mseClckInWorld.toStrBrf());
        if((!privFlags[sphereSelIDX]) && (curSelSphere!="")){            //set flags to fix sphere
            res = true;
            setPrivFlags(sphereSelIDX,true);            
        } else if((privFlags[sphereSelIDX]) && (curSelSphere!="")){
            if(pa.flags[pa.cntlKeyPressed]){            //cntl+click to deselect a sphere        
                setPrivFlags(sphereSelIDX,false);
                curSelSphere = ""; 
                res = true;
            } else {                                    //pass click through to selected sphere
                res = sphereCntls.get(curSelSphere).hndlMouseClickIndiv(mouseX, mouseY, mseClckInWorld,curMseLookVec);                
            }
        }
        return res;
    }//hndlMouseClickIndiv

    @Override
    protected boolean hndlMouseDragIndiv(int mouseX, int mouseY, int pmouseX, int pmouseY, myPoint mouseClickIn3D, myVector mseDragInWorld, int mseBtn) {
        boolean res = false;
        //pa.outStr2Scr("hndlMouseDragIndiv sphere ui drag in world mouseClickIn3D : " + mouseClickIn3D.toStrBrf() + " mseDragInWorld : " + mseDragInWorld.toStrBrf());
        if((privFlags[sphereSelIDX]) && (curSelSphere!="")){//pass drag through to selected sphere
            //pa.outStr2Scr("sphere ui drag in world mouseClickIn3D : " + mouseClickIn3D.toStrBrf() + " mseDragInWorld : " + mseDragInWorld.toStrBrf());
            res = sphereCntls.get(curSelSphere).hndlMouseDragIndiv(mouseX, mouseY, pmouseX, pmouseY, mouseClickIn3D,curMseLookVec, mseDragInWorld);
        }
        return res;
    }

    @Override
    //set gobal key signature - for score, set it at nearest measure boundary
    protected void setGlobalKeySigValIndiv(int idx, float time){    }//setCurrentKeySigVal
    @Override
    //set time signature at time passed - for score, set it at nearest measure boundary
    protected void setGlobalTimeSigValIndiv(int tsnum, int tsdenom, noteDurType _d, float time){    }//setCurrentTimeSigVal
    @Override
    //set time signature at time passed - for score, set it at nearest measure boundary
    protected void setGlobalTempoValIndiv(float tempo, float time){    }//setCurrentTimeSigVal
    @Override
    //set local at-time key signature, at time passed - for score, set it at nearest measure boundary
    protected void setLocalKeySigValIndiv(myKeySig lclKeySig, ArrayList<noteValType> lclKeyNotesAra, float time){}
    @Override
    //set time signature at time passed - for score, set it at nearest measure boundary
    protected void setLocalTimeSigValIndiv(int tsnum, int tsdenom, noteDurType _beatNoteType, float time){}
    @Override
    //set time signature at time passed - for score, set it at nearest measure boundary
    protected void setLocalTempoValIndiv(float tempo, float time){}
    
    @Override
    protected void snapMouseLocs(int oldMouseX, int oldMouseY, int[] newMouseLoc) {}    

    @Override
    protected void hndlMouseRelIndiv() {}
    @Override
    protected void endShiftKey_Indiv() {}
    @Override
    protected void endAltKey_Indiv() {}
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
    protected void resizeMe(float scale) {}
    @Override
    protected void closeMe() {}
    @Override
    protected void showMe() {}
}
