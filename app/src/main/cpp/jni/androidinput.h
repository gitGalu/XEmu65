#include "../atari.h"

struct RECT {
    int l;
    int t;
    union {
        int r;
        int w;
    };
    union {
        int b;
        int h;
    };
};
struct POINT {
    int x;
    int y;
};

enum {
    SOFTJOY_LEFT = 0,
    SOFTJOY_RIGHT,
    SOFTJOY_UP,
    SOFTJOY_DOWN,
    SOFTJOY_FIRE,
    SOFTJOY_MAXKEYS
};
#define SOFTJOY_MAXACTIONS 3
#define ACTION_NONE 0xFF
extern UBYTE softjoymap[SOFTJOY_MAXKEYS + SOFTJOY_MAXACTIONS][2];

extern UWORD Android_PortStatus;
extern UBYTE Android_TrigStatus;

void Android_NativeSpecial(int k);

void Android_NativeJoy(int k, int s);

void Android_KeyEvent(int k, int s);

void Android_SingleKeyPress(int k);

void Input_Initialize(void);

void Keyboard_Enqueue(int key);

int Keyboard_Dequeue(void);
