// ============================================================================
// TFT_eSPI setup file for the BCI Lab 3.5" TFT LCD shield (480x320,
// 8-bit parallel ILI9486/ILI9488) wired per the lab's wiring diagram.
//
// HOW TO USE (pick ONE of these two ways):
//
//  A) Simplest: open your TFT_eSPI library folder and REPLACE the contents
//     of "User_Setup.h" with the contents of this file.
//     Library folder is normally at:
//       Windows : Documents\Arduino\libraries\TFT_eSPI\User_Setup.h
//       macOS   : ~/Documents/Arduino/libraries/TFT_eSPI/User_Setup.h
//       Linux   : ~/Arduino/libraries/TFT_eSPI/User_Setup.h
//
//  B) Cleaner (keeps the original User_Setup.h untouched): copy this file
//     INTO the TFT_eSPI library folder as-is, then open
//     "User_Setup_Select.h" in that same folder, comment out the line
//         #include <User_Setup.h>
//     and add:
//         #include <User_Setup_BCI_ILI9488.h>
//
// After editing either file, close and reopen the Arduino IDE so it
// re-reads the library.
// ============================================================================

// ---- Driver chip -----------------------------------------------------------
// This shield ships with either an ILI9486 or an ILI9488 panel - they look
// identical and use the same pinout. Start with ILI9486_DRIVER. If colours
// are wrong (inverted / swapped red-blue) or the image is scrambled, comment
// this out and uncomment ILI9488_DRIVER instead, then re-upload.
#define ILI9486_DRIVER
// #define ILI9488_DRIVER

// ---- Interface: 8-bit parallel (this shield is NOT real SPI, despite the
//      silkscreen/label on some of these boards) --------------------------
#define TFT_PARALLEL_8_BIT

// ---- Control pins (matches the lab's wiring diagram) -----------------------
#define TFT_CS   5    // LCD_CS
#define TFT_DC   2    // LCD_RS  (a.k.a. RS / D-C)
#define TFT_RST  4    // LCD_RST
#define TFT_WR   15   // LCD_WR
#define TFT_RD   13   // LCD_RD

// ---- 8-bit data bus ----------------------------------------------------------
#define TFT_D0   18   // LCD_D0
#define TFT_D1   19   // LCD_D1
#define TFT_D2   23   // LCD_D2
#define TFT_D3   22   // LCD_D3
#define TFT_D4   21   // LCD_D4
#define TFT_D5   14   // LCD_D5
#define TFT_D6   27   // LCD_D6
#define TFT_D7   26   // LCD_D7

// Note: TFT_DC = GPIO2 and TFT_CS = GPIO5 are ESP32 boot-strapping pins.
// The shield pulls them through the panel so boot is normally fine, but if
// the board ever fails to boot/flash with the shield attached, unplug the
// shield while flashing, then plug it back in to run.

// ---- Fonts (keep these - the test sketch and the main firmware use them) --
#define LOAD_GLCD
#define LOAD_FONT2
#define LOAD_FONT4
#define LOAD_FONT6
#define LOAD_FONT7
#define LOAD_FONT8
#define LOAD_GFXFF
#define SMOOTH_FONT

// SPI_FREQUENCY is unused in parallel mode but TFT_eSPI expects it defined.
#define SPI_FREQUENCY 20000000
