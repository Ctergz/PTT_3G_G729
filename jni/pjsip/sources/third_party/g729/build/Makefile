TARGET      = ../lib/libg729.a
OBJPATH     = .
SRCPATH     = ../src/
SOURCES	    = $(wildcard $(SRCPATH)*.c)
OBJS        = $(patsubst %.cpp,%.o,$(patsubst %.c,%.o,$(SOURCES)))
FULLOBJS    = $(patsubst %.cpp,$(OBJPATH)/%.o,$(patsubst %.c,$(OBJPATH)/%.o,$(SOURCES)))
INCPATH	    = -I../include 
LIBPATH	    = -L ../lib -lncurses -lpthread 
LINKFLAGS   = rcs
COMPFLAGS   = -w -c -DDEBUG
CXX         = gcc

$(TARGET):$(OBJS)
	ar $(LINKFLAGS)  $(TARGET) $(FULLOBJS)
	ranlib $(TARGET)
$(OBJS):$(SOURCES)
	$(CXX) $(COMPFLAGS) $*.c* -o $(OBJPATH)/$@ $(INCPATH)

clean:
	rm -f $(OBJPATH)/*.o $(OBJS)
	rm -f $(TARGET)

