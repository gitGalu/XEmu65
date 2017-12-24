CC = /d/android/ndkstandalone/android-ndk-r15c/toolchains/arm-linux-androideabi-4.9/prebuilt/windows-x86_64/bin/arm-linux-androideabi-gcc
RC = windres

DEFS = -DHAVE_CONFIG_H
LIBS = -lz 
TARGET_BASE_NAME = atari800
TARGET = $(TARGET_BASE_NAME)
CONFIGURE_TARGET = android
CONFIGURE_HOST = win
ifeq (win, javanvm)
	FINALTARGET = $(TARGET_BASE_NAME).jar
	JAVAFLAGS = 
	JAVA = java
	JAVACFLAGS = 
	JAVAC = javac
else
	ifeq (android,android)
		FINALTARGET = android
	else
		FINALTARGET = $(TARGET)
	endif
endif

#ASFLAGS = @ASFLAGS@
CFLAGS = -O2 -Wall  -Id:/android/ndkstandalone/android-ndk-r15c/platforms/android-9/arch-arm/usr/include/
LDFLAGS = 

INSTALL = /bin/install -c
INSTALL_PROGRAM = ${INSTALL} -s
INSTALL_DATA = ${INSTALL} -m 644

BIN_DIR = /usr/local/bin
MAN_DIR = /usr/local/share/man/man1
DOC_DIR = /usr/local/share/doc/atari800

DESTDIR =

OBJS = \
	afile.o \
	antic.o \
	atari.o \
	binload.o \
	cartridge.o \
	cassette.o \
	compfile.o \
	cfg.o \
	cpu.o \
	crc32.o \
	devices.o \
	esc.o \
	gtia.o \
	img_tape.o \
	log.o \
	memory.o \
	monitor.o \
	pbi.o \
	pia.o \
	pokey.o \
	rtime.o \
	sio.o \
	sysrom.o \
	util.o \
	 input.o statesav.o ui_basic.o ui.o artifact.o colours.o colours_ntsc.o colours_pal.o colours_external.o screen.o cycle_map.o emuos_altirra.o pbi_mio.o pbi_bb.o pbi_scsi.o pokeysnd.o mzpokeysnd.o remez.o sndsave.o pbi_xld.o voicebox.o votrax.o votraxsnd.o



all: $(FINALTARGET)

# A special rule for SDL_win32_main.c to suppress warnings since this file is
# from SDL and should not have to be modified
SDL_win32_main.o: SDL_win32_main.c
	$(CC) -c -o $@ $(DEFS) -I. $(CFLAGS) -Wno-missing-declarations -Wno-missing-prototypes $<

# A special rule for win32 to not compile with -ansi -pedantic
win32/%.o: win32/%.c
	$(CC) -c -o $@ $(DEFS) -I. $(patsubst -pedantic,,$(patsubst -ansi,,$(CFLAGS))) $<

ide.o: ide.c ide.h ide_internal.h
	$(CC) -c -o $@ $(DEFS) -I. $(CFLAGS:-ansi=) $<

%.o: %.c
	$(CC) -c -o $@ $(DEFS) -I. $(CFLAGS) $<

%.ro: %.rc
	$(RC) --define WIN32 --define __MINGW32__ --include-dir . $< $@

%.o: %.cpp
	$(CC) -c -o $@ $(DEFS) -I. $(CFLAGS) $<

%.o: %.asm
	vasm -quiet -devpac -opt-allbra -Faout $(ASFLAGS) -o $@ $<

ifeq ($(CONFIGURE_HOST),javanvm)
$(TARGET_BASE_NAME).class: javanvm/$(TARGET_BASE_NAME).java | $(TARGET_BASE_NAME)_runtime.class
	$(JAVAC) -d . $(JAVACFLAGS) javanvm/$(TARGET_BASE_NAME).java
RUNTIME = _runtime
else
RUNTIME =
endif

