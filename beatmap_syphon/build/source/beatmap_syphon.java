import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import oscP5.*; 
import netP5.*; 
import themidibus.*; 
import controlP5.*; 
import codeanticode.syphon.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class beatmap_syphon extends PApplet {

//oscP5


OscP5 oscP5;
NetAddress other;

//midi output

MidiBus midi;

//slider

ControlP5 cp5;

//syphon

SyphonServer server;

//constant
int _bk = color (50, 50, 50);
int _gbk = color (80, 80, 80);
int _blink = color (142, 68, 173);
int _normal = color (255, 255, 255);
int _active = color (82, 179, 217);

final int TIMES = 0;
final int VELOCITY = 1;
final int PITCH = 2;
final int OCTAVE = 3;
final int ROTATE = 0;
final int RANDOM = 1;
final int NOFMAPS = 1;

int[] _colorOfTabs = {
  color (27, 163, 156),
  color (246, 36, 89),
  color (249, 191, 59),
  color (135, 211, 124),
};
int[] _colorOfStabs = {
  color (211, 84, 0),
  color (137, 196, 244),
  color (239, 72, 54),
  color (220, 198, 224),
};

//state
boolean activating = false;
boolean addingTimeNode = false;

int scl = 40;
int margin = scl / 2;
int gap = 50;
int nOfc = 21;
int len = nOfc * scl + margin * 2;

// int[] otDefault = {
//   0, 0, 0, 0, 0, 1,
//   1, 2, 2, 2, 2, 2,
//   0, 0, 0, 0, 0, 1,
//   1, 2, 2, 2, 2, 2,
//   0, 0, 0, 0, 0, 1,
//   1, 2, 2, 2, 2, 2,
// };
int[] otDefault = {
  0, 0, 0, 1,
  1, 2, 2, 2,
  0, 0, 0, 1,
  1, 2, 2, 2,
};
int[] midiNotes = {
  36,
  38,
  44,
  39,
  45,
  49,
};
int[] channels = {
  1,
  1,
  1,
  2,
  2,
  3,
};

ArrayList<Map> maps;

PFont pfont;
ControlFont font;

public void settings() {
  size(900, 900, P3D);
  PJOGL.profile=1;
}

public void setup() {

  // size(800, 550);
  background(_bk);

  //oscP5
  oscP5 = new OscP5(this, 12000);
  other = new NetAddress("127.0.0.1", 12002);

  //midi
  MidiBus.list();
  midi = new MidiBus(this, "Virtual MIDI Port", "p5 Port");
  // midi = new MidiBus(this, 0, 2);

  //controlP5
  cp5 = new ControlP5(this);

  // Create syhpon server to send frames out.
  server = new SyphonServer(this, "Processing Syphon");

  //font
  pfont = createFont("Arial",12,true);
  font = new ControlFont(pfont,8);
  textFont(pfont);

  //maps
  maps = new ArrayList<Map>();
  maps.add(new Map( 0, 0, 0));

}
public void draw() {
  background(_bk);

  for (int i = 0, n = maps.size(); i < n; i++) {
    Map map = maps.get(i);
    map.mouseSensed(mouseX, mouseY);
    map.update();
    map.display();
  }

  // showfr();
}

public void mousePressed() {
  for (int i = 0, n = maps.size(); i < n; i++) {
    Map map = maps.get(i);
    map.mousePressed();
  }
}
public void keyPressed() {
  if (key == 't') {
    activating = true;
  }
  if (key == 'r') {
    addingTimeNode = true;
  }

}
public void keyReleased() {
  if (key == 't') {
    activating = false;
  }
  if (key == 'r') {
    addingTimeNode = false;
  }
}

int beat = 0;
public void rawMidi(byte[] data) { // You can also use rawMidi(byte[] data, String bus_name)
  // println("clock(" + (int)(data[0] & 0xFF) + ")");
  if ((int)(data[0] & 0xFF) == 248) {
    if (beat % 24 == 0) {
      for (int i = 0, n = maps.size(); i < n; i++) {
        maps.get(i).toNext();
      }
    }
    else {
      for (int i = 0, n = maps.size(); i < n; i++) {
        maps.get(i).sendClock(beat);
      }
    }
    beat = (beat + 1);
  }
}
public void showfr() {
  fill(255);
  text( "frameRate: " + str(frameRate),10, 20);
}
class Map {
  int id;
  PGraphics canvas;
  Node[][] nodes;
  float xpos, ypos;
  Metro metro;
  int timeUnit = 200;
  int xx, yy; // TODO delete
  ArrayList<TimeNode> timeNodes;
  int beat;

  float mX, mY;

  Slider sliderOfChannel;
  int[] pitchStep = {
    36, 37, 39, 41, 43, 44, 46,
    48, 49, 51, 53, 55, 56, 58,
    60, 61, 63, 65, 67, 68, 70,
  };

  //state
  boolean mouseOver = false;

  //sense, pressed, display
  Tab[] tabs;
  SideTab[] stabs;
  int nOfTabs = 4;
  int nOfStabs = 4;

  public void init(int _i, float _x, float _y) {
    id = _i;
    timeNodes = new ArrayList<TimeNode>();
    timeNodes.add(new TimeNode(this, 0, 0));
    xpos = _x;
    ypos = _y;
    canvas = createGraphics(len, len, P3D);
    metro = new Metro(true, timeUnit);
    beat = metro.frameCount();
    nodes = new Node[nOfc][nOfc];

    for(int i = 0; i < nOfc; i++) {
      for(int j = 0; j < nOfc; j++) {
        nodes[i][j] = new Node(this, i, j);
        nodes[i][j].setOt(0);
      }
    }

    //tabs
    tabs = new Tab[nOfTabs];
    for (int i = 0; i < nOfTabs; i++) {
      tabs[i] = new Tab(this, i, _colorOfTabs[i]);
    }
    stabs = new SideTab[nOfStabs];
    for (int i = 0; i < nOfStabs; i++) {
      stabs[i] = new SideTab(this, i, _colorOfStabs[i]);
    }


    //slider for channel
    sliderOfChannel =
      cp5.addSlider("ch" + str(id))
      .setPosition(xpos, ypos + len + scl / 8)
      .setSize(100,10)
      .setRange(1,16)
      .setNumberOfTickMarks(16)
      .showTickMarks(false)
      .setCaptionLabel("midi ch")
      .setColorBackground(_gbk)
    ;
    sliderOfChannel
      .getCaptionLabel()
      .setFont(font)
    ;

  }
  Map(int _i, float _x, float _y) {
    init(_i, _x, _y);
  }


  public void update() {
    for(int i = 0; i < nOfc; i++) {
      for(int j = 0; j < nOfc; j++) {
        nodes[i][j].update();
      }
    }
  }
  public void toNext() {
    for (int i = 0, n = timeNodes.size(); i < n; i++) {
      timeNodes.get(i).toNext();
    }
  }
  public void sendClock(int b) {
    for(int i = 0; i < nOfc; i++) {
      for(int j = 0; j < nOfc; j++) {
        nodes[i][j].sendClock(b);
      }
    }
  }
  public void display() {
    canvas.beginDraw();
    backgroundDisplay();
    gridDisplay();
    nodesDisplay();
    controlPanelDisplay();

    canvas.endDraw();
    image(canvas, xpos, ypos);
    server.sendImage(canvas);
  }

  public void backgroundDisplay() {
    // canvas.background(_gbk);
    canvas.noStroke();
    canvas.fill(_gbk);
    canvas.rect(0, 0, len, len, scl / 2);
  }
  public void gridDisplay() {
    for (int i = 0; i <= nOfc; i++) {
      float p = margin + map(i, 0, nOfc, 0, nOfc * scl);
      canvas.stroke(255, 30);
      canvas.line(p, 0, p, len);
      canvas.line(0, p, len, p);
    }
  }
  public void nodesDisplay() {
    for(int i = 0; i < nOfc; i++) {
      for(int j = 0; j < nOfc; j++) {
        nodes[i][j].display();
      }
    }
  }
  public void controlPanelDisplay() {
    for (int i = 0; i < nOfTabs; i++) {
      tabs[i].display();
    }
    for (int i = 0; i < nOfStabs; i++) {
      stabs[i].display();
    }
  }
  public void mouseSensed(float _mX, float _mY) {
    mX = _mX - xpos;
    mY = _mY - ypos;

    if ( contain(mX, mY)) {
      mouseOver = true;
      int c = floor((mX - margin)/ PApplet.parseFloat(scl));
      int r = floor((mY - margin)/ PApplet.parseFloat(scl));
      for (int i = 0; i < nOfTabs; i++) {
        tabs[i].mouseOver = (c == i && r == -1);
      }
      for (int i = 0; i < nOfStabs; i++) {
        stabs[i].mouseOver = (c == -1 && r == i);
      }
    }
    else {
      mouseOver = false;
      for (int i = 0; i < nOfTabs; i++) {
        tabs[i].mouseOver = false;
      }
      for (int i = 0; i < nOfStabs; i++) {
        stabs[i].mouseOver = false;
      }
    }
  }
  public void mousePressed() {
    int c = floor((mX - margin)/ PApplet.parseFloat(scl));
    int r = floor((mY - margin)/ PApplet.parseFloat(scl));
    if ( inGrids(mX, mY) ) {
      Node node = nodes[c][r];
      if (activating) {
        node.activate();
      }
      else if (addingTimeNode) {
        timeNodes.add(new TimeNode(this, c, r));
      }
      else if (tabs[TIMES].active) {
        node.setTiming();
      }
      else if (tabs[VELOCITY].active) {
        node.setVelocity();
      }
      else if (tabs[PITCH].active) {
        node.setPitch();
      }
      else if (tabs[OCTAVE].active) {
        node.setOct();
      }
      else {
        node.rotateClockwise();
      }
    }
    else if (contain(mX, mY)) {
      //tabs in upper bar
      if (r == -1){
        for (int i = 0; i < nOfTabs; i++) {
          if (c != i) {
            tabs[i].deactivate();
          }
          else {
            tabs[i].trigger();
          }
        }
      }
      //tabs in side bar
      if (c == -1) {
        if (r == ROTATE) {
          stabs[ROTATE].trigger();
        }
        if (r == RANDOM) {

          if (tabs[TIMES].active) { randomizeTimes(); }
          else if (tabs[VELOCITY].active) { randomizeVelocity(); }
          else if (tabs[PITCH].active){ randomizePitches(); }
          else { randomizeOt(); }
          //TODO other random function
        }
      }
    }
  }

  public void randomizeOt() {
    for(int i = 0; i < nOfc; i++) {
      for(int j = 0; j < nOfc; j++) {
        nodes[i][j].randomizeOt();
      }
    }
  }
  public void randomizeTimes() {
    for(int i = 0; i < nOfc; i++) {
      for(int j = 0; j < nOfc; j++) {
        nodes[i][j].setTiming(floor(random(4)));
      }
    }
  }
  public void randomizeVelocity() {
    for(int i = 0; i < nOfc; i++) {
      for(int j = 0; j < nOfc; j++) {
        nodes[i][j].setVelocity(floor(random(7)));
      }
    }
  }
  public void randomizePitches() {
    for(int i = 0; i < nOfc; i++) {
      for(int j = 0; j < nOfc; j++) {
        nodes[i][j].setPitch(floor(random(pitchStep.length)));
      }
    }
  }

  public boolean contain(float x, float y) {
    return (x > 0)&&(x < len )&&(y > 0)&&(y < len);
  }
  public boolean inGrids(float x, float y) {
    return (x > margin)&&(x < len - margin)&&(y > margin)&&(y < len - margin);
  }
}
class Metro {
  boolean state;
  int elapsedTime;
  int localtime;
  int limit;
  int timeInterval;

  Metro(boolean ss, int ll) {
    state = ss;
    limit=ll;
    timeInterval=0;
    if(state) {
      localtime = currentTime();
    }
  }

  public int frameCount() {
    if(state) {
      return (millis() - localtime)/limit;
    }
    else {
      return timeInterval/limit;
    }
  }
  public void startPlayingAt( int fc) {
    if(state)
      stopAndReset();
    state = true;
    localtime = currentTime() - fc * limit;
  }
  public void pause() {
    if(state) {
      state = false;
      timeInterval = millis() - localtime;
    }
  }
  public void stopAndReset() {
    state = false;
    timeInterval = 0;
  }
  public float framerate() {
    return (1000.0f/limit);
  }
  public int currentTime() {
    return millis();
  }
  public void setLimit(int l) {
    pause();
    int fc = frameCount();
    limit = l;
    localtime = currentTime() - fc * limit;
    startPlayingAt(fc);
  }
  // void setTime (int fCount) { //input is the frame count
  //   timeInterval = fCount * limit;
  // }

  // void startPlaying() { // or resume from pause
  //   if(!state) {
  //     state = true;
  //     localtime = currentTime() - timeInterval;
  //   }
  // }
  // boolean bang() {
  //   if (state == true) {
  //     elapsedTime = (millis() - localtime)%limit;
  //     if (elapsedTime<=5) {
  //       return true;
  //     }
  //   }
  //   return false;
  // }
  // void adjustSpeed(float ratio) {
  //   pause();
  //   int fc = frameCount();
  //   limit /= ratio;
  //   localtime = currentTime() - fc * limit;
  //   startPlayingAt(fc);
  // }

  // float step = 1;
  // void speedUp() {
  //   pause();
  //   int fc = frameCount();
  //   float fr = 1000.0/limit;
  //   if (limit > step) {
  //     limit -= step;
  //     localtime = currentTime() - fc * limit;
  //   }
  //   startPlayingAt(fc);
  // }
  // void speedDown() {
  //   pause();
  //   int fc = frameCount();
  //   float fr = 1000.0/limit;
  //   limit += step;
  //   localtime = currentTime() - fc * limit;
  //   startPlayingAt(fc);
  // }

}
class Node {
  Map map;

  PGraphics canvas;

  //position and orientation
  TimeLine timerOfColor;
  TimeLine timerOfDisplay;

  float angle;
  int ot;
  float unitOfAngle = PI / 2;
  float rotateRate = 0.2f;
  int xpos, ypos;

  float mainAlpha = 255;
  float fadeRate = 0.1f;

  //TIMES
  int nOftiming = 1;
  int timingCount = 0;

  //VELOCITY
  int vel = 1; //64 per unit

  //PITCH
  boolean fixedPitch = false;
  int pitch;

  //OCTAVE
  int oct;

  //state
  boolean active = false;
  boolean triggering = false;

  public void init(Map _m, int _x, int _y) {
    map = _m;
    pitch = 2;
    canvas = map.canvas;
    xpos = _x;
    ypos = _y;
    angle = 0;
    ot = 0;

    timerOfColor = new TimeLine(300);
    timerOfColor.setLinerRate(2);
    timerOfColor.set1();

    timerOfDisplay = new TimeLine(300);
    timerOfDisplay.setLinerRate(2);
    timerOfDisplay.set1();
  }
  Node(Map _m, int _x, int _y) {
    init(_m, _x, _y);
  }
  Node(Map _m, int _x, int _y, int _p) {
    init(_m, _x, _y);
    pitch = _p;
  }

  public void update() {
    float al;
    if (map.tabs[TIMES].active ||
        map.tabs[VELOCITY].active ||
        map.tabs[PITCH].active ||
        map.tabs[OCTAVE].active )
        { al = 0; }
    else { al = 255; }

    angle = angle + rotateRate * (ot * unitOfAngle - angle);
    mainAlpha = mainAlpha + fadeRate * (al - mainAlpha);
  }
  public void display() {
    if (map.tabs[TIMES].active) {
      timingDisplay();
    }
    else if (map.tabs[VELOCITY].active) {
      velocityDisplay();
    }
    else if (map.tabs[PITCH].active) {
      pitchDisplay();
    }
    else if (map.tabs[OCTAVE].active) {
      octaveDisplay();
    }
    shapeDisplay();
    blinkDisplay();
  }
  public void shapeDisplay() {
    canvas.pushMatrix();
    if (active) { canvas.fill(_active, mainAlpha); }
    else { canvas.fill(_normal, mainAlpha); }

    canvas.translate(margin + scl / 2, margin + scl / 2);
    canvas.translate(xpos * scl, ypos * scl);

    canvas.rotate(angle);
    canvas.noStroke();
    canvas.beginShape();
    canvas.vertex(scl/3, 0);
    canvas.vertex(-scl/4, scl/4);
    canvas.vertex(-scl/4, -scl/4);
    canvas.endShape(CLOSE);
    // canvas.stroke(0);
    // canvas.strokeWeight(3);
    // canvas.point(0, 0);
    canvas.popMatrix();
  }
  public void timingDisplay() {
    canvas.pushMatrix();
    if (active) { canvas.fill(_active, 255 - mainAlpha); }
    else { canvas.fill(_normal, 255 - mainAlpha); }

    canvas.translate(margin + scl / 2, margin + scl / 2);
    canvas.translate(xpos * scl, ypos * scl);
    canvas.noStroke();

    switch(nOftiming) {
      case 1 :
        canvas.ellipse(0, 0, scl / 2, scl / 2);
        break;
      case 2 :
        canvas.ellipse(scl / 5, 0, scl / 3, scl / 3);
        canvas.ellipse(-1 * scl / 5, 0, scl / 3, scl / 3);
        break;
      case 3 :
        canvas.ellipse(scl / 5, 0, scl / 4, scl / 4);
        canvas.ellipse(-1 * scl / 5, scl / 5, scl / 4, scl / 4);
        canvas.ellipse(-1 * scl / 5, -1 * scl / 5, scl / 4, scl / 4);
        break;
      case 4 :
        canvas.ellipse(scl / 5, scl / 5, scl / 4, scl / 4);
        canvas.ellipse(scl / 5, -1 * scl / 5, scl / 4, scl / 4);
        canvas.ellipse(-1 * scl / 5, scl / 5, scl / 4, scl / 4);
        canvas.ellipse(-1 * scl / 5, -1 * scl / 5, scl / 4, scl / 4);
        break;
      default :
        canvas.ellipse(0, 0, scl / 2, scl / 2);
        break;
    }

    canvas.popMatrix();
  }
  public void velocityDisplay() {
    canvas.pushMatrix();
    if (active) { canvas.fill(_active, 255 - mainAlpha); }
    else { canvas.fill(_normal, 255 - mainAlpha); }
    canvas.translate(margin + scl / 4, margin + scl);
    canvas.translate(xpos * scl, ypos * scl);
    canvas.noStroke();
    canvas.rect(0, 0, scl / 2, -1 * map(getVelocity(), 0, 255, 0, scl),
                scl / 5, scl / 5, 0, 0);
    canvas.popMatrix();
  }
  public void pitchDisplay() {
    canvas.pushMatrix();
    if (active) { canvas.fill(_active, 255 - mainAlpha); }
    else { canvas.fill(_normal, 255 - mainAlpha); }
    canvas.translate(margin + scl / 4, margin + scl);
    canvas.translate(xpos * scl, ypos * scl);
    canvas.rect(0, 0, scl / 2, -1 * map(pitch, 0, PApplet.parseFloat(map.pitchStep.length), 0, PApplet.parseFloat(scl)));
    canvas.popMatrix();
  }
  public void blinkDisplay() {
    //blink
    canvas.pushMatrix();
    canvas.translate(margin, margin);
    canvas.translate(xpos * scl, ypos * scl);

    canvas.noStroke();
    canvas.fill(_blink, 200 * (1 - timerOfColor.liner()));
    canvas.rect(0, 0, scl, scl);
    canvas.popMatrix();
  }
  public void octaveDisplay() {
    canvas.pushMatrix();
    if (active) { canvas.fill(_active, 255 - mainAlpha); }
    else { canvas.fill(_normal, 255 - mainAlpha); }

    canvas.translate(margin + scl / 2, margin);
    canvas.translate(xpos * scl, ypos * scl);
    canvas.noStroke();
    canvas.rectMode(CENTER);

    float gap = scl / PApplet.parseFloat(oct + 2);
    for (int i = 0; i <= oct; i++) {
      canvas.rect(0, gap * (i + 1), scl / 2, scl / 8);
    }
    // switch(oct) {
    //   case 0 :
    //     // canvas.ellipse(0, 0, scl / 2, scl / 2);
    //
    //     break;
    //   case 1 :
    //     canvas.ellipse(scl / 5, 0, scl / 3, scl / 3);
    //     canvas.ellipse(-1 * scl / 5, 0, scl / 3, scl / 3);
    //     break;
    //   case 2 :
    //     canvas.ellipse(scl / 5, 0, scl / 4, scl / 4);
    //     canvas.ellipse(-1 * scl / 5, scl / 5, scl / 4, scl / 4);
    //     canvas.ellipse(-1 * scl / 5, -1 * scl / 5, scl / 4, scl / 4);
    //     break;
    //   default :
    //     canvas.ellipse(0, 0, scl / 2, scl / 2);
    //     break;
    // }
    canvas.rectMode(CORNER);
    canvas.popMatrix();
  }

  //signal
  public void activate() {
    active = !active;
  }
  public void trigger() {
    timingCount = 0;
    triggering = true;
    timerOfColor.startTimer();
    if (active) {
      timingCount++;
      sendOSC();
      sendMIDI();
    }

    if (map.stabs[ROTATE].active) {
      randomizeOt();
    }
  }

  public void sendClock(int b) {
    if (triggering && active) {
      if (timingCount == nOftiming) {
        timingCount = 0;
        triggering = false;
      }
      else if (b % (24 / nOftiming) == 0) {
        timingCount++;
        sendOSC();
        sendMIDI();
      }
    }
  }

  public void sendOSC() {
    OscMessage msg = new OscMessage("/m" + str(map.id));
    oscP5.send(msg, other);
  }
  public void sendMIDI() {
    int ch = floor(map.sliderOfChannel.getValue()) - 1;
    int pit = getPitch();
    int vel = getVelocity();

    if (oct > 0) {
      midi.sendNoteOn(ch, pit, vel); // Send a Midi noteOn
      midi.sendNoteOn(ch, pit + oct * 12, vel);
      delay(10);
      midi.sendNoteOff(ch, pit, vel); // Send a Midi nodeOff
      midi.sendNoteOff(ch, pit + oct * 12, vel);
    }
    else {
      midi.sendNoteOn(ch, pit, vel); // Send a Midi noteOn
      delay(10);
      midi.sendNoteOff(ch, pit, vel); // Send a Midi nodeOff
    }
  }
  //parameter adjustment
  public void setTiming() {
    nOftiming = (nOftiming % 4) + 1;
  }
  public void setTiming(int i) {
    nOftiming = i % 4 + 1;
  }
  public void setVelocity() {
    vel = (vel % 7) + 1;
  }
  public void setVelocity(int i) {
    vel = i % 7 + 1;
  }
  public int getVelocity() {
    return vel * 32;
  }
  public void setPitch() {
    pitch = (pitch + 1) % map.pitchStep.length;
  }
  public void setPitch(int i) {
    pitch = i % map.pitchStep.length;
  }
  public int getPitch() {
    return map.pitchStep[pitch];
  }
  public void setOct() {
    oct = (oct + 1) % 3;
  }
  public void setOct(int i) {
    oct = i % 3;
  }

  //utility
  public void setOt(int _o) {
    ot = _o;
  }
  public void randomizeOt() {
    if (random(1) > 0.5f) {
      rotateClockwise();
    }
    else {
      rotateCounterclockwise();
    }
  }
  public void rotateClockwise() {
    ot = ot + 1;
  }
  public void rotateCounterclockwise() {
    ot = ot - 1;
  }



}
class Tab {
  float alpha;
  boolean mouseOver = false;
  boolean active = false;
  int pos;
  Map map;
  PGraphics canvas;
  int col;

  float padding = scl * 0.125f;
  float w = scl * 0.75f;
  float h = scl * 0.33f;

  Tab(Map _m, int _p, int _c) {
    map = _m;
    canvas = map.canvas;
    pos = _p;
    col = _c;
  }

  public void display() {
    float al = active? 200 : ((mouseOver)?100:50);
    alpha = alpha + 0.2f * (al - alpha);

    canvas.pushMatrix();
    canvas.noStroke();
    canvas.fill(col, alpha);
    canvas.translate(margin + pos * scl, 0);
    canvas.rect(padding, 0, w, h);
    canvas.popMatrix();
  }

  public void trigger() {
    active = !active;
  }
  public void activate() {
    active = true;
  }
  public void deactivate() {
    active = false;
  }

}

class SideTab extends Tab {
  SideTab(Map _m, int _p, int _c) {
    super(_m, _p, _c);
  }
  public void display() {
    float al = active? 200 : ((mouseOver)?100:50);
    alpha = alpha + 0.2f * (al - alpha);

    canvas.pushMatrix();
    canvas.noStroke();
    canvas.fill(col, alpha);
    canvas.translate(0, margin + pos * scl);
    canvas.rect(0, padding, h, w);
    canvas.popMatrix();
  }
}
class TimeLine {
  boolean state;
  int localtime;
  int limit;
  int elapsedTime;
  int repeatTime = 1;
  boolean breathState = false;
  boolean loop = false;

  float linerRate = 1;

  TimeLine(int sec) {
    limit=sec;
    state=false;
  }

  TimeLine(int sec, boolean _loop) {
    limit = sec;
    loop = _loop;
    state = _loop;
  }
  public void update() {
    if (state == true) {
      elapsedTime = currentTime() - localtime;

      if (elapsedTime>PApplet.parseInt(limit)) {
        if( !loop ) {
          elapsedTime = PApplet.parseInt(limit);
          state=false;
        }
        else {
          startTimer();
        }
      }
    }
  }

  public float liner() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    float ret = pow(t, linerRate);
    return min(1, ret);
  }
  public float getPowIn(float pow) {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    float ret = pow(t, pow);
    return min(1, ret);
  }
  public float getPowOut(float pow) {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    float ret = 1 - pow(1 - t, pow);
    return min(1, ret);
  }
  public float getPowInOut(float pow) {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    float ret;
    if ((t*=2)<1) {
      ret = 0.5f * pow(t, pow);
    }
    else {
      ret = 1 - 0.5f * abs(pow(2-t, pow));
    }

    return min(1, ret);
  }
  public float sineIn() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    return min(1, 1 - cos(t * PI / 2));
  }
  public float sineOut() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    return min(1, sin(t * PI / 2));
  }
  public float sineInOut() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    return min(1, 0.5f*(1 - cos(PI*t)));
  }
  public float getBackIn(float amount) {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    return t*t*((amount+1)*t-amount);
  }
  public float getBackOut(float amount) {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    return (--t*t*((amount+1)*t + amount) + 1);
  }
  public float getBackInOut(float amount) {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    if ((t*=2)<1) {
      return 0.5f*(t*t*((amount+1)*t-amount));
    }
    else {
      return 0.5f*((t-=2)*t*((amount+1)*t+amount)+2);
    }
  }
  public float circIn() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    return (1 - sqrt(1-t*t));
  }
  public float circOut() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    return sqrt(1-(--t)*t);
  }
  public float circInOut() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    if ((t*=2) < 1) {
      return -0.5f*(sqrt(1-t*t)-1);
    }
    else {
      return 0.5f*(sqrt(1-(t-=2)*t)+1);
    }
  }
  public float bounceIn() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    return 1 - bo(1-t);
  }
  public float bounceOut() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    return bo(t);
  }
  public float bounceInOut() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    if (t<0.5f) {
      return (1-bo(1-t*2))*0.5f;
    }
    else {
      return bo(t*2-1)*0.5f+0.5f;
    }
  }
  public float bo(float t) {
    if (t < 1/2.75f) {
			return (7.5625f*t*t);
		} else if (t < 2/2.75f) {
			return (7.5625f*(t-=1.5f/2.75f)*t+0.75f);
		} else if (t < 2.5f/2.75f) {
			return (7.5625f*(t-=2.25f/2.75f)*t+0.9375f);
		} else {
			return (7.5625f*(t-=2.625f/2.75f)*t +0.984375f);
		}
  }
  public float elasticIn() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    float b = 0;
    float c = 1;
    float d = 1;
    if (t == 0)
      return b;
    if ((t /= d) == 1)
      return b + c;
    float p = d * .3f;
    float a = c;
    float s = p / 4;
    return -(a * (float) Math.pow(2, 10 * (t -= 1)) * (float) Math.sin((t * d - s) * (2 * (float) Math.PI) / p)) + b;
  }
  public float elasticOut() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    float b = 0;
    float c = 1;
    float d = 1;
    if (t == 0)
      return b;
    if ((t /= d) == 1)
      return b + c;
    float p = d * .3f;
    float a = c;
    float s = p / 4;
    return (a * (float) Math.pow(2, -10 * t) * (float) Math.sin((t * d - s) * (2 * (float) Math.PI) / p) + c + b);
  }
  public float elasticInOut() {
    update();
    float t = PApplet.parseFloat(elapsedTime)/limit;
    float b = 0;
    float c = 1;
    float d = 1;
    if (t == 0)
      return b;
    if ((t /= d / 2) == 2)
      return b + c;
    float p = d * (.3f * 1.5f);
    float a = c;
    float s = p / 4;
    if (t < 1)
      return -.5f * (a * (float) Math.pow(2, 10 * (t -= 1)) * (float) Math.sin((t * d - s) * (2 * (float) Math.PI) / p)) + b;
    return a * (float) Math.pow(2, -10 * (t -= 1)) * (float) Math.sin((t * d - s) * (2 * (float) Math.PI) / p) * .5f + c + b;
  }

  // float getElasticIn(float amp, float period) {
  //   update();
  //   float t = float(elapsedTime)/limit;
  //   if (t==0 || t==1) {
  //     return t;
  //   }
  //   float s = period/(PI*2*asin(1/amp));
  //   return -(amp*pow(2,10*(t-=1))*sin((t-s)*PI*2/period));
  // }
  // float getElasticOut(float amp, float period) {
  //   update();
  //   float t = float(elapsedTime)/limit;
  //   if (t==0 || t==1) {
  //     return t;
  //   }
  //   float s = period/(PI*2*asin(1/amp));
  //   return (amp*pow(2,-10*t)*sin((t-s)*PI*2/period )+1);
  // }
  // float getElasticInOut(float amp, float period) {
  //   update();
  //   float t = float(elapsedTime)/limit;
  //   if (t==0 || t==1) {
  //     return t;
  //   }
  //   float s = period/(PI*2*asin(1/amp));
	// 	if ((t*=2)<1) return -0.5*(amp*pow(2,10*(t-=1))*sin( (t-s)*PI*2/period ));
	// 	return amp*pow(2,-10*(t-=1))*sin((t-s)*PI*2/period)*0.5+1;
  // }

  public float repeatBreathMovement() {
    if (state == true) {
      //println("check!!!!");
      elapsedTime = currentTime() - localtime;
      if (elapsedTime>PApplet.parseInt(limit)) {
        elapsedTime = PApplet.parseInt(limit);
        if(repeatTime < 2 && breathState) {
          state = false; }
        else {
          if(breathState == true) {
            repeatTime-- ;
          }
          breathState = !breathState;
          startTimer();
        }
      }
    }

    float t = PApplet.parseFloat(elapsedTime)/limit;
    if(!breathState) {
      return pow(t, linerRate); }
    else {
      return pow((1-t), linerRate); }
  }
  public float repeatBreathMovementEndless() {
    if (state == true) {
      //println("check!!!!");
      elapsedTime = currentTime() - localtime;
      if (elapsedTime>PApplet.parseInt(limit)) {
        elapsedTime = PApplet.parseInt(limit);
        if(repeatTime < 1 && breathState) {
          state = false; }
        else {
          breathState = !breathState;
          startTimer();
        }
      }
    }

    float t = PApplet.parseFloat(elapsedTime)/limit;
    if(!breathState) {
      return pow(t, linerRate); }
    else {
      return pow((1-t), linerRate); }
  }
  public void setLinerRate(float r) { linerRate = r; }
  public void setRepeatTime(int t) { repeatTime = t; }
  public boolean startTimer() {
    if (state == true) {
      localtime = currentTime();
      elapsedTime = 0;
      return false;
    }
    else {
      localtime = currentTime();
      state=true;
      elapsedTime = 0;
      return true;
    }
  }
  public void turnOffTimer() {
    localtime = currentTime() - limit;
    state = false;
  }

  public int currentTime() {
    return millis();
  }
  public void setLoop() { loop = true; }
  public void set1() { elapsedTime = limit; }
}
class TimeNode {

  Map map;

  int xx;
  int yy;

  TimeNode(Map _m, int _x, int _y) {
    map = _m;
    xx = _x;
    yy = _y;
  }

  public void toNext() {
    map.nodes[xx][yy].trigger();

    int ot = map.nodes[xx][yy].ot;
    while(ot < 0) {
      ot += 4;
    }
    ot %= 4;
    switch(ot) {
      case 0 :
        xx = (xx + nOfc + 1) % nOfc;
        break;
      case 1 :
        yy = (yy + nOfc + 1) % nOfc;
        break;
      case 2 :
        xx = (xx + nOfc - 1) % nOfc;
        break;
      case 3 :
        yy = (yy + nOfc - 1) % nOfc;
        break;
      default:
    }
  }
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "beatmap_syphon" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
