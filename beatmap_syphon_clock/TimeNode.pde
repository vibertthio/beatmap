class TimeNode {

  Map map;

  int xx;
  int yy;

  TimeNode(Map _m, int _x, int _y) {
    map = _m;
    xx = _x;
    yy = _y;
  }

  void toNext() {
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
