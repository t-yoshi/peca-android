CXX = g++
LD = g++

CPPFLAGS = $(XCPPFLAGS) -DENABLE_BINRELOC -D_UNIX -D__cplusplus  -D_REENTRANT $(INCLUDES) 
LDFLAGS = -lpthread 
#

ifeq (DEBUG, 1)
	CPPFLAGS += -g
	LDFLAGS += -g 
endif


INCLUDES = -Icore -Icore/common 

TARGET = peercast
CORE = core
SRCHTML = ui/html
HTML = html

CORESOURCE = $(CORE)/unix/usys.cpp \
	 $(CORE)/unix/usocket.cpp \
	 $(CORE)/common/socket.cpp \
	 $(CORE)/common/servent.cpp \
	 $(CORE)/common/servhs.cpp \
	 $(CORE)/common/servmgr.cpp \
	 $(CORE)/common/xml.cpp \
	 $(CORE)/common/stream.cpp \
	 $(CORE)/common/sys.cpp \
	 $(CORE)/common/gnutella.cpp \
	 $(CORE)/common/html.cpp \
	 $(CORE)/common/channel.cpp \
	 $(CORE)/common/http.cpp \
	 $(CORE)/common/inifile.cpp \
	 $(CORE)/common/peercast.cpp \
	 $(CORE)/common/stats.cpp \
	 $(CORE)/common/mms.cpp \
	 $(CORE)/common/mp3.cpp \
	 $(CORE)/common/nsv.cpp \
	 $(CORE)/common/ogg.cpp \
	 $(CORE)/common/url.cpp \
	 $(CORE)/common/icy.cpp \
	 $(CORE)/common/pcp.cpp \
	 $(CORE)/common/jis.cpp 



COREOBJ = $(CORESOURCE:.cpp=.o)


LINUXSOURCE = ui/linux/main.cpp 
LINUXOBJ = $(LINUXSOURCE:.cpp=.o)

# Here follow the generic build rules.
all:  $(TARGET)
	

clean:
	rm  -f $(LINUXOBJ) $(COREOBJ) $(TARGET) ${LIBOBJSTATIC} ${LIBOBJSHARED} #$(COREOBJSTATIC)

%.o: %.cpp
	$(CXX) $(INCLUDES) $(CPPFLAGS) -fPIC -c $< -o $@


$(TARGET): $(LINUXOBJ) $(COREOBJ)
	$(CXX) $(LDFLAGS) -o $@ $(LINUXOBJ) $(COREOBJ) $(LIBS)
	

