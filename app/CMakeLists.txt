# Sets the minimum version of CMake required to build the native
# library. You should either keep the default value or only pass a
# value of 3.4.0 or lower.

cmake_minimum_required(VERSION 3.4.1)

# Creates and names a library, sets it as either STATIC
# or SHARED, and provides the relative paths to its source code.
# You can define multiple libraries, and CMake builds it for you.
# Gradle automatically packages shared libraries with your APK.

add_library( # Sets the name of the library.
             atari800

             # Sets the library as a shared library.
             SHARED

             # Provides a relative path to your source file(s).
             # Associated headers in the same location as their source
             # file are automatically included.
             src/main/cpp/afile.c
             src/main/cpp/antic.c
             src/main/cpp/atari.c
             src/main/cpp/binload.c
             src/main/cpp/cartridge.c
             src/main/cpp/cassette.c
             src/main/cpp/compfile.c
             src/main/cpp/cfg.c
             src/main/cpp/cpu.c
             src/main/cpp/crc32.c
             src/main/cpp/devices.c
             src/main/cpp/emuos.c
             src/main/cpp/esc.c
             src/main/cpp/gtia.c
             src/main/cpp/input.c
             src/main/cpp/img_tape.c
             src/main/cpp/log.c
             src/main/cpp/memory.c
             src/main/cpp/monitor.c
             src/main/cpp/pbi.c
             src/main/cpp/pia.c
             src/main/cpp/pokey.c
             src/main/cpp/rtime.c
             src/main/cpp/sio.c
             src/main/cpp/sysrom.c
             src/main/cpp/util.c
             src/main/cpp/statesav.c
             src/main/cpp/ui_basic.c
             src/main/cpp/ui.c
             src/main/cpp/artifact.c
             src/main/cpp/colours.c
             src/main/cpp/colours_ntsc.c
             src/main/cpp/colours_pal.c
             src/main/cpp/colours_external.c
             src/main/cpp/screen.c
             src/main/cpp/cycle_map.c
             src/main/cpp/pbi_mio.c
             src/main/cpp/pbi_bb.c
             src/main/cpp/pbi_scsi.c
             src/main/cpp/pokeysnd.c
             src/main/cpp/mzpokeysnd.c
             src/main/cpp/remez.c
             src/main/cpp/sndsave.c
             src/main/cpp/pbi_xld.c
             src/main/cpp/voicebox.c
             src/main/cpp/votrax.c
             src/main/cpp/votraxsnd.c
             src/main/cpp/jni/platform.c
             src/main/cpp/jni/sound.c
             src/main/cpp/jni/graphics.c
             src/main/cpp/jni/jni.c
             src/main/cpp/jni/androidinput.c
             )

# Searches for a specified prebuilt library and stores the path as a
# variable. Because system libraries are included in the search path by
# default, you only need to specify the name of the public NDK library
# you want to add. CMake verifies that the library exists before
# completing its build.

find_library( # Sets the name of the path variable.
              log-lib

              # Specifies the name of the NDK library that
              # you want CMake to locate.
              log )

find_library(ZLIB REQUIRED)
find_package( ZLIB REQUIRED )
include_directories( ${ZLIB_INCLUDE_DIR} )

add_library(libpng STATIC IMPORTED)
set_target_properties(libpng PROPERTIES IMPORTED_LOCATION D:/workspace/android/atari65/app/prebuilt_libs/libpng/obj/local/${ANDROID_ABI}/libpng.a)
set_target_properties(libpng PROPERTIES INTERFACE_INCLUDE_DIRECTORIES D:/workspace/android/atari65/app/prebuilt_libs/libpng/jni/)

find_library( gles-lib GLESv1_CM )

# Specifies libraries CMake should link to your target library. You
# can link multiple libraries, such as libraries you define in the
# build script, prebuilt third-party libraries, or system libraries.

target_link_libraries( # Specifies the target library.
                       atari800
                       android
                       libpng
                       ${ZLIB_LIBRARY_RELEASE}
                       ${gles-lib}

                       # Links the target library to the log library
                       # included in the NDK.
                       ${log-lib} )