$(TARGET_BASE_NAME)$(RUNTIME).class: $(TARGET)
#To compile using java bytecode directly:
#	$(JAVA) $(JAVAFLAGS) org.ibex.nestedvm.Compiler -o UnixRuntime -outfile $@ $(TARGET_BASE_NAME)$(RUNTIME) $(TARGET)
	$(JAVA) $(JAVAFLAGS) org.ibex.nestedvm.Compiler -o UnixRuntime -outformat javasource -outfile $(TARGET_BASE_NAME)$(RUNTIME).java $(TARGET_BASE_NAME)$(RUNTIME) $(TARGET)
	$(JAVAC) -d . $(JAVACFLAGS) -J-Xmx256m $(TARGET_BASE_NAME)$(RUNTIME).java
##Also, -o UnixRuntime fixes directory browsing but requires /c:/-style paths

$(TARGET_BASE_NAME).jar: $(TARGET_BASE_NAME).class $(TARGET_BASE_NAME)$(RUNTIME).class
	printf "Manifest-Version: 1.0\nMain-Class: $(TARGET_BASE_NAME)\nClass-Path: unix_runtime.jar\n" > .manifest
	jar cfm $(TARGET_BASE_NAME).jar .manifest *.class

# Allow parallel execution in sub-make with '+'
android:
	+ ndk-build -C android
	ant -f android/build.xml debug
.PHONY: android

$(TARGET): $(OBJS)
	$(CC) -o $@ $(LDFLAGS) $(OBJS) $(LIBS)

dep:
	@if ! makedepend -Y $(DEFS) -I. ${OBJS:.o=.c} 2>/dev/null; \
	then echo warning: makedepend failed; fi

clean:
	rm -f *.o *.class .manifest $(TARGET) $(TARGET_BASE_NAME).jar $(TARGET_BASE_NAME)_runtime.java core *.bak *~
	rm -f dos/*.o dos/*.bak dos/*~
	rm -f falcon/*.o falcon/*.bak falcon/*~
	rm -f sdl/*.o sdl/*.bak sdl/*~
	rm -f win32/*.o win32/*.ro win32/*.bak win32/*~
	rm -f javanvm/*.o javanvm/*.bak javanvm/*~
	rm -f atari_ntsc/*.o atari_ntsc/*.bak atari_ntsc/*~
	rm -rf android/libs android/obj android/bin android/gen


distclean: clean
	-rm -f Makefile configure config.log config.status config.h android/jni/Android.mk
	-rm -rf autom4te.cache

install: $(TARGET) installdirs
	$(INSTALL_PROGRAM) $(TARGET) ${DESTDIR}${BIN_DIR}/$(TARGET)
	$(INSTALL_DATA) atari800.man ${DESTDIR}${MAN_DIR}/atari800.1
# install also the documentation
	$(INSTALL_DATA) ../COPYING     ${DESTDIR}${DOC_DIR}/COPYING
	$(INSTALL_DATA) ../README.1ST  ${DESTDIR}${DOC_DIR}/README.1ST
	$(INSTALL_DATA) ../DOC/README  ${DESTDIR}${DOC_DIR}/README
	$(INSTALL_DATA) ../DOC/INSTALL ${DESTDIR}${DOC_DIR}/INSTALL
	$(INSTALL_DATA) ../DOC/USAGE   ${DESTDIR}${DOC_DIR}/USAGE
	$(INSTALL_DATA) ../DOC/NEWS    ${DESTDIR}${DOC_DIR}/NEWS

readme.html: $(TARGET)
	./$(TARGET) -help </dev/null | ../util/usage2html.pl \
		../DOC/readme.html.in ../DOC/USAGE ./atari.h > $@

doc: readme.html

installdirs:
	mkdir -p $(DESTDIR)$(BIN_DIR) $(DESTDIR)$(MAN_DIR) $(DESTDIR)$(DOC_DIR)
