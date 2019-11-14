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
Viewport view;
boolean viewport_show_alpha = false;
boolean log_midi = true, log_osc = true;

int port = 9999;
String ip;

PGraphics c, c_input;
int cw = 1280, ch = 720;

SyphonServer syphonserver;
SyphonClient[] syphon_clients;
int syphon_clients_index; //current syphon client
String syphon_name = "boilerplate", osc_address = syphon_name;
Log log;

PVector brush = new PVector(-1, -1);
PVector pbrush = brush;

void settings() {
  size(500, 500, P3D);
}

void setup() {
  log = new Log();

  midi_devices = midi.availableInputs();
  controlSetup();
  updateOSC(port);

  c = createGraphics(cw, ch, P3D);
  c_input = createGraphics(c.width,c.height,P3D);
  view = new Viewport(c, 400, 50, 50);
  syphonserver = new SyphonServer(this, syphon_name);
  view.resize(c);
  frameRate(60);
}

void draw() {
  background(127);
  noStroke();
  fill(100);
  rect(0, 0, width, 55);
  fill(cp5.getTab("output/syphon").getColor().getBackground());
  rect(0, 0, width, cp5.getTab("output/syphon").getHeight());


  drawGraphics();
  view.display(c);
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
  }

  c.updatePixels();
*/
  //c.fill(255, 0,0, 255);
  c.stroke(255);
  c.strokeWeight(60);
  if (mousePressed) {
    brush = mapMouseToCanvas(mouseX, mouseY, c);
    if (brush.x + brush.y != -2) {
      c.circle(brush.x, brush.y, 30);

      if (pbrush.x + pbrush.y != -2) c.line(pbrush.x, pbrush.y, brush.x, brush.y);
      pbrush = brush;
    }
  }
  c.endDraw();
}

PVector mapMouseToCanvas(int x_in, int y_in, PGraphics pg) {
  int x_min = view.display_off_x+view.view_off_w;
  int x_max = x_min+view.view_w;
  int y_min = view.display_off_y+view.view_off_h;
  int y_max = y_min+view.view_h;
  PVector out = new PVector(-1, -1);
  if (mouseX >= x_min && mouseX <= x_max &&
    mouseY >= y_min && mouseY <= y_max) {
      float x = map(mouseX, x_min, x_max, 0.0, c.width);
      float y = map(mouseY, y_min, y_max, 0.0, c.height);
      out = new PVector(x,y);
    }
    return out;
  }

void mouseReleased() {
  pbrush = new PVector(-1, -1);
}
