NOZZLE_DIR := deps/nozzle
PLOG_DIR := $(NOZZLE_DIR)/libs/plog/include
BUILD_DIR := .build

CXX := c++
AR := ar

CXXFLAGS := -std=c++17 -fno-exceptions -fno-rtti -O2 -fPIC

UNAME_S := $(shell uname -s)
ifeq ($(OS),Windows_NT)
	PLATFORM := windows
else ifeq ($(UNAME_S),Darwin)
	PLATFORM := macos
else
	PLATFORM := linux
endif

include $(NOZZLE_DIR)/sources/sources.mk

COMMON_SRCS := $(NOZZLE_COMMON_SRCS) $(NOZZLE_OPENGL_SRCS)

JNI_SRCS := src/jni/nozzle_jni.c

ifeq ($(PLATFORM),macos)
	CXXFLAGS += -DNOZZLE_PLATFORM_MACOS=1 -DNOZZLE_HAS_METAL=1 -DNOZZLE_HAS_OPENGL=1
	PLATFORM_SRCS := $(NOZZLE_MACOS_SRCS)
	JNI_LDFLAGS := -shared -framework Metal -framework IOSurface -framework Foundation -framework Accelerate -framework OpenGL -lobjc -lstdc++
	JNI_INCLUDES := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/darwin
	JNI_LIB := $(BUILD_DIR)/libnozzle_jni.jnilib
endif

ifeq ($(PLATFORM),linux)
	CXXFLAGS += -DNOZZLE_PLATFORM_LINUX=1 -DNOZZLE_HAS_DMA_BUF=1 -DNOZZLE_HAS_OPENGL=1
	PLATFORM_SRCS := $(NOZZLE_LINUX_SRCS)
	JNI_LDFLAGS := -shared -ldrm -lgbm -lEGL -lGL -lstdc++
	JNI_INCLUDES := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux
	JNI_LIB := $(BUILD_DIR)/libnozzle_jni.so
endif

ifeq ($(PLATFORM),windows)
	CXXFLAGS := -std=c++17 -fno-exceptions -fno-rtti -O2 -fPIC -DNDEBUG -DNOZZLE_PLATFORM_WINDOWS=1 -DNOZZLE_HAS_D3D11=1 -DNOZZLE_HAS_OPENGL=1
	PLATFORM_SRCS := $(NOZZLE_WINDOWS_SRCS)
	JNI_LDFLAGS := -shared -ld3d11 -ldxgi -lopengl32 -lbcrypt -lstdc++
	JNI_INCLUDES := -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/win32
	JNI_LIB := $(BUILD_DIR)/nozzle_jni.dll
endif

INCLUDES := -I$(NOZZLE_DIR)/include -I$(NOZZLE_DIR)/src -I$(PLOG_DIR)
ifeq ($(PLATFORM),linux)
	INCLUDES += -I/usr/include/libdrm
endif

ALL_SRCS := $(COMMON_SRCS) $(PLATFORM_SRCS)
ALL_OBJS := $(patsubst %.cpp,$(BUILD_DIR)/%.o,$(patsubst %.mm,$(BUILD_DIR)/%.o,$(ALL_SRCS)))
LIB := $(BUILD_DIR)/libnozzle.a

JNI_OBJS := $(patsubst %.c,$(BUILD_DIR)/%.o,$(JNI_SRCS))

.PHONY: all clean

all: $(JNI_LIB)

$(JNI_LIB): $(LIB) $(JNI_OBJS)
	@mkdir -p $(dir $@)
	$(CXX) -o $@ $(JNI_OBJS) $(LIB) $(JNI_LDFLAGS)

$(LIB): $(ALL_OBJS)
	@mkdir -p $(dir $@)
	$(AR) rcs $@ $^

$(BUILD_DIR)/%.o: %.cpp
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) $(INCLUDES) -c $< -o $@

$(BUILD_DIR)/%.o: %.mm
	@mkdir -p $(dir $@)
	$(CXX) $(CXXFLAGS) $(INCLUDES) -c $< -o $@

$(BUILD_DIR)/%.o: %.c
	@mkdir -p $(dir $@)
	$(CC) $(JNI_INCLUDES) $(INCLUDES) -c $< -o $@

clean:
	rm -rf $(BUILD_DIR)
