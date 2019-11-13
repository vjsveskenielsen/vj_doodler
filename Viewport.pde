class Viewport {
  int view_w;
  int view_h;
  int view_size;
  int view_off_w = 0, view_off_h = 0;
  int display_off_x, display_off_y;
  PGraphics bg;

  Viewport(PGraphics pg, int _view_size, int dox, int doy) {
    view_size = _view_size;
    display_off_x = dox;
    display_off_y = doy;
  }

  void display(PGraphics pg) {
    pushMatrix();
    translate(display_off_x, display_off_y);
    noStroke();
    fill(255);
    drawPointers();
    fill(100);

    if (viewport_show_alpha) image(bg, view_off_w, view_off_h, view_w, view_h);
    image(pg, view_off_w, view_off_h, view_w, view_h);
    popMatrix();
    //println(view_w, view_h);
  }

  void resize(PGraphics pg) {
    int[] dims = scaleToFit(pg.width, pg.height, view_size, view_size);
    view_off_w = dims[0];
    view_off_h = dims[1];
    view_w = dims[2];
    view_h =dims[3];
    bg = createAlphaBackground(view_w, view_h);
  }

  PGraphics createAlphaBackground(int w, int h) {

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

  void drawPointers() {
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

void updateCanvas() {
  c = createGraphics(cw, ch, P3D);
  view.resize(c);
}

void updateCanvas(int w, int h) {
  c = createGraphics(w, h, P3D);
  view.resize(c);
}

int[] scaleToFill(int in_w, int in_h, int dest_w, int dest_h) {
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

int[] scaleToFit(int in_w, int in_h, int dest_w, int dest_h) {
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
