import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import codeanticode.syphon.*; 
import controlP5.*; 
import themidibus.*; 
import oscP5.*; 
import netP5.*; 
import processing.net.*; 
import java.util.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class vj_doodler extends PApplet {

/*
This is a drawing app for VJs.
It creates a PGraphics "canvas" named c that you can draw onto,
and outputs that canvas via Syphon.
The canvas is rescalable and renamable.
It is build on top of vj_boilerplate.

It relies on:
The Syphon library by Andres Colubri
The Midibus library by Severin Smith
oscP5 and controlP5 by Andreas Schlegel
*/








MidiBus midi;
String[] midi_devices;
OscP5 oscP5;
ControlP5 cp5;
CallbackListener cb;
Textfield field_cw, field_ch, field_syphon_name, field_osc_port, field_osc_address;
Button button_ip;
ScrollableList dropdown_midi, dropdown_syphon_client;
Toggle toggle_log_osc, toggle_log_midi, toggle_view_bg;
Knob knob_brush_size;
Bang bang_clear, bang_black, bang_white;
float brush_size;
int brush_color = color(0xffFFFFFF);
Viewport vp;
boolean viewport_show_alpha = false;
boolean log_midi = true, log_osc = true;

int port = 9999;
String ip;

PGraphics c;
int cw = 1280, ch = 720;

SyphonServer syphonserver;
SyphonClient[] syphon_clients;
int syphon_clients_index; //current syphon client
String syphon_name = "vj_doodler", osc_address = syphon_name;
Log log;

PVector brush = new PVector(-1, -1);
PVector pbrush = brush;

public void settings() {
  size(620, 740, P3D);
}

public void setup() {
  log = new Log();

  midi_devices = midi.availableInputs();

  c = createGraphics(cw, ch, P3D);
  vp = new Viewport(c, 600, 10, 65);
  syphonserver = new SyphonServer(this, syphon_name);
  vp.resize(c);
  frameRate(60);
  controlSetup();
  updateOSC(port);
}

public void draw() {
  noStroke();
  background(127);

  fill(100);
  rect(0, 0, width, 55);

  fill(cp5.getTab("output/syphon").getColor().getBackground());
  rect(0, 0, width, cp5.getTab("output/syphon").getHeight());

  drawGraphics();
  vp.display(c);
  syphonserver.sendImage(c);
  log.update();
}

public void drawGraphics() {
  c.beginDraw();

  /*
  c.loadPixels();
  for (int i = 0; i < c.pixels.length; i++) {
  // The functions red(), green(), and blue() pull out the 3 color components from a pixel.
  float r = red(c.pixels[i]);
  float g = green(c.pixels[i]);
  float b = blue(c.pixels[i]);
  if (r > 0) {
  r = constrain(r-10, 0, 255);
  g = constrain(g-10, 0, 255);
  b = constrain(b-10, 0, 255);
  c.pixels[i] = color(r,g,b);
}
else c.pixels[i] = color(0,0,0);
c.updatePixels();
*/

if (mousePressed) {
  brush = mapMouseToCanvas(mouseX, mouseY, c);
  pbrush = mapMouseToCanvas(pmouseX, pmouseY, c);
  if (brush.x + brush.y != -2) {
    c.noStroke();
    c.fill(brush_color);
    c.circle(brush.x, brush.y, brush_size); //buttcap each line
    if (pbrush.x + pbrush.y != -2) {
      c.strokeWeight(brush_size);
      c.stroke(brush_color);
      c.line(pbrush.x, pbrush.y, brush.x, brush.y);
    }
  }
}
c.endDraw();
pbrush = new PVector(-1, -1); //reset pbrush

//draw brush preview
noFill();
strokeWeight(1);
stroke(255,0,0);
circle(mouseX, mouseY, scaleBrushPreview(brush_size, c));
}
/*remaps the cursor input so that what you draw inside the viewport, is scaled
correctly to the PGraphics canvas */
public PVector mapMouseToCanvas(int x_in, int y_in, PGraphics pg) {
  int x_min = vp.viewport_off_x+vp.view_off_w;
  int x_max = x_min+vp.view_w;
  int y_min = vp.viewport_off_y+vp.view_off_h;
  int y_max = y_min+vp.view_h;
  PVector out = new PVector(-1, -1);
  if (x_in >= x_min && x_in <= x_max && y_in >= y_min && y_in <= y_max) {
    float x = map(x_in, x_min, x_max, 0.0f, c.width);
    float y = map(y_in, y_min, y_max, 0.0f, c.height);
    out = new PVector(x,y);
  }
  return out;
}

public float scaleBrushPreview(float bs, PGraphics pg) {
  float out = bs;
  float scale = max(c.width, c.height)/max(width, height);
  out = scale * brush_size/2;
  return out;
}
class Log {
  String current_log;
  int counter;
  Log() {
    current_log = "No new events";
    counter = 30;
  }

  public void update() {
    fill(5);
    text(current_log, 10, height-10);
  }

  public void setText(String input) {
    String time = zeroFormat(hour()) + ":" + zeroFormat(minute()) + ":" + zeroFormat(second());
    current_log = time + " " + input;
  }
}
//function for formatting int values as strings: 1 becomes "01", 2 becomes "02"
public String zeroFormat(int input) {
  String output = Integer.toString(input);
  if (input < 10) output = "0" + output;
  return output;
}
class Viewport {
  int view_w;
  int view_h;
  int viewport_size;
  int view_off_w = 0, view_off_h = 0;
  int viewport_off_x, viewport_off_y;
  PGraphics bg;

  Viewport(PGraphics pg, int _viewport_size, int dox, int doy) {
    viewport_size = _viewport_size;
    viewport_off_x = dox;
    viewport_off_y = doy;
  }

  public void display(PGraphics pg) {
    pushMatrix();
    translate(viewport_off_x, viewport_off_y);
    noFill();
    stroke(100);
    rect(0, 0, viewport_size, viewport_size);
    noStroke();
    fill(255);
    drawPointers();

    if (viewport_show_alpha) image(bg, view_off_w, view_off_h, view_w, view_h);
    else {
      fill(0);
      rect(view_off_w, view_off_h, view_w, view_h);
    }
    image(pg, view_off_w, view_off_h, view_w, view_h);
    popMatrix();
    //println(view_w, view_h);
  }

  public void resize(PGraphics pg) {
    int[] dims = scaleToFit(pg.width, pg.height, viewport_size, viewport_size);
    view_off_w = dims[0];
    view_off_h = dims[1];
    view_w = dims[2];
    view_h =dims[3];
    bg = createAlphaBackground(view_w, view_h);
  }

  public PGraphics createAlphaBackground(int w, int h) {

    PGraphics abg = createGraphics(w, h, P2D);
    int s = 10; // size of square
    abg.beginDraw();
    abg.background(127+50);
    abg.noStroke();
    abg.fill(127-50);
    for (int x = 0; x < w; x+=s+s) {
      for (int y = 0; y < h; y+=s+s) {
        abg.rect(x, y, s, s);
      }
    }
    for (int x = s; x < w; x+=s+s) {
      for (int y = s; y < h; y+=s+s) {
        abg.rect(x, y, s, s);
      }
    }
    abg.endDraw();
    return abg;
  }

  public void drawPointers() {
    int x = view_off_w;
    int y = view_off_h;
    triangle(x, y, x-5, y, x, y-5);
    x += bg.width;
    triangle(x, y, x+5, y, x, y-5);
    y += bg.height;
    triangle(x, y, x+5, y, x, y+5);
    x = view_off_w;
    triangle(x, y, x-5, y, x, y+5);
  }
}

public void updateCanvas() {
  c = createGraphics(cw, ch, P3D);
  vp.resize(c);
}

public void updateCanvas(int w, int h) {
  c = createGraphics(w, h, P3D);
  c = createGraphics(w, h, P3D);
  vp.resize(c);
}

public int[] scaleToFill(int in_w, int in_h, int dest_w, int dest_h) {
  PVector in = new PVector((float)in_w, (float)in_h); //vector of input dimensions
  PVector dest = new PVector((float)dest_w, (float)dest_h); //vector of destination dimensions
  /*
  calculate the scaling ratios for both axis, and choose the largest for scaling
  the output dimensions to FILL the destination
  */
  float scale = max(dest.x/in.x, dest.y/in.y);
  int out_w = round(in_w *scale);
  int out_h = round(in_h *scale);
  int off_x = (dest_w - out_w) / 2;
  int off_y = (dest_h - out_h) / 2;

  int[] out = {off_x, off_y, out_w, out_h};
  return out;
}

public int[] scaleToFit(int in_w, int in_h, int dest_w, int dest_h) {
  PVector in = new PVector((float)in_w, (float)in_h); //vector of input dimensions
  PVector dest = new PVector((float)dest_w, (float)dest_h); //vector of destination dimensions
  /*
  calculate the scaling ratios for both axis, and choose the SMALLEST for scaling
  the output dimensions to FIT the destination
  */
  float scale = min(dest.x/in.x, dest.y/in.y);
  int out_w = round(in_w *scale);
  int out_h = round(in_h *scale);
  int off_x = (dest_w - out_w) / 2;
  int off_y = (dest_h - out_h) / 2;
  println("offset x:", off_x, "offset y:", off_y);

  int[] out = {off_x, off_y, out_w, out_h};
  return out;
}
public void controlSetup() {
  cp5 = new ControlP5(this);
  int xoff = 10;
  int yoff = 20;

  cb = new CallbackListener() {
    public void controlEvent(CallbackEvent theEvent) {
      switch(theEvent.getAction()) {
        case(ControlP5.ACTION_ENTER):
        cursor(HAND);
        break;
        case(ControlP5.ACTION_LEAVE):
        case(ControlP5.ACTION_RELEASEDOUTSIDE):
        cursor(ARROW);
        break;
      }
    }
  };

  cp5.getTab("default")
  .setAlwaysActive(true)
  .hideBar()
  .setWidth(-3)
  ;
  //hide default bar
  cp5.addTab("output/syphon").setActive(true);

  cp5.addTab("osc/midi")
  ;

  field_cw = cp5.addTextfield("field_cw")
  .setPosition(xoff, yoff)
  .setSize(30, 20)
  .setAutoClear(false)
  .setText(Integer.toString(cw))
  .setLabel("width")
  .setId(-1)
  .moveTo("output/syphon")
  ;

  xoff += field_cw.getWidth() + 10;
  field_ch = cp5.addTextfield("field_ch")
  .setPosition(xoff, yoff)
  .setSize(30, 20)
  .setAutoClear(false)
  .setText(Integer.toString(ch))
  .setLabel("height")
  .setId(-1)
  .moveTo("output/syphon")
  ;

  xoff += cp5.getController("field_ch").getWidth() + 10;
  field_syphon_name = cp5.addTextfield("field_syphon_name")
  .setPosition(xoff, yoff)
  .setSize(60, 20)
  .setAutoClear(false)
  .setText(syphon_name)
  .setLabel("syphon name")
  .setId(-1)
  .moveTo("output/syphon")
  ;

  xoff += field_syphon_name.getWidth() + 10;
  toggle_view_bg = cp5.addToggle("viewport_show_alpha")
  .setPosition(xoff, yoff)
  .setSize(50, 20)
  .setValue(viewport_show_alpha)
  .setLabel("alpha / none")
  .setMode(ControlP5.SWITCH)
  .setId(-1)
  .moveTo("output/syphon")
  ;

  xoff = 10; //reset position for tab "osc/midi"
  button_ip = cp5.addButton("button_ip")
  .setPosition(xoff, yoff)
  .setSize(70, 20)
  .setLabel("ip: " + ip)
  .setSwitch(false)
  .setId(-1)
  .moveTo("osc/midi")
  ;

  xoff += button_ip.getWidth() + 10;
  field_osc_port = cp5.addTextfield("field_osc_port")
  .setPosition(xoff, yoff)
  .setSize(30, 20)
  .setAutoClear(false)
  .setText(Integer.toString(port))
  .setLabel("osc port")
  .setId(-1)
  .moveTo("osc/midi")
  ;

  xoff += field_osc_port.getWidth() + 10;
  field_osc_address = cp5.addTextfield("field_osc_address")
  .setPosition(xoff, yoff)
  .setSize(50, 20)
  .setAutoClear(false)
  .setText(syphon_name)
  .setLabel("osc address")
  .setId(-1)
  .moveTo("osc/midi")
  ;

  xoff += field_osc_address.getWidth() + 10;
  toggle_log_osc = cp5.addToggle("log_osc")
  .setPosition(xoff, yoff)
  .setSize(30, 20)
  .setLabel("log osc")
  .setValue(true)
  .setId(-1)
  .moveTo("osc/midi")
  ;

  xoff += toggle_log_osc.getWidth() + 10;
  dropdown_midi = cp5.addScrollableList("dropdown_midi")
  .setPosition(xoff, yoff)
  .setSize(200, 100)
  .setOpen(false)
  .setBarHeight(20)
  .setItemHeight(20)
  .addItems(Arrays.asList(midi_devices))
  .setLabel("MIDI INPUT")
  .setId(-1)
  .moveTo("osc/midi")
  .setType(ScrollableList.LIST) // currently supported DROPDOWN and LIST
  ;

  xoff += dropdown_midi.getWidth() + 10;
  toggle_log_midi = cp5.addToggle("log_midi")
  .setPosition(xoff, yoff)
  .setSize(30, 20)
  .setLabel("log midi")
  .setValue(true)
  .setId(-1)
  .moveTo("osc/midi")
  ;

  /*  CUSTOM CONTROLS
  Add your own controls below. Use .setId(-1) to make controller
  unreachable by OSC.
  */
  xoff = vp.viewport_off_x;
  yoff = vp.viewport_off_y+vp.viewport_size+10;

  knob_brush_size = cp5.addKnob("brush_size")
  .setPosition(xoff, yoff)
  .setSize(30, 30)
  .setLabel("brush size")
  .setValue(10)
  ;

  xoff += knob_brush_size.getWidth() + 10;
  bang_clear = cp5.addBang("bang_clear")
  .setPosition(xoff, yoff)
  .setSize(30, 30)
  .setLabel("clear canvas")
  ;

  xoff += bang_clear.getWidth() + 30;
  bang_black = cp5.addBang("bang_black")
  .setPosition(xoff, yoff)
  .setSize(30, 30)
  .setLabel("black")
  .setColorForeground(color(0xff000000))
  .setColorActive(color(0xff323232))
  ;

  xoff += bang_clear.getWidth() + 10;
  bang_white = cp5.addBang("bang_white")
  .setPosition(xoff, yoff)
  .setSize(30, 30)
  .setLabel("white")
  .setColorForeground(color(0xffFFFFFF))
  .setColorActive(color(0xffe5e5e5))
  ;
}

public int evalFieldInput1(String in, int current, Controller con) {
  String name = con.getLabel();
  int out = -1;
  char[] ints = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
  char[] input = in.toCharArray();

  String txt = "value not int between 1 and 9999";
  if (input.length < 5) {
    int check = 0;
    for (char ch : input) {
      for (char i : ints) {
        if (ch == i) check++;
      }
    }

    if (input.length == check) {
      int verified_int = Integer.parseInt(in);
      txt = name + " changed from " + current + " to " + verified_int;
      if (verified_int < 1) {
        verified_int = 1;
        txt = name + " was lower than 0 and defaults to " + verified_int;
      }
      if (verified_int == current) txt = "value is not different from " + current;
      else {
        out = verified_int;
      }
    }
  }
  log.setText(txt);

  return out;
}

public boolean evalFieldInput2(String in, String current, Controller con) {
  String name = con.getLabel();
  String txt = "input to " + name + " is unchanged";
  boolean out = true;
  char[] illegal_chars = {'/', ',', '.', '(', ')', '[', ']',
  '{', '}', ' '};
  char[] input = in.toCharArray();
  if (!in.equals(current)) {
    if (input.length > 0) {
      for (char ch : input) {
        for (char i : illegal_chars) {
          if (ch == i) {
            txt = "input to " + name + " contained illegal character and was reset";
            out = false;
          }
        }
      }
    }
  }

  log.setText(txt);

  return out;
}

public void field_cw(String theText) {
  int value = evalFieldInput1(theText, cw, cp5.getController("field_cw"));
  if (value > 0) {
    cw = value;
    updateCanvas();
  }
}
public void field_ch(String theText) {
  int value = evalFieldInput1(theText, ch, cp5.getController("field_ch"));
  if (value > 0) {
    ch = value;
    updateCanvas();
  }
}

public void field_syphon_name(String input) {
  if (evalFieldInput2(input, syphon_name, field_syphon_name)) {
    syphon_name = input;
    field_osc_address.setText(input);
    osc_address = input;
    log.setText("syphon name and osc address set to " + input);
  }
  else field_syphon_name.setText(syphon_name);
}

public void field_osc_address(String input) {
  if (evalFieldInput2(input, osc_address, field_osc_address)) {
    syphon_name = input;
    log.setText("osc address set to " + input);
  }
  else field_osc_address.setText(osc_address);
}

public void dropdown_midi(int n) {
  updateMIDI(n);
  println("added " + midi_devices[n], n);
}

public void log_midi(boolean state) {
  log_midi = state;
  if (state) log.setText("started logging midi input");
  else log.setText("stopped logging midi input");
}

public void field_osc_port(String theText) {
  int value = evalFieldInput1(theText, port, field_osc_port);
  if (value > 0) {
    port = value;
    updateOSC(port);
  }
}

public void button_ip() {
  updateIP();
  log.setText("ip adress has been updated to " + ip);
}

public void bang_clear() {
  c.beginDraw();
  c.clear();
  c.endDraw();
}

public void bang_black() {
  brush_color = color(0xff000000);
}

public void bang_white() {
  brush_color = color(0xffFFFFFF);
}
public void noteOn(int channel, int pitch, int velocity) {
  if (log_midi) log.setText("Note On // Channel:"+channel + " // Pitch:"+pitch + " // Velocity:"+velocity);
}

public void noteOff(int channel, int pitch, int velocity) {
  if (log_midi) log.setText("Note Off // Channel:"+channel + " // Pitch:"+pitch + " // Velocity:"+velocity);
}

public void controllerChange(int channel, int number, int value) {
  if (log_midi) log.setText("Slider // Channel:"+channel + " // Number:" +number + " // Value: "+value);
}

public void changeSlider(String name, int value) {
  Controller con = cp5.getController(name);
  con.setValue(map(value, 0, 127, con.getMin(), con.getMax()));
}

public void updateMIDI(int n) {
 log.setText("added midi device " + midi_devices[n]);
 midi = new MidiBus(this, n, -1);
}
public void updateOSC(int p) {
  updateIP();
  oscP5 = new OscP5(this, p);
  cp5.getController("field_osc_port").setValue(p);
}

public void updateIP() {
  ip = Server.ip();
  cp5.getController("button_ip").setLabel("ip: " + ip);
}

public void oscEvent(OscMessage theOscMessage) {
  String str_in[] = split(theOscMessage.addrPattern(), '/');
  String txt = "got osc message: " + theOscMessage.addrPattern();
  if (str_in.length == 3) {
    if (str_in[1].equals(osc_address) &&
    cp5.getController(str_in[2]) != null &&
    cp5.getController(str_in[2]).getId() != -1)
    {
      Controller con = cp5.getController(str_in[2]);

      if (theOscMessage.checkTypetag("i")) {
        int value = theOscMessage.get(0).intValue();
        value = constrain(value, (int)con.getMin(), (int)con.getMax());
        con.setValue(value);
        txt += " int value: " + Integer.toString(value);
      }

      else if (theOscMessage.checkTypetag("f")) {
        float value = theOscMessage.get(0).floatValue();
        value = constrain(value, con.getMin(), con.getMax());
        con.setValue(value);
        txt += " float value: " + Float.toString(value);
      }
    }
  }
  if (log_osc) log.setText(txt);
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "vj_doodler" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
