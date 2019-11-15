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
import codeanticode.syphon.*;
import controlP5.*;
import themidibus.*;
import oscP5.*;
import netP5.*;
import processing.net.*;
import java.util.*;

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
color brush_color = color(#FFFFFF);
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

void settings() {
  size(620, 740, P3D);
}

void setup() {
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

void draw() {
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

void drawGraphics() {
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
PVector mapMouseToCanvas(int x_in, int y_in, PGraphics pg) {
  int x_min = vp.viewport_off_x+vp.view_off_w;
  int x_max = x_min+vp.view_w;
  int y_min = vp.viewport_off_y+vp.view_off_h;
  int y_max = y_min+vp.view_h;
  PVector out = new PVector(-1, -1);
  if (x_in >= x_min && x_in <= x_max && y_in >= y_min && y_in <= y_max) {
    float x = map(x_in, x_min, x_max, 0.0, c.width);
    float y = map(y_in, y_min, y_max, 0.0, c.height);
    out = new PVector(x,y);
  }
  return out;
}

float scaleBrushPreview(float bs, PGraphics pg) {
  float out = bs;
  float scale = max(c.width, c.height)/max(width, height);
  out = scale * brush_size/2;
  return out;
}
